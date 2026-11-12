package com.oneday.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneday.app.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// ============ Paleta 60 / 30 / 10 ============
// 60 % neutros: fondo y tarjetas
val Bg = Color(0xFF000000)
val CardBg = Color(0xFF111113)
val CardHigh = Color(0xFF1C1C1F)
val Line = Color(0xFF2C2C30)
// 30 % contraste: textos, botón principal y celdas llenas
val Ink = Color(0xFFF2F2F3)
val Ink2 = Color(0xFFD6D6D9)
val Muted = Color(0xFF8E8E93)
val Faint = Color(0xFF5C5C62)
// 10 % resaltado
val Accent = Color(0xFFFF7A1A)
// Semánticos (uso mínimo)
val Good = Color(0xFF3DD68C)
val Bad = Color(0xFFFF6B6B)
// Vidrio semitransparente (menú flotante, paneles)
val Glass = Color(0xD91E1E21)

private val Scheme = darkColorScheme(
    primary = Accent, onPrimary = Color(0xFF1A0E04),
    primaryContainer = CardHigh, onPrimaryContainer = Ink,
    secondary = Ink2, onSecondary = Bg,
    secondaryContainer = Color(0xFF3A3A3F), onSecondaryContainer = Ink,
    tertiary = Accent,
    background = Bg, onBackground = Ink,
    surface = Bg, onSurface = Ink,
    surfaceVariant = CardHigh, onSurfaceVariant = Muted,
    surfaceContainerLowest = Bg, surfaceContainerLow = CardBg, surfaceContainer = CardBg,
    surfaceContainerHigh = CardHigh, surfaceContainerHighest = Color(0xFF26262A),
    outline = Line, outlineVariant = Line,
    error = Bad,
)

@Composable
fun OneDayTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = Scheme,
    shapes = Shapes(extraLarge = RoundedCornerShape(28.dp), large = RoundedCornerShape(24.dp), medium = RoundedCornerShape(16.dp)),
    content = content,
)

/** Estado global de la interfaz: desenfoque de fondo y menú visible. */
object UiState {
    var blurCount by mutableIntStateOf(0)
    var hideNav by mutableStateOf(false)
}

@Composable
fun BlurWhileShown() = DisposableEffect(Unit) {
    UiState.blurCount++
    onDispose { UiState.blurCount-- }
}

@Composable
fun backgroundBlur(): Dp = animateDpAsState(if (UiState.blurCount > 0) 18.dp else 0.dp, label = "blur").value

// ============ Prioridades (dentro de la paleta) ============
fun priorityColor(p: Int) = when (p) { 2 -> Accent; 1 -> Ink2; else -> Faint }
fun priorityName(p: Int) = when (p) { 2 -> "Alta"; 1 -> "Media"; else -> "Baja" }

// ============ Componentes ============

@Composable
fun OdIcon(@DrawableRes id: Int, tint: Color = Muted, size: Dp = 20.dp, desc: String? = null) =
    Icon(painterResource(id), desc, tint = tint, modifier = Modifier.size(size))

/** Tarjeta base: gris muy oscuro, esquinas grandes. */
@Composable
fun OdCard(
    modifier: Modifier = Modifier,
    color: Color = CardBg,
    onClick: (() -> Unit)? = null,
    padding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(color)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Encabezado pequeño de tarjeta: ícono + etiqueta gris + acción opcional a la derecha. */
@Composable
fun CardLabel(text: String, @DrawableRes icon: Int? = null, sub: String? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { OdIcon(icon, Muted, 18.dp); Spacer(Modifier.width(8.dp)) }
        Column(Modifier.weight(1f)) {
            Text(text, color = Muted, style = MaterialTheme.typography.bodyLarge)
            if (sub != null) Text(sub, color = Faint, style = MaterialTheme.typography.bodySmall)
        }
        trailing?.invoke()
    }
}

@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) = OdCard(modifier) {
    if (title != null || action != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (title != null) Text(title, color = Muted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            action?.invoke()
        }
        Spacer(Modifier.height(12.dp))
    }
    content()
}

/** Recuadro punteado que enmarca el título principal de una tarjeta. */
@Composable
fun DashedBox(modifier: Modifier = Modifier, color: Color = Faint, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.drawBehind {
            val s = 1.6.dp.toPx()
            drawRoundRect(
                color, cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(width = s, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx()))),
            )
        }.padding(horizontal = 16.dp, vertical = 8.dp),
        content = content,
    )
}

/** Píldora informativa (ej. "~60 min"). */
@Composable
fun InfoPill(text: String, @DrawableRes icon: Int? = null) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(CardHigh).padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { OdIcon(icon, Muted, 16.dp); Spacer(Modifier.width(6.dp)) }
        Text(text, color = Ink2, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun Pill(text: String, color: Color = Muted) {
    Surface(shape = RoundedCornerShape(50), color = CardHigh) {
        Text(text, color = color, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

/** Botón principal blanco tipo píldora (color de contraste). */
@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Bg, disabledContainerColor = CardHigh, disabledContentColor = Faint),
    ) {
        if (icon != null) { Icon(painterResource(icon), null, tint = Color.Unspecified, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(12.dp)) }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
    }
}

/** Botón secundario oscuro tipo píldora. */
@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, color: Color = Ink, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = CardHigh, contentColor = color),
    ) { Text(text, fontWeight = FontWeight.Medium) }
}

/** Botón redondo pequeño (ajustes, agregar). */
@Composable
fun CircleButton(desc: String, accent: Boolean = false, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(if (accent) Accent else CardHigh).clickable(onClick = onClick, role = Role.Button, onClickLabel = desc),
        contentAlignment = Alignment.Center,
    ) { icon() }
}

/** Mosaico de estadística: ícono, número grande, etiqueta. */
@Composable
fun StatTile(@DrawableRes icon: Int, value: String, label: String, modifier: Modifier = Modifier, delta: String? = null, deltaGood: Boolean = true, valueColor: Color = Ink, onClick: (() -> Unit)? = null) {
    Box(
        modifier.clip(RoundedCornerShape(28.dp)).background(CardBg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 18.dp, horizontal = 10.dp),
    ) {
        if (delta != null) Text(delta, color = if (deltaGood) Good else Bad, style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.TopEnd))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            OdIcon(icon, Muted, 20.dp)
            Spacer(Modifier.height(10.dp))
            Text(value, color = valueColor, fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(label, color = Ink2, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Selector segmentado tipo píldora. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(50)).background(CardBg).padding(4.dp)) {
        options.forEachIndexed { i, o ->
            val sel = i == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(50)).background(if (sel) CardHigh else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(i) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) { Text(o, color = if (sel) Ink else Muted, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal, style = MaterialTheme.typography.bodyMedium, maxLines = 1) }
        }
    }
}

/** Casilla redonda: vacía con borde, llena en blanco con palomita oscura. */
@Composable
fun RoundCheck(checked: Boolean, onToggle: () -> Unit, desc: String) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Checkbox, onClickLabel = desc, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(if (checked) Ink else Color.Transparent)
                .then(if (checked) Modifier else Modifier.border(2.dp, Faint, CircleShape)),
            contentAlignment = Alignment.Center,
        ) { if (checked) OdIcon(R.drawable.ic_check, Bg, 16.dp) }
    }
}

/** Menú inferior flotante, semitransparente, solo íconos. */
@Composable
fun FloatingNav(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(R.drawable.ic_nav_today to "Hoy", R.drawable.ic_nav_tasks to "Pendientes", R.drawable.ic_nav_flame to "Rachas", R.drawable.ic_nav_gym to "Gym")
    Row(
        modifier.clip(RoundedCornerShape(50)).background(Glass).border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(50)).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items.forEachIndexed { i, (icon, desc) ->
            val sel = i == selected
            Box(
                Modifier.size(width = 64.dp, height = 52.dp).clip(RoundedCornerShape(50))
                    .background(if (sel) Color(0xFF2E2E33) else Color.Transparent)
                    .clickable(role = Role.Tab, onClickLabel = desc) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) { OdIcon(icon, if (sel) Accent else Muted, 24.dp, desc) }
        }
    }
}

/** Panel inferior semitransparente; el fondo se desenfoca mientras está abierto. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BlurWhileShown()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Glass,
        contentColor = Ink,
        scrimColor = Color.Black.copy(alpha = 0.35f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { Box(Modifier.padding(top = 10.dp, bottom = 8.dp).size(40.dp, 5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f))) },
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp), content = content)
    }
}

/** Diálogo con estilo de la app (fondo desenfocado y tarjeta oscura). */
@Composable
fun OdDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    BlurWhileShown()
    AlertDialog(
        onDismissRequest = onDismissRequest, confirmButton = confirmButton, dismissButton = dismissButton,
        title = title, text = text, containerColor = Color(0xF21C1C1F), shape = RoundedCornerShape(32.dp),
        titleContentColor = Ink, textContentColor = Ink2,
    )
}

@Composable
fun ScreenTitle(title: String, subtitle: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            if (subtitle != null) Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
        }
        action?.invoke()
    }
}

@Composable
fun EmptyState(emoji: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 44.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Muted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickDialog(initial: Long?, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    BlurWhileShown()
    val state = rememberDatePickerState(initialSelectedDateMillis = (initial ?: LocalDate.now().toEpochDay()) * 86_400_000L)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()) }
                onDismiss()
            }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Muted) } },
        shape = RoundedCornerShape(32.dp),
        colors = DatePickerDefaults.colors(containerColor = CardHigh),
    ) { DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = CardHigh)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickDialog(initial: Int?, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val init = initial ?: (9 * 60)
    val state = rememberTimePickerState(initialHour = init / 60, initialMinute = init % 60, is24Hour = false)
    OdDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(state.hour * 60 + state.minute); onDismiss() }) { Text("Aceptar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Muted) } },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state, colors = TimePickerDefaults.colors(
                    clockDialColor = Color(0xFF2A2A2E), selectorColor = Accent,
                    timeSelectorSelectedContainerColor = Color(0xFF3A3A3F), timeSelectorUnselectedContainerColor = Color(0xFF2A2A2E),
                    timeSelectorSelectedContentColor = Ink, periodSelectorSelectedContainerColor = Color(0xFF3A3A3F),
                    periodSelectorBorderColor = Line,
                ))
            }
        },
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String = "Eliminar", onDismiss: () -> Unit, onConfirm: () -> Unit) {
    OdDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm, color = Bad) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Muted) } },
    )
}

/** Fila de opciones tipo píldora (para usar dentro de diálogos). */
@Composable
fun ChoicePill(text: String, selected: Boolean, modifier: Modifier = Modifier, lead: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(if (selected) Ink else Color(0xFF2A2A2E)).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (lead != null) { lead(); Spacer(Modifier.width(6.dp)) }
        Text(text, color = if (selected) Bg else Ink2, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}
