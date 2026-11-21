package com.studionails.beauty.ui

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Add
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.studionails.beauty.model.Role
import com.studionails.beauty.model.addClient
import com.studionails.beauty.model.balanceOf
import com.studionails.beauty.model.digits
import com.studionails.beauty.model.formatMoney
import com.studionails.beauty.model.movesFor
import com.studionails.beauty.model.parseMoney
import com.studionails.beauty.model.prettyPhone
import com.studionails.beauty.model.removeClient
import com.studionails.beauty.model.whenLabel
import com.studionails.beauty.qr.TicketClaim
import com.studionails.beauty.qr.parseTicketQr
import com.studionails.beauty.qr.ticketQr
import java.time.LocalDate
import java.util.UUID
import kotlin.concurrent.thread

@Composable
fun WalletHome(vm: StudioViewModel) {
    val book = vm.book
    val role = book.mode ?: Role.client
    val today = LocalDate.now()
    if (role == Role.client) {
        val balance = if (vm.isDesk()) balanceOf(book, book.clientPhone) else book.confirmedBalance
        val moves = movesFor(book, book.clientPhone).take(8)
        LaunchedEffect(book.deskHost) { vm.refreshBalance() }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Billetera", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("Saldo disponible", color = Soft, fontFamily = Sans, modifier = Modifier.padding(top = 28.dp))
            Text(formatMoney(balance), fontFamily = Display, fontSize = 56.sp)
            Text(
                if (vm.isDesk()) "Este teléfono lleva la caja." else "Último saldo confirmado por la manicura.",
                color = Soft,
                fontFamily = Sans,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(Modifier.padding(top = 24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton("Recargar", Modifier.weight(1f)) { vm.open(Screen.Scan("topup")) }
                GhostButton("Pagar", Modifier.weight(1f)) { vm.open(Screen.Scan("charge")) }
            }
            MoveList("Movimientos", moves.map { it.kind to it }, today, "Todavía no hay recargas ni pagos.")
        }
        return
    }
    val moves = movesFor(book).take(6)
    val openTickets = book.tickets.filter { it.usedAt == null }.take(4)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Billetera", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
        Row(Modifier.padding(top = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton("Recargar", Modifier.weight(1f)) { vm.open(Screen.Amount("topup")) }
            GhostButton("Cobrar", Modifier.weight(1f)) { vm.open(Screen.Amount("charge")) }
        }
        openTickets.forEach { ticket ->
            Row(
                Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Card).border(1.dp, Line, RoundedCornerShape(16.dp)).clickable {
                    vm.open(Screen.Qr(ticket.id))
                }.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("${if (ticket.kind == "topup") "Recarga" else "Cobro"} ${formatMoney(ticket.cents)}", fontWeight = FontWeight.SemiBold, fontFamily = Sans)
                    Text("${ticket.name.ifBlank { "Cualquier clienta" }} · ${ticket.id}", color = Soft, fontFamily = Sans, fontSize = 13.sp)
                }
                Text("Ver QR", fontWeight = FontWeight.SemiBold, fontFamily = Sans, fontSize = 13.sp)
            }
        }
        Row(Modifier.padding(top = 28.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Clientas", fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Row(Modifier.clickable { vm.open(Screen.AddClient) }, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = Ink, modifier = Modifier.size(16.dp))
                Text(" Agregar", fontFamily = Sans, fontWeight = FontWeight.SemiBold)
            }
        }
        if (book.clients.isEmpty()) Text("Todavía no hay fichas.", color = Soft, fontFamily = Sans, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
        book.clients.forEach { client ->
            Row(
                Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Card).border(1.dp, Line, RoundedCornerShape(24.dp)).clickable {
                    vm.open(Screen.Client(client.id))
                }.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(client.name, fontWeight = FontWeight.SemiBold, fontFamily = Sans, fontSize = 18.sp)
                    Text(prettyPhone(client.phone), color = Soft, fontFamily = Sans, fontSize = 13.sp)
                }
                Text(formatMoney(balanceOf(book, client.phone)), fontFamily = Display, fontSize = 24.sp)
            }
        }
        MoveList("Últimos movimientos", moves.map { it.kind to it }, today, "Cuando una clienta escanee un código, queda aquí.")
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun MoveList(title: String, rows: List<Pair<String, com.studionails.beauty.model.WalletMove>>, today: LocalDate, empty: String) {
    Text(title, fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.padding(top = 28.dp).fillMaxWidth())
    if (rows.isEmpty()) Text(empty, color = Soft, fontFamily = Sans, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
    rows.forEach { (_, move) ->
        Row(
            Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Card).border(1.dp, Line, RoundedCornerShape(16.dp)).padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("${if (move.kind == "topup") "Recarga" else "Pago"} · ${move.name}", fontWeight = FontWeight.SemiBold, fontFamily = Sans)
                Text(whenLabel(move.at, today), color = Soft, fontFamily = Sans, fontSize = 13.sp)
            }
            Text("${if (move.kind == "topup") "+" else "−"}${formatMoney(move.cents)}", fontFamily = Display, fontSize = 20.sp)
        }
    }
}

@Composable
fun AmountScreen(vm: StudioViewModel, kind: String, clientId: String?) {
    val book = vm.book
    val locked = book.clients.find { it.id == clientId }
    var amount by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(locked?.phone ?: "") }
    var error by remember { mutableStateOf("") }
    val chosen = locked ?: book.clients.find { it.phone == phone }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        ScreenTitle(if (kind == "topup") "Recargar" else "Cobrar") {
            vm.open(if (locked != null) Screen.Client(locked.id) else Screen.Wallet)
        }
        Text(
            if (kind == "topup") "El valor que escanee la clienta entra a su saldo." else "El valor que pague se descuenta de su saldo.",
            color = Soft,
            fontFamily = Sans,
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        PaperField("Valor en USD", amount, { amount = it }, KeyboardType.Decimal)
        if (locked != null) {
            Text("Para ${locked.name}", fontFamily = Sans, fontSize = 18.sp, modifier = Modifier.padding(top = 16.dp))
        } else {
            Text("Clienta", color = Soft, fontFamily = Sans, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip("Cualquiera", phone.isEmpty()) { phone = "" }
                book.clients.forEach { client ->
                    ChoiceChip(client.name.substringBefore(" "), phone == client.phone) { phone = client.phone }
                }
            }
        }
        if (error.isNotEmpty()) Text(error, fontFamily = Sans, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Generar QR") {
            val cents = parseMoney(amount)
            if (cents == null) {
                error = "Escribe un valor, por ejemplo 20."
                return@PrimaryButton
            }
            val result = vm.issueTicket(kind, cents, chosen?.phone ?: "", chosen?.name ?: "")
            result.onSuccess { ticket ->
                vm.open(Screen.Qr(ticket.id))
            }.onFailure { error = it.message ?: "No se pudo crear el código." }
        }
    }
}

@Composable
fun QrScreen(vm: StudioViewModel, ticketId: String) {
    val ticket = vm.book.tickets.find { it.id == ticketId }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ScreenTitle(if (ticket?.kind == "charge") "Cobro" else "Recarga") { vm.open(Screen.Wallet) }
        if (ticket == null) {
            Text("Ese código ya no está.", color = Soft, fontFamily = Sans, fontSize = 18.sp, modifier = Modifier.padding(top = 16.dp))
            return@Column
        }
        Text(formatMoney(ticket.cents), fontFamily = Display, fontSize = 56.sp, modifier = Modifier.padding(top = 16.dp))
        Text(if (ticket.name.isBlank()) "Para quien lo escanee" else "Para ${ticket.name}", color = Soft, fontFamily = Sans)
        Image(
            encodeQr(ticketQr(ticket, vm.deskUrl())).asImageBitmap(),
            contentDescription = "Código QR del valor",
            modifier = Modifier.padding(top = 16.dp).size(240.dp).clip(RoundedCornerShape(24.dp)).background(Card).border(1.dp, Line, RoundedCornerShape(24.dp)).padding(12.dp),
        )
        Text("Código", color = Soft, fontFamily = Sans, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp))
        Text(ticket.id, fontFamily = Display, fontSize = 32.sp, letterSpacing = 4.sp)
        Text(
            if (ticket.usedAt != null) "Listo. ${ticket.usedName ?: "La clienta"} ya lo usó."
            else if (vm.deskUrl().isBlank()) "Conéctate al Wi-Fi para que otro teléfono pueda usarlo."
            else "Caduca en 30 minutos. Las dos tienen que estar en el mismo Wi-Fi.",
            fontFamily = Sans,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
fun ScanScreen(vm: StudioViewModel, kind: String) {
    val book = vm.book
    var typed by remember { mutableStateOf("") }
    var claim by remember { mutableStateOf<TicketClaim?>(null) }
    var phone by remember { mutableStateOf(prettyPhone(book.clientPhone)) }
    var requestId by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var done by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    fun take(raw: String) {
        val parsed = parseTicketQr(raw)
        if (parsed == null) {
            claim = null
            error = "Ese código hay que escanearlo. Las letras solas no alcanzan."
            return
        }
        if (parsed.kind != kind) {
            claim = null
            error = if (kind == "topup") "Ese código es un cobro. Ábrelo en Pagar." else "Ese código es una recarga. Ábrelo en Recargar."
            return
        }
        claim = parsed
        requestId = UUID.randomUUID().toString()
        error = ""
    }
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        val raw = result.contents ?: return@rememberLauncherForActivityResult
        take(raw)
    }
    val pending = claim
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        ScreenTitle(if (kind == "topup") "Recargar" else "Pagar") { vm.open(Screen.Wallet) }
        if (done.isNotEmpty()) {
            Text(done, fontFamily = Display, fontSize = 36.sp, modifier = Modifier.padding(top = 24.dp))
            return@Column
        }
        if (pending != null) {
            Text(if (kind == "topup") "Recarga por confirmar" else "Cobro por confirmar", color = Soft, fontFamily = Sans, modifier = Modifier.padding(top = 20.dp))
            Text(formatMoney(pending.cents), fontFamily = Display, fontSize = 56.sp)
            Text("El teléfono de la manicura confirma el valor. Este aparato no lo cambia.", color = Soft, fontFamily = Sans, fontSize = 16.sp)
            Spacer(Modifier.height(16.dp))
            PaperField("Tu teléfono", phone, { phone = it }, KeyboardType.Phone)
            if (error.isNotEmpty()) Text(error, fontFamily = Sans, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(20.dp))
            PrimaryButton(if (busy) "Confirmando..." else if (kind == "topup") "Pedir recarga" else "Pedir cobro", enabled = !busy) {
                if (digits(phone).length < 10) {
                    error = "Escribe tu teléfono, con 10 dígitos."
                    return@PrimaryButton
                }
                busy = true
                error = ""
                val id = requestId
                thread(name = "redeem", isDaemon = true) {
                    val result = vm.redeem(pending, phone, book.clientName, id)
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        busy = false
                        result.onSuccess { (next, left) ->
                            vm.commit(next)
                            done = if (kind == "topup") "Listo. Tu saldo es ${formatMoney(left)}." else "Pagado. Tu saldo es ${formatMoney(left)}."
                        }.onFailure {
                            error = it.message ?: "No se pudo confirmar."
                        }
                    }
                }
            }
            return@Column
        }
        Text("Apunta al código de la manicura.", color = Soft, fontFamily = Sans, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
        PrimaryButton("Abrir cámara") {
            scanner.launch(
                ScanOptions().apply {
                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    setPrompt("Apunta al código")
                    setBeepEnabled(false)
                },
            )
        }
        Spacer(Modifier.height(16.dp))
        PaperField("Código", typed, { typed = it })
        if (error.isNotEmpty()) Text(error, fontFamily = Sans, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Continuar") { take(typed) }
    }
}

@Composable
fun ClientScreen(vm: StudioViewModel, id: String) {
    val book = vm.book
    val client = book.clients.find { it.id == id }
    var confirming by remember { mutableStateOf(false) }
    if (client == null) {
        WalletHome(vm)
        return
    }
    val moves = movesFor(book, client.phone).take(8)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        ScreenTitle(client.name) { vm.open(Screen.Wallet) }
        Text(prettyPhone(client.phone), color = Soft, fontFamily = Sans, fontSize = 18.sp)
        Text("Saldo disponible", color = Soft, fontFamily = Sans, modifier = Modifier.padding(top = 20.dp))
        Text(formatMoney(balanceOf(book, client.phone)), fontFamily = Display, fontSize = 56.sp)
        Row(Modifier.padding(top = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton("Recargar", Modifier.weight(1f)) { vm.open(Screen.Amount("topup", client.id)) }
            GhostButton("Cobrar", Modifier.weight(1f)) { vm.open(Screen.Amount("charge", client.id)) }
        }
        MoveList("Movimientos", moves.map { it.kind to it }, LocalDate.now(), "Esta ficha todavía no tiene movimientos.")
        Spacer(Modifier.height(24.dp))
        if (confirming) {
            ButtonInk("Confirmar y eliminar") {
                vm.commit(removeClient(book, client.id))
                vm.open(Screen.Wallet)
            }
        } else {
            Text("Eliminar ficha", modifier = Modifier.clickable { confirming = true }, color = Soft, fontFamily = Sans)
        }
    }
}

@Composable
fun AddClientScreen(vm: StudioViewModel) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        ScreenTitle("Nueva clienta") { vm.open(Screen.Wallet) }
        Spacer(Modifier.height(20.dp))
        PaperField("Nombre", name, { name = it })
        Spacer(Modifier.height(12.dp))
        PaperField("Teléfono", phone, { phone = it }, KeyboardType.Phone)
        if (error.isNotEmpty()) Text(error, fontFamily = Sans, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Guardar ficha") {
            val result = addClient(vm.book, name, phone)
            result.onSuccess {
                vm.commit(it)
                vm.open(Screen.Wallet)
            }.onFailure { error = it.message ?: "No se pudo guardar." }
        }
    }
}

@Composable
private fun ButtonInk(text: String, onClick: () -> Unit) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
    ) {
        Text(text, fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    }
}

private fun encodeQr(value: String): Bitmap {
    val matrix = QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 640, 640, mapOf(EncodeHintType.MARGIN to 1))
    val bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888)
    for (x in 0 until 640) {
        for (y in 0 until 640) {
            bitmap.setPixel(x, y, if (matrix.get(x, y)) AndroidColor.parseColor("#6D4638") else AndroidColor.parseColor("#FBF6F1"))
        }
    }
    return bitmap
}
