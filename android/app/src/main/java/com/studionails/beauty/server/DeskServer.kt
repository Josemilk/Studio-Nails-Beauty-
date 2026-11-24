package com.studionails.beauty.server

import com.studionails.beauty.model.balanceOf
import com.studionails.beauty.model.digits
import com.studionails.beauty.model.movesFor
import com.studionails.beauty.network.toJson
import com.studionails.beauty.qr.TicketClaim
import com.studionails.beauty.wallet.DeskLedger
import org.json.JSONArray
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * El teléfono de la manicura escucha en la red local.
 * La clienta no mueve su saldo: pide aquí que se confirme.
 */
class DeskServer(
    private val ledger: DeskLedger,
    private val onChange: (com.studionails.beauty.model.BookState) -> Unit,
    private val accepting: () -> Boolean = { true },
) {
    @Volatile var port: Int = 0
        private set
    private var socket: ServerSocket? = null
    private var thread: Thread? = null

    fun url(): String {
        val ip = lanAddress() ?: return ""
        val open = port
        if (open == 0) return ""
        return "http://$ip:$open"
    }

    fun start() {
        if (thread != null) return
        val server = bind() ?: return
        port = server.localPort
        socket = server
        thread = Thread({
            while (!server.isClosed) {
                try {
                    val client = server.accept()
                    Thread({ handle(client) }, "desk-client").apply { isDaemon = true }.start()
                } catch (_: Exception) {
                    break
                }
            }
        }, "desk").apply { isDaemon = true; start() }
    }

    private fun bind(): ServerSocket? {
        for (candidate in 8787..8795) {
            try {
                return ServerSocket(candidate)
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun stop() {
        try {
            socket?.close()
        } catch (_: Exception) {
        }
    }

    private fun handle(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 8000
            val reader = client.getInputStream().bufferedReader(StandardCharsets.UTF_8)
            val request = reader.readLine() ?: return
            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                val cut = line.indexOf(':')
                if (cut > 0) headers[line.substring(0, cut).lowercase()] = line.substring(cut + 1).trim()
            }
            val length = headers["content-length"]?.toIntOrNull() ?: 0
            val body = if (length > 0) {
                val buffer = CharArray(length)
                var read = 0
                while (read < length) {
                    val n = reader.read(buffer, read, length - read)
                    if (n < 0) break
                    read += n
                }
                String(buffer, 0, read)
            } else ""
            val parts = request.split(" ")
            val method = parts.getOrNull(0) ?: ""
            val target = parts.getOrNull(1) ?: "/"
            val response = try {
                route(method, target, body)
            } catch (error: Exception) {
                errorJson(error.message ?: "No se pudo completar.")
            }
            val bytes = response.toByteArray(StandardCharsets.UTF_8)
            val writer = client.getOutputStream()
            writer.write("HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
            writer.write(bytes)
            writer.flush()
        }
    }

    private fun route(method: String, target: String, body: String): String {
        if (!accepting()) return errorJson("Este teléfono no es la caja del estudio.")
        val path = target.substringBefore("?")
        if (method == "GET" && path == "/v1/wallet/balance") {
            val phone = query(target, "phone")
            val state = ledger.snapshot
            val moves = movesFor(state, phone).take(12)
            return JSONObject()
                .put("ok", true)
                .put("balance", balanceOf(state, phone))
                .put("moves", JSONArray().apply { moves.forEach { put(it.toJson()) } })
                .toString()
        }
        if (method == "POST" && path == "/v1/wallet/quote") {
            val json = JSONObject(body.ifBlank { "{}" })
            val claim = claimOf(json)
            val result = ledger.quote(claim, System.currentTimeMillis())
            return result.fold(
                onSuccess = { quote ->
                    JSONObject()
                        .put("ok", true)
                        .put("cents", quote.cents)
                        .put("kind", quote.kind)
                        .put("expiresAt", quote.expiresAt)
                        .put("name", quote.name)
                        .toString()
                },
                onFailure = { errorJson(it.message ?: "No se pudo leer el código.") },
            )
        }
        if (method == "POST" && path == "/v1/wallet/redeem") {
            val json = JSONObject(body.ifBlank { "{}" })
            val claim = claimOf(json)
            val result = ledger.redeem(claim, json.optString("phone"), json.optString("name"), json.optString("requestId"), System.currentTimeMillis())
            return result.fold(
                onSuccess = { redeemed ->
                    onChange(redeemed.state)
                    val phone = digits(json.optString("phone"))
                    val moves = movesFor(redeemed.state, phone).take(12)
                    JSONObject()
                        .put("ok", true)
                        .put("balance", redeemed.balance)
                        .put("move", redeemed.move.toJson())
                        .put("moves", JSONArray().apply { moves.forEach { put(it.toJson()) } })
                        .toString()
                },
                onFailure = { errorJson(it.message ?: "No se pudo confirmar.") },
            )
        }
        return errorJson("No existe esa ruta.")
    }

    private fun claimOf(json: JSONObject) = TicketClaim(
        id = json.optString("id").uppercase(),
        kind = json.optString("kind"),
        cents = json.optInt("cents"),
        expiresAt = json.optLong("expiresAt"),
        nonce = json.optString("nonce"),
        host = "",
    )

    private fun query(target: String, key: String): String {
        val query = target.substringAfter("?", "")
        return query.split("&").firstNotNullOfOrNull { part ->
            val bits = part.split("=", limit = 2)
            if (bits.getOrNull(0) == key) URLDecoder.decode(bits.getOrNull(1) ?: "", "UTF-8") else null
        }.orEmpty()
    }

    private fun errorJson(message: String) = JSONObject().put("ok", false).put("error", message).toString()

    private fun lanAddress(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (nif in interfaces) {
                if (!nif.isUp || nif.isLoopback) continue
                for (address in nif.inetAddresses) {
                    if (address is Inet4Address && address.isSiteLocalAddress) return address.hostAddress
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
