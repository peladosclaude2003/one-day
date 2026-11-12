package com.oneday.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneday.app.R
import com.oneday.app.data.*

/**
 * Captura rápida: una frase ("dentista mañana 10am !alta") se convierte en pendiente.
 * Si dice "cada día", se guarda como hábito con recordatorio.
 */
@Composable
fun QuickAddSheet(defaultDate: Long?, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var more by remember { mutableStateOf<Task?>(null) }
    val p = remember(text) { QuickParse.parse(text) }
    val focus = remember { FocusRequester() }
    val date = p.date ?: defaultDate.takeIf { !p.daily }

    fun save() {
        if (p.title.isBlank()) return
        if (p.daily) {
            Repo.update { d -> d.copy(habits = d.habits + Habit(name = p.title, emoji = "✅", createdDay = today(), remindAt = p.time)) }
        } else {
            saveTask(Task(title = p.title, date = date, time = p.time, priority = p.priority ?: 1, remind = date != null && p.time != null))
        }
        Fx.success()
        onDismiss()
    }

    if (more != null) { TaskDialog(more!!, isNew = true, onDismiss = { more = null; onDismiss() }); return }

    GlassSheet(onDismiss) {
        Text("NUEVO", color = Muted, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            text, { text = it }, placeholder = { Text("Ej. dentista mañana 10am !alta", color = Faint) },
            singleLine = true, shape = RoundedCornerShape(20.dp),
            textStyle = MaterialTheme.typography.titleMedium,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
        Spacer(Modifier.height(12.dp))
        // Vista previa de lo que entendió
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (p.daily) InfoPill("Hábito diario", R.drawable.ic_nav_flame)
            else InfoPill(date?.let { fmtDay(it) } ?: "Sin fecha", R.drawable.ic_calendar)
            p.time?.let { InfoPill(fmtTime(it), R.drawable.ic_clock) }
            if (p.priority != null) InfoPill(priorityName(p.priority))
        }
        Spacer(Modifier.height(10.dp))
        Text("Puedes escribir: hoy, mañana, viernes, en 3 días, 15 de dic · 10am, a las 5, 8 de la noche · !alta, !baja · cada día",
            color = Faint, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Más opciones", Modifier.weight(1f), color = Ink2) {
                more = Task(title = p.title, date = date, time = p.time, priority = p.priority ?: 1, remind = date != null && p.time != null)
            }
            PrimaryButton(if (p.daily) "Crear hábito" else "Guardar", Modifier.weight(1.3f), enabled = p.title.isNotBlank()) { save() }
        }
        Spacer(Modifier.imePadding())
    }
}
