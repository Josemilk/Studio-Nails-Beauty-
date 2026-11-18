package com.studionails.beauty.data

import android.content.Context
import com.studionails.beauty.model.BookState
import com.studionails.beauty.model.parseBook
import com.studionails.beauty.model.seed
import com.studionails.beauty.model.toJson
import java.io.File

class BookStore(context: Context) {
    private val file = File(context.applicationContext.filesDir, "libreta.json")

    fun load(): BookState {
        if (!file.exists()) return seed().also(::save)
        return try {
            parseBook(file.readText())
        } catch (_: Exception) {
            seed().also(::save)
        }
    }

    fun save(state: BookState) {
        file.writeText(state.toJson())
    }
}
