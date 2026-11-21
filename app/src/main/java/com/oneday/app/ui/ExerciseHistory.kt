package com.oneday.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneday.app.data.*

/** Panel con el historial de un ejercicio, sin salir del entrenamiento en curso. */
@Composable
fun ExerciseHistorySheet(data: AppData, exerciseId: String, onDismiss: () -> Unit) {
    val ex = data.exercises.firstOrNull { it.id == exerciseId }
    val kind = ex?.kind ?: 0
    val sessions = data.workouts.sortedBy { it.startedAt }.mapNotNull { w ->
        val sets = w.entries.filter { it.exerciseId == exerciseId }.flatMap { it.sets }.filter { it.counts }
        if (sets.isEmpty()) null else w.date to sets
    }
    fun metric(st: WorkSet) = when (kind) { 1 -> (st.seconds ?: 0).toDouble(); 2 -> st.km ?: 0.0; else -> oneRm(st) }
    GlassSheet(onDismiss) {
        Text((ex?.name ?: "Ejercicio").uppercase(), color = Muted, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp)
        if (sessions.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            EmptyState("grafica", "Primera vez con este ejercicio", "Cuando termines este entrenamiento aquí verás tu progreso.")
            return@GlassSheet
        }
        val points = sessions.map { (d, sets) -> d to sets.maxOf { metric(it) } }
        val now = points.last().second
        val eightAgo = points.lastOrNull { it.first <= today() - 56 }?.second ?: points.first().second
        val change = if (eightAgo > 0) ((now - eightAgo) / eightAgo * 100).toInt() else 0
        val unit = when (kind) { 1 -> "s · mejor tiempo"; 2 -> "km · mayor distancia"; else -> "kg · 1RM estimado" }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(if (kind == 1) fmtSecs(now.toInt()) else fmtWeight(round1(now)), fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 52.sp, color = Ink, letterSpacing = (-2).sp, lineHeight = 52.sp)
            Spacer(Modifier.width(8.dp))
            Text(unit, color = Ink2, modifier = Modifier.padding(bottom = 8.dp).weight(1f))
            if (points.size > 1) Text((if (change >= 0) "+" else "−") + "${kotlin.math.abs(change)} %", color = if (change >= 0) Good else Bad,
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 10.dp).clip(RoundedCornerShape(50)).background((if (change >= 0) Good else Bad).copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 5.dp))
        }
        Spacer(Modifier.height(14.dp))
        if (points.size >= 2) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.04f)).padding(14.dp)) {
            LineChart(points, if (kind == 2) "km" else if (kind == 1) "s" else "kg", { if (kind == 1) fmtSecs(it.toInt()) else fmtWeight(round1(it)) })
        }
        Spacer(Modifier.height(12.dp))
        val all = sessions.flatMap { it.second }
        val best = all.maxBy { if (kind == 0) it.weight else metric(it) }
        val target = if (kind == 0) suggestTarget(lastSetsFor(data, exerciseId)) else null
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat("Récord", fmtSet(best).replace("×", " × "), Modifier.weight(1f))
            MiniStat("Sesiones", "${sessions.size}", Modifier.weight(1f))
            MiniStat("Hoy toca", target?.let { "${fmtWeight(it.weight)} × ${it.reps}" } ?: "—", Modifier.weight(1f), accent = true)
        }
        Spacer(Modifier.height(16.dp))
        Text("ÚLTIMAS 5 SESIONES", color = Muted, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(4.dp))
        var bestSoFar = 0.0
        val tagged = sessions.map { (d, sets) -> val m = sets.maxOf { metric(it) }; val pr = m > bestSoFar + 0.01 && bestSoFar > 0; bestSoFar = maxOf(bestSoFar, m); Triple(d, sets, pr) }
        tagged.takeLast(5).reversed().forEach { (d, sets, pr) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(fmtDay(d), color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(78.dp))
                Text(sets.joinToString("  ") { fmtSet(it) }, color = Ink, fontFamily = Display, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1)
                if (pr) { Glyph("record", Accent, 14.dp); Spacer(Modifier.width(4.dp)); Text("Récord", color = Accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier, accent: Boolean = false) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.05f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
        Text(value, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = if (accent) Accent else Ink, maxLines = 1)
    }
}
