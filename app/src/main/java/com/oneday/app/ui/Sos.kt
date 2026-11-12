package com.oneday.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneday.app.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla SOS de antojo: 60 s de respiración guiada (4 s inhala, 6 s exhala),
 * luego registra qué tan fuerte fue el antojo.
 */
@Composable
fun SosOverlay(streakId: String, onClose: () -> Unit) {
    val streak = Repo.data.streaks.firstOrNull { it.id == streakId } ?: run { onClose(); return }
    BlurWhileShown()
    var phase by remember { mutableIntStateOf(0) } // 0 respirando · 1 calificar · 2 listo
    var secondsLeft by remember { mutableIntStateOf(60) }
    var inhale by remember { mutableStateOf(true) }
    var celebrate by remember { mutableIntStateOf(0) }
    val scale = remember { Animatable(0.55f) }
    val anim = Repo.data.settings.animationsOn

    LaunchedEffect(Unit) {
        launch { while (secondsLeft > 0 && phase == 0) { delay(1000); secondsLeft-- }; if (phase == 0) { Fx.success(); phase = 1 } }
        while (phase == 0) {
            inhale = true; Fx.tick()
            scale.animateTo(1f, tween(if (anim) 4000 else 0, easing = FastOutSlowInEasing)); if (!anim) delay(4000)
            inhale = false; Fx.tick()
            scale.animateTo(0.55f, tween(if (anim) 6000 else 0, easing = FastOutSlowInEasing)); if (!anim) delay(6000)
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color(0xF2000000)).clickable(remember { MutableInteractionSource() }, null) {}.statusBarsPadding().navigationBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SIN ${streak.name.uppercase()}", color = Muted, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp, modifier = Modifier.padding(top = 16.dp))
            Spacer(Modifier.weight(1f))
            when (phase) {
                0 -> {
                    Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(280.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }.clip(CircleShape).background(CardHigh))
                        Box(Modifier.size(280.dp).graphicsLayer { scaleX = scale.value * 0.72f; scaleY = scale.value * 0.72f }.clip(CircleShape).background(Color(0xFF2E2E33)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (inhale) "Inhala" else "Exhala", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            Text("0:%02d".format(secondsLeft), color = Accent, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                    Text("El antojo dura unos minutos y luego baja. Sigue el círculo.", color = Ink2, textAlign = TextAlign.Center)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { phase = 1 }) { Text("Ya estoy mejor", color = Muted) }
                }
                1 -> {
                    Text("¿Qué tan fuerte era?", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("1 = leve · 5 = muy fuerte", color = Muted)
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        (1..5).forEach { n ->
                            ChoicePill("$n", false, Modifier.weight(1f)) {
                                Repo.update { it.copy(cravings = it.cravings + Craving(streakId, System.currentTimeMillis(), n)) }
                                Fx.success(); celebrate++; phase = 2
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                }
                else -> {
                    val n = Repo.data.cravings.count { it.streakId == streakId }
                    Text("💪", fontSize = 56.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Antojo superado", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Ya van $n. Cada uno que pasa hace más fácil el siguiente.", color = Ink2, textAlign = TextAlign.Center)
                    Spacer(Modifier.weight(1f))
                    PrimaryButton("Volver", onClick = onClose)
                }
            }
            if (phase != 2) Spacer(Modifier.height(8.dp))
        }
        Confetti(celebrate)
    }
}

/** Panel con los patrones de antojo de una racha. */
@Composable
fun CravingSheet(streak: Streak, onDismiss: () -> Unit) {
    val list = Repo.data.cravings.filter { it.streakId == streak.id }
    GlassSheet(onDismiss) {
        Text("ANTOJOS · SIN ${streak.name.uppercase()}", color = Muted, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(12.dp))
        Text("${list.size}", fontSize = 64.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("antojos superados", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Ink)
        if (list.isEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("Cuando sientas ganas, toca \"Tengo ganas\" y respira 60 segundos. Aquí verás a qué horas te pasa más.", color = Ink2)
            return@GlassSheet
        }
        val buckets = cravingBuckets(list)
        val peak = buckets.indices.maxBy { buckets[it] }
        Spacer(Modifier.height(8.dp))
        Text("Intensidad promedio ${"%.1f".format(list.map { it.intensity }.average())} de 5 · más frecuentes entre ${bucketLabel(peak)}.", color = Ink2)
        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.06f)).padding(18.dp)) {
            CardLabel("Por hora del día")
            Spacer(Modifier.height(12.dp))
            Bars(buckets, listOf("0", "3", "6", "9", "12", "15", "18", "21"), highlight = peak)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (list.size >= 5) "Puedes activar en Ajustes un aviso 30 minutos antes de tu hora difícil."
            else "Con ${5 - list.size} registro(s) más podrás activar un aviso antes de tu hora difícil.",
            color = Muted, style = MaterialTheme.typography.bodySmall,
        )
    }
}
