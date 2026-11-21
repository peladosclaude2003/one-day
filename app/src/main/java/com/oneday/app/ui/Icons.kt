package com.oneday.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Iconografía propia de One Day: rejilla 24, trazo 1.75, puntas redondas.
 * Reemplaza a todos los emojis. Las rachas y hábitos guardan la CLAVE del ícono.
 */
object Glyphs {
    data class G(val key: String, val name: String, val d: String)

    val QUIT = listOf(
        G("fumar", "Fumar", "M2.5 14h13v3.5h-13z M18.5 14v3.5 M21 14v3.5 M18.5 11c0-2 2.5-2 2.5-4.5 M3 3l18 18"),
        G("alcohol", "Alcohol", "M8 3h8l-.5 6a3.5 3.5 0 0 1-7 0z M12 12.5V20 M8.5 21h7"),
        G("refresco", "Refresco", "M6 7h12l-1.5 13.2a1.5 1.5 0 0 1-1.5 1.3H9a1.5 1.5 0 0 1-1.5-1.3z M13 7l2-4h3 M6.7 12h10.6"),
        G("redes", "Redes", "M9 2.5h6a2.5 2.5 0 0 1 2.5 2.5v14a2.5 2.5 0 0 1-2.5 2.5H9A2.5 2.5 0 0 1 6.5 19V5A2.5 2.5 0 0 1 9 2.5z M11 18.5h2"),
        G("azucar", "Azúcar", "M10.5 8.5h3a3.5 3.5 0 0 1 0 7h-3a3.5 3.5 0 0 1 0-7z M7 12L3.5 9v6z M17 12l3.5-3v6z"),
        G("cafe", "Café", "M5 9h11v5a5 5 0 0 1-5 5h-1a5 5 0 0 1-5-5z M16 11h1.5a2.5 2.5 0 0 1 0 5H16 M8.5 3.5c0 1.2 1 1.3 1 2.5 M12 3.5c0 1.2 1 1.3 1 2.5"),
        G("videojuegos", "Videojuegos", "M7 8h10a4 4 0 0 1 4 4v2.5a2.5 2.5 0 0 1-4.6 1.4L15 14H9l-1.4 1.9A2.5 2.5 0 0 1 3 14.5V12a4 4 0 0 1 4-4z M7.5 10.5v3 M6 12h3 M15.5 11.5h.01 M17.5 13h.01"),
        G("comida", "Comida rápida", "M4 11a8 8 0 0 1 16 0z M3.5 14.5h17 M5 18h14v.5a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2z"),
        G("apuestas", "Apuestas", "M7.5 3.5h9a4 4 0 0 1 4 4v9a4 4 0 0 1-4 4h-9a4 4 0 0 1-4-4v-9a4 4 0 0 1 4-4z M8.5 8.5h.01 M15.5 8.5h.01 M12 12h.01 M8.5 15.5h.01 M15.5 15.5h.01"),
        G("compras", "Compras", "M5 8h14l-1 12.5H6z M9 10V7a3 3 0 0 1 6 0v3"),
    )
    val BUILD = listOf(
        G("agua", "Agua", "M12 3s6 6.5 6 11a6 6 0 0 1-12 0c0-4.5 6-11 6-11z M9 14.5a3 3 0 0 0 3 3"),
        G("leer", "Leer", "M3 5.5c3-1.3 6-1.3 9 .8 3-2.1 6-2.1 9-.8v13c-3-1.3-6-1.3-9 .8-3-2.1-6-2.1-9-.8z M12 6.3v13"),
        G("meditar", "Meditar", "M10 5a2 2 0 1 0 4 0a2 2 0 1 0-4 0 M12 8v4.5 M6 10.5l6 2.5 6-2.5 M5 19c2.5-2 4.5-3 7-3s4.5 1 7 3"),
        G("caminar", "Caminar", "M11.2 4.5a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0-3.6 0 M11 21l2-6-2.5-2.5 1-4.5 4 3h3 M10.5 8L7 10v4 M13 15l3 6"),
        G("dormir", "Dormir", "M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z"),
        G("sano", "Comer sano", "M12 7c-4-2-8 0-8 5s3 9 6 9c1 0 1.5-.5 2-.5s1 .5 2 .5c3 0 6-4 6-9s-4-7-8-5z M12 7c0-2 1-3.5 3-4"),
        G("escribir", "Escribir", "M4 20l1-4L16 5a2 2 0 0 1 3 3L8 19z M14 7l3 3"),
        G("dientes", "Dientes", "M7 3.5c2 0 3 1 5 1s3-1 5-1c2.5 0 3.5 2 3.5 4.5 0 3-1.5 4.5-2 7.5-.5 3-1 6-2.5 6s-1.5-4-4-4-2.5 4-4 4-2-3-2.5-6c-.5-3-2-4.5-2-7.5C3.5 5.5 4.5 3.5 7 3.5z"),
        G("madrugar", "Madrugar", "M8 12a4 4 0 1 0 8 0a4 4 0 1 0-8 0 M12 2.5v2 M12 19.5v2 M2.5 12h2 M19.5 12h2 M5.3 5.3l1.4 1.4 M17.3 17.3l1.4 1.4 M5.3 18.7l1.4-1.4 M17.3 6.7l1.4-1.4"),
        G("musica", "Música", "M9 18V5l11-2v13 M4 18a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0 M15 16a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0"),
        G("estudiar", "Estudiar", "M9 18h6 M10 21h4 M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0 0 12 3z"),
        G("cardio", "Cardio", "M12 20s-7.5-4.5-7.5-10A4.5 4.5 0 0 1 12 7a4.5 4.5 0 0 1 7.5 3c0 5.5-7.5 10-7.5 10z M5 12h3l1.5-2 2 4 1.5-2h6"),
    )
    val SYSTEM = listOf(
        G("racha", "Racha", "M12 3c.5 3 4.5 5 4.5 10a4.5 4.5 0 0 1-9 0c0-2 1-3.5 2-4.5.3 1.5 1 2.5 2 2.5-.7-3 .5-6 .5-8z"),
        G("record", "Récord", "M7 4h10v5a5 5 0 0 1-10 0z M7 6H4v1a3 3 0 0 0 3 3 M17 6h3v1a3 3 0 0 1-3 3 M12 14v4 M8 20h8"),
        G("hecho", "Hecho", "M5 12.5l4.5 4.5L19 7.5"),
        G("gym", "Gimnasio", "M2 10v4 M5 7v10 M19 7v10 M22 10v4 M5 12h14"),
        G("agenda", "Agenda", "M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z M3 10h18 M8 3v4 M16 3v4"),
        G("respirar", "Respirar", "M3 8h11a3 3 0 1 0-3-3 M3 12h16a3 3 0 1 1-3 3 M3 16h7"),
        G("peso", "Peso", "M6 3h12a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3z M8.5 10a3.5 3.5 0 0 1 7 0 M12 10l1.6-2"),
        G("campana", "Recordatorio", "M6 16v-5a6 6 0 0 1 12 0v5l1.5 2h-15z M10 20.5a2 2 0 0 0 4 0"),
        G("pendientes", "Pendientes", "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M8 12.5l2.7 2.7 5.3-5.7"),
        G("reloj", "Reloj", "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M12 7v5l3 2"),
        G("capas", "Series", "M12 3l9 5-9 5-9-5z M3 12.5l9 5 9-5 M3 16.5l9 5 9-5"),
        G("rayo", "Volumen", "M13.5 2l-8.5 12h6l-1.5 8 9-12.5h-6z"),
        G("flecha", "Siguiente", "M5 12h12 M13 7l5 5-5 5"),
        G("arriba", "Subir", "M12 19V5 M6 11l6-6 6 6"),
        G("igual", "Igual", "M6 10h12 M6 14h12"),
        G("pin", "Nota", "M12 17v5 M9 3h6l-1 6 4 3v2H6v-2l4-3z"),
        G("brazo", "Fuerza", "M7 20c-2-3-2-7 1-10l3-4c1-1.5 3-1 3 .5L13 10h3a4 4 0 0 1 4 4v1a5 5 0 0 1-5 5z"),
        G("sobre", "Vacío", "M3 8l9 6 9-6 M5 5h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z"),
        G("brote", "Brote", "M12 21v-9 M12 12c0-4 3-7 8-7 0 5-3 7-8 7z M12 14c0-3-2.5-5.5-7-5.5 0 4 2.5 5.5 7 5.5z"),
        G("grafica", "Progreso", "M4 20V10 M10 20V4 M16 20v-7 M3 20h18"),
        G("ajustes", "Ajustes", "M4 7h10 M18 7h2 M4 17h2 M10 17h10 M14 5v4 M6 15v4"),
        G("mas", "Agregar", "M12 5v14 M5 12h14"),
        G("cerrar", "Cerrar", "M6 6l12 12 M18 6L6 18"),
        G("editar", "Editar", "M4 20l1-4L16 5a2 2 0 0 1 3 3L8 19z"),
        G("repetir", "Repetir", "M4 12a8 8 0 0 1 14-5.3 M18 3v4h-4 M20 12a8 8 0 0 1-14 5.3 M6 21v-4h4"),
        G("pasos", "Pasos", "M8 3.5c1.8 0 2.7 2 2.7 4.5S9.6 12 8.2 12 5.5 10.6 5.5 8 6.2 3.5 8 3.5z M6 14.5h4.5l-.3 2a2.2 2.2 0 0 1-4.3-.2z M16 8.5c1.8 0 2.5 4.5 2.5 6.5s-1.3 4-2.7 4-2.5-1.5-2.5-4 .9-6.5 2.7-6.5z M13.5 20.5H18"),
    )
    /** Íconos que el usuario puede elegir para rachas (dejar) y hábitos (construir). */
    val PICKABLE_QUIT = QUIT + BUILD.filter { it.key in setOf("dormir", "sano") }
    val PICKABLE_BUILD = BUILD + SYSTEM.filter { it.key in setOf("brazo", "gym", "hecho", "brote", "pasos") }

    private val all = (QUIT + BUILD + SYSTEM).associateBy { it.key }
    private val cache = HashMap<String, ImageVector>()

    /** Emojis guardados en versiones anteriores → clave de ícono. */
    private val FROM_EMOJI = mapOf(
        "🚭" to "fumar", "🍺" to "alcohol", "🍬" to "azucar", "📱" to "redes", "🎮" to "videojuegos", "☕" to "cafe",
        "🍔" to "comida", "🥤" to "refresco", "🌿" to "fumar", "🎰" to "apuestas", "🛒" to "compras", "💤" to "dormir",
        "💧" to "agua", "📖" to "leer", "🧘" to "meditar", "🚶" to "caminar", "😴" to "dormir", "🥗" to "sano", "🏃" to "cardio",
        "✍️" to "escribir", "✍" to "escribir", "🦷" to "dientes", "☀️" to "madrugar", "☀" to "madrugar", "🎸" to "musica",
        "🧠" to "estudiar", "✅" to "hecho",
    )

    fun keyFor(value: String, fallback: String = "racha"): String = when {
        value in all -> value
        else -> FROM_EMOJI[value] ?: FROM_EMOJI[value.replace("️", "")] ?: fallback
    }

    fun name(key: String) = all[key]?.name ?: key

    fun vector(key: String): ImageVector = cache.getOrPut(key) {
        val g = all[key] ?: all.getValue("racha")
        ImageVector.Builder(name = g.key, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(
                pathData = PathParser().parsePathString(g.d).toNodes(),
                fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            ).build()
    }
}

/** Un ícono del set, del color que se pida. */
@Composable
fun Glyph(key: String, tint: Color = Ink2, size: Dp = 24.dp, desc: String? = null, modifier: Modifier = Modifier) =
    Icon(Glyphs.vector(Glyphs.keyFor(key)), desc, tint = tint, modifier = modifier.size(size))

enum class ChipState { Normal, Pending, Done }

/** Ficha "squircle" con 3 estados: normal, pendiente hoy (naranja suave) y cumplida (blanca con palomita). */
@Composable
fun IconChip(key: String, state: ChipState = ChipState.Normal, size: Dp = 52.dp, desc: String? = null) {
    val bg = when (state) { ChipState.Normal -> CardHigh; ChipState.Pending -> AccentSoft; ChipState.Done -> Ink }
    val fg = when (state) { ChipState.Normal -> Ink2; ChipState.Pending -> Accent; ChipState.Done -> Bg }
    Box(Modifier.size(size + 6.dp), contentAlignment = Alignment.TopStart) {
        Box(
            Modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(bg)
                .then(if (state == ChipState.Pending) Modifier.border(1.dp, Accent.copy(alpha = 0.35f), RoundedCornerShape(size * 0.32f)) else Modifier),
            contentAlignment = Alignment.Center,
        ) { Glyph(Glyphs.keyFor(key), fg, size * 0.46f, desc) }
        if (state == ChipState.Done) Box(
            Modifier.align(Alignment.BottomEnd).size(size * 0.38f).clip(CircleShape).background(Bg),
            contentAlignment = Alignment.Center,
        ) { Glyph("hecho", Ink, size * 0.24f) }
    }
}

/** Rejilla para elegir el ícono de una racha o hábito. */
@Composable
fun GlyphPicker(options: List<Glyphs.G>, selected: String, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { g ->
                    val sel = g.key == Glyphs.keyFor(selected)
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(if (sel) AccentSoft else Color(0xFF2A2A2E))
                            .then(if (sel) Modifier.border(1.dp, Accent.copy(alpha = 0.5f), RoundedCornerShape(13.dp)) else Modifier)
                            .clickableNoRipple { onPick(g.key) },
                        contentAlignment = Alignment.Center,
                    ) { Glyph(g.key, if (sel) Accent else Ink2, 20.dp, g.name) }
                }
            }
        }
    }
}
