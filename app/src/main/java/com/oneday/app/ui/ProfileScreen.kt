package com.oneday.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneday.app.data.*
import com.oneday.app.steps.Steps
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Pantalla "Tú": perfil, metas y ajustes en secciones. Reemplaza la ventana larga de Ajustes. */
@Composable
fun ProfileScreen(data: AppData, onBack: () -> Unit) {
    val s = data.settings
    var section by remember { mutableStateOf<String?>(null) }
    var editName by remember { mutableStateOf(false) }
    var editWeightGoal by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    val since = listOfNotNull(
        data.habits.minOfOrNull { it.createdDay }, data.workouts.minOfOrNull { it.date },
        data.streaks.minOfOrNull { millisToEpochDay(it.start) }, data.tasks.mapNotNull { it.doneAt }.minOrNull()?.let { millisToEpochDay(it) },
    ).minOrNull() ?: today()
    val steps = data.habits.firstOrNull { it.source == "steps" }
    val name = s.userName.ifBlank { "Tú" }

    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleButton("Volver", onClick = onBack) { Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Ink) }
            Spacer(Modifier.width(12.dp))
            Text("Tú", style = MaterialTheme.typography.headlineMedium, color = Ink)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Perfil
            OdCard(onClick = { editName = true }, padding = 18.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(60.dp).clip(RoundedCornerShape(20.dp)).background(AccentSoft).border(1.dp, Accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center) {
                        Text(name.take(1).uppercase(), fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Accent)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text("Usando One Day desde el ${LocalDate.ofEpochDay(since).format(DateTimeFormatter.ofPattern("d MMM", ES)).replace(".", "")} · ${s.seenMedals.size} medalla(s)",
                            color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Glyph("editar", Faint, 18.dp, "Cambiar nombre")
                }
            }

            // Metas
            SectionLabel("Metas")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoalTile("gym", "${s.gymGoal} / sem", "días de gym", Modifier.weight(1f)) { section = "Gimnasio" }
                GoalTile("pasos", steps?.target?.let { fmtStepsShort(it) } ?: "—", "pasos al día", Modifier.weight(1f)) { section = "Pasos" }
                GoalTile("peso", s.weightGoal?.let { "${fmtWeight(it)} kg" } ?: "—", "peso meta", Modifier.weight(1f)) { editWeightGoal = true }
            }

            // Secciones
            OdCard(padding = 0.dp) {
                val rows = listOf(
                    Triple("Notificaciones", "campana", "Resumen ${if (s.summaryOn) fmtTime(s.summaryTime) else "apagado"} · gym ${if (s.gymOn) fmtTime(s.gymTime) else "apagado"}"),
                    Triple("Gimnasio", "gym", "Barra ${fmtWeight(s.barKg)} kg · botones de ${fmtWeight(s.weightStep)} kg"),
                    Triple("Pasos", "pasos", if (steps == null) "Sin conectar" else "Meta ${fmtSteps(steps.target ?: 10000)} al día"),
                    Triple("Áreas de pendientes", "pendientes", s.areas.joinToString(", ").ifEmpty { "Sin áreas" }),
                    Triple("Animaciones y vibración", "rayo", (if (s.animationsOn) "Animaciones" else "Sin animaciones") + " · " + (if (s.hapticsOn) "vibración ${if (s.hapticStrong) "fuerte" else "suave"}" else "sin vibración")),
                    Triple("Copia de seguridad", "sobre", if (s.lastBackupAt > 0) "Última: ${fmtDay(millisToEpochDay(s.lastBackupAt)).lowercase()}" else "Aún no guardas una copia"),
                )
                rows.forEachIndexed { i, (t, icon, sub) ->
                    Row(
                        Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = t) { Fx.tick(); section = t }.padding(horizontal = 18.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(CardHigh), contentAlignment = Alignment.Center) { Glyph(icon, Ink2, 19.dp) }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t, color = Ink)
                            Text(sub, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Faint)
                    }
                    if (i < rows.lastIndex) Box(Modifier.padding(start = 70.dp).fillMaxWidth().height(1.dp).background(Line.copy(alpha = 0.6f)))
                }
            }

            // Interruptores rápidos
            OdCard(padding = 18.dp) {
                ToggleRow("Vibración", if (s.hapticsOn) "Encendida" else "Apagada", s.hapticsOn) { v -> Repo.update { it.copy(settings = it.settings.copy(hapticsOn = v)) }; if (v) Fx.success() }
                Spacer(Modifier.height(8.dp))
                ToggleRow("Animaciones", if (s.animationsOn) "Encendidas" else "Apagadas", s.animationsOn) { v -> Repo.update { it.copy(settings = it.settings.copy(animationsOn = v)) } }
            }

            SecondaryButton("Repetir la bienvenida", color = Ink2) { Repo.update { it.copy(settings = it.settings.copy(onboarded = false)) }; onBack() }
            Text("One Day 1.6 · todo se guarda solo en tu teléfono", color = Faint, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }

    section?.let { sec ->
        GlassSheet({ section = null }) {
            Text(sec.uppercase(), color = Muted, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(16.dp))
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when (sec) {
                    "Notificaciones" -> NotificationSettings(data)
                    "Gimnasio" -> GymSettings(s)
                    "Pasos" -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { StepsSettings() }
                    "Áreas de pendientes" -> AreaSettings(s)
                    "Animaciones y vibración" -> MotionSettings(s)
                    else -> BackupSettings()
                }
            }
        }
    }
    if (editName) {
        var n by remember { mutableStateOf(s.userName) }
        OdDialog(
            onDismissRequest = { editName = false },
            title = { Text("Tu nombre") },
            text = { OutlinedTextField(n, { n = it }, singleLine = true, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { Repo.update { it.copy(settings = it.settings.copy(userName = n.trim())) }; editName = false }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { editName = false }) { Text("Cancelar", color = Muted) } },
        )
    }
    if (editWeightGoal) {
        var g by remember { mutableStateOf(s.weightGoal?.let { fmtWeight(it) } ?: "70") }
        OdDialog(
            onDismissRequest = { editWeightGoal = false },
            title = { Text("Peso meta") },
            text = { Stepper("Meta (kg)", g, { g = it }, step = 0.5, decimal = true) },
            confirmButton = { TextButton(onClick = { Repo.update { it.copy(settings = it.settings.copy(weightGoal = g.replace(',', '.').toDoubleOrNull())) }; editWeightGoal = false }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { editWeightGoal = false }) { Text("Cancelar", color = Muted) } },
        )
    }
}

@Composable
private fun GoalTile(icon: String, value: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.pressClick(onClick).clip(RoundedCornerShape(22.dp)).background(CardBg).border(1.dp, Hairline, RoundedCornerShape(22.dp)).padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Glyph(icon, Muted, 22.dp)
        Text(value, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Ink, maxLines = 1)
        Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}
