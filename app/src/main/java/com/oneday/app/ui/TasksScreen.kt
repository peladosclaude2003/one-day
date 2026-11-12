package com.oneday.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oneday.app.data.*

fun toggleTask(t: Task) = Repo.update { d ->
    d.copy(tasks = d.tasks.map {
        if (it.id == t.id) it.copy(done = !it.done, doneAt = if (!it.done) System.currentTimeMillis() else null) else it
    })
}

fun saveTask(t: Task) = Repo.update { d ->
    d.copy(tasks = if (d.tasks.any { it.id == t.id }) d.tasks.map { if (it.id == t.id) t else it } else d.tasks + t)
}

fun deleteTask(t: Task) = Repo.update { d -> d.copy(tasks = d.tasks.filterNot { it.id == t.id }) }

val taskOrder = compareBy<Task>({ it.date ?: Long.MAX_VALUE }, { it.time ?: Int.MAX_VALUE }, { -it.priority })

@Composable
fun TasksScreen(data: AppData) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var creating by remember { mutableStateOf(false) }
    val t = today()

    Scaffold(
        containerColor = Bg,
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }, containerColor = Accent) { Icon(Icons.Filled.Add, "Nuevo pendiente") }
        },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            val pending = data.tasks.count { !it.done }
            ScreenTitle("Pendientes", if (pending == 0) "Todo al día ✨" else "$pending por hacer")
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Por hacer", "Hoy", "Hechos").forEachIndexed { i, label ->
                    FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(label) })
                }
            }
            val groups: List<Pair<String, List<Task>>> = when (filter) {
                0 -> {
                    val open = data.tasks.filter { !it.done }.sortedWith(taskOrder)
                    listOf(
                        "⚠️ Atrasados" to open.filter { it.date != null && it.date < t },
                        "Hoy" to open.filter { it.date == t },
                        "Próximos" to open.filter { it.date != null && it.date > t },
                        "Sin fecha" to open.filter { it.date == null }.sortedByDescending { it.priority },
                    )
                }
                1 -> listOf("Hoy" to data.tasks.filter { it.date == t }.sortedWith(compareBy<Task> { it.done }.then(taskOrder)))
                else -> listOf("Completados" to data.tasks.filter { it.done }.sortedByDescending { it.doneAt ?: 0 })
            }.filter { it.second.isNotEmpty() }

            if (groups.isEmpty()) {
                EmptyState(
                    if (filter == 2) "📭" else "✅",
                    if (filter == 2) "Aún no completas nada" else "No hay pendientes",
                    if (filter == 2) "Cuando marques una tarea aparecerá aquí." else "Toca el botón + para agregar uno.",
                )
            }
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)) {
                groups.forEach { (title, list) ->
                    item(key = "h_$title") {
                        Text(title, color = Muted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp, start = 4.dp))
                    }
                    items(list, key = { it.id }) { task ->
                        TaskRow(task, showDate = filter != 1, onToggle = { toggleTask(task) }, onClick = { editing = task })
                        Spacer(Modifier.height(8.dp))
                    }
                }
                if (filter == 2 && groups.isNotEmpty()) item {
                    var confirm by remember { mutableStateOf(false) }
                    TextButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) { Text("Borrar completados", color = Bad) }
                    if (confirm) ConfirmDialog("Borrar completados", "Se eliminarán todas las tareas ya hechas.", onDismiss = { confirm = false }) {
                        Repo.update { d -> d.copy(tasks = d.tasks.filterNot { it.done }) }
                    }
                }
            }
        }
    }
    if (creating) TaskDialog(Task(title = "", date = if (filter == 1) t else null), isNew = true, onDismiss = { creating = false })
    editing?.let { TaskDialog(it, isNew = false, onDismiss = { editing = null }) }
}

@Composable
fun TaskRow(task: Task, showDate: Boolean = true, onToggle: () -> Unit, onClick: () -> Unit) {
    val overdue = !task.done && task.date != null && task.date < today()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardBg).clickable(onClick = onClick).padding(end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = task.done, onCheckedChange = { onToggle() }, colors = CheckboxDefaults.colors(checkedColor = Good))
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(
                task.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
                color = if (task.done) Muted else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            val parts = buildList {
                if (showDate && task.date != null) add(fmtDay(task.date))
                if (task.time != null) add(fmtTime(task.time))
            }
            if (parts.isNotEmpty() || task.notes.isNotBlank()) {
                Text(
                    (parts.joinToString(" · ") + if (task.notes.isNotBlank()) (if (parts.isEmpty()) "" else " · ") + task.notes else ""),
                    color = if (overdue) Bad else Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (task.remind && !task.done) Icon(Icons.Filled.Notifications, "Con recordatorio", tint = Muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(10.dp).clip(CircleShape).background(priorityColor(task.priority)))
    }
}

@Composable
fun TaskDialog(initial: Task, isNew: Boolean, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(initial.title) }
    var notes by remember { mutableStateOf(initial.notes) }
    var date by remember { mutableStateOf(initial.date) }
    var time by remember { mutableStateOf(initial.time) }
    var priority by remember { mutableIntStateOf(initial.priority) }
    var remind by remember { mutableStateOf(initial.remind) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Nuevo pendiente" else "Editar pendiente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("¿Qué tienes que hacer?") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Notas (opcional)") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { pickDate = true }, label = { Text("📅 " + (date?.let { fmtDay(it) } ?: "Sin fecha")) })
                    AssistChip(onClick = { pickTime = true }, label = { Text("🕒 " + (time?.let { fmtTime(it) } ?: "Sin hora")) })
                }
                if (date != null || time != null) {
                    TextButton(onClick = { date = null; time = null; remind = false }, contentPadding = PaddingValues(0.dp)) { Text("Quitar fecha y hora", color = Muted) }
                }
                Text("Prioridad", style = MaterialTheme.typography.labelLarge, color = Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..2).forEach { p ->
                        FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(priorityName(p)) },
                            leadingIcon = { Box(Modifier.size(8.dp).clip(CircleShape).background(priorityColor(p))) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Recordarme")
                        Text(if (date != null && time != null) "Te aviso a esa hora" else "Elige fecha y hora", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = remind && date != null && time != null, enabled = date != null && time != null, onCheckedChange = { remind = it })
                }
                if (!isNew) TextButton(onClick = { confirmDelete = true }, contentPadding = PaddingValues(0.dp)) { Text("Eliminar pendiente", color = Bad) }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                saveTask(initial.copy(title = title.trim(), notes = notes.trim(), date = date, time = time, priority = priority,
                    remind = remind && date != null && time != null))
                onDismiss()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
    if (pickDate) DatePickDialog(date, { pickDate = false }) { date = it }
    if (pickTime) TimePickDialog(time, { pickTime = false }) {
        time = it
        if (date == null) date = today()
        remind = true
    }
    if (confirmDelete) ConfirmDialog("Eliminar", "¿Eliminar \"${initial.title}\"?", onDismiss = { confirmDelete = false }) {
        deleteTask(initial); onDismiss()
    }
}
