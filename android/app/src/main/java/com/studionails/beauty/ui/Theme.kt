package com.studionails.beauty.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.studionails.beauty.R

val Paper = Color(0xFFF7F1EB)
val PaperDeep = Color(0xFFF3E6DC)
val Card = Color(0xFFFBF6F1)
val Ink = Color(0xFF6D4638)
val Soft = Color(0xFF8F5F50)
val Faint = Color(0xFF9B7668)
val Line = Color(0xFFEADFD6)
val Accent = Color(0xFFAD624C)
val Peach = Color(0xFFECC4B2)
val OnAccent = Color(0xFFFFFAF7)

val Display = FontFamily(Font(R.font.fraunces))
val Sans = FontFamily(Font(R.font.sourcesans3))

private val Scheme = lightColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    background = Paper,
    onBackground = Ink,
    surface = Card,
    onSurface = Ink,
    surfaceVariant = Peach,
    outline = Line,
)

private val Type = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 48.sp, color = Ink),
    headlineLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 36.sp, color = Ink),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Ink),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Ink),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Ink),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ink),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink),
)

@Composable
fun StudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Type, content = content)
}
