package com.oneday.app.ui

import androidx.annotation.DrawableRes
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.TextUnit
import com.oneday.app.data.Repo
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
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
val Faint = Color(0xFF7C7C82) // sube de #5C5C62 para cumplir 4.5:1 sobre tarjeta
// 10 % resaltado
val Accent = Color(0xFFFF7A1A)
val AccentSoft = Color(0xFF3B2414)  // naranja al ~15 % para fondos de chips activos
val AccentText = Color(0xFFFF9A4D)  // naranja legible sobre AccentSoft
val Hairline = Color(0x0FFFFFFF)    // filo de 1 px blanco al 6 %
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

// ============ Tipografía en dos voces ============
/** Space Grotesk: solo cifras grandes y títulos de pantalla. */
val Display = FontFamily(
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun TextStyle.inter() = copy(fontFamily = Inter, fontFeatureSettings = "tnum")
private val base = Typography()
private val AppTypography = Typography(
    displayLarge = base.displayLarge.inter(), displayMedium = base.displayMedium.inter(), displaySmall = base.displaySmall.inter(),
    headlineLarge = base.headlineLarge.copy(fontFamily = Display, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.8).sp),
    headlineSmall = base.headlineSmall.inter(),
    titleLarge = base.titleLarge.inter(), titleMedium = base.titleMedium.inter(), titleSmall = base.titleSmall.inter(),
    bodyLarge = base.bodyLarge.inter(), bodyMedium = base.bodyMedium.inter(), bodySmall = base.bodySmall.inter(),
    labelLarge = base.labelLarge.inter(), labelMedium = base.labelMedium.inter(), labelSmall = base.labelSmall.inter(),
)

@Composable
fun OneDayTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = Scheme,
    typography = AppTypography,
    shapes = Shapes(extraLarge = RoundedCornerShape(28.dp), large = RoundedCornerShape(24.dp), medium = RoundedCornerShape(16.dp)),
    content = content,
)

data class Undo(val text: String, val action: () -> Unit, val id: Long)

/** Barra flotante con "Deshacer". */
@Composable
fun UndoBar(modifier: Modifier = Modifier) {
    val u = UiState.undo ?: return
    LaunchedEffect(u.id) { kotlinx.coroutines.delay(4000); if (UiState.undo?.id == u.id) UiState.undo = null }
    Row(
        modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(50)).background(Glass)
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(50)).padding(start = 20.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(u.text, color = Ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { u.action(); UiState.undo = null; Fx.tick() }) { Text("Deshacer", color = Accent, fontWeight = FontWeight.SemiBold) }
    }
}

/** Estado global de la interfaz: desenfoque de fondo y menú visible. */
object UiState {
    var blurCount by mutableIntStateOf(0)
    var hideNav by mutableStateOf(false)
    var sosStreak by mutableStateOf<String?>(null)
    var medal by mutableStateOf<MedalMsg?>(null)
    /** Súbelo para pedir una sincronización de pasos. */
    var syncSteps by mutableIntStateOf(0)
    /** Aviso con "Deshacer" que se muestra 4 segundos. */
    var undo by mutableStateOf<Undo?>(null)
    fun showUndo(text: String, action: () -> Unit) { undo = Undo(text, action, System.nanoTime()) }
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
        modifier.fillMaxWidth().pressClick(onClick).clip(RoundedCornerShape(28.dp)).background(color)
            .border(1.dp, Hairline, RoundedCornerShape(28.dp)).padding(padding),
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
fun PrimaryButton(text: String, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null, enabled: Boolean = true, glyph: String? = null, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Bg, disabledContainerColor = CardHigh, disabledContentColor = Faint),
    ) {
        if (icon != null) { Icon(painterResource(icon), null, tint = Color.Unspecified, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(12.dp)) }
        if (glyph != null) { Glyph(glyph, if (enabled) Bg else Faint, 20.dp); Spacer(Modifier.width(8.dp)) }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        modifier.pressClick(onClick).clip(RoundedCornerShape(28.dp)).background(CardBg).border(1.dp, Hairline, RoundedCornerShape(28.dp)).padding(vertical = 18.dp, horizontal = 10.dp),
    ) {
        if (delta != null) Text(delta, color = if (deltaGood) Good else Bad, style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.TopEnd))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            OdIcon(icon, Muted, 20.dp)
            Spacer(Modifier.height(10.dp))
            RollingText(value, color = valueColor, fontSize = 30.sp, fontWeight = FontWeight.Bold)
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
    val anim = Repo.data.settings.animationsOn
    val fill by animateFloatAsState(if (checked) 1f else 0f, if (anim) spring(dampingRatio = 0.55f, stiffness = 500f) else snap(), label = "check")
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Checkbox, onClickLabel = desc) {
            if (!checked) Fx.success() else Fx.tick()
            onToggle()
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(26.dp).clip(CircleShape).border(2.dp, Faint.copy(alpha = 1f - fill), CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(26.dp).graphicsLayer { scaleX = fill; scaleY = fill }.clip(CircleShape).background(Ink), contentAlignment = Alignment.Center) {
                if (checked) DrawnCheck(Bg, 16.dp, checked)
            }
        }
    }
}

// ============ Microinteracciones (todas controlables desde Ajustes) ============

/** Vibración según Ajustes: apagada, suave o fuerte. */
object Fx {
    var view: View? = null
    private fun go(soft: Int, strong: Int) {
        val s = Repo.data.settings
        if (!s.hapticsOn) return
        view?.performHapticFeedback(if (s.hapticStrong) strong else soft, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
    }
    /** Tic leve: tocar una tarjeta, cruzar el umbral al deslizar. */
    fun tick() = go(HapticFeedbackConstants.CLOCK_TICK, HapticFeedbackConstants.VIRTUAL_KEY)
    /** Confirmación: marcar hecho. */
    fun success() = go(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY,
        HapticFeedbackConstants.LONG_PRESS,
    )
    /** Doble pulso: récord personal. */
    fun pr() { success(); view?.postDelayed({ success() }, 120) }
    /** Éxito largo: nueva medalla. */
    fun medal() { go(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.LONG_PRESS); view?.postDelayed({ success() }, 180) }
    /** Rechazo corto: eliminar o error. */
    fun reject() = go(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.VIRTUAL_KEY,
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS,
    )
}

/** Clic de tarjeta: se hunde un poco al presionarla (si las animaciones están activas) y vibra suave. */
@Composable
fun Modifier.pressClick(onClick: (() -> Unit)?): Modifier {
    if (onClick == null) return this
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val anim = Repo.data.settings.animationsOn
    val scale by animateFloatAsState(if (pressed && anim) 0.97f else 1f, spring(stiffness = 600f), label = "press")
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null) { Fx.tick(); onClick() }
}

/** Cifra que rueda al cambiar: solo gira el dígito que cambia (si está activado en Ajustes). */
@Composable
fun RollingText(text: String, color: Color = Ink, fontSize: TextUnit = TextUnit.Unspecified, fontWeight: FontWeight? = null, lineHeight: TextUnit = TextUnit.Unspecified) {
    val s = Repo.data.settings
    val family = if (fontSize != TextUnit.Unspecified && fontSize.value >= 20f) Display else null
    if (!s.rollingNumbersOn || !s.animationsOn) {
        Text(text, color = color, fontSize = fontSize, fontWeight = fontWeight, maxLines = 1, lineHeight = lineHeight, fontFamily = family); return
    }
    Row {
        // Se alinea por la derecha para que unidades, decenas… conserven su lugar.
        val chars = text.toList()
        chars.forEachIndexed { i, ch ->
            key(chars.size - i) {
                AnimatedContent(
                    targetState = ch,
                    transitionSpec = {
                        (slideInVertically(tween(260)) { it } + fadeIn(tween(260))) togetherWith (slideOutVertically(tween(260)) { -it } + fadeOut(tween(200)))
                    }, label = "digit",
                ) { c -> Text(c.toString(), color = color, fontSize = fontSize, fontWeight = fontWeight, maxLines = 1, lineHeight = lineHeight, fontFamily = family) }
            }
        }
    }
}

/** Confeti naranja y blanco que cae una vez (récords y metas). Cambia `trigger` para lanzarlo. */
@Composable
fun Confetti(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger == 0 || !Repo.data.settings.celebrationsOn || !Repo.data.settings.animationsOn) return
    val progress = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) { progress.snapTo(0f); progress.animateTo(1f, tween(1600, easing = LinearEasing)) }
    val parts = remember(trigger) {
        val r = java.util.Random(trigger.toLong())
        List(46) { floatArrayOf(r.nextFloat(), r.nextFloat() * 0.35f, (r.nextFloat() - 0.5f) * 0.5f, 0.6f + r.nextFloat() * 0.8f, r.nextFloat() * 360f, if (it % 3 == 0) 1f else 0f) }
    }
    if (progress.value >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        parts.forEach { (x0, y0, vx, speed, rot, white) ->
            val x = (x0 + vx * t) * size.width
            val y = (-0.05f + y0 * 0.2f + speed * t * t * 1.1f) * size.height
            rotate(rot + t * 540f, Offset(x, y)) {
                drawRoundRect(if (white == 1f) Ink2 else Accent, topLeft = Offset(x - 5f, y - 9f),
                    size = androidx.compose.ui.geometry.Size(10f, 18f), cornerRadius = CornerRadius(3f), alpha = (1f - t).coerceIn(0f, 1f))
            }
        }
    }
}
private operator fun FloatArray.component6() = this[5]

/** Menú inferior flotante, de vidrio. La pestaña activa se abre y muestra su nombre. */
@Composable
fun FloatingNav(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf("madrugar" to "Hoy", "pendientes" to "Pendientes", "racha" to "Rachas", "gym" to "Gym")
    Row(
        modifier.clip(RoundedCornerShape(50)).background(Glass).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(50)).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, (icon, label) ->
            val sel = i == selected
            Row(
                Modifier.height(50.dp).clip(RoundedCornerShape(50)).background(if (sel) AccentSoft else Color.Transparent)
                    .clickable(role = Role.Tab, onClickLabel = label) { Fx.tick(); onSelect(i) }
                    .animateContentSize().padding(horizontal = if (sel) 16.dp else 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Glyph(icon, if (sel) Accent else Muted, 22.dp, label)
                if (sel) { Spacer(Modifier.width(8.dp)); Text(label, color = AccentText, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge) }
            }
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
fun EmptyState(icon: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(84.dp).clip(RoundedCornerShape(27.dp)).background(CardBg).border(1.dp, Hairline, RoundedCornerShape(27.dp)), contentAlignment = Alignment.Center) {
            Glyph(icon, Muted, 38.dp)
        }
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


/** Clic sin efecto de onda (para fichas pequeñas). */
@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { Fx.tick(); onClick() }


/** Palomita que se "dibuja" de izquierda a derecha (si las animaciones están activas). */
@Composable
fun DrawnCheck(color: Color, size: Dp, checked: Boolean) {
    val anim = Repo.data.settings.animationsOn
    val p = remember { Animatable(if (anim) 0f else 1f) }
    LaunchedEffect(checked) { if (checked && anim) { p.snapTo(0f); p.animateTo(1f, tween(220, delayMillis = 60)) } else p.snapTo(1f) }
    Canvas(Modifier.size(size)) {
        val s = this.size.width / 24f
        val path = androidx.compose.ui.graphics.Path().apply { moveTo(5f * s, 12.5f * s); lineTo(9.5f * s, 17f * s); lineTo(19f * s, 7.5f * s) }
        val measure = androidx.compose.ui.graphics.PathMeasure().apply { setPath(path, false) }
        val seg = androidx.compose.ui.graphics.Path()
        measure.getSegment(0f, measure.length * p.value, seg, true)
        drawPath(seg, color, style = androidx.compose.ui.graphics.drawscope.Stroke(2.8f * s, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}
