package com.oneday.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFFFF7A1A)
val Bg = Color(0xFF101114)
val CardBg = Color(0xFF1B1D22)
val Muted = Color(0xFF9A9FAB)
val Good = Color(0xFF3DD68C)
val Warn = Color(0xFFFFC145)
val Bad = Color(0xFFFF5C5C)

private val Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF1A0E04),
    primaryContainer = Color(0xFF4A2508),
    onPrimaryContainer = Color(0xFFFFDCC4),
    secondary = Color(0xFF7FB6FF),
    secondaryContainer = Color(0xFF4A2508),
    onSecondaryContainer = Color(0xFFFFDCC4),
    background = Bg,
    onBackground = Color(0xFFECEDEF),
    surface = Bg,
    onSurface = Color(0xFFECEDEF),
    surfaceVariant = CardBg,
    onSurfaceVariant = Muted,
    surfaceContainer = CardBg,
    surfaceContainerHigh = Color(0xFF23262C),
    surfaceContainerHighest = Color(0xFF2A2D34),
    surfaceContainerLow = Color(0xFF16181C),
    outline = Color(0xFF3A3E46),
    outlineVariant = Color(0xFF2A2D34),
    error = Bad,
)

@Composable
fun OneDayTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Scheme, content = content)

fun priorityColor(p: Int) = when (p) { 2 -> Bad; 1 -> Warn; else -> Good }
fun priorityName(p: Int) = when (p) { 2 -> "Alta"; 1 -> "Media"; else -> "Baja" }
