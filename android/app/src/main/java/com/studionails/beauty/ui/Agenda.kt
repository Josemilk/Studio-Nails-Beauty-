package com.studionails.beauty.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studionails.beauty.model.Appointment
import com.studionails.beauty.model.BookInput
import com.studionails.beauty.model.BookState
import com.studionails.beauty.model.Role
import com.studionails.beauty.model.Services
import com.studionails.beauty.model.blockDates
import com.studionails.beauty.model.book
import com.studionails.beauty.model.bookedOn
import com.studionails.beauty.model.cancel
import com.studionails.beauty.model.deleteNotice
import com.studionails.beauty.model.clientAppointments
import com.studionails.beauty.model.clockLabel
import com.studionails.beauty.model.dateKey
import com.studionails.beauty.model.dialNumber
import com.studionails.beauty.model.firedNotices
import com.studionails.beauty.model.freeSlots
import com.studionails.beauty.model.fromToday
import com.studionails.beauty.model.headingDate
import com.studionails.beauty.model.isBlocked
import com.studionails.beauty.model.isSlotClosed
import com.studionails.beauty.model.isWorkDay
import com.studionails.beauty.model.longDate
import com.studionails.beauty.model.makeId
import com.studionails.beauty.model.monthDates
import com.studionails.beauty.model.monthName
import com.studionails.beauty.model.monthWeeks
import com.studionails.beauty.model.parseDate
import com.studionails.beauty.model.prettyPhone
import com.studionails.beauty.model.reminderText
import com.studionails.beauty.model.serviceById
import com.studionails.beauty.model.setClosed
import com.studionails.beauty.model.slotTimes
import com.studionails.beauty.model.stamp
import com.studionails.beauty.model.timeToMinutes
import com.studionails.beauty.model.turnosLabel
import com.studionails.beauty.model.weekOf
import com.studionails.beauty.model.weekdayName
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CitasScreen(vm: StudioViewModel, serviceId: String?) {
    val book = vm.book
    val role = book.mode ?: Role.client
    val today = LocalDate.now()
    val todayKey = dateKey(today)
    var cursor by remember { mutableStateOf(YearMonth.from(today)) }
    var day by remember { mutableStateOf<String?>(null) }
    var picking by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf(setOf<String>()) }
    var note by remember { mutableStateOf("") }
    var showPast by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val notices = firedNotices(book, role, now)
    var pendingNoticeDelete by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(notices.map { it.id }, book.readIds) {
        val ids = notices.map { it.id }
        if (ids.any { it !in book.readIds }) vm.commit(book.copy(readIds = (book.readIds + ids).distinct()))
    }
    val maxMonth = YearMonth.from(today).plusMonths(8)
    val weeks = monthWeeks(cursor.year, cursor.monthValue)
    val monthFuture = fromToday(monthDates(cursor.atDay(1)), todayKey)
    val monthShut = monthFuture.isNotEmpty() && monthFuture.all { isBlocked(book, it) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Citas", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
        Text(
            if (role == Role.client) "Elige un día y un horario libre." else "Toca un día para ver clientas o editar turnos.",
            color = Soft,
            fontFamily = Sans,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (note.isNotEmpty()) {
            Text(note, modifier = Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Peach).padding(14.dp), fontFamily = Sans)
        }
        notices.take(2).forEach { notice ->
            Column(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Card).border(1.dp, Line, RoundedCornerShape(16.dp)).combinedClickable(onClick = {}, onLongClick = { pendingNoticeDelete = notice.id }).padding(14.dp)) {
                Text(notice.title, fontWeight = FontWeight.SemiBold, fontFamily = Sans, fontSize = 13.sp)
                Text(notice.body, fontFamily = Sans)
                if (pendingNoticeDelete == notice.id) {
                    Text("Eliminar notificación", modifier = Modifier.clickable {
                        vm.commit(deleteNotice(vm.book, notice.id))
                        pendingNoticeDelete = null
                    }.padding(top = 8.dp), color = Accent, fontWeight = FontWeight.Bold, fontFamily = Sans)
                }
            }
        }
        Row(Modifier.padding(top = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.Outlined.ChevronLeft, "Mes anterior", cursor > YearMonth.from(today)) {
                cursor = cursor.minusMonths(1)
                day = null
                picked = emptySet()
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(monthName(cursor.monthValue), fontFamily = Display, fontSize = 28.sp)
                Text(cursor.year.toString(), color = Soft, fontFamily = Sans, fontSize = 13.sp)
            }
            RoundIcon(Icons.Outlined.ChevronRight, "Mes siguiente", cursor < maxMonth) {
                cursor = cursor.plusMonths(1)
                day = null
                picked = emptySet()
            }
        }
        if (role == Role.manicurist) {
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(if (monthShut) "Abrir mes" else "Bloquear mes", false) {
                    vm.commit(blockDates(book, monthFuture, !monthShut))
                }
                ChoiceChip(if (picking) "Listo" else "Varios días", picking) {
                    picking = !picking
                    picked = emptySet()
                    day = null
                }
            }
        }
        val selected = day
        if (selected != null) {
            DayPanel(vm, selected, serviceId, role) {
                day = null
                note = it
            }
        } else {
            val groups = weeks.map { week -> week.filter { date -> YearMonth.from(parseDate(date)) == cursor } }.filter { it.isNotEmpty() }
            val past = groups.filter { visible -> visible.all { it < todayKey } }
            val current = groups.filter { visible -> visible.any { it >= todayKey } }
            val shown = if (showPast) past + current else current
            if (past.isNotEmpty()) {
                Text(
                    if (showPast) "Ocultar días anteriores" else "Ver días anteriores",
                    modifier = Modifier.padding(top = 16.dp).clickable { showPast = !showPast },
                    color = Soft,
                    fontFamily = Sans,
                )
            }
            shown.forEach { visible ->
                val week = weekOf(visible.first())
                val futureWeek = fromToday(week, todayKey)
                val weekShut = futureWeek.isNotEmpty() && futureWeek.all { isBlocked(book, it) }
                Row(Modifier.padding(top = 18.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("SEMANA DEL ${visible.first().takeLast(2).trimStart('0')}", color = Soft, fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    if (role == Role.manicurist) {
                        Text(
                            if (weekShut) "Abrir semana" else "Bloquear semana",
                            modifier = Modifier.clickable { vm.commit(blockDates(book, futureWeek, !weekShut)) },
                            fontFamily = Sans,
                            fontSize = 13.sp,
                        )
                    }
                }
                visible.forEach { date ->
                    val count = freeSlots(book, date, now).size
                    val booked = bookedFor(book, role, date)
                    val pastDay = date < todayKey
                    val marked = picked.contains(date)
                    Row(
                        Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (marked) Peach else Card).border(1.dp, if (marked) Accent else Line, RoundedCornerShape(16.dp)).clickable {
                            if (picking) {
                                picked = if (marked) picked - date else picked + date
                            } else {
                                day = date
                                note = ""
                            }
                        }.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(if (date == todayKey) "Hoy" else weekdayName(date), color = Soft, fontFamily = Sans, fontSize = 13.sp)
                            Text(date.takeLast(2).trimStart('0'), fontFamily = Display, fontSize = 32.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(dayPhrase(book, date, pastDay, count), fontFamily = Sans, textAlign = TextAlign.End)
                            if (booked.isNotEmpty()) {
                                Text(
                                    if (role == Role.client) "Tu cita · ${clockLabel(booked.first().time)}" else if (booked.size == 1) "1 cita" else "${booked.size} citas",
                                    color = Soft,
                                    fontFamily = Sans,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }
            }
            if (picking) {
                Spacer(Modifier.height(12.dp))
                PrimaryButton(if (picked.size == 1) "Bloquear 1 día" else "Bloquear ${picked.size} días", enabled = picked.isNotEmpty()) {
                    vm.commit(blockDates(book, picked.toList(), true))
                    picked = emptySet()
                    picking = false
                }
                Text(
                    "Desbloquear selección",
                    modifier = Modifier.padding(top = 8.dp).clickable(enabled = picked.isNotEmpty()) {
                        vm.commit(blockDates(book, picked.toList(), false))
                        picked = emptySet()
                    },
                    color = Soft,
                    fontFamily = Sans,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DayPanel(vm: StudioViewModel, day: String, serviceId: String?, role: Role, onClose: (String) -> Unit) {
    val book = vm.book
    val now = System.currentTimeMillis()
    val todayKey = dateKey(LocalDate.now())
    var time by remember(day) { mutableStateOf("") }
    var service by remember(day) { mutableStateOf(serviceId ?: "clasica") }
    var name by remember(day) { mutableStateOf(if (role == Role.client) book.clientName else "") }
    var phone by remember(day) { mutableStateOf(if (role == Role.client) prettyPhone(book.clientPhone) else "") }
    var error by remember(day) { mutableStateOf("") }
    val open = freeSlots(book, day, now)
    val blocked = isBlocked(book, day)
    val past = day < todayKey
    val times = slotTimes(book.settings)
    val week = fromToday(weekOf(day), todayKey)
    val month = fromToday(monthDates(parseDate(day).withDayOfMonth(1)), todayKey)
    val weekShut = week.isNotEmpty() && week.all { isBlocked(book, it) }
    val monthShut = month.isNotEmpty() && month.all { isBlocked(book, it) }

    Column(Modifier.padding(top = 18.dp)) {
        Row(Modifier.clickable { onClose("") }, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = Accent)
            Text(" Cerrar", color = Accent, fontFamily = Sans, fontWeight = FontWeight.Bold)
        }
        Text(headingDate(day).replaceFirstChar { it.uppercase() }, fontFamily = Display, fontSize = 28.sp, modifier = Modifier.padding(top = 8.dp))
        Text(
            when {
                blocked -> "Día bloqueado"
                past -> "Este día ya pasó."
                else -> dayPhrase(book, day, past, open.size)
            },
            color = Soft,
            fontFamily = Sans,
        )
        if (role == Role.client) {
            bookedFor(book, Role.client, day).forEach { item ->
                Row(
                    Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Card).border(1.dp, Line, RoundedCornerShape(16.dp)).clickable {
                        vm.open(Screen.Detail(item.id, "citas"))
                    }.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(serviceById(item.serviceId)?.name ?: "Tu cita", fontWeight = FontWeight.SemiBold, fontFamily = Sans)
                        Text(clockLabel(item.time), color = Soft, fontFamily = Sans, fontSize = 13.sp)
                    }
                    Text("Ver detalles", fontWeight = FontWeight.SemiBold, fontFamily = Sans, fontSize = 13.sp)
                }
            }
        }
        if (role == Role.manicurist) {
            GhostButton(if (blocked) "Desbloquear este día" else "Bloquear este día") { vm.commit(blockDates(book, listOf(day), !blocked)) }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(if (weekShut) "Abrir semana" else "Bloquear semana", false) { vm.commit(blockDates(book, week, !weekShut)) }
                ChoiceChip(if (monthShut) "Abrir mes" else "Bloquear mes", false) { vm.commit(blockDates(book, month, !monthShut)) }
            }
            times.forEach { slot ->
                val cita = bookedOn(book, day).find { rangesHit(book, it, slot) }
                val closed = isSlotClosed(book, day, slot) || blocked
                val gone = past || stamp(day, slot) <= now
                Column(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Card).border(1.dp, Line, RoundedCornerShape(16.dp)).padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(clockLabel(slot), fontFamily = Display, fontSize = 24.sp)
                            Text(
                                when {
                                    cita != null -> cita.name
                                    closed -> "Cerrado"
                                    gone -> "Pasado"
                                    else -> "Libre"
                                },
                                color = Soft,
                                fontFamily = Sans,
                                fontSize = 13.sp,
                            )
                        }
                        if (cita != null) {
                            Text(
                                "Ver clienta",
                                modifier = Modifier.clip(RoundedCornerShape(100)).background(Accent).clickable { vm.open(Screen.Detail(cita.id, "citas")) }.padding(horizontal = 14.dp, vertical = 10.dp),
                                color = OnAccent,
                                fontFamily = Sans,
                                fontWeight = FontWeight.SemiBold,
                            )
                        } else if (!gone && !blocked) {
                            Text(
                                if (isSlotClosed(book, day, slot)) "Abrir" else "Cerrar",
                                modifier = Modifier.clip(RoundedCornerShape(100)).border(1.dp, Line, RoundedCornerShape(100)).clickable {
                                    vm.commit(setClosed(book, day, slot, !isSlotClosed(book, day, slot)))
                                }.padding(horizontal = 14.dp, vertical = 10.dp),
                                fontFamily = Sans,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    if (cita == null && !closed && !gone) {
                        Text(
                            if (time == slot) "Ocultar" else "Agendar aquí",
                            modifier = Modifier.padding(top = 8.dp).clickable { time = if (time == slot) "" else slot },
                            fontFamily = Sans,
                        )
                        if (time == slot) {
                            BookFields(service, name, phone, error, "Agendar clienta", { service = it }, { name = it }, { phone = it }) {
                                submitBook(vm, book, role, day, slot, name, phone, service) { error = it; if (it.isEmpty()) onClose("Cita agendada.") }
                            }
                        }
                    }
                }
            }
        } else {
            if (blocked) Text("Este día está cerrado.", color = Soft, fontFamily = Sans, fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp))
            else if (!past && open.isEmpty()) Text("No quedan turnos este día.", color = Soft, fontFamily = Sans, fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp))
            open.forEach { slot ->
                Text(
                    clockLabel(slot),
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (time == slot) Ink else Card).border(1.dp, if (time == slot) Ink else Line, RoundedCornerShape(16.dp)).clickable {
                        time = slot
                        error = ""
                    }.padding(vertical = 14.dp),
                    color = if (time == slot) Paper else Ink,
                    fontFamily = Display,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                )
            }
            if (time.isNotEmpty()) {
                BookFields(service, name, phone, error, "Agendar turno", { service = it }, { name = it }, { phone = it }) {
                    submitBook(vm, book, role, day, time, name, phone, service) { message ->
                        error = message
                        if (message.isEmpty()) onClose("Listo. Se avisó a la manicura y el turno ya no está libre.")
                    }
                }
            }
        }
    }
}

private fun submitBook(
    vm: StudioViewModel,
    book: BookState,
    role: Role,
    day: String,
    time: String,
    name: String,
    phone: String,
    service: String,
    done: (String) -> Unit,
) {
    val result = book(
        book,
        BookInput(day, time, name, phone, service, makeId(), System.currentTimeMillis(), role),
    )
    result.onSuccess { next ->
        vm.commit(
            if (role == Role.client) next.copy(clientName = name.trim(), clientPhone = com.studionails.beauty.model.digits(phone)) else next,
        )
        done("")
    }.onFailure { done(it.message ?: "No se pudo agendar.") }
}

@Composable
private fun BookFields(
    service: String,
    name: String,
    phone: String,
    error: String,
    action: String,
    onService: (String) -> Unit,
    onName: (String) -> Unit,
    onPhone: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(Modifier.padding(top = 12.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Services.forEach { item -> ChoiceChip(item.name, service == item.id) { onService(item.id) } }
        }
        Spacer(Modifier.height(12.dp))
        PaperField("Nombre", name, onName)
        Spacer(Modifier.height(8.dp))
        PaperField("Teléfono", phone, onPhone, KeyboardType.Phone)
        if (error.isNotEmpty()) Text(error, fontFamily = Sans, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(12.dp))
        PrimaryButton(action, onClick = onSubmit)
    }
}

@Composable
fun DetailScreen(vm: StudioViewModel, id: String, back: String) {
    val book = vm.book
    val role = book.mode ?: Role.client
    val cita = book.appointments.find { it.id == id && it.status == "booked" }
    var confirming by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        if (cita == null) {
            ScreenTitle("Cita") { vm.open(if (back == "citas") Screen.Citas() else Screen.Home) }
            Text("Esta cita ya no está en la agenda.", color = Soft, fontFamily = Sans, fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp))
            return@Column
        }
        val service = serviceById(cita.serviceId)
        ScreenTitle(if (role == Role.manicurist) cita.name else (service?.name ?: "Cita")) {
            vm.open(if (back == "citas") Screen.Citas() else Screen.Home)
        }
        Text(clockLabel(cita.time), fontFamily = Display, fontSize = 48.sp, modifier = Modifier.padding(top = 20.dp))
        Text(longDate(cita.date), fontFamily = Sans, fontSize = 18.sp)
        if (role == Role.manicurist) {
            Text("${service?.name ?: "Cita"} · ${prettyPhone(cita.phone)} · $${service?.price ?: 0}", color = Soft, fontFamily = Sans)
            Spacer(Modifier.height(20.dp))
            ContactRow(Icons.Outlined.Phone, "Llamar") { openLink(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:+${dialNumber(cita.phone)}"))) }
            ContactRow(Icons.Outlined.Message, "Mensaje") {
                openLink(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:+${dialNumber(cita.phone)}")).putExtra("sms_body", reminderText(cita)))
            }
            ContactRow(Icons.Outlined.Message, "WhatsApp") {
                val text = Uri.encode(reminderText(cita))
                openLink(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${dialNumber(cita.phone)}?text=$text")))
            }
        } else {
            Text(book.settings.studioName, color = Soft, fontFamily = Sans)
            Text("Si cancelas, la manicura recibe el aviso y el turno vuelve a quedar libre.", color = Soft, fontFamily = Sans, modifier = Modifier.padding(top = 16.dp))
        }
        Spacer(Modifier.height(28.dp))
        if (role == Role.manicurist) {
            GhostButton("Agendar otra") { vm.open(Screen.Citas(cita.serviceId)) }
            Spacer(Modifier.height(8.dp))
        }
        if (confirming) {
            PrimaryButton("Confirmar cancelación") {
                val current = vm.book.mode ?: role
                vm.commit(cancel(vm.book, cita.id, current, System.currentTimeMillis()))
                vm.open(if (back == "citas") Screen.Citas() else Screen.Home)
            }
        } else {
            Text("Cancelar cita", modifier = Modifier.padding(top = 8.dp).clickable { confirming = true }, color = Soft, fontFamily = Sans)
        }
    }
}

@Composable
private fun ContactRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.padding(top = 8.dp).fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(Card).border(1.dp, Line, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Ink)
        Text("  $label", fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(100)).background(Card).border(1.dp, Line, RoundedCornerShape(100)).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = if (enabled) Ink else Faint)
    }
}

private fun bookedFor(book: BookState, role: Role, day: String): List<Appointment> {
    val source = if (role == Role.client) clientAppointments(book) else book.appointments.filter { it.status == "booked" }
    return source.filter { it.date == day }.sortedBy { timeToMinutes(it.time) }
}

private fun rangesHit(book: BookState, appointment: Appointment, slot: String): Boolean {
    val start = timeToMinutes(slot)
    val length = book.settings.slotMin
    val other = timeToMinutes(appointment.time)
    val span = serviceById(appointment.serviceId)?.minutes ?: length
    return start < other + span && other < start + length
}

private fun dayPhrase(book: BookState, date: String, past: Boolean, count: Int): String {
    if (past) return "Pasado"
    if (!isWorkDay(date, book.settings) || isBlocked(book, date)) return "Cerrado"
    if (count == 0) return "Sin turnos disponibles"
    return turnosLabel(count)
}

private fun openLink(context: android.content.Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        /* el teléfono no tiene esa aplicación */
    }
}
