package com.oneday.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Ilustraciones de línea para los estados vacíos: mismo trazo que los íconos,
 * un solo detalle en naranja. Lienzo de 92 × 78.
 */
object Illus {
    // (trazo gris, detalle naranja)
    val ALL = mapOf(
        "pendientes" to ("M20 14h44v52H20z M28 30h18 M28 42h18 M28 54h10" to "M50 28l4 4 8-8 M50 40l4 4 8-8"),
        "agenda" to ("M8 18c10-4 20-4 30 2 10-6 20-6 30-2v44c-10-4-20-4-30 2-10-6-20-6-30-2z M38 20v44 M16 30h14 M16 38h10 M46 30h14 M46 38h12" to "M74 12l6-6 M80 20h7 M74 28l6 6"),
        "sobre" to ("M12 20h60v42H12z M12 20l30 22 30-22" to "M62 56l6 6 14-14"),
        "gym" to ("M8 62h76 M22 44v22 M30 38v28 M62 38v28 M70 44v22 M30 54h32" to "M68 10h8l-8 9h8 M78 22h6l-6 7h6"),
        "brote" to ("M30 50h32l-4 20H34z M46 50V34 M46 38c0-8 6-14 16-14 0 10-6 14-16 14z M46 43c0-6-5-11-13-11 0 8 5 11 13 11z" to "M76 8v7 M86 18h-7 M83 11l-5 5"),
        "racha" to ("M46 12c2 10 16 18 16 34a16 16 0 0 1-32 0c0-8 4-13 8-17 1 6 4 9 8 9-3-11 0-20 0-26z" to "M70 20l5-5 M74 32h7 M20 22l-5-5"),
        "grafica" to ("M12 68h70 M12 68V12 M24 68V54 M38 68V46 M52 68V40 M66 68V30" to "M24 40l14-10 14 6 20-18 M66 18h6v6"),
    )
    private val cache = HashMap<String, Pair<androidx.compose.ui.graphics.Path, androidx.compose.ui.graphics.Path>>()
    fun paths(key: String) = ALL[key]?.let { (a, b) -> cache.getOrPut(key) { PathParser().parsePathString(a).toPath() to PathParser().parsePathString(b).toPath() } }
}

@Composable
fun Illustration(key: String, modifier: Modifier = Modifier) {
    val p = Illus.paths(key) ?: return
    Canvas(modifier.size(120.dp, 102.dp)) {
        val s = size.width / 92f
        scale(s, s, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            drawPath(p.first, Faint, style = Stroke(1.75f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(p.second, Accent, style = Stroke(1.75f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
