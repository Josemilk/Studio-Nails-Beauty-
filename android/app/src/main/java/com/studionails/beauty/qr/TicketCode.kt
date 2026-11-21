package com.studionails.beauty.qr

import com.studionails.beauty.model.WalletTicket
import java.net.URLDecoder
import java.net.URLEncoder

data class TicketClaim(
    val id: String,
    val kind: String,
    val cents: Int,
    val expiresAt: Long,
    val nonce: String,
    val host: String,
)

private const val Head = "SNB1"

fun ticketQr(ticket: WalletTicket, host: String): String {
    val encodedHost = URLEncoder.encode(host, "UTF-8")
    return listOf(Head, ticket.id, ticket.kind, ticket.cents.toString(), ticket.expiresAt.toString(), ticket.nonce, encodedHost)
        .joinToString("|")
}

fun parseTicketQr(raw: String): TicketClaim? {
    val parts = raw.trim().split("|")
    if (parts.size < 7 || parts[0] != Head) return null
    val cents = parts[3].toIntOrNull() ?: return null
    val expires = parts[4].toLongOrNull() ?: return null
    val host = try {
        URLDecoder.decode(parts.subList(6, parts.size).joinToString("|"), "UTF-8")
    } catch (_: Exception) {
        ""
    }
    if (parts[1].isBlank() || parts[5].isBlank()) return null
    return TicketClaim(parts[1].uppercase(), parts[2], cents, expires, parts[5], host)
}
