package com.studionails.beauty.wallet

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.studionails.beauty.database.DeskDatabase
import com.studionails.beauty.model.BookState
import com.studionails.beauty.model.Role
import com.studionails.beauty.model.WalletMove
import com.studionails.beauty.model.WalletTicket
import com.studionails.beauty.model.balanceOf
import com.studionails.beauty.model.phoneKey
import com.studionails.beauty.model.samePhone
import com.studionails.beauty.qr.TicketClaim
import java.security.SecureRandom
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

data class Redeemed(val state: BookState, val balance: Int, val move: WalletMove)

data class DeskQuote(val cents: Int, val kind: String, val expiresAt: Long, val name: String)

/**
 * Única puerta que puede crear un ticket o mover un saldo.
 * Cada cobro o recarga entra en una transacción de SQLite.
 * El mismo requestId no se aplica dos veces.
 */
class DeskLedger(context: Context) {
    private val db = DeskDatabase(context)
    private val lock = ReentrantLock()
    private val random = SecureRandom()
    var snapshot: BookState = BookState()
        private set

    fun attach(state: BookState): BookState = lock.withLock {
        val writable = db.writableDatabase
        if (state.mode == Role.manicurist && !db.isDesk(writable)) {
            writable.beginTransaction()
            try {
                adopt(writable, state)
                writable.setTransactionSuccessful()
            } finally {
                writable.endTransaction()
            }
        }
        publish(state, writable)
    }

    fun note(state: BookState): BookState = lock.withLock {
        publish(state, db.writableDatabase)
    }

    fun contains(id: String): Boolean = lock.withLock {
        db.isDesk() && db.ticket(db.readableDatabase, id) != null
    }

    fun isDesk(): Boolean = lock.withLock { db.isDesk() }

    fun issue(state: BookState, kind: String, cents: Int, phone: String, name: String, now: Long): Result<Pair<BookState, WalletTicket>> = lock.withLock {
        if (state.mode != Role.manicurist) return Result.failure(IllegalArgumentException("Solo la manicura crea códigos."))
        if (cents <= 0) return Result.failure(IllegalArgumentException("Escribe un valor en dólares."))
        if (kind != "topup" && kind != "charge") return Result.failure(IllegalArgumentException("Ese movimiento no existe."))
        val cleanPhone = phoneKey(phone)
        if (phone.isNotBlank() && (cleanPhone.length < 10 || cleanPhone.length > 15)) {
            return Result.failure(IllegalArgumentException("El teléfono de la clienta no es válido."))
        }
        val writable = db.writableDatabase
        writable.beginTransaction()
        try {
            if (!db.imported(writable)) adopt(writable, state)
            db.markDesk(writable)
            val used = db.tickets(writable).map { it.id }.toMutableSet()
            var id: String
            do {
                id = buildString(6) { repeat(6) { append(Alphabet[random.nextInt(Alphabet.length)]) } }
            } while (!used.add(id))
            val ticket = WalletTicket(
                id = id,
                kind = kind,
                cents = cents,
                phone = cleanPhone,
                name = name.trim(),
                createdAt = now,
                expiresAt = now + HalfHour,
                nonce = nonce(),
            )
            db.insertTicket(writable, ticket)
            writable.setTransactionSuccessful()
            val next = publish(state, writable)
            return Result.success(next to ticket)
        } catch (error: Exception) {
            return Result.failure(IllegalArgumentException(error.message ?: "No se pudo crear el código."))
        } finally {
            writable.endTransaction()
        }
    }

    fun quote(claim: TicketClaim, now: Long): Result<DeskQuote> = lock.withLock {
        val ticket = db.ticket(db.readableDatabase, claim.id)
            ?: return Result.failure(IllegalArgumentException("Ese código no está en este teléfono."))
        checkTicket(ticket, claim, now).onFailure { return Result.failure(it) }
        return Result.success(DeskQuote(ticket.cents, ticket.kind, expiresOf(ticket), ticket.name))
    }

    fun redeem(claim: TicketClaim, phone: String, name: String, requestId: String, now: Long): Result<Redeemed> = lock.withLock {
        if (requestId.isBlank()) return Result.failure(IllegalArgumentException("Falta el identificador de la operación."))
        val writable = db.writableDatabase
        writable.beginTransaction()
        try {
            val prior = db.moveByRequest(writable, requestId)
            if (prior != null) {
                writable.setTransactionSuccessful()
                val next = publish(snapshot, writable)
                return Result.success(Redeemed(next, balanceOf(next, prior.phone), prior))
            }
            val ticket = db.ticket(writable, claim.id)
                ?: return Result.failure(IllegalArgumentException("Ese código no está en este teléfono."))
            checkTicket(ticket, claim, now).onFailure { return Result.failure(it) }
            val mine = phoneKey(phone)
            if (mine.length < 10 || mine.length > 15) {
                return Result.failure(IllegalArgumentException("Hace falta un teléfono de 10 dígitos."))
            }
            if (ticket.phone.isNotEmpty() && !samePhone(ticket.phone, mine)) {
                return Result.failure(IllegalArgumentException("Este código es para otra clienta."))
            }
            if (ticket.cents <= 0) return Result.failure(IllegalArgumentException("Ese código no tiene un valor válido."))
            val current = balanceOf(publish(snapshot, writable), mine)
            if (ticket.kind == "charge" && current < ticket.cents) {
                return Result.failure(IllegalArgumentException("No alcanza el saldo."))
            }
            val move = WalletMove(
                id = "m" + now.toString(36) + nonce().take(4),
                phone = mine,
                name = name.trim().ifBlank { "Clienta" },
                kind = ticket.kind,
                cents = ticket.cents,
                at = now,
                ticketId = ticket.id,
                requestId = requestId,
            )
            db.insertMove(writable, move)
            db.markUsed(writable, ticket.id, now, mine, move.name)
            writable.setTransactionSuccessful()
            val next = publish(snapshot, writable)
            return Result.success(Redeemed(next, balanceOf(next, mine), move))
        } catch (error: Exception) {
            return Result.failure(IllegalArgumentException(error.message ?: "No se pudo registrar el movimiento."))
        } finally {
            writable.endTransaction()
        }
    }

    private fun adopt(writable: SQLiteDatabase, state: BookState) {
        if (db.imported(writable)) return
        state.tickets.forEach { ticket ->
            if (db.ticket(writable, ticket.id) == null) db.insertTicket(writable, ticket.copy(nonce = ticket.nonce.ifBlank { nonce() }, expiresAt = ticket.expiresAt.takeIf { it > 0 } ?: (ticket.createdAt + HalfHour)))
        }
        state.moves.forEach { move ->
            if (move.requestId.isBlank() || db.moveByRequest(writable, move.requestId) == null) {
                try {
                    db.insertMove(writable, move)
                } catch (_: Exception) {
                }
            }
        }
        db.markImported(writable)
        db.markDesk(writable)
    }

    private fun publish(state: BookState, writable: SQLiteDatabase): BookState {
        val desk = state.mode == Role.manicurist && db.isDesk(writable)
        val next = if (desk) state.copy(tickets = db.tickets(writable), moves = db.moves(writable)) else state
        if (desk) snapshot = next
        return next
    }

    private fun checkTicket(ticket: WalletTicket, claim: TicketClaim, now: Long): Result<Unit> {
        if (claim.nonce.isNotEmpty() && ticket.nonce.isNotEmpty() && ticket.nonce != claim.nonce) {
            return Result.failure(IllegalArgumentException("Ese código no coincide. Vuelve a escanear."))
        }
        if (claim.kind.isNotEmpty() && ticket.kind != claim.kind) {
            val message = if (claim.kind == "topup") "Ese código es un cobro. Ábrelo en Pagar." else "Ese código es una recarga. Ábrelo en Recargar."
            return Result.failure(IllegalArgumentException(message))
        }
        if (claim.cents > 0 && claim.cents != ticket.cents) {
            return Result.failure(IllegalArgumentException("El valor no coincide con el código. Vuelve a escanear."))
        }
        if (now > expiresOf(ticket)) return Result.failure(IllegalArgumentException("Ese código caducó. Pide uno nuevo."))
        if (ticket.usedAt != null) return Result.failure(IllegalArgumentException("Ese código ya se usó."))
        return Result.success(Unit)
    }

    private fun expiresOf(ticket: WalletTicket): Long {
        return if (ticket.expiresAt > 0) ticket.expiresAt else ticket.createdAt + HalfHour
    }

    private fun nonce(): String = buildString(16) { repeat(16) { append(Alphabet[random.nextInt(Alphabet.length)]) } }

    private companion object {
        const val HalfHour = 30 * 60 * 1000L
        const val Alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}
