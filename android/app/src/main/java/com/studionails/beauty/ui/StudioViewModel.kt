package com.studionails.beauty.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.studionails.beauty.data.BookStore
import com.studionails.beauty.model.BookState
import com.studionails.beauty.model.Role
import com.studionails.beauty.notify.Reminders

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
    var book by mutableStateOf(store.load())
        private set
    var screen by mutableStateOf<Screen>(if (book.mode == null) Screen.Gate else Screen.Home)
        private set
    private val backStack = mutableListOf<Screen>()

    init {
        Reminders.sync(app, book)
    }

    fun open(next: Screen) {
        if (next == screen) return
        backStack.add(screen)
        screen = next
    }

    fun back(): Boolean {
        if (backStack.isEmpty()) {
            if (screen != Screen.Home && book.mode != null) {
                screen = Screen.Home
                return true
            }
            return false
        }
        screen = backStack.removeAt(backStack.lastIndex)
        return true
    }

    fun commit(next: BookState) {
        val previous = book
        book = next
        store.save(next)
        val context = getApplication<Application>()
        Reminders.sync(context, next)
        Reminders.flashNew(context, previous, next)
    }

    fun choose(role: Role) {
        commit(book.copy(mode = role))
        backStack.clear()
        screen = Screen.Home
    }
}
