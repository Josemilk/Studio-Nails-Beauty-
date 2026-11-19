package com.studionails.beauty.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StudioPhoto(name: String, modifier: Modifier = Modifier, description: String? = null) {
    val context = LocalContext.current
    val bitmap = remember(name) {
        context.assets.open("studio/$name").use { BitmapFactory.decodeStream(it) }.asImageBitmap()
    }
    Image(bitmap, description, modifier.clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
}

@Composable
fun PaperField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialBody,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        colors = fieldColors(),
    )
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Accent,
    unfocusedBorderColor = Line,
    focusedContainerColor = Card,
    unfocusedContainerColor = Card,
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    cursorColor = Accent,
    focusedLabelColor = Soft,
    unfocusedLabelColor = Soft,
)

private val MaterialBody = androidx.compose.ui.text.TextStyle(fontFamily = Sans, fontSize = 18.sp, color = Ink)

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent, disabledContainerColor = Line, disabledContentColor = Soft),
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = Sans)
    }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Card, contentColor = Ink),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = Sans)
    }
}

@Composable
fun ScreenTitle(title: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onBack)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = Soft)
            Text(" Volver", color = Soft, fontFamily = Sans, fontSize = 16.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(title, style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
    }
}

@Composable
fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(if (selected) Ink else Card)
            .border(1.dp, if (selected) Ink else Line, RoundedCornerShape(100))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        color = if (selected) Paper else Ink,
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
    )
}

@Composable
fun IconAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(100)).background(Card).border(1.dp, Line, RoundedCornerShape(100))) {
        Icon(icon, contentDescription = label, tint = Ink)
    }
}

@Composable
fun SectionGap() {
    Spacer(Modifier.height(8.dp))
}

@Composable
fun Stack(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
}
