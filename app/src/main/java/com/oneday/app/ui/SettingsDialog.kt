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
import androidx.compose.ui.unit.sp
import com.oneday.app.data.*
import java.time.LocalDate

private fun set(f: (Settings) -> Settings) = Repo.update { it.copy(settings = f(it.settings)) }

@Composable
fun SectionLabel(t: String) = Text(t.uppercase(), color = Faint, letterSpacing = 1.2.sp, style = MaterialTheme.typography.labelLarge)

@Composable
fun NotificationSettings(data: AppData) {
    val s = data.settings
    var pickSummary by remember { mutableStateOf(false) }
    var pickGym by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ToggleRow("Resumen del día", "Cada mañana te digo qué tienes pendiente", s.summaryOn) { v -> set { it.copy(summaryOn = v) } }
        if (s.summaryOn) ChoicePill(fmtTime(s.summaryTime), false, lead = { Glyph("reloj", Muted, 16.dp) }) { pickSummary = true }
        ToggleRow("Recordatorio de gimnasio", "Solo si ese día aún no entrenas", s.gymOn) { v -> set { it.copy(gymOn = v) } }
        if (s.gymOn) {
            ChoicePill(fmtTime(s.gymTime), false, lead = { Glyph("reloj", Muted, 16.dp) }) { pickGym = true }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("L", "M", "X", "J", "V", "S", "D").forEachIndexed { i, l ->
                    val d = i + 1
                    ChoicePill(l, d in s.gymDays, Modifier.weight(1f)) { set { it.copy(gymDays = if (d in it.gymDays) it.gymDays - d else it.gymDays + d) } }
                }
            }
        }
        ToggleRow("Logros de rachas", "Te aviso al llegar a 1, 3, 7, 30… días", s.milestonesOn) { v -> set { it.copy(milestonesOn = v) } }
        val alert = cravingAlertTime(data.cravings)
        ToggleRow("Aviso antes de tus antojos", if (alert != null) "Te aviso a las ${fmtTime(alert)}, 30 min antes de tu hora difícil" else "Se activa con 5 antojos registrados", s.cravingAlertOn && alert != null) { v ->
            if (alert != null) set { it.copy(cravingAlertOn = v) }
        }
    }
    if (pickSummary) TimePickDialog(s.summaryTime, { pickSummary = false }) { v -> set { it.copy(summaryTime = v) } }
    if (pickGym) TimePickDialog(s.gymTime, { pickGym = false }) { v -> set { it.copy(gymTime = v) } }
}

@Composable
fun MotionSettings(s: Settings) = Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    ToggleRow("Animaciones", "Casillas, tarjetas y transiciones con movimiento", s.animationsOn) { v -> set { it.copy(animationsOn = v) } }
    if (s.animationsOn) {
        ToggleRow("Números que ruedan", "Las cifras grandes giran al cambiar", s.rollingNumbersOn) { v -> set { it.copy(rollingNumbersOn = v) } }
        ToggleRow("Celebraciones", "Medallas, destellos y confeti en logros", s.celebrationsOn) { v -> set { it.copy(celebrationsOn = v) } }
    }
    ToggleRow("Vibración", "Al marcar, guardar y tocar tarjetas", s.hapticsOn) { v -> set { it.copy(hapticsOn = v) }; if (v) Fx.success() }
    if (s.hapticsOn) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoicePill("Suave", !s.hapticStrong, Modifier.weight(1f)) { set { it.copy(hapticStrong = false) }; Fx.success() }
        ChoicePill("Fuerte", s.hapticStrong, Modifier.weight(1f)) { set { it.copy(hapticStrong = true) }; Fx.success() }
    }
}

@Composable
fun GymSettings(s: Settings) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Meta: ${s.gymGoal} día(s) por semana", color = Ink)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..7).forEach { n -> ChoicePill("$n", s.gymGoal == n, Modifier.weight(1f)) { set { it.copy(gymGoal = n) } } }
    }
    Text("Botones − / + del peso", color = Ink)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(0.5, 1.0, 2.0, 2.5, 5.0).forEach { st -> ChoicePill("${fmtWeight(st)} kg", s.weightStep == st, Modifier.weight(1f)) { set { it.copy(weightStep = st) } } }
    }
    Text("Siempre puedes tocar el número y escribir el peso exacto, por ejemplo 5.9.", color = Muted, style = MaterialTheme.typography.bodySmall)
    Text("Peso de la barra", color = Ink)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(20.0, 15.0, 10.0, 7.0).forEach { b -> ChoicePill("${fmtWeight(b)} kg", s.barKg == b, Modifier.weight(1f)) { set { it.copy(barKg = b) } } }
    }
    Text("Discos en tu gym", color = Ink)
    listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25, 0.5).chunked(4).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { p ->
                ChoicePill(fmtWeight(p), p in s.plates, Modifier.weight(1f)) {
                    set { it.copy(plates = if (p in it.plates) it.plates - p else (it.plates + p).sortedDescending()) }
                }
            }
        }
    }
}

@Composable
fun AreaSettings(s: Settings) {
    var name by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        s.areas.forEach { a ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(a, color = Ink, modifier = Modifier.weight(1f))
                IconButton(onClick = { Fx.reject(); set { it.copy(areas = it.areas - a) } }) { Glyph("cerrar", Faint, 18.dp, "Quitar $a") }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(name, { name = it }, label = { Text("Nueva área") }, singleLine = true, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), modifier = Modifier.weight(1f))
            TextButton(enabled = name.isNotBlank(), onClick = {
                val n = name.trim().replaceFirstChar { it.uppercase() }
                if (n !in s.areas) set { it.copy(areas = it.areas + n) }
                name = ""
            }) { Text("Agregar") }
        }
        Text("Quitar un área no borra sus pendientes.", color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun BackupSettings() {
    val ctx = LocalContext.current
    var confirmImport by remember { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            ctx.contentResolver.openOutputStream(uri)!!.use { it.write(Repo.exportJson().toByteArray()) }
        }.onSuccess { set { it.copy(lastBackupAt = System.currentTimeMillis()) }; Toast.makeText(ctx, "Copia guardada", Toast.LENGTH_SHORT).show() }
            .onFailure { Toast.makeText(ctx, "No se pudo guardar la copia", Toast.LENGTH_LONG).show() }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val ok = runCatching { ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }.getOrNull()?.let { Repo.importJson(it) } ?: false
            Toast.makeText(ctx, if (ok) "Datos restaurados" else "Ese archivo no es una copia válida de One Day", Toast.LENGTH_LONG).show()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tus datos viven solo en este teléfono. Guarda una copia de vez en cuando (por ejemplo en Google Drive) para no perderlos si cambias de celular.", color = Ink2, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton("Guardar copia", Modifier.weight(1f)) { exporter.launch("one-day-${LocalDate.now()}.json") }
            SecondaryButton("Restaurar", Modifier.weight(1f)) { confirmImport = true }
        }
    }
    if (confirmImport) ConfirmDialog("Restaurar copia", "Los datos actuales se reemplazarán por los de la copia que elijas.", "Elegir archivo", { confirmImport = false }) {
        importer.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*"))
    }
}

@Composable
fun ToggleRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink)
            Text(sub, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
