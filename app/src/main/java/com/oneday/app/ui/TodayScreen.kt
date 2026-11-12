package com.oneday.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneday.app.R
import com.oneday.app.data.*
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun TodayScreen(data: AppData, onOpenSettings: () -> Unit, onGoTab: (Int) -> Unit) {
    val t = today()
    val now = rememberNow()
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Task?>(null) }
    val greeting = when (LocalTime.now().hour) { in 5..11 -> "Buenos días"; in 12..18 -> "Buenas tardes"; else -> "Buenas noches" }

    val todays = data.tasks.filter { it.date == t }.sortedWith(compareBy<Task> { it.done }.then(taskOrder))
    val overdue = data.tasks.filter { !it.done && it.date != null && it.date < t }
    val doneCount = todays.count { it.done }
    val next = todays.firstOrNull { !it.done }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 120.dp)) {
        ScreenTitle(greeting, fmtLongDate(LocalDate.now())) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircleButton("Ajustes", onClick = onOpenSettings) { Icon(Icons.Filled.Settings, null, tint = Muted) }
                CircleButton("Agregar al día", accent = true, onClick = { creating = true }) { Icon(Icons.Filled.Add, null, tint = Bg) }
            }
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Tarjeta principal: lo siguiente por hacer
            OdCard(padding = 22.dp) {
                Text(if (next != null) "Siguiente" else "Hoy", color = Muted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                DashedBox {
                    Text(next?.title ?: if (todays.isNotEmpty()) "¡Día completo!" else "Día libre",
                        fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 36.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        next != null -> "${todays.size - doneCount} pendiente(s) para hoy"
                        todays.isNotEmpty() -> "Terminaste todo lo de hoy 🎉"
                        else -> "No tienes nada agendado para hoy"
                    }, color = Faint, style = MaterialTheme.typography.bodyMedium,
                )
                if (next != null) {
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoPill(next.time?.let { fmtTime(it) } ?: "Sin hora", R.drawable.ic_clock)
                        InfoPill("Prioridad ${priorityName(next.priority).lowercase()}")
                    }
                }
                Spacer(Modifier.height(20.dp))
                if (next != null) PrimaryButton("Marcar como hecha") { toggleTask(next) }
                else PrimaryButton("Planear mi día") { creating = true }
            }

            // Mosaicos
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(R.drawable.ic_nav_tasks, if (todays.isEmpty()) "0" else "$doneCount/${todays.size}", "hechas hoy", Modifier.weight(1f)) { onGoTab(1) }
                val top = data.streaks.maxByOrNull { streakDays(it, now) }
                StatTile(R.drawable.ic_nav_flame, top?.let { "${streakDays(it, now)}" } ?: "–", top?.let { "días sin ${it.name.lowercase()}" } ?: "rachas", Modifier.weight(1f), valueColor = if (top != null) Accent else Ink) { onGoTab(2) }
                val monday = mondayOf(t)
                val gymDays = data.workouts.map { it.date }.filter { it >= monday }.distinct().size
                StatTile(R.drawable.ic_nav_gym, "$gymDays/${data.settings.gymGoal}", "gym semana", Modifier.weight(1f)) { onGoTab(3) }
            }

            // Agenda
            OdCard {
                CardLabel("Agenda de hoy", R.drawable.ic_calendar)
                Spacer(Modifier.height(8.dp))
                if (todays.isEmpty()) Text("No tienes nada para hoy.", color = Faint, modifier = Modifier.padding(vertical = 8.dp))
                todays.forEach { task -> AgendaRow(task, onToggle = { toggleTask(task) }, onClick = { editing = task }) }
                if (overdue.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("${overdue.size} pendiente(s) atrasado(s) · ver", color = Bad, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onGoTab(1) }.padding(vertical = 6.dp, horizontal = 4.dp))
                }
            }

            // Rachas
            if (data.streaks.isNotEmpty()) OdCard(onClick = { onGoTab(2) }) {
                CardLabel("Rachas", R.drawable.ic_nav_flame)
                Spacer(Modifier.height(6.dp))
                data.streaks.forEach { s ->
                    val days = streakDays(s, now)
                    val prev = prevMilestone(days); val nxt = nextMilestone(days)
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(s.emoji, fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Sin ${s.name.lowercase()}", color = Ink)
                            Spacer(Modifier.height(6.dp))
                            ThinBar(((days - prev).toFloat() / (nxt - prev)).coerceIn(0f, 1f))
                        }
                        Spacer(Modifier.width(16.dp))
                        Text("$days d", color = Ink, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Semana de gimnasio
            OdCard(onClick = { onGoTab(3) }) {
                CardLabel("Gimnasio esta semana", R.drawable.ic_nav_gym, trailing = {
                    Text(if (data.activeWorkout != null) "En curso" else if (data.workouts.any { it.date == t }) "✓ Hoy" else "", color = if (data.activeWorkout != null) Accent else Good, style = MaterialTheme.typography.bodyMedium)
                })
                Spacer(Modifier.height(14.dp))
                WeekDots(data)
            }
        }
    }
    if (creating) TaskDialog(Task(title = "", date = t), isNew = true, onDismiss = { creating = false })
    editing?.let { TaskDialog(it, isNew = false, onDismiss = { editing = null }) }
}

@Composable
fun ThinBar(progress: Float) {
    Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(CardHigh)) {
        Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(50)).background(Accent))
    }
}

@Composable
private fun AgendaRow(task: Task, onToggle: () -> Unit, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RoundCheck(task.done, onToggle, task.title)
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(task.title, color = if (task.done) Faint else Ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
            if (task.time != null) Text(fmtTime(task.time), color = Faint, style = MaterialTheme.typography.bodySmall)
        }
        Box(Modifier.padding(end = 6.dp).size(8.dp).clip(CircleShape).background(priorityColor(task.priority)))
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
                Box(
                    Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(50))
                        .background(if (on) Ink2 else CardHigh)
                        .then(if (isToday) Modifier.border2() else Modifier),
                )
                Spacer(Modifier.height(6.dp))
                Text(l, style = MaterialTheme.typography.labelSmall, color = if (isToday) Accent else Faint, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

private fun Modifier.border2() = this.border(2.5.dp, Accent, RoundedCornerShape(50))
