package com.studionails.beauty.model

import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

enum class Role { manicurist, client }

data class Settings(
    val code: String = "MESA",
    val ownerName: String = "Manicura",
    val ownerPhone: String = "2145550100",
    val studioName: String = "Studio Nails & Beauty",
    val workDays: List<Int> = listOf(2, 3, 4, 5, 6),
    val startMin: Int = 10 * 60,
    val endMin: Int = 20 * 60,
    val slotMin: Int = 60,
)

data class Service(
    val id: String,
    val name: String,
    val minutesLabel: String,
    val minutes: Int,
    val price: Int,
    val photo: String,
)

val Services = listOf(
    Service("clasica", "Manicura clásica", "30-45 min", 45, 25, "clasica.jpg"),
    Service("art", "Nail Art", "45-60 min", 60, 35, "art.jpg"),
    Service("gel", "Esmaltado en Gel", "60 min", 60, 40, "clasica.jpg"),
    Service("cuidado", "Cuidado de cutícula", "30 min", 30, 20, "art.jpg"),
)

fun serviceById(id: String?) = Services.find { it.id == id }

data class Appointment(
    val id: String,
    val date: String,
    val time: String,
    val name: String,
    val phone: String,
    val serviceId: String? = null,
    val paid: Boolean = false,
    val status: String = "booked",
    val createdAt: Long = 0,
    val createdBy: Role? = null,
    val cancelledAt: Long? = null,
    val cancelledBy: Role? = null,
)

data class StudioClient(val id: String, val name: String, val phone: String)

data class WalletTicket(
    val id: String,
    val kind: String,
    val cents: Int,
    val phone: String = "",
    val name: String = "",
    val createdAt: Long = 0,
    val usedAt: Long? = null,
    val usedBy: String? = null,
    val usedName: String? = null,
)

data class WalletMove(
    val id: String,
    val phone: String,
    val name: String,
    val kind: String,
    val cents: Int,
    val at: Long,
    val ticketId: String,
)

data class Notice(
    val id: String,
    val role: Role,
    val at: Long,
    val title: String,
    val body: String,
    val appointmentId: String? = null,
)

data class BookState(
    val mode: Role? = null,
    val settings: Settings = Settings(),
    val appointments: List<Appointment> = emptyList(),
    val readIds: List<String> = emptyList(),
    val blocked: List<String> = emptyList(),
    val closedSlots: List<String> = emptyList(),
    val clientName: String = "",
    val clientPhone: String = "",
    val clients: List<StudioClient> = emptyList(),
    val tickets: List<WalletTicket> = emptyList(),
    val moves: List<WalletMove> = emptyList(),
    val hiddenNoticeIds: List<String> = emptyList(),
    val hiddenMoveIds: List<String> = emptyList(),
)

private val Months = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)
private val Weekdays = listOf("domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado")
private val TicketAlphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
private val Hour = 60 * 60 * 1000L
private val Minute = 60 * 1000L
private val random = SecureRandom()

fun digits(phone: String) = phone.filter { it.isDigit() }

fun normalizePhone(phone: String): String? {
    val raw = digits(phone)
    val local = when {
        raw.length == 8 -> raw
        raw.length == 11 && raw.startsWith("53") -> raw.drop(2)
        else -> return null
    }
    return "53$local"
}

fun prettyPhone(phone: String): String {
    val normalized = normalizePhone(phone) ?: return phone.trim()
    return "+53 ${normalized.drop(2)}"
}

fun dialNumber(phone: String): String = normalizePhone(phone) ?: digits(phone)

fun pad(n: Int) = n.toString().padStart(2, '0')

fun dateKey(date: LocalDate) = "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth)

fun parseDate(key: String): LocalDate = LocalDate.parse(key)

fun jsDay(date: LocalDate) = date.dayOfWeek.value % 7

fun timeToMinutes(time: String): Int {
    val parts = time.split(":")
    return (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
}

fun minutesToTime(mins: Int) = "${pad(mins / 60)}:${pad(mins % 60)}"

fun stamp(date: String, time: String): Long {
    val day = parseDate(date)
    val mins = timeToMinutes(time)
    return day.atTime(mins / 60, mins % 60).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun clockLabel(time: String): String {
    val mins = timeToMinutes(time)
    val h = mins / 60
    val m = mins % 60
    val suffix = if (h >= 12) "PM" else "AM"
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "$h12:${pad(m)} $suffix"
}

fun monthName(month: Int) = Months[month - 1].replaceFirstChar { it.uppercase() }

fun weekdayName(date: String) = Weekdays[jsDay(parseDate(date))].replaceFirstChar { it.uppercase() }

fun longDate(date: String): String {
    val d = parseDate(date)
    return "${Weekdays[jsDay(d)]} ${d.dayOfMonth} de ${Months[d.monthValue - 1]}"
}

fun headingDate(date: String): String {
    val d = parseDate(date)
    val name = Weekdays[jsDay(d)]
    return name.replaceFirstChar { it.uppercase() } + " ${d.dayOfMonth}"
}

fun relativeDay(date: String, today: LocalDate): String {
    val key = dateKey(today)
    val tomorrow = dateKey(today.plusDays(1))
    return when (date) {
        key -> "Hoy"
        tomorrow -> "Mañana"
        else -> headingDate(date)
    }
}

fun formatMoney(cents: Int): String {
    val sign = if (cents < 0) "-" else ""
    val absCents = abs(cents)
    val dollars = absCents / 100
    val rest = absCents % 100
    return if (rest == 0) "$sign$$dollars" else "$sign$$dollars.${pad(rest)}"
}

fun parseMoney(raw: String): Int? {
    val clean = raw.trim().replace("$", "").replace(" ", "").replace(",", ".")
    if (!Regex("""^\d+(\.\d{1,2})?$""").matches(clean)) return null
    val parts = clean.split(".")
    val cents = parts[0].toInt() * 100 + (parts.getOrNull(1) ?: "").padEnd(2, '0').toInt()
    if (cents <= 0 || cents > 500_000) return null
    return cents
}

fun makeId(): String = "c" + System.currentTimeMillis().toString(36) + random.nextInt(36 * 36).toString(36)

fun slotTimes(settings: Settings): List<String> {
    val out = mutableListOf<String>()
    var t = settings.startMin
    while (t + settings.slotMin <= settings.endMin) {
        out += minutesToTime(t)
        t += settings.slotMin
    }
    return out
}

fun isWorkDay(date: String, settings: Settings) = settings.workDays.contains(jsDay(parseDate(date)))

fun isBlocked(state: BookState, date: String) = state.blocked.contains(date)

fun slotKey(date: String, time: String) = "$date|$time"

fun isSlotClosed(state: BookState, date: String, time: String) = state.closedSlots.contains(slotKey(date, time))

fun appointmentMinutes(state: BookState, appointment: Appointment) =
    serviceById(appointment.serviceId)?.minutes ?: state.settings.slotMin

private fun rangesOverlap(aStart: Int, aLen: Int, bStart: Int, bLen: Int) =
    aStart < bStart + bLen && bStart < aStart + aLen

fun bookedOn(state: BookState, date: String) =
    state.appointments.filter { it.date == date && it.status == "booked" }

fun slotTaken(state: BookState, date: String, time: String, minutes: Int = state.settings.slotMin): Boolean {
    val start = timeToMinutes(time)
    return bookedOn(state, date).any { appointment ->
        rangesOverlap(start, minutes, timeToMinutes(appointment.time), appointmentMinutes(state, appointment))
    }
}

fun isPastSlot(date: String, time: String, now: Long) = stamp(date, time) <= now

fun freeSlots(state: BookState, date: String, now: Long, minutes: Int = state.settings.slotMin): List<String> {
    if (!isWorkDay(date, state.settings) || isBlocked(state, date)) return emptyList()
    return slotTimes(state.settings).filter { time ->
        !isSlotClosed(state, date, time) && !slotTaken(state, date, time, minutes) && !isPastSlot(date, time, now)
    }
}

fun turnosLabel(count: Int) = if (count == 1) "1 turno disponible" else "$count turnos disponibles"

fun monthDates(monthStart: LocalDate): List<String> {
    val last = monthStart.lengthOfMonth()
    return (1..last).map { dateKey(monthStart.withDayOfMonth(it)) }
}

fun weekOf(date: String): List<String> {
    val day = parseDate(date)
    val start = day.minusDays((jsDay(day)).toLong())
    return (0..6).map { dateKey(start.plusDays(it.toLong())) }
}

fun monthWeeks(year: Int, month: Int): List<List<String>> {
    val first = LocalDate.of(year, month, 1)
    var cursor = first.minusDays(jsDay(first).toLong())
    val last = first.withDayOfMonth(first.lengthOfMonth())
    val weeks = mutableListOf<List<String>>()
    while (!cursor.isAfter(last)) {
        weeks += (0..6).map { dateKey(cursor.plusDays(it.toLong())) }
        cursor = cursor.plusDays(7)
    }
    return weeks
}

fun fromToday(dates: List<String>, today: String) = dates.filter { it >= today }

fun blockDates(state: BookState, dates: List<String>, blocked: Boolean): BookState {
    val next = state.blocked.toMutableSet()
    dates.forEach { if (blocked) next += it else next -= it }
    return state.copy(blocked = next.sorted())
}

fun setClosed(state: BookState, date: String, time: String, closed: Boolean): BookState {
    if (closed && slotTaken(state, date, time)) return state
    val key = slotKey(date, time)
    val has = state.closedSlots.contains(key)
    if (closed == has) return state
    val slots = if (closed) state.closedSlots + key else state.closedSlots.filter { it != key }
    return state.copy(closedSlots = slots)
}

data class BookInput(
    val date: String,
    val time: String,
    val name: String,
    val phone: String,
    val serviceId: String,
    val id: String,
    val now: Long,
    val createdBy: Role,
)

fun book(state: BookState, input: BookInput): Result<BookState> {
    val name = input.name.trim()
    val phone = normalizePhone(input.phone) ?: return Result.failure(IllegalArgumentException("El teléfono debe tener el formato +53 xxxxxxxx."))
    val service = serviceById(input.serviceId) ?: return Result.failure(IllegalArgumentException("Elige un servicio."))
    if (input.time.isBlank()) return Result.failure(IllegalArgumentException("Elige una hora."))
    if (name.length < 2) return Result.failure(IllegalArgumentException("Escribe el nombre."))
    if (phone.length != 10) return Result.failure(IllegalArgumentException("El teléfono debe tener el formato +53 xxxxxxxx."))
    if (!isWorkDay(input.date, state.settings)) return Result.failure(IllegalArgumentException("Ese día la mesa está cerrada."))
    if (isBlocked(state, input.date)) return Result.failure(IllegalArgumentException("Ese día está bloqueado."))
    if (isSlotClosed(state, input.date, input.time)) return Result.failure(IllegalArgumentException("Esa hora está cerrada."))
    if (isPastSlot(input.date, input.time, input.now)) return Result.failure(IllegalArgumentException("Esa hora ya pasó."))
    if (slotTaken(state, input.date, input.time, service.minutes)) return Result.failure(IllegalArgumentException("Esa hora ya está ocupada."))
    val appointment = Appointment(
        id = input.id,
        date = input.date,
        time = input.time,
        name = name,
        phone = phone,
        serviceId = service.id,
        paid = false,
        status = "booked",
        createdAt = input.now,
        createdBy = input.createdBy,
    )
    return Result.success(state.copy(appointments = state.appointments + appointment))
}

fun cancel(state: BookState, id: String, by: Role, now: Long) = state.copy(
    appointments = state.appointments.map { appointment ->
        if (appointment.id == id && appointment.status == "booked") {
            appointment.copy(status = "cancelled", cancelledAt = now, cancelledBy = by)
        } else appointment
    },
)

fun saveProfile(state: BookState, role: Role, name: String, phone: String): Result<BookState> {
    val cleanName = name.trim()
    val cleanPhone = normalizePhone(phone) ?: ""
    if (cleanName.length < 2) return Result.failure(IllegalArgumentException("Escribe el nombre."))
    if (cleanPhone.length != 10) return Result.failure(IllegalArgumentException("El teléfono debe tener el formato +53 xxxxxxxx."))
    if (role == Role.client) {
        val prev = digits(state.clientPhone)
        return Result.success(
            state.copy(
                clientName = cleanName,
                clientPhone = cleanPhone,
                appointments = state.appointments.map { appointment ->
                    if (prev.isNotEmpty() && digits(appointment.phone) == prev && appointment.status == "booked") {
                        appointment.copy(name = cleanName, phone = cleanPhone)
                    } else appointment
                },
            ),
        )
    }
    return Result.success(state.copy(settings = state.settings.copy(ownerName = cleanName, ownerPhone = cleanPhone)))
}

fun clientAppointments(state: BookState): List<Appointment> {
    val phone = digits(state.clientPhone)
    if (phone.isEmpty()) return emptyList()
    return state.appointments
        .filter { it.status == "booked" && digits(it.phone) == phone }
        .sortedBy { stamp(it.date, it.time) }
}

fun nextAppointment(state: BookState, now: Long, role: Role): Appointment? {
    val source = if (role == Role.client) clientAppointments(state) else state.appointments.filter { it.status == "booked" }
    return source
        .filter { stamp(it.date, it.time) + appointmentMinutes(state, it) * Minute >= now }
        .minByOrNull { stamp(it.date, it.time) }
}

fun reminderText(appointment: Appointment) =
    "Hola ${appointment.name}, te recuerdo tu cita el ${longDate(appointment.date)} a las ${appointment.time}."

private fun eveStamp(date: String): Long {
    val eve = parseDate(date).minusDays(1).atTime(19, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    return eve
}

fun noticesFor(state: BookState): List<Notice> {
    val notices = mutableListOf<Notice>()
    val booked = state.appointments.filter { it.status == "booked" }
    for (appointment in state.appointments) {
        if (appointment.status == "booked" && appointment.createdBy == Role.client) {
            notices += Notice(
                id = "booked-${appointment.id}",
                role = Role.manicurist,
                at = appointment.createdAt,
                title = "Turno agendado",
                body = "${appointment.name} agendó el ${longDate(appointment.date)} a las ${clockLabel(appointment.time)}.",
                appointmentId = appointment.id,
            )
        }
        if (appointment.status == "cancelled" && appointment.cancelledAt != null && appointment.cancelledBy != null) {
            val role = if (appointment.cancelledBy == Role.client) Role.manicurist else Role.client
            val whenLabel = longDate(appointment.date)
            notices += Notice(
                id = "cancel-${appointment.id}",
                role = role,
                at = appointment.cancelledAt,
                title = if (role == Role.manicurist) "Cita cancelada" else "Tu cita se canceló",
                body = if (role == Role.manicurist) {
                    "${appointment.name} canceló su cita del $whenLabel a las ${clockLabel(appointment.time)}."
                } else {
                    "La cita del $whenLabel a las ${clockLabel(appointment.time)} fue cancelada."
                },
                appointmentId = appointment.id,
            )
        }
    }
    val offsets = listOf(
        Triple("12h", 12 * Hour, "En 12 horas"),
        Triple("5h", 5 * Hour, "En 5 horas"),
        Triple("2h", 2 * Hour, "En 2 horas"),
        Triple("30m", 30 * Minute, "En 30 minutos"),
    )
    for (appointment in booked) {
        val start = stamp(appointment.date, appointment.time)
        val eve = eveStamp(appointment.date)
        if (eve > appointment.createdAt && eve < start) {
            notices += Notice("eve-${appointment.id}", Role.client, eve, "Mañana tienes cita", "A las ${clockLabel(appointment.time)}.", appointment.id)
        }
        for ((key, offset, title) in offsets) {
            val at = start - offset
            if (at > appointment.createdAt && at < start) {
                notices += Notice("c-$key-${appointment.id}", Role.client, at, title, "Tu cita es a las ${clockLabel(appointment.time)}.", appointment.id)
            }
        }
        val hourBefore = start - Hour
        if (hourBefore > appointment.createdAt && hourBefore < start) {
            notices += Notice("h1-${appointment.id}", Role.manicurist, hourBefore, "En 1 hora", "${appointment.name} a las ${clockLabel(appointment.time)}.", appointment.id)
        }
    }
    booked.groupBy { it.date }.forEach { (date, list) ->
        val eve = eveStamp(date)
        val included = list.filter { it.createdAt < eve && stamp(it.date, it.time) > eve }.sortedBy { timeToMinutes(it.time) }
        if (included.isEmpty()) return@forEach
        notices += Notice(
            id = "digest-$date",
            role = Role.manicurist,
            at = eve,
            title = if (included.size == 1) "Mañana, una cita" else "Mañana, ${included.size} citas",
            body = included.joinToString(" · ") { "${clockLabel(it.time)} ${it.name}" },
        )
    }
    return notices.filterNot { it.id in state.hiddenNoticeIds || "${it.role.name}:${it.id}" in state.hiddenNoticeIds }
}

fun firedNotices(state: BookState, role: Role, now: Long): List<Notice> {
    val windowStart = now - 36 * Hour
    return noticesFor(state)
        .filter { it.role == role && it.at <= now && (it.at >= windowStart || it.id.startsWith("cancel") || it.id.startsWith("booked")) }
        .sortedByDescending { it.at }
}

fun unreadCount(state: BookState, now: Long): Int {
    val role = state.mode ?: return 0
    return firedNotices(state, role, now).count { it.id !in state.readIds }
}

fun balanceOf(state: BookState, phone: String): Int {
    val key = digits(phone)
    if (key.isEmpty()) return 0
    return state.moves.fold(0) { sum, move ->
        if (digits(move.phone) != key) sum else sum + if (move.kind == "topup") move.cents else -move.cents
    }
}

fun movesFor(state: BookState, phone: String? = null) = state.moves
    .filter { it.id !in state.hiddenMoveIds && (phone == null || digits(it.phone) == digits(phone)) }
    .sortedByDescending { it.at }

fun qrPayload(id: String) = "SNB:$id"

fun readTicketCode(raw: String): String {
    val text = raw.trim().uppercase().replace(" ", "")
    val tagged = Regex("""SNB:([A-Z2-9]{6})""").find(text)?.groupValues?.getOrNull(1)
    if (tagged != null) return tagged
    return if (Regex("""^[A-Z2-9]{6}$""").matches(text)) text else ""
}

fun addClient(state: BookState, name: String, phone: String): Result<BookState> {
    val cleanName = name.trim()
    val cleanPhone = normalizePhone(phone) ?: ""
    if (cleanName.length < 2) return Result.failure(IllegalArgumentException("Escribe el nombre."))
    if (cleanPhone.length != 10) return Result.failure(IllegalArgumentException("El teléfono debe tener el formato +53 xxxxxxxx."))
    if (state.clients.any { digits(it.phone) == cleanPhone }) return Result.failure(IllegalArgumentException("Esa clienta ya está en la lista."))
    return Result.success(state.copy(clients = state.clients + StudioClient(makeId(), cleanName, cleanPhone)))
}

fun removeClient(state: BookState, id: String) = state.copy(clients = state.clients.filter { it.id != id })

fun deleteTicket(state: BookState, id: String): BookState =
    state.copy(tickets = state.tickets.filterNot { it.id == id && it.usedAt == null })

fun deleteMove(state: BookState, id: String): BookState =
    state.copy(hiddenMoveIds = (state.hiddenMoveIds + id).distinct())

fun deleteAppointment(state: BookState, id: String): BookState =
    state.copy(appointments = state.appointments.filterNot { it.id == id }, readIds = state.readIds.filterNot { it.contains(id) }, hiddenNoticeIds = (state.hiddenNoticeIds + noticesFor(state).filter { it.appointmentId == id }.map { "${it.role.name}:${it.id}" }).distinct())

fun deleteNotice(state: BookState, id: String, role: Role): BookState =
    state.copy(hiddenNoticeIds = (state.hiddenNoticeIds + "${role.name}:$id").distinct(), readIds = state.readIds.filterNot { it == id })

fun createTicket(state: BookState, kind: String, cents: Int, phone: String, name: String, now: Long): Result<Pair<BookState, WalletTicket>> {
    if (cents <= 0) return Result.failure(IllegalArgumentException("Escribe un valor en dólares."))
    val cleanPhone = if (phone.isBlank()) "" else normalizePhone(phone) ?: return Result.failure(IllegalArgumentException("El teléfono debe tener el formato +53 xxxxxxxx."))
    val used = state.tickets.map { it.id }.toMutableSet()
    var id: String
    do {
        id = buildString(6) { repeat(6) { append(TicketAlphabet[random.nextInt(TicketAlphabet.length)]) } }
    } while (!used.add(id))
    val ticket = WalletTicket(id, kind, cents, cleanPhone, name.trim(), now)
    return Result.success(state.copy(tickets = listOf(ticket) + state.tickets) to ticket)
}

fun redeemTicket(state: BookState, raw: String, expected: String, phone: String, name: String, now: Long): Result<Pair<BookState, Int>> {
    val id = readTicketCode(raw)
    if (id.isEmpty()) return Result.failure(IllegalArgumentException("Ese código no es de Studio Nails."))
    val ticket = state.tickets.find { it.id == id } ?: return Result.failure(IllegalArgumentException("No encuentro ese código."))
    if (ticket.usedAt != null) return Result.failure(IllegalArgumentException("Ese código ya se usó."))
    if (ticket.kind != expected) {
        val message = if (expected == "topup") "Ese código es un cobro. Ábrelo en Pagar." else "Ese código es una recarga. Ábrelo en Recargar."
        return Result.failure(IllegalArgumentException(message))
    }
    val mine = normalizePhone(phone) ?: ""
    if (mine.length != 10) return Result.failure(IllegalArgumentException("Guarda tu teléfono en ajustes antes de usar la billetera."))
    if (ticket.phone.isNotEmpty() && digits(ticket.phone) != mine) return Result.failure(IllegalArgumentException("Este código es para otra clienta."))
    if (ticket.kind == "charge" && balanceOf(state, mine) < ticket.cents) return Result.failure(IllegalArgumentException("No alcanza el saldo."))
    val move = WalletMove("m" + now.toString(36), mine, name.trim().ifBlank { "Clienta" }, ticket.kind, ticket.cents, now, ticket.id)
    val next = state.copy(
        moves = listOf(move) + state.moves,
        tickets = state.tickets.map { if (it.id == ticket.id) it.copy(usedAt = now, usedBy = mine, usedName = move.name) else it },
    )
    return Result.success(next to balanceOf(next, mine))
}

fun whenLabel(at: Long, today: LocalDate): String {
    val dateTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(at), ZoneId.systemDefault())
    val hm = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    val key = dateKey(dateTime.toLocalDate())
    return when (key) {
        dateKey(today) -> "Hoy $hm"
        dateKey(today.plusDays(1)) -> "Mañana $hm"
        else -> "${dateTime.dayOfMonth} ${Months[dateTime.monthValue - 1]} $hm"
    }
}

fun seed(now: LocalDateTime = LocalDateTime.now()): BookState = BookState(
    settings = Settings(ownerName = "Manicura"),
    clientName = "",
    clientPhone = "",
)

fun BookState.toJson(): String {
    fun role(value: Role?) = value?.name ?: ""
    val root = JSONObject()
    root.put("mode", role(mode))
    root.put("clientName", clientName)
    root.put("clientPhone", clientPhone)
    root.put("settings", JSONObject().apply {
        put("code", settings.code)
        put("ownerName", settings.ownerName)
        put("ownerPhone", settings.ownerPhone)
        put("studioName", settings.studioName)
        put("workDays", JSONArray(settings.workDays))
        put("startMin", settings.startMin)
        put("endMin", settings.endMin)
        put("slotMin", settings.slotMin)
    })
    root.put("appointments", JSONArray().apply {
        appointments.forEach { item ->
            put(JSONObject().apply {
                put("id", item.id)
                put("date", item.date)
                put("time", item.time)
                put("name", item.name)
                put("phone", item.phone)
                put("serviceId", item.serviceId ?: "")
                put("paid", item.paid)
                put("status", item.status)
                put("createdAt", item.createdAt)
                put("createdBy", role(item.createdBy))
                put("cancelledAt", item.cancelledAt ?: 0)
                put("cancelledBy", role(item.cancelledBy))
            })
        }
    })
    root.put("readIds", JSONArray(readIds))
    root.put("hiddenNoticeIds", JSONArray(hiddenNoticeIds))
    root.put("hiddenMoveIds", JSONArray(hiddenMoveIds))
    root.put("blocked", JSONArray(blocked))
    root.put("closedSlots", JSONArray(closedSlots))
    root.put("clients", JSONArray().apply {
        clients.forEach { put(JSONObject().apply { put("id", it.id); put("name", it.name); put("phone", it.phone) }) }
    })
    root.put("tickets", JSONArray().apply {
        tickets.forEach { ticket ->
            put(JSONObject().apply {
                put("id", ticket.id)
                put("kind", ticket.kind)
                put("cents", ticket.cents)
                put("phone", ticket.phone)
                put("name", ticket.name)
                put("createdAt", ticket.createdAt)
                put("usedAt", ticket.usedAt ?: 0)
                put("usedBy", ticket.usedBy ?: "")
                put("usedName", ticket.usedName ?: "")
            })
        }
    })
    root.put("moves", JSONArray().apply {
        moves.forEach { move ->
            put(JSONObject().apply {
                put("id", move.id)
                put("phone", move.phone)
                put("name", move.name)
                put("kind", move.kind)
                put("cents", move.cents)
                put("at", move.at)
                put("ticketId", move.ticketId)
            })
        }
    })
    return root.toString()
}

fun parseBook(raw: String): BookState {
    val root = JSONObject(raw)
    val settingsJson = root.optJSONObject("settings") ?: JSONObject()
    val settings = Settings(
        code = settingsJson.optString("code", "MESA"),
        ownerName = settingsJson.optString("ownerName", "Manicura").ifBlank { "Manicura" },
        ownerPhone = normalizePhone(settingsJson.optString("ownerPhone", "")) ?: "",
        studioName = "Studio Nails & Beauty",
        workDays = settingsJson.optJSONArray("workDays")?.toIntList() ?: listOf(2, 3, 4, 5, 6),
        startMin = settingsJson.optInt("startMin", 10 * 60),
        endMin = settingsJson.optInt("endMin", 20 * 60),
        slotMin = settingsJson.optInt("slotMin", 60),
    )
    return BookState(
        mode = root.optString("mode").toRole(),
        settings = settings,
        appointments = root.optJSONArray("appointments").toList { item ->
            Appointment(
                id = item.getString("id"),
                date = item.getString("date"),
                time = item.getString("time"),
                name = item.optString("name"),
                phone = normalizePhone(item.optString("phone")) ?: "",
                serviceId = item.optString("serviceId").ifBlank { null },
                paid = item.optBoolean("paid"),
                status = item.optString("status", "booked"),
                createdAt = item.optLong("createdAt"),
                createdBy = item.optString("createdBy").toRole(),
                cancelledAt = item.optLong("cancelledAt").takeIf { it > 0 },
                cancelledBy = item.optString("cancelledBy").toRole(),
            )
        }.filterNot { it.id.startsWith("seed-") },
        readIds = root.optJSONArray("readIds").toStringList(),
        hiddenNoticeIds = root.optJSONArray("hiddenNoticeIds").toStringList(),
        hiddenMoveIds = root.optJSONArray("hiddenMoveIds").toStringList(),
        blocked = root.optJSONArray("blocked").toStringList(),
        closedSlots = root.optJSONArray("closedSlots").toStringList(),
        clientName = root.optString("clientName", "").takeUnless { it == "Elena Ruiz" } ?: "",
        clientPhone = normalizePhone(root.optString("clientPhone", "").takeUnless { it == "2145550148" } ?: "") ?: "",
        clients = root.optJSONArray("clients").toList { item ->
            StudioClient(item.getString("id"), item.optString("name"), normalizePhone(item.optString("phone")) ?: "")
        }.filter { it.id !in setOf("elena", "marisol") && it.name.isNotBlank() && it.phone.isNotBlank() },
        tickets = root.optJSONArray("tickets").toList { item ->
            WalletTicket(
                id = item.getString("id"),
                kind = item.optString("kind"),
                cents = item.optInt("cents"),
                phone = normalizePhone(item.optString("phone")) ?: "",
                name = item.optString("name"),
                createdAt = item.optLong("createdAt"),
                usedAt = item.optLong("usedAt").takeIf { it > 0 },
                usedBy = item.optString("usedBy").ifBlank { null },
                usedName = item.optString("usedName").ifBlank { null },
            )
        },
        moves = root.optJSONArray("moves").toList { item ->
            WalletMove(
                id = item.getString("id"),
                phone = normalizePhone(item.optString("phone")) ?: "",
                name = item.optString("name"),
                kind = item.optString("kind"),
                cents = item.optInt("cents"),
                at = item.optLong("at"),
                ticketId = item.optString("ticketId"),
            )
        },
    )
}

private fun String.toRole() = when (this) {
    "manicurist" -> Role.manicurist
    "client" -> Role.client
    else -> null
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).map { getString(it) }
}

private fun JSONArray.toIntList() = (0 until length()).map { getInt(it) }

private fun <T> JSONArray?.toList(map: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return (0 until length()).map { map(getJSONObject(it)) }
}
