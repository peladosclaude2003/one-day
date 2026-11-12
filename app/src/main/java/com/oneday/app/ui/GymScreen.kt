package com.oneday.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneday.app.R
import com.oneday.app.data.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private fun exName(data: AppData, id: String) = data.exercises.firstOrNull { it.id == id }?.name ?: "Ejercicio eliminado"

object GymUi { var minimized by mutableStateOf(false) }

@Composable
fun GymScreen(data: AppData) {
    val active = data.activeWorkout
    if (active != null && !GymUi.minimized) { ActiveWorkout(data, active); return }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column {
        ScreenTitle("Gimnasio", gymSubtitle(data))
        Segmented(listOf("Resumen", "Historial", "Progreso", "Rutinas"), tab, { tab = it }, Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(12.dp))
        when (tab) {
            0 -> GymOverview(data)
            1 -> HistoryTab(data)
            2 -> ProgressTab(data)
            else -> RoutinesTab(data)
        }
    }
}

fun gymSubtitle(data: AppData): String {
    val monday = mondayOf(today())
    val n = data.workouts.map { it.date }.filter { it >= monday }.distinct().size
    return "Esta semana: $n de ${data.settings.gymGoal} días"
}

fun startWorkout(name: String, exerciseIds: List<String>) {
    GymUi.minimized = false
    Repo.update { d -> d.copy(activeWorkout = Workout(date = today(), name = name, entries = exerciseIds.map { WorkEntry(it) })) }
}

// ---------- Resumen ----------

@Composable
private fun GymOverview(data: AppData) {
    var statSheet by remember { mutableStateOf<Int?>(null) }
    var pickRoutine by remember { mutableStateOf(false) }
    var weightSheet by remember { mutableStateOf(false) }
    val recent = data.recentWorkouts(30)
    val hours = recent.sumOf { it.minutes } / 60.0
    val reps = recent.sumOf { it.reps() }
    val maxKg = recent.flatMap { w -> w.entries.flatMap { it.sets } }.maxOfOrNull { it.weight } ?: 0.0

    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HeroWorkoutCard(data) { pickRoutine = true }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(R.drawable.ic_clock, "%.1f".format(hours), "horas", Modifier.weight(1f)) { statSheet = 0 }
            StatTile(R.drawable.ic_layers, fmtCompact(reps.toDouble()), "reps", Modifier.weight(1f)) { statSheet = 1 }
            StatTile(R.drawable.ic_trophy, fmtWeight(maxKg), "kg máx", Modifier.weight(1f)) { statSheet = 2 }
        }

        val trainedDays = data.workouts.map { it.date }.toSet()
        val n30 = recent.map { it.date }.distinct().size
        OdCard {
            CardLabel("Últimos 30 días", trailing = { Text("$n30 · meta ${data.settings.gymGoal}/sem", color = Faint, style = MaterialTheme.typography.bodyMedium) })
            Spacer(Modifier.height(14.dp))
            Last30Grid(trainedDays, data.settings.gymDays)
            Spacer(Modifier.height(12.dp))
            Last30Legend()
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
            val vol = data.weeklyVolume(8)
            OdCard(Modifier.weight(1f).fillMaxHeight(), padding = 0.dp) {
                Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp)) {
                    CardLabel("Volumen", R.drawable.ic_bolt, "8 semanas")
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(fmtCompact(vol.sum()), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text(" kg", color = Muted, modifier = Modifier.padding(bottom = 3.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                AreaTrend(vol)
            }
            val week = data.weekReps()
            // Compara contra los mismos días de la semana pasada (lunes → hoy).
            val upTo = LocalDate.now().dayOfWeek.value
            val last = data.weekReps(1).take(upTo).sum()
            val sum = week.sum()
            OdCard(Modifier.weight(1f).fillMaxHeight(), padding = 18.dp) {
                CardLabel("Esta semana", R.drawable.ic_layers, "Reps por día")
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$sum", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(" reps", color = Muted, modifier = Modifier.padding(bottom = 3.dp).weight(1f))
                    if (last > 0) {
                        val pct = ((sum - last) * 100.0 / last).toInt()
                        Text((if (pct >= 0) "+" else "") + "$pct%", color = if (pct >= 0) Good else Bad, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                WeekBars(week, barArea = 40.dp)
            }
        }

        BodyWeightCard(data) { weightSheet = true }
    }

    statSheet?.let { StatSheet(data, it) { statSheet = null } }
    if (pickRoutine) RoutinePickerSheet(data) { pickRoutine = false }
    if (weightSheet) WeightSheet(data) { weightSheet = false }
}

@Composable
private fun HeroWorkoutCard(data: AppData, onPickOther: () -> Unit) {
    val active = data.activeWorkout
    val trainedToday = data.workouts.filter { it.date == today() }
    OdCard(padding = 22.dp) {
        if (active != null) {
            val mins = ((System.currentTimeMillis() - active.startedAt) / 60_000).toInt()
            Text("En curso", color = Accent, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            DashedBox { Text(shortName(active.name), fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 2) }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill("$mins min", R.drawable.ic_clock)
                InfoPill("${active.setCount()} series", R.drawable.ic_layers)
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton("Continuar entrenamiento", icon = R.drawable.ic_play) { GymUi.minimized = false }
            return@OdCard
        }
        val r = data.suggestedRoutine()
        Text("Hoy", color = Muted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))
        DashedBox { Text(r?.let { shortName(it.name) } ?: "Libre", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        Spacer(Modifier.height(8.dp))
        Text(data.routines.joinToString(", ") { shortName(it.name) }, color = Faint, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(16.dp))
        if (r != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoPill("~${data.estimatedMinutes(r)} min", R.drawable.ic_clock)
            InfoPill("${r.exerciseIds.size} ejercicios", R.drawable.ic_nav_gym)
        }
        if (trainedToday.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("✓ Hoy ya entrenaste: ${trainedToday.joinToString { shortName(it.name) }}", color = Good, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Empezar entrenamiento", icon = R.drawable.ic_play) {
            if (r != null) startWorkout(r.name, r.exerciseIds) else startWorkout("Entrenamiento libre", emptyList())
        }
        TextButton(onClick = onPickOther, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) { Text("Elegir otra rutina", color = Muted) }
    }
}

@Composable
private fun RoutinePickerSheet(data: AppData, onDismiss: () -> Unit) {
    GlassSheet(onDismiss) {
        Text("ELEGIR RUTINA", color = Muted, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(12.dp))
        data.routines.forEach { r ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(20.dp)).background(Color08)
                    .clickable { startWorkout(r.name, r.exerciseIds); onDismiss() }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(r.name, color = Ink, fontWeight = FontWeight.SemiBold)
                    Text("${r.exerciseIds.size} ejercicios · ~${data.estimatedMinutes(r)} min", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                OdIcon(R.drawable.ic_play, Ink, 26.dp)
            }
        }
        Spacer(Modifier.height(8.dp))
        SecondaryButton("Entrenamiento libre (sin rutina)") { startWorkout("Entrenamiento libre", emptyList()); onDismiss() }
    }
}

private val Color08 = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f)

@Composable
private fun StatSheet(data: AppData, kind: Int, onDismiss: () -> Unit) {
    val recent = data.recentWorkouts(30)
    val sets = recent.flatMap { w -> w.entries.flatMap { it.sets } }
    val reps = sets.sumOf { it.reps }
    GlassSheet(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ÚLTIMOS 30 DÍAS", color = Muted, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp, modifier = Modifier.weight(1f))
            OdIcon(listOf(R.drawable.ic_clock, R.drawable.ic_layers, R.drawable.ic_trophy)[kind], Muted, 26.dp)
        }
        Spacer(Modifier.height(12.dp))
        val (big, label, desc) = when (kind) {
            0 -> {
                val mins = recent.sumOf { it.minutes }
                Triple("%.1f".format(mins / 60.0), "horas entrenando",
                    if (recent.isEmpty()) "Aún no hay entrenamientos en este periodo."
                    else "${recent.size} entrenamiento(s) — unos ${mins / recent.size} min por sesión en promedio.")
            }
            1 -> Triple(fmtCompact(reps.toDouble()), "repeticiones registradas",
                if (sets.isEmpty()) "Aún no hay series en este periodo."
                else "${sets.size} series completadas — unas ${"%.1f".format(reps.toDouble() / sets.size)} reps por serie en promedio.")
            else -> {
                val best = recent.flatMap { w -> w.entries.flatMap { e -> e.sets.map { Triple(w, e.exerciseId, it) } } }.maxByOrNull { it.third.weight }
                Triple(best?.let { fmtWeight(it.third.weight) } ?: "0", "kg, tu peso máximo",
                    best?.let { "Logrado en ${exName(data, it.second)} (${fmtWeight(it.third.weight)} kg × ${it.third.reps}) el ${fmtDay(it.first.date).lowercase()}." } ?: "Aún no hay series en este periodo.")
            }
        }
        Text(big, fontSize = 72.sp, fontWeight = FontWeight.Bold, color = Ink, lineHeight = 76.sp)
        Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.height(10.dp))
        Text(desc, color = Ink2, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color08).padding(18.dp)) {
            CardLabel("Esta semana", R.drawable.ic_layers, "Reps por día")
            Spacer(Modifier.height(12.dp))
            WeekBars(data.weekReps(), barArea = 120.dp, compact = false)
        }
        Spacer(Modifier.height(20.dp))
        Text("De un vistazo", color = Muted, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(6.dp))
        val top = recent.flatMap { w -> w.entries.map { it.exerciseId } }.groupingBy { it }.eachCount().maxByOrNull { it.value }
        GlanceRow("Entrenamientos", "${recent.size}")
        GlanceRow("Días distintos", "${recent.map { it.date }.distinct().size}")
        GlanceRow("Series", "${sets.size}")
        GlanceRow("Volumen total", "${fmtCompact(recent.sumOf { it.volume() })} kg")
        if (top != null) GlanceRow("Ejercicio más frecuente", exName(data, top.key))
    }
}

@Composable
private fun GlanceRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, color = Muted, modifier = Modifier.weight(1f))
        Text(value, color = Ink, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

// ---------- Peso corporal ----------

@Composable
private fun BodyWeightCard(data: AppData, onClick: () -> Unit) {
    val weights = data.bodyWeights.sortedBy { it.date }
    val last = weights.lastOrNull()
    val goal = data.settings.weightGoal
    OdCard(onClick = onClick) {
        CardLabel("Peso corporal", R.drawable.ic_scale, trailing = { Icon(Icons.Filled.KeyboardArrowRight, null, tint = Faint) })
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(last?.let { fmtWeight(it.kg) } ?: "—", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(" kg", color = Muted, modifier = Modifier.padding(bottom = 4.dp).weight(1f))
            Text(
                when {
                    last == null -> "Toca para registrar"
                    goal == null -> "Sin meta"
                    else -> "Meta · ${fmtWeight(round1(kotlin.math.abs(goal - last.kg)))} kg por llegar"
                }, color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        if (last != null && goal != null) {
            Text(if (goal > last.kg) "Subir a ${fmtWeight(goal)} kg" else if (goal < last.kg) "Bajar a ${fmtWeight(goal)} kg" else "¡Meta alcanzada! 🎉", color = Faint, style = MaterialTheme.typography.bodySmall)
        }
        if (weights.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            WeightSpark(weights.takeLast(12).map { it.kg }, goal)
        }
    }
}

@Composable
private fun WeightSheet(data: AppData, onDismiss: () -> Unit) {
    val last = data.bodyWeights.maxByOrNull { it.date }
    var weight by remember { mutableStateOf(last?.let { fmtWeight(it.kg) } ?: "70") }
    var goal by remember { mutableStateOf(data.settings.weightGoal?.let { fmtWeight(it) } ?: "") }
    GlassSheet(onDismiss) {
        Text("PESO CORPORAL", color = Muted, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(16.dp))
        Stepper("Peso de hoy (kg)", weight, { weight = it }, step = 0.1, decimal = true)
        Spacer(Modifier.height(12.dp))
        Stepper("Meta (kg, opcional)", goal, { goal = it }, step = 0.5, decimal = true)
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Guardar", enabled = weight.replace(',', '.').toDoubleOrNull() != null) {
            val w = weight.replace(',', '.').toDouble()
            val g = goal.replace(',', '.').toDoubleOrNull()
            Repo.update { d ->
                d.copy(bodyWeights = d.bodyWeights.filterNot { it.date == today() } + BodyWeight(today(), round1(w)),
                    settings = d.settings.copy(weightGoal = g))
            }
            onDismiss()
        }
        val hist = data.bodyWeights.sortedByDescending { it.date }.take(6)
        if (hist.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("Registros recientes", color = Muted)
            hist.forEach { GlanceRow(fmtDay(it.date), "${fmtWeight(it.kg)} kg") }
        }
    }
}

// ---------- Entrenamiento en curso ----------

@Composable
private fun ActiveWorkout(data: AppData, w: Workout) {
    var addEx by remember { mutableStateOf(false) }
    var editSet by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (entrada, serie); serie = -1 para nueva
    var confirmDiscard by remember { mutableStateOf(false) }
    val now = rememberNow()
    val mins = ((now - w.startedAt) / 60_000).toInt()
    fun upd(f: (Workout) -> Workout) = Repo.update { d -> d.copy(activeWorkout = d.activeWorkout?.let(f)) }
    DisposableEffect(Unit) { UiState.hideNav = true; onDispose { UiState.hideNav = false } }
    BackHandler { GymUi.minimized = true }

    Column {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleButton("Minimizar", onClick = { GymUi.minimized = true }) { Icon(Icons.Filled.KeyboardArrowDown, null, tint = Ink) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("En curso", color = Accent, style = MaterialTheme.typography.bodySmall)
                Text(shortName(w.name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoPill("$mins min", R.drawable.ic_clock)
            InfoPill("${w.setCount()} series", R.drawable.ic_layers)
            InfoPill("${fmtCompact(w.volume())} kg", R.drawable.ic_bolt)
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (w.entries.isEmpty()) item { EmptyState("🏋️", "Agrega tu primer ejercicio", "Usa el botón de abajo.") }
            w.entries.forEachIndexed { ei, e ->
                item(key = "e$ei${e.exerciseId}") {
                    val prev = lastSetsFor(data, e.exerciseId)
                    OdCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(exName(data, e.exerciseId), color = Ink, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                if (prev != null) Text("Última vez: " + prev.joinToString("  ") { "${fmtWeight(it.weight)}×${it.reps}" }, color = Faint, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { upd { it.copy(entries = it.entries.filterIndexed { i, _ -> i != ei }) } }) { Icon(Icons.Filled.Close, "Quitar ejercicio", tint = Faint) }
                        }
                        e.sets.forEachIndexed { si, s ->
                            Row(
                                Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(50)).background(CardHigh)
                                    .clickable { editSet = ei to si }.padding(horizontal = 18.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${si + 1}", color = Faint, modifier = Modifier.width(28.dp))
                                Text("${fmtWeight(s.weight)} kg", fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.weight(1f))
                                Text("× ${s.reps}", fontWeight = FontWeight.SemiBold, color = Ink2)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.clip(RoundedCornerShape(50)).border1().clickable { editSet = ei to -1 }.padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) { Icon(Icons.Filled.Add, null, tint = Ink2, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Agregar serie", color = Ink2) }
                    }
                }
            }
            item { SecondaryButton("+ Agregar ejercicio") { addEx = true } }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Descartar", Modifier.weight(1f), color = Bad) { confirmDiscard = true }
            PrimaryButton("Terminar", Modifier.weight(1.4f)) {
                Repo.update { d ->
                    val done = w.copy(minutes = mins, entries = w.entries.filter { it.sets.isNotEmpty() })
                    d.copy(activeWorkout = null, workouts = if (done.entries.isEmpty()) d.workouts else d.workouts + done)
                }
            }
        }
    }

    if (addEx) ExercisePicker(data, onDismiss = { addEx = false }) { id -> upd { it.copy(entries = it.entries + WorkEntry(id)) } }
    editSet?.let { (ei, si) ->
        val entry = w.entries.getOrNull(ei)
        if (entry == null) { editSet = null; return@let }
        val base = entry.sets.getOrNull(si) ?: entry.sets.lastOrNull() ?: lastSetsFor(data, entry.exerciseId)?.firstOrNull()
        SetDialog(exName(data, entry.exerciseId), base, canDelete = si >= 0, onDismiss = { editSet = null },
            onDelete = { upd { it.copy(entries = it.entries.mapIndexed { i, en -> if (i == ei) en.copy(sets = en.sets.filterIndexed { j, _ -> j != si }) else en }) } },
        ) { set ->
            upd { it.copy(entries = it.entries.mapIndexed { i, en ->
                if (i != ei) en else en.copy(sets = if (si < 0) en.sets + set else en.sets.mapIndexed { j, s -> if (j == si) set else s })
            }) }
        }
    }
    if (confirmDiscard) ConfirmDialog("Descartar entrenamiento", "Se perderán las series de este entrenamiento.", "Descartar", { confirmDiscard = false }) {
        Repo.update { it.copy(activeWorkout = null) }
    }
}

private fun Modifier.border1() = this.border(1.dp, Line, RoundedCornerShape(50))

@Composable
private fun SetDialog(title: String, base: WorkSet?, canDelete: Boolean, onDismiss: () -> Unit, onDelete: () -> Unit, onSave: (WorkSet) -> Unit) {
    var weight by remember { mutableStateOf(base?.let { fmtWeight(it.weight) } ?: "") }
    var reps by remember { mutableStateOf(base?.reps?.toString() ?: "") }
    val w = weight.replace(',', '.').toDoubleOrNull()
    val r = reps.toIntOrNull()
    OdDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Stepper("Peso (kg)", weight, { weight = it }, step = 2.5, decimal = true)
                Stepper("Repeticiones", reps, { reps = it }, step = 1.0, decimal = false)
                if (canDelete) TextButton(onClick = { onDelete(); onDismiss() }, contentPadding = PaddingValues(0.dp)) { Text("Eliminar serie", color = Bad) }
            }
        },
        confirmButton = { TextButton(enabled = w != null && r != null && r > 0, onClick = { onSave(WorkSet(w!!, r!!)); onDismiss() }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Muted) } },
    )
}

@Composable
fun Stepper(label: String, value: String, onChange: (String) -> Unit, step: Double, decimal: Boolean) {
    fun bump(dir: Int) {
        val v = (value.replace(',', '.').toDoubleOrNull() ?: 0.0) + dir * step
        val c = v.coerceAtLeast(0.0)
        onChange(if (decimal) fmtWeight(round1(c)) else c.toInt().toString())
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircleButton("Menos", onClick = { bump(-1) }) { Text("−", fontSize = 22.sp, color = Ink) }
        OutlinedTextField(
            value, { onChange(it.filter { ch -> ch.isDigit() || (decimal && (ch == '.' || ch == ',')) }) },
            label = { Text(label) }, singleLine = true, shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
        )
        CircleButton("Más", onClick = { bump(1) }) { Text("+", fontSize = 22.sp, color = Ink) }
    }
}

@Composable
private fun ExercisePicker(data: AppData, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    OdDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elegir ejercicio") },
        text = {
            Column {
                OutlinedTextField(query, { query = it }, label = { Text("Buscar") }, singleLine = true, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.heightIn(max = 380.dp).padding(top = 8.dp)) {
                    val list = data.exercises.filter { query.isBlank() || it.name.contains(query, true) || it.muscle.contains(query, true) }
                    MUSCLES.forEach { m ->
                        val group = list.filter { it.muscle == m }
                        if (group.isNotEmpty()) {
                            item { Text(m.uppercase(), color = Faint, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)) }
                            items(group, key = { it.id }) { ex ->
                                Text(ex.name, color = Ink, modifier = Modifier.fillMaxWidth().clickable { onPick(ex.id); onDismiss() }.padding(vertical = 10.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { creating = true }) { Text("+ Crear ejercicio") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar", color = Muted) } },
    )
    if (creating) ExerciseDialog(onDismiss = { creating = false }) { onPick(it.id); onDismiss() }
}

@Composable
fun ExerciseDialog(onDismiss: () -> Unit, onCreated: (Exercise) -> Unit = {}) {
    var name by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf(MUSCLES.first()) }
    OdDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo ejercicio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
                Text("Grupo muscular", color = Muted, style = MaterialTheme.typography.labelLarge)
                MUSCLES.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { m -> ChoicePill(m, muscle == m, Modifier.weight(1f)) { muscle = m } }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                val ex = Exercise(name = name.trim(), muscle = muscle)
                Repo.update { it.copy(exercises = it.exercises + ex) }
                onCreated(ex); onDismiss()
            }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Muted) } },
    )
}

// ---------- Historial ----------

@Composable
private fun HistoryTab(data: AppData) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var open by remember { mutableStateOf<Workout?>(null) }
    val trainedDays = data.workouts.map { it.date }.toSet()
    val monthWorkouts = data.workouts.filter { YearMonth.from(LocalDate.ofEpochDay(it.date)) == month }.sortedByDescending { it.startedAt }

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OdCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Filled.KeyboardArrowLeft, "Mes anterior", tint = Muted) }
                    Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", ES)).replaceFirstChar { it.uppercase() },
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, color = Ink)
                    IconButton(onClick = { month = month.plusMonths(1) }, enabled = month < YearMonth.now()) { Icon(Icons.Filled.KeyboardArrowRight, "Mes siguiente", tint = Muted) }
                }
                MonthGrid(month, trainedDays)
                Spacer(Modifier.height(8.dp))
                Text("${monthWorkouts.map { it.date }.distinct().size} día(s) entrenados este mes", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (monthWorkouts.isEmpty()) item { EmptyState("📆", "Sin entrenamientos este mes", "Cuando termines uno aparecerá aquí.") }
        items(monthWorkouts, key = { it.id }) { w ->
            OdCard(onClick = { open = w }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(shortName(w.name), fontWeight = FontWeight.SemiBold, color = Ink)
                        Text("${fmtDay(w.date)} · ${w.entries.size} ejercicios · ${w.setCount()} series · ${w.minutes} min", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("${fmtCompact(w.volume())} kg", color = Ink2, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    open?.let { w ->
        var confirm by remember { mutableStateOf(false) }
        OdDialog(
            onDismissRequest = { open = null },
            title = { Text(shortName(w.name)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("${fmtLongDate(LocalDate.ofEpochDay(w.date))} · ${w.minutes} min", color = Muted)
                    w.entries.forEach { e ->
                        Spacer(Modifier.height(12.dp))
                        Text(exName(data, e.exerciseId), fontWeight = FontWeight.SemiBold, color = Ink)
                        Text(e.sets.joinToString("   ") { "${fmtWeight(it.weight)} kg × ${it.reps}" }, color = Muted)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = null }) { Text("Cerrar") } },
            dismissButton = { TextButton(onClick = { confirm = true }) { Text("Eliminar", color = Bad) } },
        )
        if (confirm) ConfirmDialog("Eliminar entrenamiento", "¿Seguro? No se puede deshacer.", onDismiss = { confirm = false }) {
            Repo.update { d -> d.copy(workouts = d.workouts.filterNot { it.id == w.id }) }
            open = null
        }
    }
}

@Composable
fun MonthGrid(month: YearMonth, marked: Set<Long>) {
    val offset = month.atDay(1).dayOfWeek.value - 1
    val days = month.lengthOfMonth()
    val t = today()
    Row { listOf("L", "M", "X", "J", "V", "S", "D").forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = Faint, style = MaterialTheme.typography.labelSmall) } }
    Spacer(Modifier.height(4.dp))
    for (row in 0 until (offset + days + 6) / 7) {
        Row {
            for (col in 0 until 7) {
                val idx = row * 7 + col - offset + 1
                Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                    if (idx in 1..days) {
                        val ed = month.atDay(idx).toEpochDay()
                        val on = ed in marked
                        Box(
                            Modifier.fillMaxSize().clip(CircleShape)
                                .background(if (on) Ink2 else androidx.compose.ui.graphics.Color.Transparent)
                                .then(if (ed == t) Modifier.border(2.dp, Accent, CircleShape) else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("$idx", style = MaterialTheme.typography.bodySmall,
                                color = if (on) Bg else if (ed == t) Accent else Ink2,
                                fontWeight = if (on || ed == t) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

// ---------- Progreso ----------

@Composable
private fun ProgressTab(data: AppData) {
    val withData = data.workouts.flatMap { w -> w.entries.filter { it.sets.isNotEmpty() }.map { it.exerciseId } }
        .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
    if (withData.isEmpty()) {
        EmptyState("📈", "Aún no hay progreso que mostrar", "Termina tu primer entrenamiento y aquí verás cómo suben tus pesos.")
        return
    }
    var selected by rememberSaveable { mutableStateOf(withData.first()) }
    if (selected !in withData) selected = withData.first()

    val points = data.workouts.sortedBy { it.startedAt }.mapNotNull { w ->
        val sets = w.entries.filter { it.exerciseId == selected }.flatMap { it.sets }
        if (sets.isEmpty()) null else Triple(w.date, sets.maxOf { it.weight }, sets)
    }
    val allSets = points.flatMap { it.third }
    val best = allSets.maxByOrNull { it.weight }!!
    val best1rm = allSets.maxOf { oneRm(it) }
    val diff = round1(points.last().second - points.first().second)

    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(withData) { id -> ChoicePill(exName(data, id), id == selected) { selected = id } }
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(R.drawable.ic_trophy, fmtWeight(best.weight), "récord × ${best.reps}", Modifier.weight(1f))
                StatTile(R.drawable.ic_bolt, fmtWeight(round1(best1rm)), "1RM estimado", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(R.drawable.ic_nav_gym, (if (diff >= 0) "+" else "") + fmtWeight(diff), "kg de cambio", Modifier.weight(1f))
                StatTile(R.drawable.ic_calendar, "${points.size}", "sesiones", Modifier.weight(1f))
            }
            OdCard {
                CardLabel("Peso máximo por sesión", sub = "kg · toca un punto")
                Spacer(Modifier.height(12.dp))
                if (points.size < 2) Text("Necesitas al menos 2 sesiones para ver la gráfica.", color = Muted)
                else LineChart(points.map { it.first to it.second })
            }
            OdCard {
                CardLabel("Últimas sesiones")
                Spacer(Modifier.height(8.dp))
                points.takeLast(6).reversed().forEach { (date, _, sets) ->
                    Row(Modifier.padding(vertical = 6.dp)) {
                        Text(fmtDay(date), color = Muted, modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodyMedium)
                        Text(sets.joinToString("  ") { "${fmtWeight(it.weight)}×${it.reps}" }, color = Ink, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

// ---------- Rutinas ----------

@Composable
private fun RoutinesTab(data: AppData) {
    var editing by remember { mutableStateOf<Routine?>(null) }
    var creating by remember { mutableStateOf(false) }
    var newExercise by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(data.routines, key = { it.id }) { r ->
            OdCard(onClick = { editing = r }) {
                Text(r.name, fontWeight = FontWeight.SemiBold, color = Ink)
                Spacer(Modifier.height(4.dp))
                Text(r.exerciseIds.joinToString(" · ") { exName(data, it) }, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item { PrimaryButton("Nueva rutina") { creating = true } }
        item { SecondaryButton("Crear ejercicio personalizado") { newExercise = true } }
        item { Text("Tienes ${data.exercises.size} ejercicios disponibles.", color = Faint, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp)) }
    }
    if (creating) RoutineDialog(data, null) { creating = false }
    editing?.let { RoutineDialog(data, it) { editing = null } }
    if (newExercise) ExerciseDialog(onDismiss = { newExercise = false })
}

@Composable
private fun RoutineDialog(data: AppData, initial: Routine?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    val chosen = remember { mutableStateListOf<String>().apply { addAll(initial?.exerciseIds ?: emptyList()) } }
    var confirmDelete by remember { mutableStateOf(false) }
    OdDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nueva rutina" else "Editar rutina") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre (ej. Pecho y tríceps)") }, singleLine = true, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
                Text("${chosen.size} ejercicio(s), en el orden en que los marques", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    MUSCLES.forEach { m ->
                        val group = data.exercises.filter { it.muscle == m }
                        if (group.isNotEmpty()) {
                            item { Text(m.uppercase(), color = Faint, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 12.dp)) }
                            items(group, key = { it.id }) { ex ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    RoundCheck(ex.id in chosen, { if (ex.id in chosen) chosen.remove(ex.id) else chosen.add(ex.id) }, ex.name)
                                    Text(ex.name, color = Ink, modifier = Modifier.weight(1f))
                                    val pos = chosen.indexOf(ex.id)
                                    if (pos >= 0) Text("${pos + 1}", color = Accent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    if (initial != null) item { TextButton(onClick = { confirmDelete = true }) { Text("Eliminar rutina", color = Bad) } }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && chosen.isNotEmpty(), onClick = {
                val r = (initial ?: Routine(name = "", exerciseIds = emptyList())).copy(name = name.trim(), exerciseIds = chosen.toList())
                Repo.update { d -> d.copy(routines = if (initial == null) d.routines + r else d.routines.map { if (it.id == r.id) r else it }) }
                onDismiss()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Muted) } },
    )
    if (confirmDelete && initial != null) ConfirmDialog("Eliminar rutina", "¿Eliminar \"${initial.name}\"? Tu historial no se borra.", onDismiss = { confirmDelete = false }) {
        Repo.update { d -> d.copy(routines = d.routines.filterNot { it.id == initial.id }) }
        onDismiss()
    }
}
