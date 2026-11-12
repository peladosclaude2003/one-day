package com.oneday.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oneday.app.data.*
import java.time.LocalDate

private fun Modifier.dashedPill(color: Color) = drawBehind {
    val s = 1.5.dp.toPx()
    drawRoundRect(color, topLeft = Offset(s / 2, s / 2), size = size.copy(size.width - s, size.height - s),
        cornerRadius = CornerRadius(size.height / 2),
        style = Stroke(s, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))))
}

/**
 * Cuadrícula de los últimos 30 días (3 filas × 10), de la más vieja a hoy.
 * Lleno = entrenaste · punteado = tocaba gym y no fuiste · borde fino = descanso · borde naranja = hoy.
 */
@Composable
fun Last30Grid(trained: Set<Long>, plannedDays: Set<Int>) {
    val t = today()
    val days = (29 downTo 0).map { t - it }
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        days.chunked(10).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                row.forEach { d ->
                    val on = d in trained
                    val isToday = d == t
                    val planned = LocalDate.ofEpochDay(d).dayOfWeek.value in plannedDays
                    val shape = RoundedCornerShape(50)
                    Box(
                        Modifier.weight(1f).height(19.dp).clip(shape)
                            .then(
                                when {
                                    on -> Modifier.background(Ink2)
                                    isToday -> Modifier
                                    planned -> Modifier.dashedPill(Faint)
                                    else -> Modifier.background(CardBg).border(1.dp, Line, shape)
                                }
                            )
                            .then(if (isToday) Modifier.border(2.5.dp, Accent, shape) else Modifier)
                            .semantics { contentDescription = fmtDay(d) + if (on) ": entrenaste" else "" }
                    )
                }
            }
        }
    }
}

@Composable
fun Last30Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem("Entrenaste") { Modifier.background(Ink2) }
        LegendItem("Tocaba") { Modifier.dashedPill(Faint) }
        LegendItem("Descanso") { Modifier.border(1.dp, Line, RoundedCornerShape(50)) }
        LegendItem("Hoy") { Modifier.border(2.dp, Accent, RoundedCornerShape(50)) }
    }
}

@Composable
private fun LegendItem(text: String, style: () -> Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(18.dp, 10.dp).clip(RoundedCornerShape(50)).then(style()))
        Spacer(Modifier.width(5.dp))
        Text(text, color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}

/** Área de tendencia suavizada, pensada para ir pegada al borde inferior de una tarjeta. */
@Composable
fun AreaTrend(values: List<Double>, modifier: Modifier = Modifier, height: Dp = 96.dp) {
    Canvas(modifier.fillMaxWidth().height(height)) {
        if (values.isEmpty()) return@Canvas
        val maxV = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
        val n = values.size
        val stepX = size.width / (n - 1).coerceAtLeast(1)
        val top = 10.dp.toPx()
        val h = size.height - top
        fun pt(i: Int) = Offset(i * stepX, top + h - (values[i] / maxV * h * 0.75f).toFloat() - h * 0.12f)
        val line = Path().apply {
            moveTo(pt(0).x, pt(0).y)
            for (i in 1 until n) {
                val a = pt(i - 1); val b = pt(i); val mx = (a.x + b.x) / 2
                cubicTo(mx, a.y, mx, b.y, b.x, b.y)
            }
        }
        val area = Path().apply { addPath(line); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
        drawPath(area, Brush.verticalGradient(listOf(Ink2.copy(alpha = 0.38f), Ink2.copy(alpha = 0.06f))))
        drawPath(line, Ink2.copy(alpha = 0.9f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** Barras de lunes a domingo con el valor encima. La barra de hoy va en naranja. */
@Composable
fun WeekBars(values: List<Int>, modifier: Modifier = Modifier, barArea: Dp = 56.dp, compact: Boolean = true) {
    val maxV = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val todayIdx = LocalDate.now().dayOfWeek.value - 1
    val letters = listOf("L", "M", "X", "J", "V", "S", "D")
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 10.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, v ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                if (v > 0) Text("$v", color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Box(Modifier.height(barArea).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    if (v > 0) Box(
                        Modifier.fillMaxWidth().fillMaxHeight(v / maxV.toFloat())
                            .clip(RoundedCornerShape(if (compact) 6.dp else 10.dp))
                            .background(if (i == todayIdx) Accent else Color(0xFF3A3A3F))
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(letters[i], color = if (i == todayIdx) Accent else Faint, style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (i == todayIdx) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Línea pequeña de peso corporal con la meta punteada. */
@Composable
fun WeightSpark(values: List<Double>, goal: Double?, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(44.dp)) {
        val all = values + listOfNotNull(goal)
        if (all.isEmpty()) return@Canvas
        val lo = all.min() - 1; val hi = all.max() + 1
        fun y(v: Double) = (size.height - ((v - lo) / (hi - lo) * size.height)).toFloat()
        if (goal != null) drawLine(Faint, Offset(0f, y(goal)), Offset(size.width, y(goal)), strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())))
        if (values.size >= 2) {
            val step = size.width / (values.size - 1)
            val p = Path()
            values.forEachIndexed { i, v -> if (i == 0) p.moveTo(0f, y(v)) else p.lineTo(i * step, y(v)) }
            drawPath(p, Ink2, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(Accent, 4.dp.toPx(), Offset(size.width, y(values.last())))
        }
    }
}

/** Línea de progreso de un ejercicio. Toca un punto para ver su valor. */
@Composable
fun LineChart(points: List<Pair<Long, Double>>) {
    var sel by remember(points) { mutableIntStateOf(points.lastIndex) }
    val minV = points.minOf { it.second }
    val maxV = points.maxOf { it.second }
    val pad = ((maxV - minV) * 0.15).coerceAtLeast(2.5)
    val lo = (minV - pad).coerceAtLeast(0.0)
    val hi = maxV + pad
    val p = points[sel]
    Text("${fmtWeight(p.second)} kg · ${fmtDay(p.first)}", fontWeight = FontWeight.SemiBold, color = Ink)
    Spacer(Modifier.height(10.dp))
    Row {
        Column(Modifier.height(170.dp).padding(end = 6.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(fmtWeight(round1(hi)), color = Faint, style = MaterialTheme.typography.labelSmall)
            Text(fmtWeight(round1((hi + lo) / 2)), color = Faint, style = MaterialTheme.typography.labelSmall)
            Text(fmtWeight(round1(lo)), color = Faint, style = MaterialTheme.typography.labelSmall)
        }
        Canvas(
            Modifier.weight(1f).height(170.dp).pointerInput(points) {
                detectTapGestures { o ->
                    val step = size.width / (points.size - 1f)
                    sel = kotlin.math.round(o.x / step).toInt().coerceIn(0, points.lastIndex)
                }
            }
        ) {
            val inset = 6.dp.toPx()
            val h = size.height - inset * 2
            val stepX = (size.width - inset * 2) / (points.size - 1).coerceAtLeast(1)
            fun pos(i: Int) = Offset(inset + i * stepX, inset + h - ((points[i].second - lo) / (hi - lo) * h).toFloat())
            for (g in 0..2) {
                val y = inset + h * g / 2f
                drawLine(Line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            val line = Path()
            points.indices.forEach { i -> val o = pos(i); if (i == 0) line.moveTo(o.x, o.y) else line.lineTo(o.x, o.y) }
            val area = Path().apply { addPath(line); lineTo(pos(points.lastIndex).x, size.height); lineTo(pos(0).x, size.height); close() }
            drawPath(area, Brush.verticalGradient(listOf(Ink2.copy(alpha = 0.18f), Color.Transparent)))
            drawPath(line, Ink2, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            val sp = pos(sel)
            drawLine(Line, Offset(sp.x, 0f), Offset(sp.x, size.height), strokeWidth = 1.dp.toPx())
            points.indices.forEach { i ->
                val o = pos(i)
                val r = if (i == sel) 6.dp.toPx() else 4.dp.toPx()
                drawCircle(CardBg, r + 2.dp.toPx(), o)
                drawCircle(if (i == sel) Accent else Ink2, r, o)
            }
        }
    }
    Row(Modifier.padding(start = 30.dp, top = 4.dp)) {
        Text(fmtDay(points.first().first), color = Faint, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Text(fmtDay(points.last().first), color = Faint, style = MaterialTheme.typography.labelSmall)
    }
}

/** Barras simples con etiqueta abajo; la barra `highlight` va en naranja. */
@Composable
fun Bars(values: List<Int>, labels: List<String>, highlight: Int? = null, barArea: Dp = 90.dp) {
    val maxV = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, v ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                if (v > 0) Text("$v", color = Muted, style = MaterialTheme.typography.labelSmall)
                Box(Modifier.height(barArea).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    if (v > 0) Box(Modifier.fillMaxWidth().fillMaxHeight(v / maxV.toFloat()).clip(RoundedCornerShape(8.dp))
                        .background(if (i == highlight) Accent else Color(0xFF3A3A3F)))
                }
                Spacer(Modifier.height(6.dp))
                Text(labels[i], color = if (i == highlight) Accent else Faint, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}
