package com.oneday.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    var mode by rememberSaveable { mutableIntStateOf(0) } // 0 dejar · 1 construir
    var editing by remember { mutableStateOf<Streak?>(null) }
    var creating by remember { mutableStateOf(false) }
    var resetting by remember { mutableStateOf<Streak?>(null) }
    var cravingsOf by remember { mutableStateOf<Streak?>(null) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var creatingHabit by remember { mutableStateOf(false) }
    val now = rememberNow()

    Column {
        ScreenTitle("Rachas", "Un día a la vez") {
            CircleButton(if (mode == 0) "Nueva racha" else "Nuevo hábito", accent = true, onClick = { if (mode == 0) creating = true else creatingHabit = true }) { Icon(Icons.Filled.Add, null, tint = Bg) }
        }
        Segmented(listOf("Dejar", "Construir"), mode, { mode = it }, Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        if (mode == 0) {
            if (data.streaks.isEmpty()) EmptyState("🔥", "Empieza tu primera racha", "Agrega algo que quieras dejar (fumar, alcohol, azúcar, redes…) y cuenta cada día que lo logras.")
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(data.streaks, key = { it.id }) { s ->
                    StreakCard(s, now, data.cravings.count { it.streakId == s.id }, onEdit = { editing = s }, onReset = { resetting = s },
                        onSos = { UiState.sosStreak = s.id }, onCravings = { cravingsOf = s })
                }
            }
        } else {
            if (data.habits.isEmpty()) EmptyState("🌱", "Construye un hábito", "Algo pequeño que quieras hacer cada día: tomar agua, leer, meditar. Tienes 1 día de gracia por semana.")
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(data.habits, key = { it.id }) { h -> HabitCard(h, onEdit = { editingHabit = h }) }
            }
        }
    }
    if (creating) StreakDialog(null) { creating = false }
    editing?.let { s -> StreakDialog(s) { editing = null } }
    if (creatingHabit) HabitDialog(null) { creatingHabit = false }
    editingHabit?.let { h -> HabitDialog(h) { editingHabit = null } }
    cravingsOf?.let { s -> CravingSheet(s) { cravingsOf = null } }
    resetting?.let { s ->
        OdDialog(
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
            dismissButton = { TextButton(onClick = { resetting = null }) { Text("Cancelar", color = Muted) } },
        )
    }
}

fun toggleHabitToday(h: Habit) = Repo.update { d ->
    val t = today()
    d.copy(habits = d.habits.map { if (it.id == h.id) it.copy(doneDays = if (t in it.doneDays) it.doneDays - t else it.doneDays + t) else it })
}

@Composable
fun HabitCard(h: Habit, onEdit: () -> Unit) {
    val t = today()
    val done = t in h.doneDays
    val streak = habitStreak(h)
    var celebrate by remember { mutableIntStateOf(0) }
    Box {
        OdCard(padding = 22.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(CardHigh), contentAlignment = Alignment.Center) { Text(h.emoji, fontSize = 26.sp) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(h.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text(
                        (h.remindAt?.let { "Recordatorio ${fmtTime(it)} · " } ?: "") + if (graceLeft(h)) "1 día de gracia disponible" else "Gracia usada esta semana",
                        color = Faint, style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Editar", tint = Faint) }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                RollingText("$streak", color = Accent, fontSize = 56.sp, fontWeight = FontWeight.Bold, lineHeight = 56.sp)
                Spacer(Modifier.width(8.dp))
                Text(if (streak == 1) "día seguido" else "días seguidos", color = Ink, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 8.dp)) {
                    OdIcon(com.oneday.app.R.drawable.ic_trophy, Muted, 18.dp)
                    Text("${maxOf(habitBest(h), streak)} d", fontWeight = FontWeight.SemiBold, color = Ink)
                }
            }
            Spacer(Modifier.height(12.dp))
            // Últimos 7 días
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (6 downTo 0).forEach { ago ->
                    val d = t - ago
                    val on = d in h.doneDays
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(50))
                            .background(if (on) Ink2 else CardHigh)
                            .then(if (ago == 0) Modifier.border(2.dp, Accent, RoundedCornerShape(50)) else Modifier))
                        Spacer(Modifier.height(4.dp))
                        Text(listOf("L", "M", "X", "J", "V", "S", "D")[java.time.LocalDate.ofEpochDay(d).dayOfWeek.value - 1],
                            color = if (ago == 0) Accent else Faint, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (done) SecondaryButton("✓ Hecho hoy · deshacer", color = Ink2) { Fx.tick(); toggleHabitToday(h) }
            else PrimaryButton("Marcar hecho hoy") {
                Fx.success(); toggleHabitToday(h)
                if (habitStreak(h.copy(doneDays = h.doneDays + t)).toLong() in MILESTONES) celebrate++
            }
        }
        Confetti(celebrate, Modifier.matchParentSize())
    }
}

private val HABIT_EMOJIS = listOf("💧", "📖", "🧘", "🚶", "😴", "🥗", "🏃", "✍️", "🦷", "☀️", "🎸", "🧠")

@Composable
fun HabitDialog(initial: Habit?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "💧") }
    var remind by remember { mutableStateOf(initial?.remindAt) }
    var pickTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    OdDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuevo hábito" else "Editar hábito") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("¿Qué quieres hacer cada día?") }, singleLine = true, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
                Text("Ícono", color = Muted, style = MaterialTheme.typography.labelLarge)
                EmojiGrid(HABIT_EMOJIS, emoji) { emoji = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Recordatorio diario", color = Ink)
                        Text("Solo si ese día no lo has marcado", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = remind != null, onCheckedChange = { if (it) pickTime = true else remind = null })
                }
                if (remind != null) ChoicePill(fmtTime(remind!!), false, lead = { OdIcon(com.oneday.app.R.drawable.ic_clock, Muted, 16.dp) }) { pickTime = true }
                if (initial != null) TextButton(onClick = { confirmDelete = true }, contentPadding = PaddingValues(0.dp)) { Text("Eliminar hábito", color = Bad) }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                val h = (initial ?: Habit(name = "", createdDay = today())).copy(name = name.trim(), emoji = emoji, remindAt = remind)
                Repo.update { d -> d.copy(habits = if (initial == null) d.habits + h else d.habits.map { if (it.id == h.id) h else it }) }
                onDismiss()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Muted) } },
    )
    if (pickTime) TimePickDialog(remind ?: (20 * 60), { pickTime = false }) { remind = it }
    if (confirmDelete && initial != null) ConfirmDialog("Eliminar hábito", "¿Eliminar \"${initial.name}\"? Se perderá su historial.", onDismiss = { confirmDelete = false }) {
        Repo.update { d -> d.copy(habits = d.habits.filterNot { it.id == initial.id }) }
        onDismiss()
    }
}

@Composable
fun EmojiGrid(options: List<String>, selected: String, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { e ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (e == selected) Ink2.copy(alpha = 0.35f) else Color(0xFF2A2A2E),
                        modifier = Modifier.size(40.dp).clickable { onPick(e) },
                    ) { Box(contentAlignment = Alignment.Center) { Text(e, fontSize = 20.sp) } }
                }
            }
        }
    }
}

@Composable
fun StreakCard(s: Streak, now: Long, cravings: Int, onEdit: () -> Unit, onReset: () -> Unit, onSos: () -> Unit, onCravings: () -> Unit) {
    val days = streakDays(s, now)
    val hours = streakHours(s, now)
    val best = maxOf(s.bestDays, days)
    val prev = prevMilestone(days)
    val next = nextMilestone(days)
    val progress = ((days - prev).toFloat() / (next - prev).toFloat()).coerceIn(0f, 1f)
    OdCard(padding = 22.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(CardHigh), contentAlignment = Alignment.Center) { Text(s.emoji, fontSize = 26.sp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Sin ${s.name.lowercase()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(motivation(days), color = Faint, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Editar", tint = Faint) }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            RollingText("$days", color = Accent, fontSize = 64.sp, fontWeight = FontWeight.Bold, lineHeight = 64.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.padding(bottom = 8.dp)) {
                Text(if (days == 1L) "día" else "días", style = MaterialTheme.typography.titleMedium, color = Ink)
                Text("$hours h", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 8.dp)) {
                OdIcon(com.oneday.app.R.drawable.ic_trophy, Muted, 18.dp)
                Text("$best d", fontWeight = FontWeight.SemiBold, color = Ink)
                Text("récord", color = Faint, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(14.dp))
        ThinBar(progress)
        Spacer(Modifier.height(8.dp))
        Row {
            Text("Próxima meta: $next días", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text("faltan ${next - days}", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Tengo ganas", Modifier.weight(1.3f), onClick = onSos)
            SecondaryButton("Recaí", Modifier.weight(1f), color = Ink2, onClick = onReset)
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).clickable(onClick = onCravings).padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                (if (cravings > 0) "$cravings antojo(s) superado(s)" else "Sin antojos registrados") + if (s.resets > 0) " · ${s.resets} reinicio(s)" else "",
                color = Faint, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f),
            )
            Text("Ver patrones ›", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
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

    OdDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nueva racha" else "Editar racha") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("¿Qué quieres dejar? (ej. Fumar)") }, singleLine = true, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
                Text("Ícono", color = Muted, style = MaterialTheme.typography.labelLarge)
                EmojiGrid(EMOJIS, emoji) { emoji = it }
                Text("¿Desde cuándo?", color = Muted, style = MaterialTheme.typography.labelLarge)
                ChoicePill(fmtDay(startDay), false, lead = { OdIcon(com.oneday.app.R.drawable.ic_calendar, Muted, 16.dp) }) { pickDate = true }
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Muted) } },
    )
    if (pickDate) DatePickDialog(startDay, { pickDate = false }) { startDay = minOf(it, today()) }
    if (confirmDelete && initial != null) ConfirmDialog("Eliminar racha", "¿Eliminar la racha \"${initial.name}\"? Se perderá su historial.", onDismiss = { confirmDelete = false }) {
        Repo.update { d -> d.copy(streaks = d.streaks.filterNot { it.id == initial.id }) }
        onDismiss()
    }
}
