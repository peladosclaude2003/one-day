package com.oneday.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneday.app.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Material de la medalla: sube con el reto. */
enum class Tier(val fill: Color, val edge: Color, val inner: Color, val num: Color) {
    Gris(Color(0xFF1C1C1F), Color(0xFF3A3A3F), Color(0xFF2C2C30), Ink2),
    Blanco(Color(0xFF2A2A2E), Ink2, Color(0xFF5C5C62), Ink),
    Pleno(Ink, Ink, Accent, Bg),
    Naranja(Accent, Accent, Color(0xFF1A0E04), Color(0xFF1A0E04)),
    Cerrada(Bg, Color(0xFF3A3A3F), Color(0xFF1C1C1F), Color(0xFF5C5C62)),
}

/** Hitos que dan medalla (rachas y hábitos). */
val MEDAL_DAYS = listOf(7L, 30L, 90L, 365L)
fun tierFor(days: Long) = when (days) { 7L -> Tier.Gris; 30L -> Tier.Blanco; 90L -> Tier.Pleno; else -> Tier.Naranja }

/** Medalla circular: aro exterior, aro interior y 4 marcas, como una esfera de reloj. */
@Composable
fun Medal(number: String, unit: String, tier: Tier, size: Dp = 64.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).semantics { contentDescription = "Medalla $number $unit" + if (tier == Tier.Cerrada) ", bloqueada" else "" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.toPx() / 2
            val c = Offset(r, r)
            val sw = (size.toPx() / 60f).coerceAtLeast(1.2f)
            drawCircle(tier.fill, r - sw, c)
            drawCircle(tier.edge, r - sw, c, style = Stroke(sw * 1.4f,
                pathEffect = if (tier == Tier.Cerrada) PathEffect.dashPathEffect(floatArrayOf(sw * 4, sw * 3.2f)) else null))
            drawCircle(tier.inner, r * 0.76f, c, style = Stroke(if (tier == Tier.Pleno) sw * 2.6f else sw))
            val a = r - sw * 1.5f; val b = r * 0.88f
            listOf(Offset(0f, -1f), Offset(1f, 0f), Offset(0f, 1f), Offset(-1f, 0f)).forEach { d ->
                drawLine(tier.inner, Offset(c.x + d.x * a, c.y + d.y * a), Offset(c.x + d.x * b, c.y + d.y * b), sw, StrokeCap.Round)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(number, fontFamily = Display, fontWeight = FontWeight.Bold, color = tier.num,
                fontSize = (size.value * if (number.length >= 3) 0.26f else 0.32f).sp, lineHeight = (size.value * 0.32f).sp, letterSpacing = (-0.5).sp)
            if (size >= 52.dp) Text(unit, color = tier.num.copy(alpha = 0.8f), fontSize = (size.value * 0.085f).sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        }
    }
}

/** Vitrina: las 4 medallas de una racha o hábito, conseguidas o bloqueadas. */
@Composable
fun MedalShelf(best: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        MEDAL_DAYS.forEach { d ->
            val got = best >= d
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Medal("$d", "DÍAS", if (got) tierFor(d) else Tier.Cerrada, 54.dp)
                Text(when (d) { 7L -> "Semana"; 30L -> "Mes"; 90L -> "Trimestre"; else -> "Año" }, color = if (got) Ink2 else Faint, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

data class MedalMsg(val number: String, val unit: String, val tier: Tier, val title: String, val sub: String, val id: Long = System.nanoTime(), val share: ShareSpec? = null)

/** Aviso de vidrio que baja desde arriba con la medalla nueva (3 s). */
@Composable
fun MedalToast(modifier: Modifier = Modifier) {
    val m = UiState.medal ?: return
    val anim = Repo.data.settings.animationsOn && Repo.data.settings.celebrationsOn
    val y = remember(m.id) { Animatable(if (anim) -140f else 0f) }
    val rot = remember(m.id) { Animatable(if (anim) 15f else 0f) }
    LaunchedEffect(m.id) {
        Fx.medal()
        launch { y.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)) }
        launch { rot.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 260f)) }
        delay(3200)
        if (anim) y.animateTo(-160f, tween(260))
        if (UiState.medal?.id == m.id) UiState.medal = null
    }
    Box(modifier.fillMaxWidth().padding(horizontal = 16.dp).graphicsLayer { translationY = y.value * density }) {
        Box(Modifier.matchParentSize().clip(RoundedCornerShape(26.dp)).background(Brush.radialGradient(listOf(Accent.copy(alpha = 0.18f), Color.Transparent))))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Glass).border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(26.dp))
                .padding(start = 14.dp, end = 18.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Medal(m.number, m.unit, m.tier, 60.dp, Modifier.graphicsLayer { rotationZ = rot.value })
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("NUEVA MEDALLA", color = AccentText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                Text(m.title, color = Ink, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(m.sub, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Box(Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).background(CardHigh)
                .clickableNoRipple { UiState.share = m.share ?: ShareSpec(m.number, m.unit, m.tier, m.title, null, listOf("FECHA" to shareDate())); UiState.medal = null },
                contentAlignment = Alignment.Center) { Glyph("compartir", Ink, 20.dp, "Compartir") }
        }
    }
}

/** Revisa rachas y hábitos y muestra la medalla nueva más alta que aún no viste. */
fun checkNewMedals() {
    val d = Repo.data
    val seen = d.settings.seenMedals
    val newSeen = mutableSetOf<String>()
    var show: MedalMsg? = null
    d.streaks.forEach { s ->
        val days = maxOf(streakDays(s), 0)
        val got = MEDAL_DAYS.filter { days >= it }
        got.forEach { g -> val k = "s:${s.id}:$g"; if (k !in seen) { newSeen += k; show = MedalMsg("$g", "DÍAS", tierFor(g), "$g días sin ${s.name.lowercase()}", medalLine(g), share = streakShare(s)) } }
    }
    d.habits.forEach { h ->
        val st = habitStreak(h).toLong()
        MEDAL_DAYS.filter { st >= it }.forEach { g -> val k = "h:${h.id}:$g"; if (k !in seen) { newSeen += k; show = MedalMsg("$g", "DÍAS", tierFor(g), "$g días: ${h.name.lowercase()}", medalLine(g), share = habitShare(h)) } }
    }
    if (newSeen.isNotEmpty()) {
        Repo.update { it.copy(settings = it.settings.copy(seenMedals = it.settings.seenMedals + newSeen)) }
        show?.let { UiState.medal = it }
    }
}

private fun medalLine(d: Long) = when (d) {
    7L -> "Tu primera semana. Guárdala en tu vitrina."
    30L -> "Un mes completo. Esto ya es un hábito."
    90L -> "Tres meses. Pocas personas llegan aquí."
    else -> "Un año. Eres otra persona."
}
