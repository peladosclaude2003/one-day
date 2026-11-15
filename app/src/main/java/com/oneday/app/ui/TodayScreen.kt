package com.oneday.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.oneday.app.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneday.app.data.*
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun TodayScreen(data: AppData, onOpenSettings: () -> Unit, onGoTab: (Int) -> Unit) {
    val t = today()
    val now = rememberNow()
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var day by rememberSaveable { mutableLongStateOf(t) }
    val name = data.settings.userName.trim().split(" ").firstOrNull().orEmpty()
    val greeting = when (LocalTime.now().hour) { in 5..11 -> "Buenos días"; in 12..18 -> "Buenas tardes"; else -> "Buenas noches" } +
        if (name.isNotEmpty()) ", $name" else ""

    val todays = data.tasks.filter { it.date == t }.sortedWith(compareBy<Task> { it.done }.then(taskOrder))
    val dayTasks = data.tasks.filter { it.date == day }.sortedWith(compareBy<Task> { it.time ?: Int.MAX_VALUE })
    val overdue = data.tasks.filter { !it.done && it.date != null && it.date < t }
    val next = todays.firstOrNull { !it.done }
    val doneCount = todays.count { it.done }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 120.dp)) {
        ScreenTitle(greeting, fmtLongDate(LocalDate.now())) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircleButton("Ajustes", onClick = onOpenSettings) { Glyph("ajustes", Ink2, 20.dp) }
                CircleButton("Agregar al día", accent = true, onClick = { creating = true }) { Glyph("mas", Bg, 20.dp) }
            }
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            WeekStrip(data, day) { day = it }

            // Tarjeta principal: lo siguiente
            OdCard(padding = 20.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (next != null) "SIGUIENTE" else "HOY", color = Muted, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    next?.time?.let { tm ->
                        val mins = tm - (LocalTime.now().hour * 60 + LocalTime.now().minute)
                        val label = when {
                            mins in 1..59 -> "en $mins min"
                            mins >= 60 -> "en ${mins / 60} h ${if (mins % 60 > 0) "${mins % 60} min" else ""}".trim()
                            mins == 0 -> "ahora"
                            else -> "hace ${-mins} min".takeIf { mins > -60 } ?: "atrasado"
                        }
                        Row(Modifier.clip(RoundedCornerShape(50)).background(AccentSoft).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Glyph("reloj", AccentText, 14.dp); Spacer(Modifier.width(6.dp))
                            Text(label, color = AccentText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                DashedBox {
                    Text(next?.title ?: if (todays.isNotEmpty()) "Día completo" else "Día libre",
                        fontFamily = Display, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 2,
                        overflow = TextOverflow.Ellipsis, lineHeight = 32.sp, letterSpacing = (-0.6).sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        next != null -> "${todays.size - doneCount} pendiente(s) para hoy"
                        todays.isNotEmpty() -> "Terminaste todo lo de hoy"
                        else -> "No tienes nada agendado para hoy"
                    }, color = Muted, style = MaterialTheme.typography.bodyMedium,
                )
                if (next != null) {
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        next.time?.let { InfoPill(fmtTime(it)) }
                        next.area?.let { InfoPill(it) }
                        InfoPill("Prioridad ${priorityName(next.priority).lowercase()}")
                    }
                }
                Spacer(Modifier.height(18.dp))
                if (next != null) Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    PrimaryButton("Hecho", Modifier.weight(1f), glyph = "hecho") { Fx.success(); completeWithUndo(next) }
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(CardHigh).clickable(role = Role.Button, onClickLabel = "Pasar a mañana") { Fx.tick(); postponeWithUndo(next) },
                        contentAlignment = Alignment.Center,
                    ) { Glyph("flecha", Ink2, 22.dp) }
                } else PrimaryButton("Planear mi día", glyph = "mas") { creating = true }
            }

            // Hábitos como fichas
            if (data.habits.isNotEmpty()) {
                val doneH = data.habits.count { t in it.doneDays }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Hábitos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.weight(1f).padding(start = 4.dp))
                    Text("$doneH de ${data.habits.size}", color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 4.dp))
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(data.habits, key = { it.id }) { h -> HabitTile(h, t) }
                }
            }

            // Agenda del día elegido
            OdCard(padding = 0.dp) {
                Row(Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Glyph("agenda", Muted, 18.dp); Spacer(Modifier.width(8.dp))
                    Text(if (day == t) "Agenda de hoy" else "Agenda · ${fmtDay(day).lowercase()}", color = Muted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    if (day != t) TextButton(onClick = { day = t }) { Text("Hoy", color = Accent) }
                }
                if (dayTasks.isEmpty()) Text("Nada agendado.", color = Faint, modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp))
                dayTasks.forEachIndexed { i, task ->
                    SwipeTask(task) {
                        Box(Modifier.background(CardBg)) { AgendaRow(task, last = i == dayTasks.lastIndex, onToggle = { toggleTask(task) }, onClick = { editing = task }) }
                    }
                }
                if (overdue.isNotEmpty() && day == t) {
                    Text("${overdue.size} pendiente(s) atrasado(s) · ver", color = Bad, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).clip(RoundedCornerShape(50)).clickable { onGoTab(1) }.padding(8.dp))
                }
                Spacer(Modifier.height(8.dp))
            }

            // Rachas
            if (data.streaks.isNotEmpty()) OdCard(onClick = { onGoTab(2) }) {
                CardLabel("Rachas", R.drawable.ic_nav_flame)
                Spacer(Modifier.height(6.dp))
                data.streaks.forEach { s ->
                    val days = streakDays(s, now)
                    val prev = prevMilestone(days); val nxt = nextMilestone(days)
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconChip(s.emoji, size = 40.dp, desc = s.name)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Sin ${s.name.lowercase()}", color = Ink)
                            Spacer(Modifier.height(6.dp))
                            ThinBar(((days - prev).toFloat() / (nxt - prev)).coerceIn(0f, 1f))
                        }
                        Spacer(Modifier.width(16.dp))
                        Text("$days d", color = Ink, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }

            // Semana de gimnasio
            OdCard(onClick = { onGoTab(3) }) {
                CardLabel("Gimnasio esta semana", R.drawable.ic_nav_gym, trailing = {
                    Text(if (data.activeWorkout != null) "En curso" else if (data.workouts.any { it.date == t }) "Hoy listo" else "",
                        color = if (data.activeWorkout != null) Accent else Good, style = MaterialTheme.typography.bodyMedium)
                })
                Spacer(Modifier.height(14.dp))
                WeekDots(data)
            }
        }
    }
    if (creating) QuickAddSheet(defaultDate = day) { creating = false }
    editing?.let { TaskDialog(it, isNew = false, onDismiss = { editing = null }) }
}

/** Tira de la semana: toca un día para ver su agenda. Punto claro = hiciste algo ese día. */
@Composable
fun WeekStrip(data: AppData, selected: Long, onPick: (Long) -> Unit) {
    val t = today()
    val monday = mondayOf(t)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("L", "M", "X", "J", "V", "S", "D").forEachIndexed { i, l ->
            val d = monday + i
            val sel = d == selected
            val active = data.tasks.any { it.done && it.doneAt?.let { a -> millisToEpochDay(a) } == d } ||
                data.habits.any { d in it.doneDays } || data.workouts.any { it.date == d }
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (sel) CardHigh else CardBg)
                    .then(if (d == t) Modifier.border(1.5.dp, Accent, RoundedCornerShape(16.dp)) else Modifier.border(1.dp, Hairline, RoundedCornerShape(16.dp)))
                    .clickable(role = Role.Tab, onClickLabel = fmtDay(d)) { Fx.tick(); onPick(d) }.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(l, color = Muted, style = MaterialTheme.typography.labelSmall)
                Text("${LocalDate.ofEpochDay(d).dayOfMonth}", fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    color = if (d == t) Accent else if (d > t) Muted else Ink)
                Box(Modifier.size(5.dp).clip(CircleShape).background(when { d == t -> Accent; active -> Ink2; else -> Line }))
            }
        }
    }
}

/** Ficha de hábito con anillo de avance; tocarla marca (o suma) el de hoy. */
@Composable
fun HabitTile(h: Habit, t: Long) {
    val done = t in h.doneDays
    val p = when { done -> 1f; h.target != null -> (h.countToday(t).toFloat() / h.target).coerceIn(0f, 1f); else -> 0f }
    val anim by androidx.compose.animation.core.animateFloatAsState(p, label = "ring")
    Column(
        Modifier.width(96.dp).clip(RoundedCornerShape(22.dp)).background(CardBg).border(1.dp, Hairline, RoundedCornerShape(22.dp))
            .clickable(role = Role.Button, onClickLabel = h.name) {
                if (h.target != null) { addHabitAmount(h, h.step); if (h.addAmount(h.step).let { t in it.doneDays } && !done) Fx.success() else Fx.tick() }
                else { if (!done) Fx.success() else Fx.tick(); toggleHabitToday(h) }
            }.padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(50.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val sw = 3.dp.toPx()
                drawArc(Line, 0f, 360f, false, style = Stroke(sw), topLeft = androidx.compose.ui.geometry.Offset(sw / 2, sw / 2), size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw))
                if (anim > 0f) drawArc(if (done) Ink else Accent, -90f, 360f * anim, false, style = Stroke(sw, cap = StrokeCap.Round),
                    topLeft = androidx.compose.ui.geometry.Offset(sw / 2, sw / 2), size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw))
            }
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(when { done -> Ink; p > 0f -> AccentSoft; else -> CardHigh }), contentAlignment = Alignment.Center) {
                Glyph(h.emoji, when { done -> Bg; p > 0f -> Accent; else -> Ink2 }, 19.dp, h.name)
            }
        }
        Text(h.name, color = Ink2, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center, minLines = 2, lineHeight = 16.sp)
        Text(
            when { h.target != null -> "${h.countToday(t)} / ${h.target}"; done -> "Hecho"; else -> "Pendiente" },
            color = if (done) Ink2 else Muted, style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun AgendaRow(task: Task, last: Boolean, onToggle: () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(task.time?.let { "%d:%02d".format(it / 60, it % 60) } ?: "—", fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 15.sp,
            color = if (task.done) Faint else if (task.priority == 2) Accent else Ink2, modifier = Modifier.width(48.dp))
        Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
            Text(task.title, color = if (task.done) Muted else Ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.done) TextDecoration.LineThrough else null)
            val sub = listOfNotNull(task.area, task.time?.let { fmtTime(it) }).joinToString(" · ")
            if (sub.isNotEmpty()) Text(sub, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        RoundCheck(task.done, onToggle, task.title)
    }
    if (!last) Box(Modifier.padding(start = 66.dp, end = 18.dp).fillMaxWidth().height(1.dp).background(Color1F))
}

private val Color1F = androidx.compose.ui.graphics.Color(0xFF1F1F23)

@Composable
fun ThinBar(progress: Float) {
    Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(CardHigh)) {
        Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(50)).background(Accent))
    }
}

@Composable
fun WeekDots(data: AppData) {
    val now = LocalDate.now()
    val monday = now.minusDays((now.dayOfWeek.value - 1).toLong())
    val trained = data.workouts.map { it.date }.toSet()
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("L", "M", "X", "J", "V", "S", "D").forEachIndexed { i, l ->
            val day = monday.plusDays(i.toLong())
            val on = day.toEpochDay() in trained
            val isToday = day == now
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(50)).background(if (on) Ink2 else CardHigh)
                    .then(if (isToday) Modifier.border(2.5.dp, Accent, RoundedCornerShape(50)) else Modifier))
                Spacer(Modifier.height(6.dp))
                Text(l, style = MaterialTheme.typography.labelSmall, color = if (isToday) Accent else Faint, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}
