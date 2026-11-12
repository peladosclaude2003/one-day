package com.oneday.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneday.app.data.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private fun exName(data: AppData, id: String) = data.exercises.firstOrNull { it.id == id }?.name ?: "Ejercicio eliminado"

@Composable
fun GymScreen(data: AppData) {
    val active = data.activeWorkout
    if (active != null) { ActiveWorkout(data, active); return }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column {
        ScreenTitle("Gimnasio", gymSubtitle(data))
        ScrollableTabRow(selectedTabIndex = tab, containerColor = Bg, edgePadding = 16.dp, divider = {}) {
            listOf("Entrenar", "Historial", "Progreso", "Rutinas").forEachIndexed { i, l ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(l) }, unselectedContentColor = Muted)
            }
        }
        when (tab) {
            0 -> StartWorkout(data)
            1 -> HistoryTab(data)
            2 -> ProgressTab(data)
            else -> RoutinesTab(data)
        }
    }
}

fun gymSubtitle(data: AppData): String {
    val now = LocalDate.now()
    val weekStart = now.minusDays((now.dayOfWeek.value - 1).toLong()).toEpochDay()
    val n = data.workouts.map { it.date }.filter { it >= weekStart }.distinct().size
    return "Esta semana: $n día(s) entrenados"
}

fun startWorkout(name: String, exerciseIds: List<String>) = Repo.update { d ->
    d.copy(activeWorkout = Workout(date = today(), name = name, entries = exerciseIds.map { WorkEntry(it) }))
}

@Composable
private fun StartWorkout(data: AppData) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            val trained = data.workouts.filter { it.date == today() }
            if (trained.isNotEmpty()) {
                SectionCard { Text("✅ Hoy ya entrenaste: ${trained.joinToString { it.name }}", color = Good) }
            }
        }
        item { Text("Elige una rutina", color = Muted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) }
        items(data.routines, key = { it.id }) { r ->
            val last = data.workouts.filter { it.name == r.name }.maxByOrNull { it.startedAt }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardBg).clickable { startWorkout(r.name, r.exerciseIds) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(r.name, fontWeight = FontWeight.SemiBold)
                    Text("${r.exerciseIds.size} ejercicios" + (last?.let { " · última vez: ${fmtDay(it.date).lowercase()}" } ?: ""), color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Filled.PlayArrow, "Empezar", tint = Accent)
            }
        }
        item {
            OutlinedButton(onClick = { startWorkout("Entrenamiento libre", emptyList()) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Text("Entrenamiento libre (sin rutina)")
            }
        }
    }
}

// ---------- Entrenamiento en curso ----------

@Composable
private fun ActiveWorkout(data: AppData, w: Workout) {
    var addEx by remember { mutableStateOf(false) }
    var editSet by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (entrada, serie) ; serie = -1 para nueva
    var confirmDiscard by remember { mutableStateOf(false) }
    val now = rememberNow()
    val mins = ((now - w.startedAt) / 60_000).toInt()
    fun upd(f: (Workout) -> Workout) = Repo.update { d -> d.copy(activeWorkout = d.activeWorkout?.let(f)) }
    BackHandler { /* evita salir por accidente */ }

    Column {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(w.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("⏱ $mins min · ${w.setCount()} series · ${fmtWeight(round1(w.volume()))} kg de volumen", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (w.entries.isEmpty()) item { EmptyState("🏋️", "Agrega tu primer ejercicio", "Usa el botón de abajo.") }
            w.entries.forEachIndexed { ei, e ->
                item(key = "e$ei${e.exerciseId}") {
                    val prev = lastSetsFor(data, e.exerciseId)
                    SectionCard(title = exName(data, e.exerciseId), action = {
                        IconButton(onClick = { upd { it.copy(entries = it.entries.filterIndexed { i, _ -> i != ei }) } }) { Icon(Icons.Filled.Close, "Quitar ejercicio", tint = Muted) }
                    }) {
                        if (prev != null) Text("Última vez: " + prev.joinToString("  ") { "${fmtWeight(it.weight)}×${it.reps}" }, color = Muted, style = MaterialTheme.typography.bodySmall)
                        e.sets.forEachIndexed { si, s ->
                            Row(
                                Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { editSet = ei to si }.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Serie ${si + 1}", color = Muted, modifier = Modifier.width(72.dp))
                                Text("${fmtWeight(s.weight)} kg", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text("× ${s.reps} reps", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { editSet = ei to -1 }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("Agregar serie") }
                    }
                }
            }
            item {
                OutlinedButton(onClick = { addEx = true }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Agregar ejercicio")
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { confirmDiscard = true }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Descartar", color = Bad) }
            Button(onClick = {
                Repo.update { d ->
                    val done = w.copy(minutes = mins, entries = w.entries.filter { it.sets.isNotEmpty() })
                    d.copy(activeWorkout = null, workouts = if (done.entries.isEmpty()) d.workouts else d.workouts + done)
                }
            }, modifier = Modifier.weight(1.4f).height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Terminar ✔", fontWeight = FontWeight.Bold) }
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

@Composable
private fun SetDialog(title: String, base: WorkSet?, canDelete: Boolean, onDismiss: () -> Unit, onDelete: () -> Unit, onSave: (WorkSet) -> Unit) {
    var weight by remember { mutableStateOf(base?.let { fmtWeight(it.weight) } ?: "") }
    var reps by remember { mutableStateOf(base?.reps?.toString() ?: "") }
    val w = weight.replace(',', '.').toDoubleOrNull()
    val r = reps.toIntOrNull()
    AlertDialog(
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun Stepper(label: String, value: String, onChange: (String) -> Unit, step: Double, decimal: Boolean) {
    fun bump(dir: Int) {
        val v = (value.replace(',', '.').toDoubleOrNull() ?: 0.0) + dir * step
        val c = v.coerceAtLeast(0.0)
        onChange(if (decimal) fmtWeight(round1(c)) else c.toInt().toString())
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = { bump(-1) }) { Text("−", fontSize = 20.sp) }
        OutlinedTextField(
            value, { onChange(it.filter { ch -> ch.isDigit() || (decimal && (ch == '.' || ch == ',')) }) },
            label = { Text(label) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
        )
        FilledTonalIconButton(onClick = { bump(1) }) { Text("+", fontSize = 20.sp) }
    }
}

@Composable
private fun ExercisePicker(data: AppData, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elegir ejercicio") },
        text = {
            Column {
                OutlinedTextField(query, { query = it }, label = { Text("Buscar") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.heightIn(max = 380.dp).padding(top = 8.dp)) {
                    val list = data.exercises.filter { query.isBlank() || it.name.contains(query, true) || it.muscle.contains(query, true) }
                    MUSCLES.forEach { m ->
                        val group = list.filter { it.muscle == m }
                        if (group.isNotEmpty()) {
                            item { Text(m, color = Accent, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)) }
                            items(group, key = { it.id }) { ex ->
                                Text(ex.name, modifier = Modifier.fillMaxWidth().clickable { onPick(ex.id); onDismiss() }.padding(vertical = 10.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { creating = true }) { Text("+ Crear ejercicio") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
    if (creating) ExerciseDialog(onDismiss = { creating = false }) { onPick(it.id); onDismiss() }
}

@Composable
fun ExerciseDialog(onDismiss: () -> Unit, onCreated: (Exercise) -> Unit = {}) {
    var name by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf(MUSCLES.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo ejercicio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Grupo muscular", color = Muted, style = MaterialTheme.typography.labelLarge)
                MUSCLES.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { m -> FilterChip(selected = muscle == m, onClick = { muscle = m }, label = { Text(m) }) }
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

// ---------- Historial ----------

@Composable
private fun HistoryTab(data: AppData) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var open by remember { mutableStateOf<Workout?>(null) }
    val trainedDays = data.workouts.map { it.date }.toSet()
    val monthWorkouts = data.workouts.filter { LocalDate.ofEpochDay(it.date).let { d -> YearMonth.from(d) == month } }.sortedByDescending { it.startedAt }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Filled.KeyboardArrowLeft, "Mes anterior") }
                    Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", ES)).replaceFirstChar { it.uppercase() },
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { month = month.plusMonths(1) }, enabled = month < YearMonth.now()) { Icon(Icons.Filled.KeyboardArrowRight, "Mes siguiente") }
                }
                MonthGrid(month, trainedDays)
                Spacer(Modifier.height(8.dp))
                Text("${monthWorkouts.map { it.date }.distinct().size} día(s) entrenados este mes", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (monthWorkouts.isEmpty()) item { EmptyState("📆", "Sin entrenamientos este mes", "Cuando termines uno aparecerá aquí.") }
        items(monthWorkouts, key = { it.id }) { w ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardBg).clickable { open = w }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(w.name, fontWeight = FontWeight.SemiBold)
                    Text("${fmtDay(w.date)} · ${w.entries.size} ejercicios · ${w.setCount()} series · ${w.minutes} min", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Text("${fmtWeight(round1(w.volume()))} kg", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    open?.let { w ->
        var confirm by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { open = null },
            title = { Text(w.name) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("${fmtLongDate(LocalDate.ofEpochDay(w.date))} · ${w.minutes} min", color = Muted)
                    w.entries.forEach { e ->
                        Spacer(Modifier.height(10.dp))
                        Text(exName(data, e.exerciseId), fontWeight = FontWeight.SemiBold)
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
    val first = month.atDay(1)
    val offset = first.dayOfWeek.value - 1
    val days = month.lengthOfMonth()
    val t = today()
    Row { listOf("L", "M", "X", "J", "V", "S", "D").forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = Muted, style = MaterialTheme.typography.labelSmall) } }
    Spacer(Modifier.height(4.dp))
    val cells = offset + days
    for (row in 0 until (cells + 6) / 7) {
        Row {
            for (col in 0 until 7) {
                val idx = row * 7 + col - offset + 1
                Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                    if (idx in 1..days) {
                        val ed = month.atDay(idx).toEpochDay()
                        val on = ed in marked
                        Box(
                            Modifier.fillMaxSize().clip(CircleShape).background(if (on) Accent else androidx.compose.ui.graphics.Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("$idx", style = MaterialTheme.typography.bodySmall,
                                color = if (on) MaterialTheme.colorScheme.onPrimary else if (ed == t) Accent else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (on || ed == t) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

// ---------- Progreso ----------

@OptIn(ExperimentalMaterial3Api::class)
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
    var expanded by remember { mutableStateOf(false) }

    // Un punto por sesión: el peso máximo levantado ese día.
    val points = data.workouts.sortedBy { it.startedAt }.mapNotNull { w ->
        val sets = w.entries.filter { it.exerciseId == selected }.flatMap { it.sets }
        if (sets.isEmpty()) null else Triple(w.date, sets.maxOf { it.weight }, sets)
    }
    val allSets = points.flatMap { it.third }
    val best = allSets.maxByOrNull { it.weight }!!
    val best1rm = allSets.maxOf { oneRm(it) }
    val first = points.first().second
    val last = points.last().second

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                exName(data, selected), {}, readOnly = true, label = { Text("Ejercicio") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                withData.forEach { id -> DropdownMenuItem(text = { Text(exName(data, id)) }, onClick = { selected = id; expanded = false }) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Récord", "${fmtWeight(best.weight)} kg", "× ${best.reps} reps", Modifier.weight(1f))
            StatTile("1RM estimado", "${fmtWeight(round1(best1rm))} kg", "fórmula Epley", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val diff = round1(last - first)
            StatTile("Cambio total", (if (diff >= 0) "+" else "") + fmtWeight(diff) + " kg", "desde tu 1.ª sesión", Modifier.weight(1f))
            StatTile("Sesiones", "${points.size}", "${allSets.size} series en total", Modifier.weight(1f))
        }
        SectionCard(title = "Peso máximo por sesión (kg)") {
            if (points.size < 2) Text("Necesitas al menos 2 sesiones para ver la gráfica.", color = Muted)
            else LineChart(points.map { it.first to it.second })
        }
        SectionCard(title = "Últimas sesiones") {
            points.takeLast(6).reversed().forEach { (date, _, sets) ->
                Row(Modifier.padding(vertical = 4.dp)) {
                    Text(fmtDay(date), color = Muted, modifier = Modifier.width(90.dp), style = MaterialTheme.typography.bodySmall)
                    Text(sets.joinToString("  ") { "${fmtWeight(it.weight)}×${it.reps}" }, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun StatTile(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBg, contentColor = MaterialTheme.colorScheme.onSurface)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = Muted, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(sub, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Gráfica de línea simple: un punto por sesión. Toca un punto para ver su valor. */
@Composable
fun LineChart(points: List<Pair<Long, Double>>) {
    var sel by remember(points) { mutableIntStateOf(points.lastIndex) }
    val lineColor = Accent
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val surface = CardBg
    val minV = points.minOf { it.second }
    val maxV = points.maxOf { it.second }
    val pad = ((maxV - minV) * 0.15).coerceAtLeast(2.5)
    val lo = (minV - pad).coerceAtLeast(0.0)
    val hi = maxV + pad
    val p = points[sel]
    Text("${fmtWeight(p.second)} kg · ${fmtDay(p.first)}", fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    Row {
        Column(Modifier.height(180.dp).padding(end = 6.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(fmtWeight(round1(hi)), color = Muted, style = MaterialTheme.typography.labelSmall)
            Text(fmtWeight(round1((hi + lo) / 2)), color = Muted, style = MaterialTheme.typography.labelSmall)
            Text(fmtWeight(round1(lo)), color = Muted, style = MaterialTheme.typography.labelSmall)
        }
        Canvas(
            Modifier.weight(1f).height(180.dp).pointerInput(points) {
                detectTapGestures { o ->
                    val step = if (points.size > 1) size.width / (points.size - 1f) else 0f
                    sel = if (step == 0f) 0 else (o.x / step).let { kotlin.math.round(it).toInt() }.coerceIn(0, points.lastIndex)
                }
            }
        ) {
            val inset = 6.dp.toPx()
            val h = size.height - inset * 2
            val stepX = (size.width - inset * 2) / (points.size - 1).coerceAtLeast(1)
            fun pos(i: Int) = Offset(inset + i * stepX, inset + h - ((points[i].second - lo) / (hi - lo) * h).toFloat())
            for (g in 0..2) {
                val y = inset + h * g / 2f
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            val path = Path()
            points.indices.forEach { i -> val o = pos(i); if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y) }
            drawPath(path, lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            val sp = pos(sel)
            drawLine(gridColor, Offset(sp.x, 0f), Offset(sp.x, size.height), strokeWidth = 1.dp.toPx())
            points.indices.forEach { i ->
                val o = pos(i)
                val r = if (i == sel) 6.dp.toPx() else 4.dp.toPx()
                drawCircle(surface, r + 2.dp.toPx(), o)
                drawCircle(lineColor, r, o)
            }
        }
    }
    Row(Modifier.padding(start = 30.dp, top = 4.dp)) {
        Text(fmtDay(points.first().first), color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Text(fmtDay(points.last().first), color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}

// ---------- Rutinas ----------

@Composable
private fun RoutinesTab(data: AppData) {
    var editing by remember { mutableStateOf<Routine?>(null) }
    var creating by remember { mutableStateOf(false) }
    var newExercise by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(data.routines, key = { it.id }) { r ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardBg).clickable { editing = r }.padding(16.dp)) {
                Text(r.name, fontWeight = FontWeight.SemiBold)
                Text(r.exerciseIds.joinToString(" · ") { exName(data, it) }, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) { Text("+ Nueva rutina") }
        }
        item {
            OutlinedButton(onClick = { newExercise = true }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) { Text("+ Crear ejercicio personalizado") }
        }
        item { Text("Tienes ${data.exercises.size} ejercicios disponibles.", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp)) }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nueva rutina" else "Editar rutina") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre (ej. Pecho y tríceps)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("${chosen.size} ejercicio(s) elegidos, en el orden en que los marques", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    MUSCLES.forEach { m ->
                        val group = data.exercises.filter { it.muscle == m }
                        if (group.isNotEmpty()) {
                            item { Text(m, color = Accent, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 10.dp)) }
                            items(group, key = { it.id }) { ex ->
                                Row(Modifier.fillMaxWidth().clickable { if (ex.id in chosen) chosen.remove(ex.id) else chosen.add(ex.id) }, verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = ex.id in chosen, onCheckedChange = { if (it) chosen.add(ex.id) else chosen.remove(ex.id) })
                                    Text(ex.name, modifier = Modifier.weight(1f))
                                    val pos = chosen.indexOf(ex.id)
                                    if (pos >= 0) Text("${pos + 1}", color = Muted)
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
    if (confirmDelete && initial != null) ConfirmDialog("Eliminar rutina", "¿Eliminar \"${initial.name}\"? Tu historial no se borra.", onDismiss = { confirmDelete = false }) {
        Repo.update { d -> d.copy(routines = d.routines.filterNot { it.id == initial.id }) }
        onDismiss()
    }
}
