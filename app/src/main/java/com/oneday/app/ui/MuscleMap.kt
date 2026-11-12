package com.oneday.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oneday.app.data.*

/** Grupos que aparecen en el mapa (Cardio y Otro no se dibujan). */
val MAP_MUSCLES = listOf("Pecho", "Espalda", "Hombro", "Bíceps", "Tríceps", "Abdomen", "Pierna")

/** Series por grupo muscular desde el lunes. */
fun AppData.weeklySetsByMuscle(): Map<String, Int> {
    val monday = mondayOf(today())
    val muscleOf = exercises.associate { it.id to it.muscle }
    val out = MAP_MUSCLES.associateWith { 0 }.toMutableMap()
    workouts.filter { it.date >= monday }.forEach { w ->
        w.entries.forEach { e -> muscleOf[e.exerciseId]?.let { m -> if (m in out) out[m] = out[m]!! + e.sets.size } }
    }
    return out
}

private val BodyBase = Color(0xFF1E1E22)
private val MuscleZero = Color(0xFF34343A)
private const val FULL_SETS = 12f

fun muscleColor(sets: Int) = lerp(MuscleZero, Ink2, (sets / FULL_SETS).coerceIn(0f, 1f))

/** Silueta de frente y espalda; cada grupo se aclara con más series y el más rezagado lleva borde naranja. */
@Composable
fun MuscleMap(sets: Map<String, Int>, modifier: Modifier = Modifier) {
    val total = sets.values.sum()
    val weakest = if (total == 0) null else sets.minBy { it.value }.key
    Row(modifier.fillMaxWidth().semantics { contentDescription = "Mapa muscular de la semana" }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(true, false).forEach { front ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.fillMaxWidth().height(220.dp)) { drawBody(front, sets, weakest) }
                Text(if (front) "Frente" else "Espalda", color = Faint, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun DrawScope.drawBody(front: Boolean, sets: Map<String, Int>, weakest: String?) {
    val s = minOf(size.width / 100f, size.height / 210f)
    val ox = (size.width - 100f * s) / 2f
    val oy = (size.height - 210f * s) / 2f
    fun oval(cx: Float, cy: Float, w: Float, h: Float, c: Color, outline: Boolean = false) {
        val tl = Offset(ox + (cx - w / 2) * s, oy + (cy - h / 2) * s); val sz = Size(w * s, h * s)
        drawOval(c, tl, sz)
        if (outline) drawOval(Accent, tl, sz, style = Stroke(2.dp.toPx()))
    }
    fun rr(x0: Float, y0: Float, x1: Float, y1: Float, r: Float, c: Color, outline: Boolean = false) {
        val tl = Offset(ox + x0 * s, oy + y0 * s); val sz = Size((x1 - x0) * s, (y1 - y0) * s)
        drawRoundRect(c, tl, sz, CornerRadius(r * s))
        if (outline) drawRoundRect(Accent, tl, sz, CornerRadius(r * s), style = Stroke(2.dp.toPx()))
    }
    fun m(name: String) = muscleColor(sets[name] ?: 0)
    fun w(name: String) = name == weakest

    // Silueta base
    oval(50f, 14f, 20f, 22f, BodyBase)
    rr(45f, 23f, 55f, 31f, 3f, BodyBase)
    rr(31f, 30f, 69f, 98f, 12f, BodyBase)
    for (x in listOf(22f, 78f)) { oval(x, 82f, 9f, 26f, BodyBase); oval(x - if (x < 50) 1f else -1f, 98f, 8f, 8f, BodyBase) }
    rr(35f, 92f, 65f, 110f, 9f, BodyBase)
    for (x in listOf(42f, 58f)) { oval(x, 128f, 16f, 44f, BodyBase); oval(x, 170f, 11f, 36f, BodyBase); oval(x, 192f, 13f, 7f, BodyBase) }

    // Hombros (frente y espalda)
    for (x in listOf(30f, 70f)) oval(x, 38f, 18f, 16f, m("Hombro"), w("Hombro"))
    if (front) {
        for (x in listOf(41f, 59f)) oval(x, 47f, 19f, 15f, m("Pecho"), w("Pecho"))
        rr(43f, 57f, 57f, 92f, 6f, m("Abdomen"), w("Abdomen"))
        for (x in listOf(25f, 75f)) oval(x, 58f, 10f, 22f, m("Bíceps"), w("Bíceps"))
        for (x in listOf(42f, 58f)) oval(x, 128f, 14f, 40f, m("Pierna"), w("Pierna"))
    } else {
        oval(50f, 35f, 28f, 13f, m("Espalda"), w("Espalda"))
        for (x in listOf(39f, 61f)) oval(x, 57f, 15f, 30f, m("Espalda"), w("Espalda"))
        rr(44f, 72f, 56f, 92f, 5f, m("Espalda"), w("Espalda"))
        for (x in listOf(25f, 75f)) oval(x, 58f, 10f, 22f, m("Tríceps"), w("Tríceps"))
        for (x in listOf(43f, 57f)) oval(x, 104f, 15f, 13f, m("Pierna"), w("Pierna"))
        for (x in listOf(42f, 58f)) { oval(x, 133f, 13f, 34f, m("Pierna"), w("Pierna")); oval(x, 167f, 11f, 24f, m("Pierna"), w("Pierna")) }
    }
}

/** Tarjeta completa: silueta + lista de series por grupo. */
@Composable
fun MuscleCard(data: AppData) {
    val sets = data.weeklySetsByMuscle()
    val total = sets.values.sum()
    val weakest = if (total == 0) null else sets.minBy { it.value }.key
    OdCard {
        CardLabel("Músculos esta semana", com.oneday.app.R.drawable.ic_nav_gym, if (total == 0) "Aún sin series esta semana" else "$total series desde el lunes")
        Spacer(Modifier.height(10.dp))
        MuscleMap(sets)
        Spacer(Modifier.height(12.dp))
        sets.entries.sortedByDescending { it.value }.forEach { (name, n) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (name == weakest) Accent else muscleColor(n)))
                Spacer(Modifier.width(10.dp))
                Text(name, color = Ink, modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodyMedium)
                Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50)).background(CardHigh)) {
                    Box(Modifier.fillMaxWidth((n / FULL_SETS).coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(50)).background(muscleColor(n)))
                }
                Spacer(Modifier.width(10.dp))
                Text("$n", color = if (name == weakest) Accent else Muted, fontWeight = if (name == weakest) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(24.dp))
            }
        }
        if (weakest != null) {
            Spacer(Modifier.height(6.dp))
            Text("Menos trabajado: $weakest. La referencia es de 10 a 20 series por grupo a la semana.", color = Faint, style = MaterialTheme.typography.bodySmall)
        }
    }
}
