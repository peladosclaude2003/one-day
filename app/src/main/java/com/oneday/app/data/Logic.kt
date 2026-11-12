package com.oneday.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

val ES: Locale = Locale.forLanguageTag("es-MX")
const val DAY_MS = 24L * 60 * 60 * 1000

fun today(): Long = LocalDate.now().toEpochDay()

fun streakDays(s: Streak, now: Long = System.currentTimeMillis()) = ((now - s.start).coerceAtLeast(0)) / DAY_MS
fun streakHours(s: Streak, now: Long = System.currentTimeMillis()) = (((now - s.start).coerceAtLeast(0)) % DAY_MS) / (60L * 60 * 1000)

val MILESTONES = listOf(1L, 3, 7, 14, 21, 30, 60, 90, 180, 365, 500, 730, 1000)
fun nextMilestone(days: Long): Long = MILESTONES.firstOrNull { it > days } ?: ((days / 365) + 1) * 365
fun prevMilestone(days: Long): Long = MILESTONES.lastOrNull { it <= days } ?: 0

fun fmtTime(min: Int): String {
    val h = min / 60; val m = min % 60
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "%d:%02d %s".format(h12, m, if (h < 12) "a. m." else "p. m.")
}

fun fmtDay(epochDay: Long): String {
    val t = today()
    return when (epochDay) {
        t -> "Hoy"
        t + 1 -> "Mañana"
        t - 1 -> "Ayer"
        else -> LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("EEE d MMM", ES)).replace(".", "")
    }
}

fun fmtLongDate(d: LocalDate): String =
    d.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", ES)).replaceFirstChar { it.uppercase() }

fun fmtWeight(w: Double): String = if (w % 1.0 == 0.0) w.toInt().toString() else "%.1f".format(w)

fun millisToEpochDay(ms: Long): Long = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

/** Momento (millis) de una fecha + hora local. */
fun atMillis(epochDay: Long, minutes: Int): Long =
    LocalDate.ofEpochDay(epochDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + minutes * 60_000L

fun Workout.volume(): Double = entries.sumOf { e -> e.sets.sumOf { it.weight * it.reps } }
fun Workout.setCount(): Int = entries.sumOf { it.sets.size }

/** 1RM estimado (fórmula de Epley). */
fun oneRm(s: WorkSet): Double = if (s.reps <= 1) s.weight else s.weight * (1 + s.reps / 30.0)

fun lastSetsFor(data: AppData, exerciseId: String): List<WorkSet>? =
    data.workouts.sortedByDescending { it.startedAt }
        .firstNotNullOfOrNull { w -> w.entries.firstOrNull { it.exerciseId == exerciseId && it.sets.isNotEmpty() }?.sets }

fun round1(d: Double) = (d * 10).roundToInt() / 10.0
