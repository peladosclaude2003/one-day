package com.oneday.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.oneday.app.data.*
import java.time.LocalDate

@Composable
fun SettingsDialog(data: AppData, onDismiss: () -> Unit) {
    val s = data.settings
    val ctx = LocalContext.current
    fun set(f: (Settings) -> Settings) = Repo.update { it.copy(settings = f(it.settings)) }
    var pickSummary by remember { mutableStateOf(false) }
    var pickGym by remember { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf(false) }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            ctx.contentResolver.openOutputStream(uri)!!.use { it.write(Repo.exportJson().toByteArray()) }
        }.onSuccess { Toast.makeText(ctx, "Copia guardada ✅", Toast.LENGTH_SHORT).show() }
            .onFailure { Toast.makeText(ctx, "No se pudo guardar la copia", Toast.LENGTH_LONG).show() }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val ok = runCatching { ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }.getOrNull()?.let { Repo.importJson(it) } ?: false
            Toast.makeText(ctx, if (ok) "Datos restaurados ✅" else "Ese archivo no es una copia válida de One Day", Toast.LENGTH_LONG).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustes") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Notificaciones", color = Accent, style = MaterialTheme.typography.labelLarge)
                ToggleRow("Resumen del día", "Cada mañana te digo qué tienes pendiente", s.summaryOn) { v -> set { it.copy(summaryOn = v) } }
                if (s.summaryOn) AssistChip(onClick = { pickSummary = true }, label = { Text("🕒 " + fmtTime(s.summaryTime)) })

                ToggleRow("Recordatorio de gimnasio", "Solo si ese día aún no entrenas", s.gymOn) { v -> set { it.copy(gymOn = v) } }
                if (s.gymOn) {
                    AssistChip(onClick = { pickGym = true }, label = { Text("🕒 " + fmtTime(s.gymTime)) })
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("L", "M", "X", "J", "V", "S", "D").forEachIndexed { i, l ->
                            val d = i + 1
                            FilterChip(selected = d in s.gymDays, onClick = {
                                set { it.copy(gymDays = if (d in it.gymDays) it.gymDays - d else it.gymDays + d) }
                            }, label = { Text(l) }, modifier = Modifier.weight(1f))
                        }
                    }
                }
                ToggleRow("Logros de rachas", "Te felicito al llegar a 1, 3, 7, 30… días", s.milestonesOn) { v -> set { it.copy(milestonesOn = v) } }

                HorizontalDivider()
                Text("Copia de seguridad", color = Accent, style = MaterialTheme.typography.labelLarge)
                Text("Tus datos viven solo en este teléfono. Guarda una copia de vez en cuando (por ejemplo en Google Drive) para no perderlos si cambias de celular.", color = Muted, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { exporter.launch("one-day-${LocalDate.now()}.json") }, modifier = Modifier.weight(1f)) { Text("Guardar copia") }
                    OutlinedButton(onClick = { confirmImport = true }, modifier = Modifier.weight(1f)) { Text("Restaurar") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } },
    )
    if (pickSummary) TimePickDialog(s.summaryTime, { pickSummary = false }) { v -> set { it.copy(summaryTime = v) } }
    if (pickGym) TimePickDialog(s.gymTime, { pickGym = false }) { v -> set { it.copy(gymTime = v) } }
    if (confirmImport) ConfirmDialog("Restaurar copia", "Los datos actuales se reemplazarán por los de la copia que elijas.", "Elegir archivo", { confirmImport = false }) {
        importer.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*"))
    }
}

@Composable
private fun ToggleRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(sub, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
