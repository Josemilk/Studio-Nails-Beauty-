package com.studionails.beauty.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studionails.beauty.model.Role
import com.studionails.beauty.model.Services
import com.studionails.beauty.model.appointmentMinutes
import com.studionails.beauty.model.clockLabel
import com.studionails.beauty.model.phoneKey
import com.studionails.beauty.model.firedNotices
import com.studionails.beauty.model.longDate
import com.studionails.beauty.model.minutesToTime
import com.studionails.beauty.model.nextAppointment
import com.studionails.beauty.model.prettyPhone
import com.studionails.beauty.model.relativeDay
import com.studionails.beauty.model.saveProfile
import com.studionails.beauty.model.serviceById
import com.studionails.beauty.model.slotTimes
import com.studionails.beauty.model.stamp
import com.studionails.beauty.model.unreadCount
import java.time.LocalDate

@Composable
fun StudioRoot(vm: StudioViewModel) {
    val screen = vm.screen
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(vm.book.mode) {
        if (vm.book.mode != null && Build.VERSION.SDK_INT >= 33) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val showNav = screen is Screen.Home || screen is Screen.Citas || screen is Screen.Wallet || screen is Screen.History
    Box(Modifier.fillMaxSize().background(Paper), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 520.dp).fillMaxHeight().statusBarsPadding()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (screen) {
                    Screen.Gate -> GateScreen(vm)
                    Screen.Home -> HomeScreen(vm)
                    is Screen.Citas -> CitasScreen(vm, screen.serviceId)
                    Screen.Wallet -> WalletHome(vm)
                    Screen.History -> HistoryScreen(vm)
                    Screen.Services -> ServicesScreen(vm)
                    is Screen.Detail -> DetailScreen(vm, screen.id, screen.back)
                    Screen.Settings -> SettingsScreen(vm)
                    Screen.Hours -> HoursScreen(vm)
                    is Screen.Amount -> AmountScreen(vm, screen.kind, screen.clientId)
                    is Screen.Qr -> QrScreen(vm, screen.ticketId)
                    is Screen.Scan -> ScanScreen(vm, screen.kind)
                    is Screen.Client -> ClientScreen(vm, screen.id)
                    Screen.AddClient -> AddClientScreen(vm)
                }
            }
            if (showNav) {
                BottomNav(
                    tab = when (screen) {
                        is Screen.Citas -> "citas"
                        Screen.Wallet -> "wallet"
                        Screen.History -> "history"
                        else -> "home"
                    },
                    badge = unreadCount(vm.book, System.currentTimeMillis()),
                    onTab = { tab ->
                        vm.open(
                            when (tab) {
                                "citas" -> Screen.Citas()
                                "wallet" -> Screen.Wallet
                                "history" -> Screen.History
                                else -> Screen.Home
                            },
                        )
                    },
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
        }
    }
}

@Composable
private fun GateScreen(vm: StudioViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        StudioPhoto("mesa.jpg", Modifier.fillMaxWidth().height(280.dp), "Manicura natural sobre lino")
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Studio Nails & Beauty", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
            Text("La agenda del estudio. Sin cuentas.", color = Soft, fontFamily = Sans, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            PrimaryButton("Soy la manicura") { vm.choose(Role.manicurist) }
            GhostButton("Soy clienta") { vm.choose(Role.client) }
        }
    }
}

@Composable
private fun HomeScreen(vm: StudioViewModel) {
    val book = vm.book
    val role = book.mode ?: Role.client
    var query by remember { mutableStateOf("") }
    val today = LocalDate.now()
    val now = System.currentTimeMillis()
    val name = if (role == Role.client) book.clientName.trim().substringBefore(" ").ifBlank { "Ana" } else book.settings.ownerName
    val q = query.trim().lowercase()
    val services = Services.filter { q.isEmpty() || it.name.lowercase().contains(q) }
    val cards = if (q.isEmpty()) services.take(2) else services
    val people = if (role == Role.manicurist && q.isNotEmpty()) {
        book.appointments.filter { it.status == "booked" && it.name.lowercase().contains(q) }
    } else emptyList()
    val next = nextAppointment(book, now, role)
    val notice = firedNotices(book, role, now).firstOrNull { it.id !in book.readIds }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text("Hola, $name 👋", style = androidx.compose.material3.MaterialTheme.typography.displayLarge, modifier = Modifier.weight(1f))
            IconAction(Icons.Outlined.Settings, "Ajustes") { vm.open(Screen.Settings) }
        }
        Text("¿Qué servicio necesitas hoy?", color = Soft, fontSize = 20.sp, fontFamily = Sans, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(16.dp))
        OutlinedSearch(query) { query = it }
        if (notice != null) {
            Column(
                Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Peach).clickable { vm.open(Screen.Citas()) }.padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = Ink, modifier = Modifier.size(18.dp))
                    Text("  ${notice.title}", fontWeight = FontWeight.SemiBold, fontFamily = Sans)
                }
                Text(notice.body, fontFamily = Sans, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Row(Modifier.padding(top = 28.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Servicios recomendados", fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("Ver todo", modifier = Modifier.clickable { vm.open(Screen.Services) }, fontFamily = Sans, fontSize = 16.sp)
        }
        if (cards.isEmpty() && people.isEmpty()) {
            Text("Nada coincide con tu búsqueda.", color = Soft, fontFamily = Sans, fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp))
        }
        Row(Modifier.padding(top = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            cards.take(2).forEach { service ->
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(Card).border(1.dp, Line, RoundedCornerShape(24.dp)).clickable {
                        vm.open(Screen.Citas(service.id))
                    }.padding(12.dp),
                ) {
                    StudioPhoto(service.photo, Modifier.fillMaxWidth().height(96.dp))
                    Text(service.name, fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(top = 10.dp))
                    Text("${service.minutesLabel} · $${service.price}", color = Soft, fontFamily = Sans, fontSize = 13.sp)
                }
            }
        }
        people.forEach { item ->
            Row(
                Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Card).border(1.dp, Line, RoundedCornerShape(16.dp)).clickable {
                    vm.open(Screen.Detail(item.id, "home"))
                }.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(item.name, fontWeight = FontWeight.SemiBold, fontFamily = Sans)
                    Text("${relativeDay(item.date, today)} · ${clockLabel(item.time)}", color = Soft, fontFamily = Sans, fontSize = 13.sp)
                }
                Text(serviceById(item.serviceId)?.name ?: "", color = Soft, fontFamily = Sans, fontSize = 13.sp)
            }
        }
        Text("Próxima cita", fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.padding(top = 28.dp))
        if (next == null) {
            Text("No hay citas por delante.", color = Soft, fontFamily = Sans, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
        } else {
            val service = serviceById(next.serviceId)
            Row(
                Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PaperDeep).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Peach), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Accent)
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(service?.name ?: "Cita", fontWeight = FontWeight.SemiBold, fontFamily = Sans, fontSize = 16.sp)
                    Text(
                        "${relativeDay(next.date, today)} · ${clockLabel(next.time)} · ${if (role == Role.client) book.settings.studioName else next.name}",
                        color = Soft,
                        fontFamily = Sans,
                        fontSize = 13.sp,
                    )
                }
                Text(
                    "Ver detalles",
                    modifier = Modifier.clip(RoundedCornerShape(100)).background(Accent).clickable { vm.open(Screen.Detail(next.id, "home")) }.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = OnAccent,
                    fontFamily = Sans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Reservar ahora") { vm.open(Screen.Citas()) }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun OutlinedSearch(value: String, onValue: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Buscar servicios...", color = Faint, fontFamily = Sans) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Faint) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = fieldColors(),
    )
}

@Composable
private fun ServicesScreen(vm: StudioViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        ScreenTitle("Servicios") { vm.open(Screen.Home) }
        Spacer(Modifier.height(16.dp))
        Services.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                row.forEach { service ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(Card).border(1.dp, Line, RoundedCornerShape(24.dp)).clickable {
                            vm.open(Screen.Citas(service.id))
                        }.padding(12.dp),
                    ) {
                        StudioPhoto(service.photo, Modifier.fillMaxWidth().height(96.dp))
                        Text(service.name, fontWeight = FontWeight.SemiBold, fontFamily = Sans, modifier = Modifier.padding(top = 8.dp))
                        Text("${service.minutesLabel} · $${service.price}", color = Soft, fontSize = 13.sp, fontFamily = Sans)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HistoryScreen(vm: StudioViewModel) {
    val book = vm.book
    val role = book.mode ?: Role.client
    val now = System.currentTimeMillis()
    val source = if (role == Role.client) book.appointments.filter { phoneKey(it.phone) == phoneKey(book.clientPhone) } else book.appointments
    val rows = source.filter { it.status == "cancelled" || stamp(it.date, it.time) + appointmentMinutes(book, it) * 60_000 < now }
        .sortedByDescending { stamp(it.date, it.time) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Historial", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
        if (rows.isEmpty()) Text("Todavía no hay citas pasadas.", color = Soft, fontFamily = Sans, fontSize = 18.sp, modifier = Modifier.padding(top = 16.dp))
        rows.forEach { item ->
            Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Card).border(1.dp, Line, RoundedCornerShape(24.dp)).padding(16.dp)) {
                Text(serviceById(item.serviceId)?.name ?: "Cita", fontWeight = FontWeight.SemiBold, fontFamily = Sans, fontSize = 18.sp)
                Text("${item.name} · ${longDate(item.date)} · ${clockLabel(item.time)}", color = Soft, fontFamily = Sans)
                Text(
                    when {
                        item.status == "cancelled" -> "Cancelada"
                        item.paid -> "Pagada"
                        else -> "Sin cobrar"
                    },
                    color = Soft,
                    fontFamily = Sans,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Text(
            if (role == Role.client) "Entrar como manicura" else "Entrar como clienta",
            modifier = Modifier.padding(top = 28.dp).clickable { vm.choose(if (role == Role.client) Role.manicurist else Role.client) },
            color = Soft,
            fontFamily = Sans,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsScreen(vm: StudioViewModel) {
    val book = vm.book
    val role = book.mode ?: Role.client
    var name by remember { mutableStateOf(if (role == Role.client) book.clientName else book.settings.ownerName) }
    var phone by remember { mutableStateOf(prettyPhone(if (role == Role.client) book.clientPhone else book.settings.ownerPhone)) }
    var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        ScreenTitle("Ajustes") { vm.open(Screen.Home) }
        Text("Este nombre es el que saluda en el inicio.", color = Soft, fontFamily = Sans, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(20.dp))
        PaperField("Nombre", name, { name = it })
        Spacer(Modifier.height(12.dp))
        PaperField("Teléfono", phone, { phone = it }, KeyboardType.Phone)
        if (error.isNotEmpty()) Text(error, fontFamily = Sans, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Guardar") {
            val result = saveProfile(book, role, name, phone)
            result.onSuccess {
                vm.commit(it)
                vm.open(Screen.Home)
            }.onFailure { error = it.message ?: "No se pudo guardar." }
        }
        if (role == Role.manicurist) {
            Spacer(Modifier.height(12.dp))
            GhostButton("Horario de la mesa") { vm.open(Screen.Hours) }
        }
        Text(
            if (role == Role.client) "Entrar como manicura" else "Entrar como clienta",
            modifier = Modifier.padding(top = 28.dp).clickable { vm.choose(if (role == Role.client) Role.manicurist else Role.client) },
            color = Soft,
            fontFamily = Sans,
        )
    }
}

@Composable
private fun HoursScreen(vm: StudioViewModel) {
    var draft by remember { mutableStateOf(vm.book.settings) }
    val starts = listOf(8, 9, 10, 11, 12).map { it * 60 }
    val ends = listOf(16, 17, 18, 19, 20, 21).map { it * 60 }
    val lengths = listOf(30, 45, 60, 90)
    val letters = listOf("D", "L", "M", "X", "J", "V", "S")
    val valid = draft.endMin > draft.startMin && draft.workDays.isNotEmpty() && slotTimes(draft).isNotEmpty()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        ScreenTitle("Horario") { vm.open(Screen.Settings) }
        Text("La clienta solo ve estas horas.", color = Soft, fontFamily = Sans, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            letters.forEachIndexed { index, letter ->
                val on = draft.workDays.contains(index)
                Text(
                    letter,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(100)).background(if (on) Ink else Card).border(1.dp, if (on) Ink else Line, RoundedCornerShape(100)).clickable {
                        val days = if (on) draft.workDays.filter { it != index } else (draft.workDays + index).sorted()
                        draft = draft.copy(workDays = days)
                    }.padding(top = 10.dp),
                    color = if (on) Paper else Soft,
                    fontFamily = Sans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Stepper("Abre", minutesToTime(draft.startMin), starts, draft.startMin) { draft = draft.copy(startMin = it) }
        Stepper("Cierra", minutesToTime(draft.endMin), ends, draft.endMin) { draft = draft.copy(endMin = it) }
        Stepper("Cada cita", "${draft.slotMin} min", lengths, draft.slotMin) { draft = draft.copy(slotMin = it) }
        Spacer(Modifier.height(28.dp))
        PrimaryButton("Guardar horario", enabled = valid) {
            vm.commit(vm.book.copy(settings = draft))
            vm.open(Screen.Settings)
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, options: List<Int>, current: Int, onPick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontFamily = Sans, fontSize = 18.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundStep("−") { step(options, current, -1, onPick) }
            Text(value, modifier = Modifier.padding(horizontal = 12.dp), fontFamily = Sans, fontSize = 18.sp)
            RoundStep("+") { step(options, current, 1, onPick) }
        }
    }
}

private fun step(options: List<Int>, current: Int, dir: Int, onPick: (Int) -> Unit) {
    val next = options.getOrNull(options.indexOf(current) + dir) ?: return
    onPick(next)
}

@Composable
private fun RoundStep(label: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(100)).background(Card).border(1.dp, Line, RoundedCornerShape(100)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 20.sp, fontFamily = Sans)
    }
}

@Composable
private fun BottomNav(tab: String, badge: Int, onTab: (String) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        Triple("home", "Home", Icons.Outlined.Home),
        Triple("citas", "Citas", Icons.Outlined.CalendarMonth),
        Triple("wallet", "Billetera", Icons.Outlined.AccountBalanceWallet),
        Triple("history", "Historial", Icons.Outlined.History),
    )
    Row(
        modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Card).border(1.dp, Line, RoundedCornerShape(28.dp)).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        items.forEach { (id, label, icon) ->
            val active = tab == id
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onTab(id) }.padding(vertical = 4.dp)) {
                Box {
                    Icon(icon as ImageVector, contentDescription = label, tint = if (active) Accent else Faint)
                    if (id == "citas" && badge > 0) {
                        Box(Modifier.align(Alignment.TopEnd).size(8.dp).clip(RoundedCornerShape(100)).background(Accent))
                    }
                }
                Text(label, color = if (active) Accent else Faint, fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
    }
}
