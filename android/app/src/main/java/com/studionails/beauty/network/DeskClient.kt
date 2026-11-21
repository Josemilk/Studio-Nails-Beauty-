package com.studionails.beauty.network

import com.studionails.beauty.model.WalletMove
import com.studionails.beauty.qr.TicketClaim
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class DeskReceipt(
    val balance: Int,
    val move: WalletMove,
    val moves: List<WalletMove>,
)

object DeskClient {
    fun redeem(host: String, claim: TicketClaim, phone: String, name: String, requestId: String): Result<DeskReceipt> {
        val body = JSONObject()
            .put("id", claim.id)
            .put("nonce", claim.nonce)
            .put("kind", claim.kind)
            .put("phone", phone)
            .put("name", name)
            .put("requestId", requestId)
        val response = post("$host/v1/wallet/redeem", body) 
        return response.fold(
            onSuccess = { json -> readReceipt(json) },
            onFailure = { Result.failure(it) },
        )
    }

    fun balance(host: String, phone: String): Result<Pair<Int, List<WalletMove>>> {
        val response = get("$host/v1/wallet/balance?phone=${java.net.URLEncoder.encode(phone, "UTF-8")}")
        return response.fold(
            onSuccess = { json ->
                if (!json.optBoolean("ok")) Result.failure(IllegalArgumentException(json.optString("error", "Sin respuesta.")))
                else Result.success(json.optInt("balance") to json.optJSONArray("moves").toMoves())
            },
            onFailure = { Result.failure(it) },
        )
    }

    private fun readReceipt(json: JSONObject): Result<DeskReceipt> {
        if (!json.optBoolean("ok")) return Result.failure(IllegalArgumentException(json.optString("error", "No se pudo confirmar.")))
        val moveJson = json.optJSONObject("move") ?: return Result.failure(IllegalArgumentException("La manicura no devolvió el movimiento."))
        val move = moveJson.toMove()
        val moves = json.optJSONArray("moves").toMoves().ifEmpty { listOf(move) }
        return Result.success(DeskReceipt(json.optInt("balance"), move, moves))
    }

    private fun post(url: String, body: JSONObject) = call(url) { connection ->
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.outputStream.use { it.write(body.toString().toByteArray()) }
    }

    private fun get(url: String) = call(url) { connection ->
        connection.requestMethod = "GET"
    }

    private fun call(url: String, prepare: (HttpURLConnection) -> Unit): Result<JSONObject> {
        return try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                prepare(this)
            }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (text.isBlank()) Result.failure(IllegalArgumentException(unreachable()))
            else Result.success(JSONObject(text))
        } catch (_: Exception) {
            Result.failure(IllegalArgumentException(unreachable()))
        }
    }

    private fun unreachable() = "No llego al teléfono de la manicura. Las dos tienen que estar en el mismo Wi-Fi y la app de ella tiene que estar abierta."
}

private fun JSONObject.toMove() = WalletMove(
    id = optString("id"),
    phone = optString("phone"),
    name = optString("name"),
    kind = optString("kind"),
    cents = optInt("cents"),
    at = optLong("at"),
    ticketId = optString("ticketId"),
    requestId = optString("requestId"),
)

private fun JSONArray?.toMoves(): List<WalletMove> {
    if (this == null) return emptyList()
    return (0 until length()).map { getJSONObject(it).toMove() }
}

fun WalletMove.toJson() = JSONObject()
    .put("id", id)
    .put("phone", phone)
    .put("name", name)
    .put("kind", kind)
    .put("cents", cents)
    .put("at", at)
    .put("ticketId", ticketId)
    .put("requestId", requestId)
