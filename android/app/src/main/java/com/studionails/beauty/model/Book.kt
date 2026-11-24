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
    val ownerName: String = "Ana",
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
    val expiresAt: Long = 0,
    val nonce: String = "",
)

data class WalletMove(
    val id: String,
    val phone: String,
    val name: String,
    val kind: String,
    val cents: Int,
    val at: Long,
    val ticketId: String,
    val requestId: String = "",
)

data class HeldRedeem(
    val requestId: String,
    val id: String,
    val kind: String,
    val cents: Int,
    val expiresAt: Long,
    val nonce: String,
    val host: String,
    val phone: String,
    val name: String,
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
    val clientName: String = "Elena Ruiz",
    val clientPhone: String = "2145550148",
    val clients: List<StudioClient> = emptyList(),
    val tickets: List<WalletTicket> = emptyList(),
    val moves: List<WalletMove> = emptyList(),
    val confirmedBalance: Int = 0,
    val deskHost: String = "",
    val heldRedeem: HeldRedeem? = null,
)

private val Months = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)
private val Weekdays = listOf("domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado")
private val Hour = 60 * 60 * 1000L
private val Minute = 60 * 1000L
private val random = SecureRandom()

fun digits(phone: String) = phone.filter { it.isDigit() }

fun phoneKey(phone: String): String {
    val raw = digits(phone)
    return if (raw.length == 11 && raw.startsWith("1")) raw.drop(1) else raw
}

fun samePhone(a: String, b: String): Boolean {
    val left = phoneKey(a)
    val right = phoneKey(b)
    return left.length >= 10 && left == right
}

fun prettyPhone(phone: String): String {
    val raw = digits(phone)
    val n = if (raw.length == 11 && raw.startsWith("1")) raw.drop(1) else raw
    if (n.length != 10) return phone.trim()
    return "(${n.take(3)}) ${n.substring(3, 6)} ${n.substring(6)}"
}

fun dialNumber(phone: String): String {
    val d = digits(phone)
    return if (d.length == 10) "1$d" else d
}

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
    val phone = digits(input.phone)
    val service = serviceById(input.serviceId) ?: return Result.failure(IllegalArgumentException("Elige un servicio."))
    if (input.time.isBlank()) return Result.failure(IllegalArgumentException("Elige una hora."))
    if (name.length < 2) return Result.failure(IllegalArgumentException("Escribe el nombre."))
    if (phone.length < 10 || phone.length > 15) return Result.failure(IllegalArgumentException("El teléfono necesita 10 dígitos."))
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
    val cleanPhone = digits(phone)
    if (cleanName.length < 2) return Result.failure(IllegalArgumentException("Escribe el nombre."))
    if (cleanPhone.length < 10 || cleanPhone.length > 15) return Result.failure(IllegalArgumentException("El teléfono necesita 10 dígitos."))
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
    return notices
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
    val key = phoneKey(phone)
    if (key.isEmpty()) return 0
    return state.moves.fold(0) { sum, move ->
        if (phoneKey(move.phone) != key) sum else sum + if (move.kind == "topup") move.cents else -move.cents
    }
}

fun movesFor(state: BookState, phone: String? = null) = state.moves
    .filter { phone == null || phoneKey(it.phone) == phoneKey(phone) }
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
    val cleanPhone = phoneKey(phone)
    if (cleanName.length < 2) return Result.failure(IllegalArgumentException("Escribe el nombre."))
    if (cleanPhone.length < 10 || cleanPhone.length > 15) return Result.failure(IllegalArgumentException("El teléfono necesita 10 dígitos."))
    if (state.clients.any { phoneKey(it.phone) == cleanPhone }) return Result.failure(IllegalArgumentException("Esa clienta ya está en la lista."))
    return Result.success(state.copy(clients = state.clients + StudioClient(makeId(), cleanName, cleanPhone)))
}

fun removeClient(state: BookState, id: String) = state.copy(clients = state.clients.filter { it.id != id })

fun createTicket(state: BookState, kind: String, cents: Int, phone: String, name: String, now: Long): Result<Pair<BookState, WalletTicket>> {
    return Result.failure(IllegalArgumentException("El código solo se crea en el teléfono de la manicura."))
}

fun redeemTicket(state: BookState, raw: String, expected: String, phone: String, name: String, now: Long): Result<Pair<BookState, Int>> {
    return Result.failure(IllegalArgumentException("El saldo solo lo confirma el teléfono de la manicura."))
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

fun seed(now: LocalDateTime = LocalDateTime.now()): BookState {
    val settings = Settings()
    val zone = ZoneId.systemDefault()
    val createdEarly = now.minusDays(1).withHour(9).withMinute(0).atZone(zone).toInstant().toEpochMilli()
    fun nextWork(offset: Long): LocalDate {
        var day = now.toLocalDate().plusDays(offset)
        repeat(21) {
            if (settings.workDays.contains(jsDay(day))) return day
            day = day.plusDays(1)
        }
        return day
    }
    val soon = nextWork(1)
    val later = nextWork(3)
    val laterKey = if (dateKey(later) == dateKey(soon)) dateKey(nextWork(5)) else dateKey(later)
    val appointments = mutableListOf(
        Appointment("seed-elena-past", dateKey(now.toLocalDate().minusDays(1)), "15:00", "Elena Ruiz", "2145550148", "clasica", true, "booked", createdEarly - 24 * Hour),
        Appointment("seed-elena-soon", dateKey(soon), "11:00", "Elena Ruiz", "2145550148", "clasica", false, "booked", createdEarly),
        Appointment("seed-marisol-soon", dateKey(soon), "16:00", "Marisol Vega", "9725550172", "art", true, "booked", createdEarly),
        Appointment("seed-elena-later", laterKey, "12:00", "Elena Ruiz", "2145550148", "cuidado", false, "booked", createdEarly),
    )
    if (settings.workDays.contains(jsDay(now.toLocalDate()))) {
        val nowMin = now.hour * 60 + now.minute
        val near = slotTimes(settings).minByOrNull { time ->
            val delta = timeToMinutes(time) - nowMin
            when {
                delta in 15..100 -> delta
                delta in -50..-5 -> 200 + abs(delta)
                else -> Int.MAX_VALUE
            }
        }
        if (near != null && timeToMinutes(near).let { delta ->
                val d = delta - nowMin
                d in 15..100 || d in -50..-5
            }
        ) {
            appointments += Appointment("seed-elena-near", dateKey(now.toLocalDate()), near, "Elena Ruiz", "2145550148", "gel", false, "booked", createdEarly)
        }
    }
    return BookState(
        settings = settings,
        appointments = appointments,
        clientName = "Elena Ruiz",
        clientPhone = "2145550148",
        clients = listOf(
            StudioClient("elena", "Elena Ruiz", "2145550148"),
            StudioClient("marisol", "Marisol Vega", "9725550172"),
        ),
    )
}

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
                put("expiresAt", ticket.expiresAt)
                put("nonce", ticket.nonce)
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
                put("requestId", move.requestId)
            })
        }
    })
    root.put("confirmedBalance", confirmedBalance)
    root.put("deskHost", deskHost)
    heldRedeem?.let { held ->
        root.put("heldRedeem", JSONObject().apply {
            put("requestId", held.requestId)
            put("id", held.id)
            put("kind", held.kind)
            put("cents", held.cents)
            put("expiresAt", held.expiresAt)
            put("nonce", held.nonce)
            put("host", held.host)
            put("phone", held.phone)
            put("name", held.name)
        })
    }
    return root.toString()
}

fun parseBook(raw: String): BookState {
    val root = JSONObject(raw)
    val settingsJson = root.optJSONObject("settings") ?: JSONObject()
    val settings = Settings(
        code = settingsJson.optString("code", "MESA"),
        ownerName = settingsJson.optString("ownerName", "Ana").ifBlank { "Ana" },
        ownerPhone = settingsJson.optString("ownerPhone", ""),
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
                phone = item.optString("phone"),
                serviceId = item.optString("serviceId").ifBlank { null },
                paid = item.optBoolean("paid"),
                status = item.optString("status", "booked"),
                createdAt = item.optLong("createdAt"),
                createdBy = item.optString("createdBy").toRole(),
                cancelledAt = item.optLong("cancelledAt").takeIf { it > 0 },
                cancelledBy = item.optString("cancelledBy").toRole(),
            )
        },
        readIds = root.optJSONArray("readIds").toStringList(),
        blocked = root.optJSONArray("blocked").toStringList(),
        closedSlots = root.optJSONArray("closedSlots").toStringList(),
        clientName = root.optString("clientName", "Elena Ruiz"),
        clientPhone = root.optString("clientPhone", "2145550148"),
        clients = root.optJSONArray("clients").toList { item ->
            StudioClient(item.getString("id"), item.optString("name"), digits(item.optString("phone")))
        }.filter { it.name.isNotBlank() && it.phone.isNotBlank() },
        tickets = root.optJSONArray("tickets").toList { item ->
            WalletTicket(
                id = item.getString("id"),
                kind = item.optString("kind"),
                cents = item.optInt("cents"),
                phone = item.optString("phone"),
                name = item.optString("name"),
                createdAt = item.optLong("createdAt"),
                usedAt = item.optLong("usedAt").takeIf { it > 0 },
                usedBy = item.optString("usedBy").ifBlank { null },
                usedName = item.optString("usedName").ifBlank { null },
                expiresAt = item.optLong("expiresAt"),
                nonce = item.optString("nonce"),
            )
        },
        moves = root.optJSONArray("moves").toList { item ->
            WalletMove(
                id = item.getString("id"),
                phone = item.optString("phone"),
                name = item.optString("name"),
                kind = item.optString("kind"),
                cents = item.optInt("cents"),
                at = item.optLong("at"),
                ticketId = item.optString("ticketId"),
                requestId = item.optString("requestId"),
            )
        },
        confirmedBalance = root.optInt("confirmedBalance"),
        deskHost = root.optString("deskHost"),
        heldRedeem = root.optJSONObject("heldRedeem")?.toHeld(),
    )
}

private fun JSONObject.toHeld(): HeldRedeem? {
    val requestId = optString("requestId")
    val id = optString("id")
    if (requestId.isBlank() || id.isBlank()) return null
    return HeldRedeem(
        requestId = requestId,
        id = id,
        kind = optString("kind"),
        cents = optInt("cents"),
        expiresAt = optLong("expiresAt"),
        nonce = optString("nonce"),
        host = optString("host"),
        phone = optString("phone"),
        name = optString("name"),
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
