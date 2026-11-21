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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.graphicsLayer
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
    Repo.update { d -> d.copy(activeWorkout = Workout(date = today(), name = name, entries = exerciseIds.map { WorkEntry(it, plannedSets(d, it)) })) }
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
    val maxKg = recent.flatMap { w -> w.entries.flatMap { it.sets } }.filter { it.counts }.maxOfOrNull { it.weight } ?: 0.0

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

        MuscleCard(data)

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
            Row(verticalAlignment = Alignment.CenterVertically) { Glyph("hecho", Good, 16.dp); Spacer(Modifier.width(6.dp)); Text("Hoy ya entrenaste: ${trainedToday.joinToString { shortName(it.name) }}", color = Good, style = MaterialTheme.typography.bodyMedium) }
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
        data.workouts.maxByOrNull { it.startedAt }?.let { last ->
            Spacer(Modifier.height(8.dp))
            SecondaryButton("Repetir última: ${shortName(last.name)} · ${fmtDay(last.date).lowercase()}", color = Ink2) { repeatWorkout(last); onDismiss() }
        }
        Spacer(Modifier.height(8.dp))
        SecondaryButton("Entrenamiento libre (sin rutina)") { startWorkout("Entrenamiento libre", emptyList()); onDismiss() }
    }
}

private val Color08 = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f)

@Composable
private fun StatSheet(data: AppData, kind: Int, onDismiss: () -> Unit) {
    val recent = data.recentWorkouts(30)
    val sets = recent.flatMap { w -> w.entries.flatMap { it.sets } }.filter { it.counts }
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
    val avg = movingAvg7(weights)
    val avgNow = avg.lastOrNull()?.second
    val rate = weeklyChange(weights)
    OdCard(onClick = onClick) {
        CardLabel("Peso corporal", R.drawable.ic_scale, trailing = { Icon(Icons.Filled.KeyboardArrowRight, null, tint = Faint) })
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            RollingText(avgNow?.let { fmtWeight(round1(it)) } ?: "—", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(" kg", color = Muted, modifier = Modifier.padding(bottom = 4.dp))
            Text("  media 7 días", color = Faint, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp).weight(1f))
            if (rate != null) Text(
                (if (rate >= 0) "+" else "−") + fmtWeight(round1(kotlin.math.abs(rate))) + " kg/sem",
                color = Ink2, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Text(
            when {
                last == null -> "Toca para registrar tu peso"
                goal == null -> "Último registro: ${fmtWeight(last.kg)} kg (${fmtDay(last.date).lowercase()}) · sin meta"
                else -> {
                    val gap = goal - (avgNow ?: last.kg)
                    val toward = rate != null && rate != 0.0 && (gap > 0) == (rate > 0)
                    if (kotlin.math.abs(gap) < 0.15) "¡Meta de ${fmtWeight(goal)} kg alcanzada!"
                    else "Meta ${fmtWeight(goal)} kg · faltan ${fmtWeight(round1(kotlin.math.abs(gap)))} kg" +
                        if (toward) " · ~${kotlin.math.ceil(kotlin.math.abs(gap / rate!!)).toInt()} semanas a este ritmo" else ""
                }
            }, color = Muted, style = MaterialTheme.typography.bodySmall,
        )
        if (weights.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            val recent = weights.filter { it.date > (weights.last().date - 42) }
            WeightSpark(recent.map { it.date to it.kg }, movingAvg7(weights).filter { it.first > (weights.last().date - 42) }, goal)
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
    var dialogSet by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var noteFor by remember { mutableStateOf<String?>(null) }
    var focus by rememberSaveable { mutableIntStateOf(w.entries.indexOfFirst { e -> e.sets.isEmpty() || e.sets.any { !it.done } }.coerceAtLeast(0)) }
    var activeSet by remember { mutableStateOf<Int?>(null) }
    val bests = remember(data.workouts) { w.entries.map { it.exerciseId }.distinct().associateWith { bestsFor(data, it) } }
    val now = rememberNow()
    val elapsed = ((now - w.startedAt) / 1000).toInt()
    fun upd(f: (Workout) -> Workout) = Repo.update { d -> d.copy(activeWorkout = d.activeWorkout?.let(f)) }
    fun setAt(ei: Int, si: Int, f: (WorkSet) -> WorkSet) = upd { it.copy(entries = it.entries.mapIndexed { i, en ->
        if (i != ei) en else en.copy(sets = en.sets.mapIndexed { j, st -> if (j == si) f(st) else st }) }) }
    DisposableEffect(Unit) { UiState.hideNav = true; onDispose { UiState.hideNav = false } }
    BackHandler { GymUi.minimized = true }

    val total = w.entries.sumOf { it.sets.size }
    val doneN = w.entries.sumOf { e -> e.sets.count { it.done } }
    val prog by androidx.compose.animation.core.animateFloatAsState(if (total == 0) 0f else doneN / total.toFloat(), label = "prog")

    /** Marca/desmarca una serie; si es récord, medalla y doble pulso. */
    fun toggleDone(ei: Int, si: Int) {
        val e = w.entries[ei]; val st = e.sets[si]
        val nowDone = !st.done
        if (nowDone) {
            val prevBest = bests[e.exerciseId]
            val hadPr = e.sets.any { isPr(it, prevBest) }
            val after = st.copy(done = true)
            if (isPr(after, prevBest) && !hadPr && Repo.data.settings.celebrationsOn) {
                Fx.pr()
                UiState.medal = MedalMsg("PR", "RÉCORD", Tier.Naranja, "Récord en ${exName(data, e.exerciseId)}", fmtSet(after).replace("×", " kg × ").let { if (after.km != null || after.seconds != null) fmtSet(after) else it })
            } else Fx.success()
            // la siguiente serie pendiente pasa a ser la activa
            activeSet = e.sets.indices.firstOrNull { it > si && !e.sets[it].done }
        } else Fx.tick()
        setAt(ei, si) { it.copy(done = nowDone) }
    }

    Column {
        // Encabezado
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 20.dp, top = 16.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleButton("Minimizar", onClick = { GymUi.minimized = true }) { Icon(Icons.Filled.KeyboardArrowDown, null, tint = Ink) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("En curso · %d:%02d".format(elapsed / 60, elapsed % 60), color = Accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(shortName(w.name), fontFamily = Display, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = (-0.5).sp)
            }
            Text("$doneN / $total", fontFamily = Display, color = Muted, fontSize = 15.sp)
        }
        Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(CardHigh)) {
            Box(Modifier.fillMaxWidth(prog).fillMaxHeight().clip(RoundedCornerShape(50)).background(Accent))
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (w.entries.isEmpty()) item { EmptyState("gym", "Agrega tu primer ejercicio", "Usa el botón de abajo.") }
            w.entries.forEachIndexed { ei, e ->
                item(key = "e$ei${e.exerciseId}") {
                    val exercise = data.exercises.firstOrNull { it.id == e.exerciseId }
                    if (ei != focus) CollapsedExercise(data, e, exercise) { focus = ei; activeSet = null }
                    else ExpandedExercise(
                        data, e, exercise, bests[e.exerciseId], activeSet,
                        onSelect = { si -> activeSet = if (activeSet == si) null else si },
                        onToggle = { si -> toggleDone(ei, si) },
                        onChange = { si, f -> setAt(ei, si, f) },
                        onMore = { si -> dialogSet = ei to si },
                        onAdd = {
                            val base = e.sets.lastOrNull()?.copy(done = false, rpe = null)
                                ?: (if ((exercise?.kind ?: 0) == 0) suggestTarget(lastSetsFor(data, e.exerciseId))?.let { WorkSet(it.weight, it.reps, done = false) } else null)
                                ?: when (exercise?.kind ?: 0) { 1 -> WorkSet(0.0, 0, seconds = 30, done = false); 2 -> WorkSet(0.0, 0, seconds = 1200, km = 2.0, done = false); else -> WorkSet(20.0, 10, done = false) }
                            upd { it.copy(entries = it.entries.mapIndexed { i, en -> if (i == ei) en.copy(sets = en.sets + base) else en }) }
                            activeSet = e.sets.size; Fx.tick()
                        },
                        onRemove = { upd { it.copy(entries = it.entries.filterIndexed { i, _ -> i != ei }) }; focus = 0; activeSet = null },
                        onNote = { noteFor = e.exerciseId },
                    )
                }
            }
            item { SecondaryButton("+ Agregar ejercicio") { addEx = true } }
            item {
                OutlinedTextField(w.note, { v -> upd { it.copy(note = v) } }, label = { Text("Nota de la sesión") },
                    placeholder = { Text("Cómo te sentiste, qué cambiar…", color = Faint) }, shape = RoundedCornerShape(20.dp), maxLines = 3, modifier = Modifier.fillMaxWidth())
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Descartar", Modifier.width(118.dp), color = Bad) { Fx.reject(); confirmDiscard = true }
            PrimaryButton("Terminar entrenamiento", Modifier.weight(1f)) {
                Fx.success()
                Repo.update { d ->
                    val fin = w.copy(minutes = elapsed / 60, entries = w.entries.map { en -> en.copy(sets = en.sets.filter { it.done }) }.filter { it.sets.isNotEmpty() })
                    d.copy(activeWorkout = null, workouts = if (fin.entries.isEmpty()) d.workouts else d.workouts + fin)
                }
            }
        }
    }

    if (addEx) ExercisePicker(data, onDismiss = { addEx = false }) { id ->
        upd { it.copy(entries = it.entries + WorkEntry(id, plannedSets(data, id))) }; focus = w.entries.size; activeSet = null
    }
    dialogSet?.let { (ei, si) ->
        val entry = w.entries.getOrNull(ei)
        val base = entry?.sets?.getOrNull(si)
        if (entry == null || base == null) { dialogSet = null; return@let }
        SetDialog(data.exercises.firstOrNull { it.id == entry.exerciseId }, base, canDelete = true, onDismiss = { dialogSet = null },
            onDelete = { Fx.reject(); upd { it.copy(entries = it.entries.mapIndexed { i, en -> if (i == ei) en.copy(sets = en.sets.filterIndexed { j, _ -> j != si }) else en }) }; activeSet = null },
        ) { set -> setAt(ei, si) { set.copy(done = it.done) } }
    }
    noteFor?.let { id -> ExerciseNoteDialog(data, id) { noteFor = null } }
    if (confirmDiscard) ConfirmDialog("Descartar entrenamiento", "Se perderán las series de este entrenamiento.", "Descartar", { confirmDiscard = false }) {
        Repo.update { it.copy(activeWorkout = null) }
    }
}

@Composable
private fun CollapsedExercise(data: AppData, e: WorkEntry, ex: Exercise?, onClick: () -> Unit) {
    val done = e.sets.count { it.done }
    val target = if ((ex?.kind ?: 0) == 0) suggestTarget(lastSetsFor(data, e.exerciseId)) else null
    OdCard(onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip("gym", if (e.sets.isNotEmpty() && done == e.sets.size) ChipState.Done else ChipState.Normal, 40.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(ex?.name ?: "Ejercicio", color = Ink, fontWeight = FontWeight.Medium)
                Text("$done / ${e.sets.size} series" + (target?.let { " · objetivo ${fmtWeight(it.weight)} × ${it.reps}" } ?: ""), color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = Faint)
        }
    }
}

@Composable
private fun ExpandedExercise(
    data: AppData, e: WorkEntry, ex: Exercise?, best: Bests?, activeSet: Int?,
    onSelect: (Int) -> Unit, onToggle: (Int) -> Unit, onChange: (Int, (WorkSet) -> WorkSet) -> Unit,
    onMore: (Int) -> Unit, onAdd: () -> Unit, onRemove: () -> Unit, onNote: () -> Unit,
) {
    val kind = ex?.kind ?: 0
    val prev = lastSetsFor(data, e.exerciseId)
    val target = if (kind == 0) suggestTarget(prev) else null
    OdCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip("gym", size = 44.dp)
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(ex?.name ?: "Ejercicio", color = Ink, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Row(Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onNote).padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Glyph("pin", Muted, 13.dp); Spacer(Modifier.width(5.dp))
                    Text(ex?.note?.ifBlank { null } ?: "Agregar nota", color = if (ex?.note.isNullOrBlank()) Faint else Ink2, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (target != null) Row(Modifier.clip(RoundedCornerShape(50)).background(if (target.up) AccentSoft else CardHigh).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Glyph(if (target.up) "arriba" else "igual", if (target.up) AccentText else Muted, 13.dp); Spacer(Modifier.width(4.dp))
                Text("${fmtWeight(target.weight)} kg", color = if (target.up) AccentText else Ink2, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = onRemove) { Glyph("cerrar", Faint, 18.dp, "Quitar ejercicio") }
        }
        Spacer(Modifier.height(10.dp))
        // Encabezado de la tabla
        val (c3, c4) = when (kind) { 1 -> "SEG" to ""; 2 -> "KM" to "MIN"; else -> "KG" to "REPS" }
        Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("SERIE" to 44.dp, "ANTERIOR" to 0.dp).forEach { (t, w) ->
                Text(t, color = Muted, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold,
                    modifier = if (w > 0.dp) Modifier.width(w) else Modifier.weight(1f))
            }
            Text(c3, color = Muted, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(70.dp), textAlign = TextAlign.Center)
            Text(c4, color = Muted, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
            Spacer(Modifier.width(44.dp))
        }
        Spacer(Modifier.height(4.dp))
        var n = 0
        e.sets.forEachIndexed { si, st ->
            if (st.kind != SET_WARMUP) n++
            val sel = activeSet == si
            val pr = isPr(st, best)
            val tag = setTag(st.kind) ?: "$n"
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (sel) CardHigh else Color.Transparent)
                    .then(if (sel) Modifier.border(1.5.dp, Accent, RoundedCornerShape(16.dp)) else Modifier)
                    .clickable { Fx.tick(); onSelect(si) }.padding(horizontal = 6.dp, vertical = 6.dp)
                    .graphicsLayer { alpha = if (st.kind == SET_WARMUP || (!st.done && !sel)) 0.6f else 1f },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(44.dp)) {
                    if (pr) Glyph("record", Accent, 18.dp, "Récord")
                    else Text(tag, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (st.kind == SET_FAILURE) Accent else Ink2)
                }
                Text(prev?.getOrNull(si)?.let { fmtSet(it).replace("×", " × ") } ?: "—", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                val (v3, v4) = when (kind) {
                    1 -> fmtSecs(st.seconds ?: 0) to ""
                    2 -> fmtWeight(round1(st.km ?: 0.0)) to "${(st.seconds ?: 0) / 60}"
                    else -> fmtWeight(st.weight) to "${st.reps}"
                }
                CellValue(v3, st.done, Modifier.width(70.dp))
                CellValue(v4, st.done, Modifier.width(52.dp))
                Spacer(Modifier.width(6.dp))
                // Casilla de hecho
                val fill by androidx.compose.animation.core.animateFloatAsState(if (st.done) 1f else 0f, label = "c")
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Ink.copy(alpha = fill))
                        .border(2.dp, Line.copy(alpha = 1f - fill), RoundedCornerShape(12.dp))
                        .clickable(role = Role.Checkbox, onClickLabel = "Serie hecha") { onToggle(si) },
                    contentAlignment = Alignment.Center,
                ) { if (st.done) DrawnCheck(Bg, 18.dp, st.done) }
            }
            if (sel) InlineSetEditor(kind, st, onChange = { f -> onChange(si, f) }, onMore = { onMore(si) })
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).border(1.dp, Line, RoundedCornerShape(50)).clickable(onClick = onAdd).padding(vertical = 11.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) { Glyph("mas", Ink2, 16.dp); Spacer(Modifier.width(6.dp)); Text("Serie", color = Ink2) }
    }
}

@Composable
private fun CellValue(v: String, done: Boolean, modifier: Modifier) {
    Box(modifier.height(36.dp).padding(horizontal = 3.dp).clip(RoundedCornerShape(12.dp)).background(if (done || v.isEmpty()) Color.Transparent else Color(0xFF26262A)),
        contentAlignment = Alignment.Center) {
        Text(v, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
    }
}

/** Edición en línea de la serie seleccionada: sin ventanas emergentes. */
@Composable
private fun InlineSetEditor(kind: Int, st: WorkSet, onChange: ((WorkSet) -> WorkSet) -> Unit, onMore: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp).clip(RoundedCornerShape(20.dp)).background(CardHigh).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            when (kind) {
                1 -> PillStepper(fmtSecs(st.seconds ?: 0), "segundos", Modifier.weight(1f),
                    { onChange { it.copy(seconds = ((it.seconds ?: 0) - 5).coerceAtLeast(0)) } }, { onChange { it.copy(seconds = (it.seconds ?: 0) + 5) } },
                    typed = (st.seconds ?: 0).toDouble(), decimals = false, onType = { v -> onChange { it.copy(seconds = v.toInt()) } })
                2 -> {
                    PillStepper(fmtWeight(round1(st.km ?: 0.0)), "km", Modifier.weight(1f),
                        { onChange { it.copy(km = round1(((it.km ?: 0.0) - 0.1).coerceAtLeast(0.0))) } }, { onChange { it.copy(km = round1((it.km ?: 0.0) + 0.1)) } },
                        typed = st.km ?: 0.0, onType = { v -> onChange { it.copy(km = v) } })
                    PillStepper("${(st.seconds ?: 0) / 60}", "min", Modifier.weight(1f),
                        { onChange { it.copy(seconds = ((it.seconds ?: 0) - 60).coerceAtLeast(0)) } }, { onChange { it.copy(seconds = (it.seconds ?: 0) + 60) } },
                        typed = ((st.seconds ?: 0) / 60).toDouble(), decimals = false, onType = { v -> onChange { it.copy(seconds = v.toInt() * 60) } })
                }
                else -> {
                    val inc = Repo.data.settings.weightStep
                    PillStepper(fmtWeight(st.weight), "kg", Modifier.weight(1f),
                        { onChange { it.copy(weight = Math.round((it.weight - inc).coerceAtLeast(0.0) * 100) / 100.0) } },
                        { onChange { it.copy(weight = Math.round((it.weight + inc) * 100) / 100.0) } },
                        typed = st.weight, onType = { v -> onChange { it.copy(weight = v) } })
                    PillStepper("${st.reps}", "reps", Modifier.weight(1f),
                        { onChange { it.copy(reps = (it.reps - 1).coerceAtLeast(0)) } }, { onChange { it.copy(reps = it.reps + 1) } },
                        typed = st.reps.toDouble(), decimals = false, onType = { v -> onChange { it.copy(reps = v.toInt()) } })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            SET_KINDS.forEachIndexed { i, k ->
                val sel = st.kind == i
                Box(Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(10.dp)).background(if (sel) Ink else Color(0xFF26262A))
                    .clickable { Fx.tick(); onChange { it.copy(kind = i) } }, contentAlignment = Alignment.Center) {
                    Text(k, color = if (sel) Bg else Ink2, style = MaterialTheme.typography.labelMedium, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Esfuerzo", color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 4.dp))
            (6..10).forEach { n ->
                val sel = st.rpe == n
                Box(Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(10.dp)).background(if (sel) Ink else Color(0xFF26262A))
                    .clickable { Fx.tick(); onChange { it.copy(rpe = if (sel) null else n) } }, contentAlignment = Alignment.Center) {
                    Text("$n", color = if (sel) Bg else Ink2, style = MaterialTheme.typography.labelLarge, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                }
            }
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF26262A)).clickable(onClickLabel = "Más opciones") { onMore() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.MoreVert, "Más opciones", tint = Ink2, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun PillStepper(value: String, unit: String, modifier: Modifier, onMinus: () -> Unit, onPlus: () -> Unit,
                        typed: Double? = null, decimals: Boolean = true, onType: ((Double) -> Unit)? = null) {
    Row(modifier.clip(RoundedCornerShape(50)).background(CardBg).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF26262A)).clickable(onClickLabel = "Menos $unit") { Fx.tick(); onMinus() }, contentAlignment = Alignment.Center) {
            Text("−", color = Ink, fontSize = 20.sp)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            if (onType != null && typed != null) TypedNumber(typed, decimals, onType)
            else RollingText(value, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(unit, color = Muted, style = MaterialTheme.typography.labelSmall)
        }
        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF26262A)).clickable(onClickLabel = "Más $unit") { Fx.tick(); onPlus() }, contentAlignment = Alignment.Center) {
            Text("+", color = Ink, fontSize = 20.sp)
        }
    }
}

@Composable
private fun ExerciseNoteDialog(data: AppData, id: String, onDismiss: () -> Unit) {
    val ex = data.exercises.firstOrNull { it.id == id } ?: run { onDismiss(); return }
    var note by remember { mutableStateOf(ex.note) }
    OdDialog(
        onDismissRequest = onDismiss,
        title = { Text(ex.name) },
        text = {
            Column {
                Text("Una nota fija que verás cada vez que hagas este ejercicio.", color = Muted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(note, { note = it }, placeholder = { Text("Ej. asiento en 4, agarre cerrado", color = Faint) }, shape = RoundedCornerShape(18.dp), maxLines = 3, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { Repo.update { d -> d.copy(exercises = d.exercises.map { if (it.id == id) it.copy(note = note.trim()) else it }) }; onDismiss() }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Muted) } },
    )
}

/** Empieza un entrenamiento copiando ejercicios y series de una sesión anterior. */
fun repeatWorkout(w: Workout) {
    GymUi.minimized = false
    Repo.update { d -> d.copy(activeWorkout = Workout(date = today(), name = w.name, entries = w.entries.map { e -> e.copy(sets = e.sets.map { it.copy(rpe = null, done = false) }) })) }
}

private fun Modifier.border1() = this.border(1.dp, Line, RoundedCornerShape(50))

@Composable
fun Stepper(label: String, value: String, onChange: (String) -> Unit, step: Double, decimal: Boolean) {
    fun bump(dir: Int) {
        val v = (value.replace(',', '.').toDoubleOrNull() ?: 0.0) + dir * step
        val c = v.coerceAtLeast(0.0)
        onChange(if (decimal) fmtWeight(Math.round(c * 100) / 100.0) else c.toInt().toString())
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
    var kind by remember { mutableIntStateOf(0) }
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
                Text("Se mide por", color = Muted, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Peso × reps", "Tiempo", "Tiempo y km").forEachIndexed { i, k -> ChoicePill(k, kind == i, Modifier.weight(1f)) { kind = i } }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                val ex = Exercise(name = name.trim(), muscle = muscle, kind = kind)
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
        if (monthWorkouts.isEmpty()) item { EmptyState("agenda", "Sin entrenamientos este mes", "Cuando termines uno aparecerá aquí.") }
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
                    if (w.note.isNotBlank()) Text("“${w.note}”", color = Ink2, modifier = Modifier.padding(top = 8.dp))
                    w.entries.forEach { e ->
                        Spacer(Modifier.height(12.dp))
                        Text(exName(data, e.exerciseId), fontWeight = FontWeight.SemiBold, color = Ink)
                        Text(e.sets.joinToString("   ") { st -> (setTag(st.kind)?.let { "$it " } ?: "") + fmtSet(st) + (st.rpe?.let { " @$it" } ?: "") }, color = Muted)
                    }
                    Spacer(Modifier.height(16.dp))
                    if (data.activeWorkout == null) PrimaryButton("Repetir esta sesión", icon = R.drawable.ic_play) { repeatWorkout(w); open = null }
                    else Text("Termina el entrenamiento en curso para poder repetir esta sesión.", color = Faint, style = MaterialTheme.typography.bodySmall)
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
    var mode by rememberSaveable { mutableIntStateOf(0) }
    Column {
        Segmented(listOf("Por grupo muscular", "Por ejercicio"), mode, { mode = it }, Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(12.dp))
        if (mode == 0) GroupProgress(data) else ExerciseProgress(data)
    }
}

@Composable
private fun ExerciseProgress(data: AppData) {
    val withData = data.workouts.flatMap { w -> w.entries.filter { it.sets.isNotEmpty() }.map { it.exerciseId } }
        .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
    if (withData.isEmpty()) {
        EmptyState("grafica", "Aún no hay progreso que mostrar", "Termina tu primer entrenamiento y aquí verás cómo suben tus pesos.")
        return
    }
    var selected by rememberSaveable { mutableStateOf(withData.first()) }
    if (selected !in withData) selected = withData.first()

    val kind = data.exercises.firstOrNull { it.id == selected }?.kind ?: 0
    fun metric(st: WorkSet) = when (kind) { 1 -> (st.seconds ?: 0).toDouble(); 2 -> st.km ?: 0.0; else -> st.weight }
    val unit = when (kind) { 1 -> "s"; 2 -> "km"; else -> "kg" }
    val points = data.workouts.sortedBy { it.startedAt }.mapNotNull { w ->
        val sets = w.entries.filter { it.exerciseId == selected }.flatMap { it.sets }.filter { it.counts }
        if (sets.isEmpty()) null else Triple(w.date, sets.maxOf { metric(it) }, sets)
    }
    if (points.isEmpty()) { EmptyState("grafica", "Solo hay calentamientos", "Registra series normales para ver tu progreso."); return }
    val allSets = points.flatMap { it.third }
    val best = allSets.maxBy { metric(it) }
    val best1rm = allSets.maxOf { oneRm(it) }
    val diff = round1(points.last().second - points.first().second)
    fun fmtM(v: Double) = if (kind == 1) fmtSecs(v.toInt()) else fmtWeight(round1(v))

    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(withData) { id -> ChoicePill(exName(data, id), id == selected) { selected = id } }
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                when (kind) {
                    0 -> {
                        StatTile(R.drawable.ic_trophy, fmtWeight(best.weight), "récord × ${best.reps}", Modifier.weight(1f))
                        StatTile(R.drawable.ic_bolt, fmtWeight(round1(best1rm)), "1RM estimado", Modifier.weight(1f))
                    }
                    1 -> {
                        StatTile(R.drawable.ic_trophy, fmtSecs(best.seconds ?: 0), "récord", Modifier.weight(1f))
                        StatTile(R.drawable.ic_clock, fmtSecs(allSets.sumOf { it.seconds ?: 0 }), "tiempo total", Modifier.weight(1f))
                    }
                    else -> {
                        StatTile(R.drawable.ic_trophy, fmtWeight(round1(best.km ?: 0.0)), "km récord", Modifier.weight(1f))
                        StatTile(R.drawable.ic_bolt, fmtWeight(round1(allSets.sumOf { it.km ?: 0.0 })), "km totales", Modifier.weight(1f))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(R.drawable.ic_nav_gym, (if (diff >= 0) "+" else "−") + fmtM(kotlin.math.abs(diff)), "$unit de cambio", Modifier.weight(1f))
                StatTile(R.drawable.ic_calendar, "${points.size}", "sesiones", Modifier.weight(1f))
            }
            OdCard {
                CardLabel(when (kind) { 1 -> "Mejor tiempo por sesión"; 2 -> "Distancia máxima por sesión"; else -> "Peso máximo por sesión" }, sub = "$unit · toca un punto")
                Spacer(Modifier.height(12.dp))
                if (points.size < 2) Text("Necesitas al menos 2 sesiones para ver la gráfica.", color = Muted)
                else LineChart(points.map { it.first to it.second }, unit, ::fmtM)
            }
            OdCard {
                CardLabel("Últimas sesiones")
                Spacer(Modifier.height(8.dp))
                points.takeLast(6).reversed().forEach { (date, _, sets) ->
                    Row(Modifier.padding(vertical = 6.dp)) {
                        Text(fmtDay(date), color = Muted, modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodyMedium)
                        Text(sets.joinToString("  ") { fmtSet(it) }, color = Ink, style = MaterialTheme.typography.bodyMedium)
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


/** Número que se toca y se escribe directo (ej. 7, 8 o 5.9 kg). */
@Composable
private fun TypedNumber(value: Double, decimals: Boolean, onValue: (Double) -> Unit) {
    fun show(v: Double) = if (decimals) fmtWeight(v) else v.toInt().toString()
    var text by remember { mutableStateOf(show(value)) }
    LaunchedEffect(value) { if (text.replace(',', '.').toDoubleOrNull() != value) text = show(value) }
    androidx.compose.foundation.text.BasicTextField(
        value = text,
        onValueChange = { s ->
            val f = s.filter { it.isDigit() || (decimals && (it == '.' || it == ',')) }.take(6)
            text = f
            f.replace(',', '.').toDoubleOrNull()?.let(onValue)
        },
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink, textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimals) KeyboardType.Decimal else KeyboardType.Number),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Escribir valor" },
    )
}
