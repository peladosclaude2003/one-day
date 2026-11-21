package com.oneday.app.ui

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import com.oneday.app.data.*
import com.oneday.app.steps.Steps
import kotlinx.coroutines.launch
import java.time.LocalDate

fun fmtSteps(n: Int): String = "%,d".format(java.util.Locale.US, n)
fun fmtStepsShort(n: Int): String = if (n >= 1000) "%.1fk".format(java.util.Locale.US, n / 1000.0).replace(".0k", "k") else "$n"

/** Pide los permisos (Health Connect y, de respaldo, actividad física), crea el hábito y sincroniza. */
@Composable
fun rememberStepsConnector(goal: Int = Steps.GOAL_DEFAULT): () -> Unit {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    fun finish() { Steps.createHabit(goal); scope.launch { Steps.sync(ctx) } }
    val ar = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { finish() }
    fun askSensor() { if (Build.VERSION.SDK_INT >= 29 && Steps.sensorAvailable(ctx) && !Steps.sensorGranted(ctx)) ar.launch(android.Manifest.permission.ACTIVITY_RECOGNITION) else finish() }
    val hc = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { askSensor() }
    return {
        if (Steps.hcAvailable(ctx)) runCatching { hc.launch(Steps.HC_PERMS) }.onFailure { askSensor() } else askSensor()
    }
}

/** Tarjeta en Hoy para conectar los pasos (se muestra una vez). */
@Composable
fun StepsConnectCard() {
    val connect = rememberStepsConnector()
    OdCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip("pasos", ChipState.Pending, 46.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Cuenta tus pasos", color = Ink, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Text("Meta: ${fmtSteps(Steps.GOAL_DEFAULT)} al día", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("One Day leerá los pasos de Samsung Health (vía Health Connect) o, si no, los del sensor del teléfono. Se llenan solos en un hábito \"Caminar\" con racha y medallas.",
            color = Ink2, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Ahora no", Modifier.weight(1f), color = Ink2) { Repo.update { it.copy(settings = it.settings.copy(stepsAsked = true)) } }
            PrimaryButton("Conectar", Modifier.weight(1.3f), glyph = "pasos") { connect() }
        }
    }
}

/** Contenido del hábito de pasos: avance de hoy, semana en barras, fuente y sugerencia de meta. */
@Composable
fun StepsDetail(h: Habit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val t = today()
    val n = h.countToday(t)
    val goal = h.target ?: Steps.GOAL_DEFAULT
    var hc by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) { hc = Steps.hcGranted(ctx) }
    Row(verticalAlignment = Alignment.Bottom) {
        RollingText(fmtSteps(n), color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(" / ${fmtSteps(goal)} pasos", color = Muted, modifier = Modifier.padding(bottom = 4.dp))
        Spacer(Modifier.weight(1f))
        if (n >= goal) Row(Modifier.padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Glyph("hecho", Good, 16.dp); Spacer(Modifier.width(4.dp)); Text("Meta cumplida", color = Good, style = MaterialTheme.typography.bodyMedium)
        } else Text("faltan ${fmtSteps(goal - n)}", color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 4.dp))
    }
    Spacer(Modifier.height(8.dp))
    ThinBar((n.toFloat() / goal).coerceIn(0f, 1f))
    Spacer(Modifier.height(16.dp))
    // Semana: barra de hoy en naranja, la meta como referencia
    val monday = mondayOf(t)
    val week = (0 until 7).map { h.counts[monday + it] ?: 0 }
    WeekBars(week.map { it / 100 }, barArea = 70.dp, compact = false)
    Text("Barras en cientos de pasos · lunes a domingo", color = Faint, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    Spacer(Modifier.height(12.dp))
    val mins = ((System.currentTimeMillis() - Steps.lastSync(ctx)) / 60000).toInt()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            (when (hc) { true -> "Health Connect"; false -> if (Steps.sensorAvailable(ctx)) "Sensor del teléfono" else "Sin fuente de pasos"; null -> "…" }) +
                if (Steps.lastSync(ctx) > 0) " · actualizado hace ${if (mins < 1) "un momento" else "$mins min"}" else "",
            color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { Fx.tick(); scope.launch { Steps.sync(ctx); hc = Steps.hcGranted(ctx) } }) { Text("Actualizar", color = Accent) }
    }
    // Meta que se ajusta sola
    val sug = Steps.suggestedGoal(h)
    if (sug != null && System.currentTimeMillis() - Repo.data.settings.stepsGoalDismissedAt > 14L * DAY_MS) {
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(AccentSoft).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Llevas 3 semanas superando tu meta.", color = Ink, fontWeight = FontWeight.SemiBold)
            Text("¿La subimos a ${fmtSteps(sug)} pasos?", color = Ink2, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Ahora no", Modifier.weight(1f), color = Ink2) { Repo.update { it.copy(settings = it.settings.copy(stepsGoalDismissedAt = System.currentTimeMillis())) } }
                PrimaryButton("Subir meta", Modifier.weight(1f)) { Fx.success(); Repo.update { d -> d.copy(habits = d.habits.map { if (it.id == h.id) it.copy(target = sug) else it }) } }
            }
        }
    }
}

/** Sección de pasos en Ajustes. */
@Composable
fun StepsSettings() {
    val ctx = LocalContext.current
    val connect = rememberStepsConnector()
    val h = Repo.data.habits.firstOrNull { it.source == "steps" }
    var hc by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(h) { hc = Steps.hcGranted(ctx) }
    Text("PASOS", color = Faint, letterSpacing = 1.2.sp, style = MaterialTheme.typography.labelLarge)
    Text(
        when {
            h == null -> "Sin conectar"
            hc == true -> "Conectado a Health Connect" + if (Steps.sensorGranted(ctx)) " y al sensor" else ""
            Steps.sensorGranted(ctx) && Steps.sensorAvailable(ctx) -> "Usando el sensor del teléfono"
            else -> "Sin permisos: toca Conectar"
        }, color = Ink,
    )
    if (h != null) {
        Text("Meta diaria", color = Muted, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(6000, 8000, 10000, 12000, 15000).forEach { g ->
                ChoicePill(fmtStepsShort(g), h.target == g, Modifier.weight(1f)) { Repo.update { d -> d.copy(habits = d.habits.map { if (it.id == h.id) it.copy(target = g) else it }) } }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SecondaryButton(if (h == null) "Conectar" else "Revisar permisos", Modifier.weight(1f)) { connect() }
        if (Steps.hcAvailable(ctx)) SecondaryButton("Health Connect", Modifier.weight(1f)) {
            runCatching { ctx.startActivity(Intent("android.health.connect.action.HEALTH_HOME_SETTINGS")) }
        }
    }
    Text("En tu Samsung: abre Samsung Health › Ajustes › Health Connect y permite compartir \"Pasos\".", color = Muted, style = MaterialTheme.typography.bodySmall)
}
