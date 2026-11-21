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
import com.studionails.beauty.model.Role
import com.studionails.beauty.model.WalletTicket
import com.studionails.beauty.model.digits
import com.studionails.beauty.network.DeskClient
import com.studionails.beauty.notify.Reminders
import com.studionails.beauty.qr.TicketClaim
import com.studionails.beauty.server.DeskServer
import com.studionails.beauty.wallet.DeskLedger
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
    private val server = DeskServer(ledger) { next -> main.post { persist(next) } }
    var book by mutableStateOf(ledger.attach(store.load()))
        private set
    var screen by mutableStateOf<Screen>(if (book.mode == null) Screen.Gate else Screen.Home)
        private set

    init {
        Reminders.sync(app, book)
        thread(name = "desk-start", isDaemon = true) { server.start() }
    }

    fun isDesk() = ledger.isDesk()

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
        if (ledger.contains(claim.id)) {
            return ledger.redeem(claim, phone, name, requestId, now).map { redeemed ->
                remembered(redeemed.state, phone, name, redeemed.balance, claim.host) to redeemed.balance
            }
        }
        val host = claim.host.ifBlank { book.deskHost }
        if (!host.startsWith("http")) {
            return Result.failure(IllegalArgumentException("No llego al teléfono de la manicura. Conéctense al mismo Wi-Fi y genera el código otra vez."))
        }
        return DeskClient.redeem(host, claim, phone, name, requestId).map { receipt ->
            val key = digits(phone)
            val others = book.moves.filter { digits(it.phone) != key }
            val next = book.copy(moves = receipt.moves + others)
            remembered(next, phone, name, receipt.balance, host) to receipt.balance
        }
    }

    fun refreshBalance() {
        val host = book.deskHost
        val phone = book.clientPhone
        if (ledger.isDesk() || !host.startsWith("http") || digits(phone).length < 10) return
        val current = book
        thread(name = "desk-balance", isDaemon = true) {
            val result = DeskClient.balance(host, phone)
            main.post {
                result.onSuccess { (balance, moves) ->
                    val key = digits(phone)
                    val others = book.moves.filter { digits(it.phone) != key }
                    if (book.deskHost == current.deskHost) commit(book.copy(moves = moves + others, confirmedBalance = balance))
                }
            }
        }
    }

    override fun onCleared() {
        server.stop()
    }

    private fun remembered(state: BookState, phone: String, name: String, balance: Int, host: String): BookState {
        val key = digits(phone)
        val asClient = state.mode == Role.client
        return state.copy(
            confirmedBalance = balance,
            deskHost = host.ifBlank { state.deskHost },
            clientPhone = if (asClient && key.length >= 10) key else state.clientPhone,
            clientName = if (asClient && name.isNotBlank()) name.trim() else state.clientName,
        )
    }

    private fun persist(next: BookState) {
        val previous = book
        book = next
        store.save(next)
        val context = getApplication<Application>()
        Reminders.sync(context, next)
        Reminders.flashNew(context, previous, next)
    }
}
