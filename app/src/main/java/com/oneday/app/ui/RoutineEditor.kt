package com.oneday.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneday.app.data.*
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Editor de rutina: orden arrastrando, series y repeticiones objetivo, y superseries. */
@Composable
fun RoutineEditor(data: AppData, initial: Routine, isNew: Boolean, onClose: () -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    // cada fila lleva un id estable para poder arrastrarla
    val rows = remember { mutableStateListOf<Pair<String, RoutineItem>>().apply { addAll(initial.plan.map { newId() to it }) } }
    var picking by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { UiState.hideNav = true; onDispose { UiState.hideNav = false } }
    BackHandler(onBack = onClose)

    fun regroup() {
        // reetiqueta superseries como A, B, C… en orden de aparición
        var letter = 'A'; val map = HashMap<String, String>()
        for (i in rows.indices) { val g = rows[i].second.group ?: continue; rows[i] = rows[i].first to rows[i].second.copy(group = map.getOrPut(g) { (letter++).toString() }) }
        // una superserie de un solo ejercicio deja de serlo
        val counts = rows.mapNotNull { it.second.group }.groupingBy { it }.eachCount()
        for (i in rows.indices) if (rows[i].second.group?.let { counts[it] == 1 } == true) rows[i] = rows[i].first to rows[i].second.copy(group = null)
    }
    fun save() {
        val items = rows.map { it.second }
        val r = initial.copy(name = name.trim(), items = items, exerciseIds = items.map { it.exerciseId })
        Repo.update { d -> d.copy(routines = if (isNew) d.routines + r else d.routines.map { if (it.id == r.id) r else it }) }
        Fx.success(); onClose()
    }

    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        val f = rows.indexOfFirst { it.first == from.key }; val t = rows.indexOfFirst { it.first == to.key }
        if (f >= 0 && t >= 0) { rows.add(t, rows.removeAt(f)); Fx.tick() }
    }

    Column(Modifier.fillMaxSize().background(Bg)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleButton("Volver", onClick = onClose) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Ink) }
            Spacer(Modifier.width(12.dp))
            Text(if (isNew) "Nueva rutina" else "Editar rutina", style = MaterialTheme.typography.headlineMedium, color = Ink, modifier = Modifier.weight(1f))
        }
        LazyColumn(Modifier.weight(1f), state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item(key = "name") {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre (ej. Pecho y tríceps)") }, singleLine = true, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text("Mantén presionado un ejercicio para cambiar su orden.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            itemsIndexed(rows, key = { _, r -> r.first }) { i, (key, item) ->
                ReorderableItem(reorder, key = key) { dragging ->
                    val elev by animateDpAsState(if (dragging) 16.dp else 0.dp, label = "elev")
                    val ex = data.exercises.firstOrNull { it.id == item.exerciseId }
                    fun upd(f: (RoutineItem) -> RoutineItem) { rows[i] = key to f(item) }
                    Column(
                        Modifier.shadow(elev, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(if (dragging) CardHigh else CardBg)
                            .border(1.dp, if (item.group != null) Accent.copy(alpha = 0.35f) else Hairline, RoundedCornerShape(24.dp))
                            .longPressDraggableHandle(onDragStarted = { Fx.tick() }).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Glyph("capas", Faint, 18.dp, "Arrastrar")
                            Spacer(Modifier.width(10.dp))
                            if (item.group != null) {
                                Text("${item.group}${rows.take(i + 1).count { it.second.group == item.group }}", color = AccentText, fontFamily = Display, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(AccentSoft).padding(horizontal = 7.dp, vertical = 2.dp))
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(ex?.name ?: "Ejercicio", color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { Fx.reject(); rows.removeAt(i); regroup() }) { Glyph("cerrar", Faint, 18.dp, "Quitar") }
                        }
                        if ((ex?.kind ?: 0) == 0) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MiniStepper("Series", item.sets, 1..10, Modifier.weight(1f)) { v -> upd { it.copy(sets = v) } }
                            MiniStepper("Reps mín", item.repsMin, 1..50, Modifier.weight(1f)) { v -> upd { it.copy(repsMin = v, repsMax = maxOf(it.repsMax, v)) } }
                            MiniStepper("Reps máx", item.repsMax, 1..50, Modifier.weight(1f)) { v -> upd { it.copy(repsMax = v, repsMin = minOf(it.repsMin, v)) } }
                        } else MiniStepper("Series", item.sets, 1..10, Modifier.fillMaxWidth()) { v -> upd { it.copy(sets = v) } }
                        if (i < rows.lastIndex) {
                            val linked = item.group != null && item.group == rows[i + 1].second.group
                            Row(Modifier.clip(RoundedCornerShape(50)).clickable {
                                Fx.tick()
                                if (linked) rows[i + 1] = rows[i + 1].first to rows[i + 1].second.copy(group = null)
                                else { val g = item.group ?: "N${System.nanoTime()}"; rows[i] = key to item.copy(group = g); rows[i + 1] = rows[i + 1].first to rows[i + 1].second.copy(group = g) }
                                regroup()
                            }.padding(vertical = 4.dp, horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(20.dp).clip(CircleShape).background(if (linked) Accent else CardHigh), contentAlignment = Alignment.Center) {
                                    if (linked) Glyph("hecho", Bg, 12.dp)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text("Superserie con el siguiente", color = if (linked) AccentText else Muted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            item(key = "add") { SecondaryButton("+ Agregar ejercicio") { picking = true } }
            if (!isNew) item(key = "del") { TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("Eliminar rutina", color = Bad) } }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            PrimaryButton("Guardar rutina", enabled = name.isNotBlank() && rows.isNotEmpty()) { regroup(); save() }
        }
    }
    if (picking) ExercisePicker(data, onDismiss = { picking = false }) { id -> rows.add(newId() to RoutineItem(id)) }
    if (confirmDelete) ConfirmDialog("Eliminar rutina", "¿Eliminar \"${initial.name}\"? Tu historial no se borra.", onDismiss = { confirmDelete = false }) {
        Repo.update { d -> d.copy(routines = d.routines.filterNot { it.id == initial.id }) }
        onClose()
    }
}

@Composable
private fun MiniStepper(label: String, value: Int, range: IntRange, modifier: Modifier, onChange: (Int) -> Unit) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(CardHigh).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("−", color = Ink, fontSize = 20.sp, modifier = Modifier.clip(CircleShape).clickable { if (value > range.first) { Fx.tick(); onChange(value - 1) } }.padding(horizontal = 12.dp, vertical = 2.dp))
            Text("$value", fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
            Text("+", color = Ink, fontSize = 20.sp, modifier = Modifier.clip(CircleShape).clickable { if (value < range.last) { Fx.tick(); onChange(value + 1) } }.padding(horizontal = 12.dp, vertical = 2.dp))
        }
    }
}
