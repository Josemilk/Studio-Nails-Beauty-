package com.studionails.beauty.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.studionails.beauty.model.WalletMove
import com.studionails.beauty.model.WalletTicket

/**
 * Libro de caja del teléfono de la manicura.
 * Los movimientos solo se insertan. No hay actualización ni borrado.
 */
class DeskDatabase(context: Context) : SQLiteOpenHelper(context.applicationContext, "desk.db", null, 1) {
    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE meta (k TEXT PRIMARY KEY, v TEXT NOT NULL)")
        db.execSQL(
            """
            CREATE TABLE tickets (
              id TEXT PRIMARY KEY,
              kind TEXT NOT NULL,
              cents INTEGER NOT NULL,
              phone TEXT NOT NULL,
              name TEXT NOT NULL,
              created_at INTEGER NOT NULL,
              expires_at INTEGER NOT NULL,
              nonce TEXT NOT NULL,
              used_at INTEGER,
              used_by TEXT,
              used_name TEXT
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE moves (
              id TEXT PRIMARY KEY,
              phone TEXT NOT NULL,
              name TEXT NOT NULL,
              kind TEXT NOT NULL,
              cents INTEGER NOT NULL,
              at INTEGER NOT NULL,
              ticket_id TEXT NOT NULL,
              request_id TEXT
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX moves_request ON moves(request_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun isDesk(db: SQLiteDatabase = readableDatabase): Boolean = meta(db, "desk") == "1"

    fun markDesk(db: SQLiteDatabase) {
        putMeta(db, "desk", "1")
    }

    fun imported(db: SQLiteDatabase): Boolean = meta(db, "imported") == "1"

    fun markImported(db: SQLiteDatabase) {
        putMeta(db, "imported", "1")
    }

    fun tickets(db: SQLiteDatabase = readableDatabase): List<WalletTicket> {
        val out = mutableListOf<WalletTicket>()
        db.query("tickets", null, null, null, null, null, "created_at DESC").use { cursor ->
            while (cursor.moveToNext()) out += cursor.toTicket()
        }
        return out
    }

    fun moves(db: SQLiteDatabase = readableDatabase): List<WalletMove> {
        val out = mutableListOf<WalletMove>()
        db.query("moves", null, null, null, null, null, "at DESC").use { cursor ->
            while (cursor.moveToNext()) out += cursor.toMove()
        }
        return out
    }

    fun ticket(db: SQLiteDatabase, id: String): WalletTicket? {
        db.query("tickets", null, "id = ?", arrayOf(id), null, null, null).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toTicket() else null
        }
    }

    fun moveByRequest(db: SQLiteDatabase, requestId: String): WalletMove? {
        if (requestId.isBlank()) return null
        db.query("moves", null, "request_id = ?", arrayOf(requestId), null, null, null).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toMove() else null
        }
    }

    fun insertTicket(db: SQLiteDatabase, ticket: WalletTicket) {
        val values = ContentValues().apply {
            put("id", ticket.id)
            put("kind", ticket.kind)
            put("cents", ticket.cents)
            put("phone", ticket.phone)
            put("name", ticket.name)
            put("created_at", ticket.createdAt)
            put("expires_at", ticket.expiresAt)
            put("nonce", ticket.nonce)
            if (ticket.usedAt != null) put("used_at", ticket.usedAt)
            put("used_by", ticket.usedBy)
            put("used_name", ticket.usedName)
        }
        db.insertOrThrow("tickets", null, values)
    }

    fun insertMove(db: SQLiteDatabase, move: WalletMove) {
        val values = ContentValues().apply {
            put("id", move.id)
            put("phone", move.phone)
            put("name", move.name)
            put("kind", move.kind)
            put("cents", move.cents)
            put("at", move.at)
            put("ticket_id", move.ticketId)
            if (move.requestId.isNotBlank()) put("request_id", move.requestId)
        }
        db.insertOrThrow("moves", null, values)
    }

    fun markUsed(db: SQLiteDatabase, id: String, at: Long, phone: String, name: String) {
        val values = ContentValues().apply {
            put("used_at", at)
            put("used_by", phone)
            put("used_name", name)
        }
        db.update("tickets", values, "id = ?", arrayOf(id))
    }

    private fun meta(db: SQLiteDatabase, key: String): String? {
        db.query("meta", arrayOf("v"), "k = ?", arrayOf(key), null, null, null).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private fun putMeta(db: SQLiteDatabase, key: String, value: String) {
        val values = ContentValues().apply {
            put("k", key)
            put("v", value)
        }
        db.insertWithOnConflict("meta", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun android.database.Cursor.toTicket() = WalletTicket(
        id = getString(getColumnIndexOrThrow("id")),
        kind = getString(getColumnIndexOrThrow("kind")),
        cents = getInt(getColumnIndexOrThrow("cents")),
        phone = getString(getColumnIndexOrThrow("phone")),
        name = getString(getColumnIndexOrThrow("name")),
        createdAt = getLong(getColumnIndexOrThrow("created_at")),
        expiresAt = getLong(getColumnIndexOrThrow("expires_at")),
        nonce = getString(getColumnIndexOrThrow("nonce")),
        usedAt = if (isNull(getColumnIndexOrThrow("used_at"))) null else getLong(getColumnIndexOrThrow("used_at")),
        usedBy = getString(getColumnIndexOrThrow("used_by"))?.ifBlank { null },
        usedName = getString(getColumnIndexOrThrow("used_name"))?.ifBlank { null },
    )

    private fun android.database.Cursor.toMove() = WalletMove(
        id = getString(getColumnIndexOrThrow("id")),
        phone = getString(getColumnIndexOrThrow("phone")),
        name = getString(getColumnIndexOrThrow("name")),
        kind = getString(getColumnIndexOrThrow("kind")),
        cents = getInt(getColumnIndexOrThrow("cents")),
        at = getLong(getColumnIndexOrThrow("at")),
        ticketId = getString(getColumnIndexOrThrow("ticket_id")),
        requestId = getString(getColumnIndexOrThrow("request_id")) ?: "",
    )
}
