package com.oneday.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneday.app.data.*
import java.time.LocalDate

private fun pct(v: Int?) = when { v == null -> "—"; v > 300 -> "×${v / 100 + 1}"; v > 0 -> "+$v %"; v < 0 -> "−${-v} %"; else -> "0 %" }
private fun pctColor(v: Int?) = when { v == null || v == 0 -> Muted; v > 0 -> Good; else -> Bad }

/** Progreso por grupo muscular: cómo mejora cada zona del cuerpo y cómo se comparan entre sí. */
@Composable
fun GroupProgress(data: AppData) {
    val groups = PROGRESS_GROUPS
    val trained = groups.filter { g -> data.groupWeeks(g, 52).any { it.sets > 0 } }
    if (trained.isEmpty()) {
        EmptyState("grafica", "Aún no hay progreso que mostrar", "Termina tus primeros entrenamientos y aquí verás cómo mejora cada grupo muscular.")
        return
    }
    var sel by rememberSaveable { mutableStateOf(trained.first()) }
    if (sel !in groups) sel = trained.first()

    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Comparativa entre grupos
        Text("Fuerza ganada desde que empezaste", color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp))
        groups.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { g ->
                    val idx = data.strengthIndex(g)
                    val gain = idx?.let { (it - 100).toInt() }
                    val trend = data.groupTrend(g)
                    val isSel = g == sel
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(if (isSel) CardHigh else CardBg)
                            .border(if (isSel) 1.5.dp else 1.dp, if (isSel) Accent else Hairline, RoundedCornerShape(22.dp))
                            .clickable(role = Role.Tab, onClickLabel = g) { Fx.tick(); sel = g }.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(g, color = if (isSel) Ink else Ink2, fontWeight = FontWeight.SemiBold)
                        Text(if (gain == null) "Sin datos" else pct(gain), fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 24.sp,
                            color = if (gain == null) Faint else if (isSel) Accent else Ink)
                        Text("Volumen 4 sem: ${pct(trend)}", color = pctColor(trend), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Detalle del grupo elegido
        val weeks = data.groupWeeks(sel, 12)
        val last4 = weeks.takeLast(4); val prev4 = weeks.dropLast(4).takeLast(4)
        val now = data.strengthIndex(sel)
        val before = data.strengthIndex(sel, today() - 28)
        OdCard {
            CardLabel("Volumen semanal · $sel", sub = "kg levantados por semana, últimas 12")
            Spacer(Modifier.height(14.dp))
            val maxV = weeks.maxOf { it.volume }.coerceAtLeast(1.0)
            Bars(
                weeks.map { (it.volume / maxV * 100).toInt().coerceAtLeast(if (it.volume > 0) 1 else 0) },
                weeks.map { "${LocalDate.ofEpochDay(it.monday).dayOfMonth}" },
                highlight = weeks.lastIndex, barArea = 110.dp, showValues = false,
            )
            Spacer(Modifier.height(6.dp))
            Text("Semana actual: ${fmtCompact(weeks.last().volume)} kg · mejor semana: ${fmtCompact(maxV)} kg", color = Muted, style = MaterialTheme.typography.bodySmall)
        }

        OdCard {
            CardLabel("Comparativa · $sel", sub = "últimas 4 semanas contra las 4 anteriores")
            Spacer(Modifier.height(10.dp))
            CompareRow("Volumen", "${fmtCompact(prev4.sumOf { it.volume })} kg", "${fmtCompact(last4.sumOf { it.volume })} kg",
                prev4.sumOf { it.volume }.let { p -> if (p > 0) ((last4.sumOf { it.volume } - p) / p * 100).toInt() else null })
            CompareRow("Series", "${prev4.sumOf { it.sets }}", "${last4.sumOf { it.sets }}",
                prev4.sumOf { it.sets }.let { p -> if (p > 0) ((last4.sumOf { it.sets } - p) * 100 / p) else null })
            CompareRow("Fuerza", before?.let { "${it.toInt()}" } ?: "—", now?.let { "${it.toInt()}" } ?: "—",
                if (before != null && now != null && before > 0) ((now - before) / before * 100).toInt() else null)
            Text("Fuerza = tu 1RM estimado en los ejercicios de este grupo; 100 es como empezaste.", color = Faint, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
        }

        // Evolución de la fuerza
        val points = (11 downTo 0).mapNotNull { w ->
            val end = mondayOf(today()) - w * 7 + 6
            data.strengthIndex(sel, end)?.let { (end.coerceAtMost(today())) to it }
        }.distinctBy { it.second }
        if (points.size >= 2) OdCard {
            CardLabel("Evolución de tu fuerza · $sel", sub = "índice · toca un punto")
            Spacer(Modifier.height(12.dp))
            LineChart(points, "", { "${it.toInt()}" })
        }

        // Ejercicios del grupo
        val exs = data.exercises.filter { groupOf(it.muscle) == sel && it.kind == 0 }.mapNotNull { ex ->
            val tops = data.workouts.sortedBy { it.startedAt }.mapNotNull { w -> w.entries.filter { it.exerciseId == ex.id }.flatMap { it.sets }.filter { it.counts && it.weight > 0 }.maxOfOrNull { oneRm(it) } }
            if (tops.isEmpty()) null else Triple(ex, tops.first(), tops.max())
        }.sortedByDescending { (_, a, b) -> b / a }
        if (exs.isNotEmpty()) OdCard {
            CardLabel("Ejercicios de $sel", sub = "1RM estimado: primera sesión → mejor")
            Spacer(Modifier.height(6.dp))
            exs.forEach { (ex, first, best) ->
                val g = ((best - first) / first * 100).toInt()
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(ex.name, color = Ink, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text("${fmtWeight(round1(first))} → ${fmtWeight(round1(best))} kg", color = Ink2, style = MaterialTheme.typography.bodyMedium)
                    Text(pct(g), color = pctColor(g), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun CompareRow(label: String, before: String, now: String, change: Int?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodyMedium)
        Text(before, color = Ink2, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium)
        Glyph("flecha", Faint, 14.dp, modifier = Modifier.padding(horizontal = 8.dp))
        Text(now, color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(pct(change), color = pctColor(change), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
    }
}
