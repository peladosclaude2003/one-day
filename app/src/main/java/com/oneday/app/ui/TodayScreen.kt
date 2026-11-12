package com.oneday.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
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
    val hour = LocalTime.now().hour
    val greeting = when (hour) { in 5..11 -> "Buenos días"; in 12..18 -> "Buenas tardes"; else -> "Buenas noches" }

    val todays = data.tasks.filter { it.date == t }.sortedWith(compareBy<Task> { it.done }.then(taskOrder))
    val overdue = data.tasks.filter { !it.done && it.date != null && it.date < t }
    val doneCount = todays.count { it.done }

    Scaffold(
        containerColor = Bg,
        floatingActionButton = { FloatingActionButton(onClick = { creating = true }, containerColor = Accent) { Icon(Icons.Filled.Add, "Agregar al día") } },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
            ScreenTitle("$greeting 👋", fmtLongDate(LocalDate.now())) {
                IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, "Ajustes", tint = Muted) }
            }
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Progreso del día
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { if (todays.isEmpty()) 0f else doneCount / todays.size.toFloat() },
                                modifier = Modifier.size(64.dp), strokeWidth = 7.dp, color = Accent,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest, strokeCap = StrokeCap.Round, gapSize = 0.dp,
                            )
                            Text(if (todays.isEmpty()) "–" else "$doneCount/${todays.size}", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                when {
                                    todays.isEmpty() -> "Tu día está libre"
                                    doneCount == todays.size -> "¡Día completado! 🎉"
                                    else -> "Te faltan ${todays.size - doneCount} pendiente(s)"
                                }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                            )
                            Text(if (todays.isEmpty()) "Toca + para planear tu día" else "Sigue así, un paso a la vez", color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Agenda
                SectionCard(title = "📅 Agenda de hoy") {
                    if (todays.isEmpty()) Text("No tienes nada para hoy.", color = Muted)
                    todays.forEach { task ->
                        TaskRow(task, showDate = false, onToggle = { toggleTask(task) }, onClick = { editing = task })
                        Spacer(Modifier.height(6.dp))
                    }
                    if (overdue.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("⚠️ ${overdue.size} pendiente(s) atrasado(s) · ver", color = Bad, style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable { onGoTab(1) }.padding(vertical = 4.dp))
                    }
                }

                // Rachas
                SectionCard(title = "🔥 Rachas", action = { TextButton(onClick = { onGoTab(2) }) { Text("Ver") } }) {
                    if (data.streaks.isEmpty()) Text("Aún no tienes rachas. Agrega una en la pestaña Rachas.", color = Muted)
                    data.streaks.forEach { s ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(s.emoji, fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Text("Sin ${s.name.lowercase()}", modifier = Modifier.weight(1f))
                            Text("${streakDays(s, now)} días", color = Accent, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Gimnasio
                SectionCard(title = "🏋️ Gimnasio", action = { TextButton(onClick = { onGoTab(3) }) { Text(if (data.activeWorkout != null) "Continuar" else "Entrenar") } }) {
                    val trained = data.workouts.filter { it.date == t }
                    Text(
                        when {
                            data.activeWorkout != null -> "Tienes un entrenamiento en curso: ${data.activeWorkout.name}"
                            trained.isNotEmpty() -> "✅ Hoy entrenaste: ${trained.joinToString { it.name }}"
                            else -> "Aún no entrenas hoy."
                        }, color = if (trained.isNotEmpty()) Good else MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(10.dp))
                    WeekDots(data)
                }
            }
        }
    }
    if (creating) TaskDialog(Task(title = "", date = t), isNew = true, onDismiss = { creating = false })
    editing?.let { TaskDialog(it, isNew = false, onDismiss = { editing = null }) }
}

@Composable
fun WeekDots(data: AppData) {
    val now = LocalDate.now()
    val monday = now.minusDays((now.dayOfWeek.value - 1).toLong())
    val trained = data.workouts.map { it.date }.toSet()
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        listOf("L", "M", "X", "J", "V", "S", "D").forEachIndexed { i, l ->
            val day = monday.plusDays(i.toLong())
            val on = day.toEpochDay() in trained
            val isToday = day == now
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape)
                        .background(if (on) Accent else MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) { Text(if (on) "✓" else "", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(4.dp))
                Text(l, style = MaterialTheme.typography.labelSmall, color = if (isToday) Accent else Muted, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}
