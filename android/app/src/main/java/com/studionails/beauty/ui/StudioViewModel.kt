package com.studionails.beauty.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.studionails.beauty.data.BookStore
import com.studionails.beauty.model.BookState
import com.studionails.beauty.model.HeldRedeem
import com.studionails.beauty.model.Role
import com.studionails.beauty.model.WalletTicket
import com.studionails.beauty.model.phoneKey
import com.studionails.beauty.network.DeskClient
import com.studionails.beauty.notify.Reminders
import com.studionails.beauty.qr.TicketClaim
import com.studionails.beauty.server.DeskServer
import com.studionails.beauty.wallet.DeskLedger
import com.studionails.beauty.wallet.DeskQuote
import kotlin.concurrent.thread

sealed interface Screen {
    data object Gate : Screen
    data object Home : Screen
    data class Citas(val serviceId: String? = null) : Screen
    data object Wallet : Screen
    data object History : Screen
    data object Services : Screen
    data class Detail(val id: String, val back: String) : Screen
    data object Settings : Screen
    data object Hours : Screen
    data class Amount(val kind: String, val clientId: String? = null) : Screen
    data class Qr(val ticketId: String) : Screen
    data class Scan(val kind: String) : Screen
    data class Client(val id: String) : Screen
    data object AddClient : Screen
}

class StudioViewModel(app: Application) : AndroidViewModel(app) {
    private val store = BookStore(app)
    private val ledger = DeskLedger(app)
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var deskOpen = false
    private val server = DeskServer(ledger, { next -> main.post { persist(next) } }, { deskOpen })
    var book by mutableStateOf(ledger.attach(store.load()))
        private set
    var screen by mutableStateOf<Screen>(if (book.mode == null) Screen.Gate else Screen.Home)
        private set

    init {
        deskOpen = book.mode == Role.manicurist
        Reminders.sync(app, book)
        thread(name = "desk-start", isDaemon = true) { server.start() }
        retryHeld()
    }

    fun isDesk() = book.mode == Role.manicurist && ledger.isDesk()

    fun deskUrl() = server.url()

    fun open(next: Screen) {
        screen = next
    }

    fun commit(next: BookState) {
        persist(ledger.note(next))
    }

    fun choose(role: Role) {
        val next = book.copy(mode = role)
        commit(if (role == Role.manicurist) ledger.attach(next) else next)
        screen = Screen.Home
    }

    fun issueTicket(kind: String, cents: Int, phone: String, name: String): Result<WalletTicket> {
        val result = ledger.issue(book, kind, cents, phone, name, System.currentTimeMillis())
        result.onSuccess { (next, _) -> persist(next) }
        return result.map { it.second }
    }

    fun redeem(claim: TicketClaim, phone: String, name: String, requestId: String): Result<Pair<BookState, Int>> {
        val now = System.currentTimeMillis()
        if (book.mode == Role.manicurist && ledger.isDesk()) {
            return ledger.redeem(claim, phone, name, requestId, now).map { redeemed ->
                remembered(redeemed.state, phone, name, redeemed.balance, claim.host).copy(heldRedeem = null) to redeemed.balance
            }
        }
        val host = claim.host.ifBlank { book.deskHost }
        if (!host.startsWith("http")) {
            return Result.failure(IllegalArgumentException("No llego al teléfono de la manicura. Conéctense al mismo Wi-Fi y genera el código otra vez."))
        }
        return DeskClient.redeem(host, claim, phone, name, requestId).map { receipt ->
            val key = phoneKey(phone)
            val others = book.moves.filter { phoneKey(it.phone) != key }
            val next = book.copy(moves = receipt.moves + others, heldRedeem = null)
            remembered(next, phone, name, receipt.balance, host) to receipt.balance
        }
    }

    fun quote(claim: TicketClaim): Result<DeskQuote> {
        if (book.mode == Role.manicurist && ledger.isDesk()) {
            return ledger.quote(claim, System.currentTimeMillis())
        }
        val host = claim.host.ifBlank { book.deskHost }
        if (!host.startsWith("http")) {
            return Result.failure(IllegalArgumentException("No llego al teléfono de la manicura. Conéctense al mismo Wi-Fi y genera el código otra vez."))
        }
        return DeskClient.quote(host, claim)
    }

    fun retryHeld() {
        val held = book.heldRedeem ?: return
        if (book.mode != Role.client || !held.host.startsWith("http")) return
        val current = book
        thread(name = "desk-retry", isDaemon = true) {
            val claim = TicketClaim(held.id, held.kind, held.cents, held.expiresAt, held.nonce, held.host)
            val result = DeskClient.redeem(held.host, claim, held.phone, held.name, held.requestId)
            main.post {
                if (book.heldRedeem?.requestId != held.requestId) return@post
                result.onSuccess { receipt ->
                    val key = phoneKey(held.phone)
                    val others = book.moves.filter { phoneKey(it.phone) != key }
                    commit(remembered(book.copy(moves = receipt.moves + others, heldRedeem = null), held.phone, held.name, receipt.balance, held.host))
                }.onFailure { error ->
                    if (!isOffline(error) && book.heldRedeem?.requestId == current.heldRedeem?.requestId) {
                        commit(book.copy(heldRedeem = null))
                    }
                }
            }
        }
    }

    fun refreshBalance() {
        val host = book.deskHost
        val phone = book.clientPhone
        if (isDesk() || !host.startsWith("http") || phoneKey(phone).length < 10) return
        val current = book
        thread(name = "desk-balance", isDaemon = true) {
            val result = DeskClient.balance(host, phone)
            main.post {
                result.onSuccess { (balance, moves) ->
                    val key = phoneKey(phone)
                    val others = book.moves.filter { phoneKey(it.phone) != key }
                    if (book.deskHost == current.deskHost) commit(book.copy(moves = moves + others, confirmedBalance = balance))
                }
            }
        }
    }

    override fun onCleared() {
        server.stop()
    }

    private fun remembered(state: BookState, phone: String, name: String, balance: Int, host: String): BookState {
        val key = phoneKey(phone)
        val asClient = state.mode == Role.client
        return state.copy(
            confirmedBalance = balance,
            deskHost = host.ifBlank { state.deskHost },
            clientPhone = if (asClient && key.length >= 10) key else state.clientPhone,
            clientName = if (asClient && name.isNotBlank()) name.trim() else state.clientName,
        )
    }

    private fun isOffline(error: Throwable) = error.message?.startsWith("No llego") == true

    private fun persist(next: BookState) {
        deskOpen = next.mode == Role.manicurist
        val previous = book
        book = next
        store.save(next)
        val context = getApplication<Application>()
        Reminders.sync(context, next)
        Reminders.flashNew(context, previous, next)
    }
}
