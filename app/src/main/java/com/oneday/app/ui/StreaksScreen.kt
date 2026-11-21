package com.oneday.app.ui

import androidx.compose.foundation.clickable
import com.oneday.app.R
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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
    else -> "Eres imparable."
}

@Composable
fun StreaksScreen(data: AppData) {
    var mode by rememberSaveable { mutableIntStateOf(0) } // 0 dejar · 1 construir
    var editing by remember { mutableStateOf<Streak?>(null) }
    var creating by remember { mutableStateOf(false) }
    var resetting by remember { mutableStateOf<Streak?>(null) }
    var cravingsOf by remember { mutableStateOf<Streak?>(null) }
    var heroId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var creatingHabit by remember { mutableStateOf(false) }
    val now = rememberNow()

    Column {
        ScreenTitle("Rachas", "Un día a la vez") {
            CircleButton(if (mode == 0) "Nueva racha" else "Nuevo hábito", accent = true, onClick = { if (mode == 0) creating = true else creatingHabit = true }) { Icon(Icons.Filled.Add, null, tint = Bg) }
        }
        Segmented(listOf("Dejar", "Construir"), mode, { mode = it }, Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        if (data.streaks.size + data.habits.size > 1) TipBubble("tip_reorder", "Mantén presionada una tarjeta para cambiar su orden.", Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        if (mode == 0) {
            if (data.streaks.isEmpty()) EmptyState("racha", "Empieza tu primera racha", "Agrega algo que quieras dejar (fumar, alcohol, azúcar, redes…) y cuenta cada día que lo logras.")
            val hero = data.streaks.firstOrNull { it.id == heroId } ?: data.streaks.maxByOrNull { streakDays(it, now) }
            val sListState = androidx.compose.foundation.lazy.rememberLazyListState()
            val sReorder = rememberReorderableLazyListState(sListState) { from, to ->
                Repo.update { d -> val l = d.streaks.toMutableList(); val f = l.indexOfFirst { it.id == from.key }; val t = l.indexOfFirst { it.id == to.key }
                    if (f >= 0 && t >= 0) l.add(t, l.removeAt(f)); d.copy(streaks = l) }
                Fx.tick()
            }
            LazyColumn(state = sListState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                hero?.let { s ->
                    item(key = "hero") {
                        StreakCard(s, now, data.cravings.count { it.streakId == s.id }, onEdit = { editing = s }, onReset = { resetting = s },
                            onSos = { UiState.sosStreak = s.id }, onCravings = { cravingsOf = s })
                    }
                }
                items(data.streaks.filter { it.id != hero?.id }, key = { it.id }) { s ->
                    ReorderableItem(sReorder, key = s.id) { dragging ->
                        val elev by androidx.compose.animation.core.animateDpAsState(if (dragging) 16.dp else 0.dp, label = "e")
                        Box(Modifier.shadow(elev, RoundedCornerShape(28.dp)).longPressDraggableHandle(onDragStarted = { Fx.tick() })) { StreakRow(s, now) { heroId = s.id } }
                    }
                }
            }
        } else {
            if (data.habits.isEmpty()) EmptyState("brote", "Construye un hábito", "Algo pequeño que quieras hacer cada día: tomar agua, leer, meditar. Tienes 1 día de gracia por semana.")
            val hListState = androidx.compose.foundation.lazy.rememberLazyListState()
            val hReorder = rememberReorderableLazyListState(hListState) { from, to ->
                Repo.update { d -> val l = d.habits.toMutableList(); val f = l.indexOfFirst { it.id == from.key }; val t = l.indexOfFirst { it.id == to.key }
                    if (f >= 0 && t >= 0) l.add(t, l.removeAt(f)); d.copy(habits = l) }
                Fx.tick()
            }
            LazyColumn(state = hListState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(data.habits, key = { it.id }) { h ->
                    ReorderableItem(hReorder, key = h.id) { dragging ->
                        val elev by androidx.compose.animation.core.animateDpAsState(if (dragging) 16.dp else 0.dp, label = "e")
                        Box(Modifier.shadow(elev, RoundedCornerShape(28.dp)).longPressDraggableHandle(onDragStarted = { Fx.tick() })) { HabitCard(h, onEdit = { editingHabit = h }) }
                    }
                }
            }
        }
    }
    if (creating) StreakDialog(null) { creating = false }
    editing?.let { s -> StreakDialog(s) { editing = null } }
    if (creatingHabit) HabitDialog(null) { creatingHabit = false }
    editingHabit?.let { h -> HabitDialog(h) { editingHabit = null } }
    cravingsOf?.let { s -> CravingSheet(s) { cravingsOf = null } }
    resetting?.let { s -> RelapseDialog(s) { resetting = null } }
}

/** Reiniciar una racha registrando qué la provocó. */
@Composable
fun RelapseDialog(s: Streak, onDismiss: () -> Unit) {
    var trigger by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    OdDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Reiniciar racha?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("No pasa nada, recaer es parte del proceso. Tu récord de ${maxOf(s.bestDays, streakDays(s))} día(s) se guarda y empiezas de nuevo hoy.")
                Text("¿Qué la provocó?", color = Muted, style = MaterialTheme.typography.labelLarge)
                TRIGGERS.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { tr -> ChoicePill(tr, trigger == tr, Modifier.weight(1f)) { trigger = tr } }
                    }
                }
                OutlinedTextField(note, { note = it }, label = { Text("Nota (opcional)") }, singleLine = true, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val days = streakDays(s)
                Repo.update { d -> d.copy(
                    streaks = d.streaks.map { if (it.id == s.id) it.copy(bestDays = maxOf(it.bestDays, days), start = System.currentTimeMillis(), resets = it.resets + 1) else it },
                    relapses = d.relapses + Relapse(s.id, System.currentTimeMillis(), trigger ?: "Otro", note.trim(), days),
                ) }
                onDismiss()
            }) { Text("Reiniciar", color = Bad) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Muted) } },
    )
}

fun addHabitAmount(h: Habit, delta: Int) = Repo.update { d -> d.copy(habits = d.habits.map { if (it.id == h.id) it.addAmount(delta) else it }) }

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
                val (flip, _) = rememberGoalCelebration(done)
                Box(Modifier.graphicsLayer { rotationY = flip; cameraDistance = 12f * density }) { IconChip(h.emoji, if (done) ChipState.Done else ChipState.Pending, 50.dp, h.name) }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(h.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text(
                        (h.remindAt?.let { "Recordatorio ${fmtTime(it)} · " } ?: "") + if (graceLeft(h)) "1 día de gracia disponible" else "Gracia usada esta semana",
                        color = Faint, style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = { Fx.tick(); UiState.share = habitShare(h) }) { Glyph("compartir", Faint, 20.dp, "Compartir") }
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Editar", tint = Faint) }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                RollingText("$streak", color = Accent, fontSize = 64.sp, fontWeight = FontWeight.Bold, lineHeight = 56.sp)
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
            MedalShelf(maxOf(habitBest(h), streak).toLong())
            Spacer(Modifier.height(16.dp))
            if (h.source == "steps") StepsDetail(h)
            else if (h.target != null) {
                val n = h.countToday()
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Hoy", color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 3.dp))
                    Spacer(Modifier.width(8.dp))
                    RollingText("$n", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(" / ${h.target} ${h.unit}", color = Muted, modifier = Modifier.padding(bottom = 3.dp))
                    Spacer(Modifier.weight(1f))
                    if (done) Row(Modifier.padding(bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) { Glyph("hecho", Good, 16.dp); Spacer(Modifier.width(4.dp)); Text("Meta cumplida", color = Good, style = MaterialTheme.typography.bodyMedium) }
                }
                Spacer(Modifier.height(8.dp))
                ThinBar((n.toFloat() / h.target).coerceIn(0f, 1f))
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircleButton("Restar", onClick = { Fx.tick(); addHabitAmount(h, -h.step) }) { Text("−", color = Ink, fontSize = 22.sp) }
                    PrimaryButton("Sumar ${h.step}", Modifier.weight(1f)) {
                        val wasDone = done
                        addHabitAmount(h, h.step)
                        val after = h.addAmount(h.step)
                        if (!wasDone && t in after.doneDays) Fx.success() else Fx.tick()
                    }
                }
            } else if (done) SecondaryButton("Hecho hoy · deshacer", color = Ink2) { Fx.tick(); toggleHabitToday(h) }
            else PrimaryButton("Marcar hecho hoy") {
                Fx.success(); toggleHabitToday(h)
            }
        }
        Confetti(celebrate, Modifier.matchParentSize())
    }
}


@Composable
fun HabitDialog(initial: Habit?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "agua") }
    var remind by remember { mutableStateOf(initial?.remindAt) }
    var withAmount by remember { mutableStateOf(initial?.target != null) }
    var target by remember { mutableStateOf((initial?.target ?: 8).toString()) }
    var unit by remember { mutableStateOf(initial?.unit?.ifEmpty { null } ?: "vasos") }
    var step by remember { mutableStateOf((initial?.step ?: 1).toString()) }
    var pickTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    OdDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuevo hábito" else "Editar hábito") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("¿Qué quieres hacer cada día?") }, singleLine = true, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
                Text("Ícono", color = Muted, style = MaterialTheme.typography.labelLarge)
                GlyphPicker(Glyphs.PICKABLE_BUILD, emoji) { emoji = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Con cantidad", color = Ink)
                        Text("Ej. 8 vasos, 20 minutos, 10000 pasos", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = withAmount, onCheckedChange = { withAmount = it })
                }
                if (withAmount) {
                    Stepper("Meta diaria", target, { target = it }, step = 1.0, decimal = false)
                    OutlinedTextField(unit, { unit = it }, label = { Text("Unidad") }, singleLine = true, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
                    Stepper("Cada toque suma", step, { step = it }, step = 1.0, decimal = false)
                }
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
                val goal = target.toIntOrNull()?.takeIf { withAmount && it > 0 }
                val h = (initial ?: Habit(name = "", createdDay = today())).copy(name = name.trim(), emoji = emoji, remindAt = remind,
                    target = goal, unit = if (goal != null) unit.trim() else "", step = step.toIntOrNull()?.coerceAtLeast(1) ?: 1)
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


/** Racha protagonista: ícono dentro del anillo hacia el próximo hito, número gigante, motivo, camino de hitos y vitrina. */
@Composable
fun StreakCard(s: Streak, now: Long, cravings: Int, onEdit: () -> Unit, onReset: () -> Unit, onSos: () -> Unit, onCravings: () -> Unit) {
    val days = streakDays(s, now)
    val hours = streakHours(s, now)
    val best = maxOf(s.bestDays, days)
    val prev = prevMilestone(days)
    val next = nextMilestone(days)
    val progress = ((days - prev).toFloat() / (next - prev).toFloat()).coerceIn(0f, 1f)
    val ring by androidx.compose.animation.core.animateFloatAsState(progress, label = "ring")
    OdCard(padding = 20.dp) {
        Box(Modifier.fillMaxWidth()) {
            IconButton(onClick = onEdit, modifier = Modifier.align(Alignment.TopEnd)) { Glyph("editar", Faint, 20.dp, "Editar racha") }
            IconButton(onClick = { Fx.tick(); UiState.share = streakShare(s) }, modifier = Modifier.align(Alignment.TopStart)) { Glyph("compartir", Faint, 20.dp, "Compartir racha") }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        val sw = 6.dp.toPx()
                        val tl = androidx.compose.ui.geometry.Offset(sw / 2, sw / 2)
                        val sz = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw)
                        drawArc(Color(0xFF1F1F23), 0f, 360f, false, tl, sz, style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
                        drawArc(Accent, -90f, 360f * ring, false, tl, sz, style = androidx.compose.ui.graphics.drawscope.Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    }
                    Box(Modifier.size(80.dp).clip(RoundedCornerShape(26.dp)).background(CardHigh).border(1.dp, Hairline, RoundedCornerShape(26.dp)), contentAlignment = Alignment.Center) {
                        Glyph(s.emoji, Ink, 38.dp, s.name)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    RollingText("$days", color = Accent, fontSize = 84.sp, fontWeight = FontWeight.Bold, lineHeight = 80.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.padding(bottom = 12.dp)) {
                        Text(if (days == 1L) "día sin ${s.name.lowercase()}" else "días sin ${s.name.lowercase()}", color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("y $hours h", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(if (s.reason.isNotBlank()) "“${s.reason}”" else motivation(days), color = if (s.reason.isNotBlank()) Ink2 else Muted,
                    style = MaterialTheme.typography.bodyLarge, fontStyle = if (s.reason.isNotBlank()) androidx.compose.ui.text.font.FontStyle.Italic else null,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        Spacer(Modifier.height(18.dp))
        MilestonePath(days)
        Spacer(Modifier.height(8.dp))
        Text("Próximo hito: $next días · faltan ${next - days}", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Tengo ganas", Modifier.weight(1.4f), glyph = "respirar", onClick = onSos)
            SecondaryButton("Recaí", Modifier.weight(1f), color = Ink2, onClick = onReset)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(R.drawable.ic_trophy, "$best d", "tu récord", Modifier.weight(1f))
            StatTile(R.drawable.ic_nav_flame, "$cravings", "antojos superados", Modifier.weight(1f), onClick = onCravings)
        }
        Spacer(Modifier.height(18.dp))
        Text("VITRINA", color = Muted, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.6.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        MedalShelf(best)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).clickable(onClick = onCravings).padding(vertical = 10.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (s.resets > 0) "${s.resets} reinicio(s)" else "Sin reinicios", color = Faint, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text("Ver patrones", color = Muted, style = MaterialTheme.typography.bodySmall)
            Glyph("flecha", Muted, 14.dp)
        }
    }
}

/** Camino de hitos: 1 · 3 · 7 · 14 · 30 · 60 · 90; los cumplidos llenos, el próximo con aro naranja. */
@Composable
fun MilestonePath(days: Long) {
    val ms = listOf(1L, 3, 7, 14, 30, 60, 90).let { base -> if (days >= 90) base.drop(3) + listOf(180L, 365L, 500L) else base }
    val next = ms.firstOrNull { it > days }
    Box(Modifier.fillMaxWidth().height(22.dp), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.padding(horizontal = 11.dp).fillMaxWidth().height(2.dp).background(Color(0xFF26262A)))
        val doneFrac = (ms.indexOfLast { it <= days }.coerceAtLeast(0)).toFloat() / (ms.size - 1)
        Box(Modifier.padding(horizontal = 11.dp).fillMaxWidth(doneFrac).height(2.dp).background(Ink2))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            ms.forEach { m ->
                Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                    when {
                        m <= days -> Box(Modifier.size(14.dp).clip(CircleShape).background(Ink2))
                        m == next -> Box(Modifier.size(18.dp).clip(CircleShape).background(Bg).border(2.5.dp, Accent, CircleShape))
                        else -> Box(Modifier.size(14.dp).clip(CircleShape).background(CardBg).border(2.dp, Line, CircleShape))
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ms.forEach { m ->
            Text("$m", modifier = Modifier.width(22.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelSmall,
                color = when { m == next -> Accent; m <= days -> Ink2; else -> Faint }, fontWeight = if (m == next) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

/** Racha secundaria compacta; al tocarla pasa a ser la protagonista. */
@Composable
fun StreakRow(s: Streak, now: Long, onClick: () -> Unit) {
    val days = streakDays(s, now)
    val prev = prevMilestone(days); val next = nextMilestone(days)
    OdCard(onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip(s.emoji, size = 46.dp, desc = s.name)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Sin ${s.name.lowercase()}", color = Ink, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                ThinBar(((days - prev).toFloat() / (next - prev)).coerceIn(0f, 1f))
            }
            Spacer(Modifier.width(14.dp))
            Text("$days", fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Ink)
        }
    }
}

@Composable
fun StreakDialog(initial: Streak?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "fumar") }
    var reason by remember { mutableStateOf(initial?.reason ?: "") }
    var startDay by remember { mutableStateOf(initial?.start?.let { millisToEpochDay(it) } ?: today()) }
    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    OdDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nueva racha" else "Editar racha") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("¿Qué quieres dejar? (ej. Fumar)") }, singleLine = true, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(reason, { reason = it }, label = { Text("Tu motivo (opcional)") }, placeholder = { Text("Ej. por mi hija", color = Faint) },
                    singleLine = true, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
                Text("Ícono", color = Muted, style = MaterialTheme.typography.labelLarge)
                GlyphPicker(Glyphs.PICKABLE_QUIT, emoji) { emoji = it }
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
                val s = (initial ?: Streak(name = "", start = start)).copy(name = name.trim(), emoji = emoji, start = start, reason = reason.trim())
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
