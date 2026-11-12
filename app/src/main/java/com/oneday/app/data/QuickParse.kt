package com.oneday.app.data

import java.time.LocalDate
import java.time.LocalTime

/** Resultado de interpretar una frase como "dentista mañana 10am !alta". */
data class Parsed(
    val title: String,
    val date: Long? = null,
    val time: Int? = null,
    val priority: Int? = null,
    val daily: Boolean = false,
)

/**
 * Entiende fechas, horas, prioridad y repetición escritas en español, sin internet.
 * Ejemplos: "pagar luz viernes", "dentista mañana 10am !alta", "leer cada día 9pm",
 * "gym en 3 días a las 7", "cita 15 de diciembre 4:30pm".
 */
object QuickParse {
    private val WEEKDAYS = listOf("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo")
    private val MONTHS = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
    private val MONTHS_SHORT = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

    /** Minúsculas y sin acentos, con la MISMA longitud que el original (para poder recortar). */
    private fun norm(s: String) = s.lowercase().map {
        when (it) { 'á' -> 'a'; 'é' -> 'e'; 'í' -> 'i'; 'ó' -> 'o'; 'ú', 'ü' -> 'u'; 'ñ' -> 'n'; else -> it }
    }.joinToString("")

    fun parse(input: String, now: LocalDate = LocalDate.now(), nowTime: LocalTime = LocalTime.now()): Parsed {
        val n = norm(input)
        val remove = mutableListOf<IntRange>()
        fun taken(r: IntRange) = remove.any { it.first <= r.last && r.first <= it.last }
        fun grab(regex: Regex, f: (MatchResult) -> Boolean) {
            for (m in regex.findAll(n)) if (!taken(m.range) && f(m)) remove += m.range
        }
        val b = "(?<![\\p{L}\\d])" // inicio de palabra
        val e = "(?![\\p{L}\\d])"  // fin de palabra

        var priority: Int? = null
        grab(Regex("$b(!+alta|!a|!!!?|urgente|!+media|!m|!+baja|!b)$e|(?<=\\s|^)(!!!?)(?=\\s|$)")) { m ->
            priority = when (m.value.trim('!').ifEmpty { "a" }) { "alta", "a", "urgente" -> 2; "media", "m" -> 1; "baja", "b" -> 0; else -> 2 }
            true
        }

        var daily = false
        grab(Regex("$b(cada dia|todos los dias|diariamente|diario)$e")) { daily = true; true }

        var time: Int? = null
        grab(Regex("$b(mediodia|medianoche)$e")) { m -> time = if (m.value == "mediodia") 12 * 60 else 0; true }
        // 10am · 10:30 pm · a las 5 · 8 de la noche · 7 hrs
        grab(Regex("$b(?:a las? )?(\\d{1,2})(?::(\\d{2}))?\\s*(a\\.?\\s?m\\.?|p\\.?\\s?m\\.?|hrs?|h)?(?: de la (manana|tarde|noche))?$e")) { m ->
            if (time != null) return@grab false
            val hasPrefix = m.value.startsWith("a la")
            val (hS, minS, suf, part) = m.destructured
            if (!hasPrefix && minS.isEmpty() && suf.isEmpty() && part.isEmpty()) return@grab false
            var h = hS.toInt(); val min = minS.ifEmpty { "0" }.toInt()
            if (h > 23 || min > 59) return@grab false
            val s = suf.replace(".", "").replace(" ", "")
            when {
                s == "pm" || part == "tarde" || part == "noche" -> if (h < 12) h += 12
                s == "am" || part == "manana" -> if (h == 12) h = 0
                s.isEmpty() && part.isEmpty() && h in 1..7 && minS.isEmpty() -> h += 12 // "a las 5" = 5 p. m.
            }
            time = h * 60 + min
            true
        }

        var date: Long? = null
        val t = now.toEpochDay()
        grab(Regex("$b(pasado manana)$e")) { date = t + 2; true }
        grab(Regex("$b(manana)$e")) { if (date != null) false else { date = t + 1; true } }
        grab(Regex("$b(hoy|esta noche)$e")) { if (date != null) false else { date = t; true } }
        grab(Regex("$b(en|dentro de) (\\d{1,3}) dias?$e")) { m -> if (date != null) false else { date = t + m.groupValues[2].toInt(); true } }
        grab(Regex("$b(?:el |este |(?:el )?proximo )?(${WEEKDAYS.joinToString("|")})$e")) { m ->
            if (date != null) return@grab false
            val wd = WEEKDAYS.indexOf(m.groupValues[1]) + 1
            var diff = (wd - now.dayOfWeek.value + 7) % 7
            if (m.value.contains("proximo") && diff == 0) diff = 7
            date = t + diff
            true
        }
        grab(Regex("$b(\\d{1,2}) de (${MONTHS.joinToString("|")})$e|$b(\\d{1,2}) (${MONTHS_SHORT.joinToString("|")})\\.?$e")) { m ->
            if (date != null) return@grab false
            val day = (m.groupValues[1].ifEmpty { m.groupValues[3] }).toInt()
            val month = if (m.groupValues[2].isNotEmpty()) MONTHS.indexOf(m.groupValues[2]) + 1 else MONTHS_SHORT.indexOf(m.groupValues[4]) + 1
            val d = runCatching { LocalDate.of(now.year, month, day) }.getOrNull() ?: return@grab false
            date = (if (d.isBefore(now)) d.plusYears(1) else d).toEpochDay()
            true
        }
        grab(Regex("$b(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?$e")) { m ->
            if (date != null) return@grab false
            val (dS, mS, yS) = m.destructured
            val y = when { yS.isEmpty() -> now.year; yS.length == 2 -> 2000 + yS.toInt(); else -> yS.toInt() }
            val d = runCatching { LocalDate.of(y, mS.toInt(), dS.toInt()) }.getOrNull() ?: return@grab false
            date = (if (yS.isEmpty() && d.isBefore(now)) d.plusYears(1) else d).toEpochDay()
            true
        }

        // Una hora sin fecha: hoy si aún no pasa, si no mañana.
        if (time != null && date == null && !daily) {
            date = if (time!! > nowTime.hour * 60 + nowTime.minute) t else t + 1
        }

        // Título = la frase sin lo que se interpretó.
        val sb = StringBuilder()
        input.forEachIndexed { i, c -> sb.append(if (remove.any { i in it }) ' ' else c) }
        var title = sb.toString().replace(Regex("\\s+"), " ").trim()
        val filler = setOf("el", "la", "a", "para", "de", "en", "las", "los", "y", "este", "esta")
        var words = title.split(" ").filter { it.isNotEmpty() }
        while (words.isNotEmpty() && norm(words.last()) in filler) words = words.dropLast(1)
        while (words.size > 1 && norm(words.first()) in setOf("el", "la")) words = words.drop(1)
        title = words.joinToString(" ").replaceFirstChar { it.uppercase() }
        return Parsed(title, date, time, priority, daily)
    }
}
