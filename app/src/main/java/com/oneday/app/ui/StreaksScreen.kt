package com.oneday.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneday.app.data.*
import kotlinx.coroutines.delay

@Composable
fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }
    return now
}

fun motivation(days: Long) = when {
    days < 1 -> "El primer día es el más importante. Tú puedes."
    days < 3 -> "Las primeras 72 horas son las más duras. Aguanta."
    days < 7 -> "¡Vas por tu primera semana!"
    days < 30 -> "Tu cuerpo ya lo nota. Sigue sumando días."
    days < 90 -> "Más de un mes. Esto ya es un hábito nuevo."
    else -> "Eres imparable. 💪"
}

@Composable
fun StreaksScreen(data: AppData) {
    var editing by remember { mutableStateOf<Streak?>(null) }
    var creating by remember { mutableStateOf(false) }
    var resetting by remember { mutableStateOf<Streak?>(null) }
    val now = rememberNow()

    Scaffold(
        containerColor = Bg,
        floatingActionButton = { FloatingActionButton(onClick = { creating = true }, containerColor = Accent) { Icon(Icons.Filled.Add, "Nueva racha") } },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            ScreenTitle("Rachas", "Un día a la vez")
            if (data.streaks.isEmpty()) {
                EmptyState("🔥", "Empieza tu primera racha", "Agrega algo que quieras dejar (fumar, alcohol, azúcar, redes…) y cuenta cada día que lo logras.")
            }
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(data.streaks, key = { it.id }) { s ->
                    StreakCard(s, now, onEdit = { editing = s }, onReset = { resetting = s })
                }
            }
        }
    }
    if (creating) StreakDialog(null) { creating = false }
    editing?.let { s -> StreakDialog(s) { editing = null } }
    resetting?.let { s ->
        AlertDialog(
            onDismissRequest = { resetting = null },
            title = { Text("¿Reiniciar racha?") },
            text = { Text("No pasa nada, recaer es parte del proceso. Tu récord de ${maxOf(s.bestDays, streakDays(s))} día(s) se guarda y empiezas de nuevo hoy.") },
            confirmButton = {
                TextButton(onClick = {
                    Repo.update { d -> d.copy(streaks = d.streaks.map {
                        if (it.id == s.id) it.copy(bestDays = maxOf(it.bestDays, streakDays(it)), start = System.currentTimeMillis(), resets = it.resets + 1) else it
                    }) }
                    resetting = null
                }) { Text("Reiniciar", color = Bad) }
            },
            dismissButton = { TextButton(onClick = { resetting = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
fun StreakCard(s: Streak, now: Long, onEdit: () -> Unit, onReset: () -> Unit) {
    val days = streakDays(s, now)
    val hours = streakHours(s, now)
    val best = maxOf(s.bestDays, days)
    val prev = prevMilestone(days)
    val next = nextMilestone(days)
    val progress = ((days - prev).toFloat() / (next - prev).toFloat()).coerceIn(0f, 1f)
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.emoji, fontSize = 32.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Sin ${s.name.lowercase()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(motivation(days), color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Editar", tint = Muted) }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$days", fontSize = 52.sp, fontWeight = FontWeight.Bold, color = Accent, lineHeight = 52.sp)
            Spacer(Modifier.width(8.dp))
            Text(if (days == 1L) "día" else "días", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            Spacer(Modifier.width(6.dp))
            Text("$hours h", color = Muted, modifier = Modifier.padding(bottom = 9.dp))
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 6.dp)) {
                Text("🏆 Récord", color = Muted, style = MaterialTheme.typography.labelMedium)
                Text("$best día(s)", fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp), color = Accent, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {})
        Spacer(Modifier.height(6.dp))
        Row {
            Text("Próxima meta: $next días", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text("faltan ${next - days}", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Recaí, reiniciar contador") }
        if (s.resets > 0) Text("Reinicios: ${s.resets}", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
    }
}

private val EMOJIS = listOf("🚭", "🍺", "🍬", "📱", "🎮", "☕", "🍔", "🥤", "🌿", "🎰", "🛒", "💤")

@Composable
fun StreakDialog(initial: Streak?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "🚭") }
    var startDay by remember { mutableStateOf(initial?.start?.let { millisToEpochDay(it) } ?: today()) }
    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nueva racha" else "Editar racha") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("¿Qué quieres dejar? (ej. Fumar)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Ícono", color = Muted, style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    EMOJIS.chunked(6).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { e ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (e == emoji) Accent.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.size(40.dp).clickable { emoji = e },
                                ) { Box(contentAlignment = Alignment.Center) { Text(e, fontSize = 20.sp) } }
                            }
                        }
                    }
                }
                Text("¿Desde cuándo?", color = Muted, style = MaterialTheme.typography.labelLarge)
                AssistChip(onClick = { pickDate = true }, label = { Text("📅 " + fmtDay(startDay)) })
                if (initial != null) TextButton(onClick = { confirmDelete = true }, contentPadding = PaddingValues(0.dp)) { Text("Eliminar racha", color = Bad) }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                val now = System.currentTimeMillis()
                // Si la fecha elegida es hoy, cuenta desde este momento; si es otro día, desde la hora actual de ese día.
                val start = if (startDay == today()) (if (initial != null && millisToEpochDay(initial.start) == today()) initial.start else now)
                    else now - (today() - startDay) * DAY_MS
                val s = (initial ?: Streak(name = "", start = start)).copy(name = name.trim(), emoji = emoji, start = start)
                Repo.update { d -> d.copy(streaks = if (initial == null) d.streaks + s else d.streaks.map { if (it.id == s.id) s else it }) }
                onDismiss()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
    if (pickDate) DatePickDialog(startDay, { pickDate = false }) { startDay = minOf(it, today()) }
    if (confirmDelete && initial != null) ConfirmDialog("Eliminar racha", "¿Eliminar la racha \"${initial.name}\"? Se perderá su historial.", onDismiss = { confirmDelete = false }) {
        Repo.update { d -> d.copy(streaks = d.streaks.filterNot { it.id == initial.id }) }
        onDismiss()
    }
}
