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

// ---------- Estadísticas del gimnasio ----------

fun fmtCompact(v: Double): String = when {
    v >= 1_000_000 -> "%.1fM".format(v / 1_000_000)
    v >= 10_000 -> "%.0fk".format(v / 1000)
    v >= 1_000 -> "%.1fk".format(v / 1000)
    else -> fmtWeight(round1(v))
}

fun Workout.reps(): Int = entries.sumOf { e -> e.sets.sumOf { it.reps } }

fun mondayOf(epochDay: Long): Long {
    val d = LocalDate.ofEpochDay(epochDay)
    return epochDay - (d.dayOfWeek.value - 1)
}

/** Entrenamientos de los últimos [days] días (incluye hoy). */
fun AppData.recentWorkouts(days: Int): List<Workout> = workouts.filter { it.date > today() - days }

/** Volumen (kg) por semana, las últimas [weeks] semanas, de la más vieja a la actual. */
fun AppData.weeklyVolume(weeks: Int = 8): List<Double> {
    val thisMonday = mondayOf(today())
    return (weeks - 1 downTo 0).map { w ->
        val start = thisMonday - w * 7
        workouts.filter { it.date in start until start + 7 }.sumOf { it.volume() }
    }
}

/** Repeticiones por día de la semana actual (lunes a domingo). */
fun AppData.weekReps(offsetWeeks: Int = 0): List<Int> {
    val monday = mondayOf(today()) - offsetWeeks * 7
    return (0 until 7).map { i -> workouts.filter { it.date == monday + i }.sumOf { it.reps() } }
}

/** Siguiente rutina sugerida: la que sigue a la última que hiciste. */
fun AppData.suggestedRoutine(): Routine? {
    if (routines.isEmpty()) return null
    val last = workouts.maxByOrNull { it.startedAt } ?: return routines.first()
    val idx = routines.indexOfFirst { it.name == last.name }
    return if (idx < 0) routines.first() else routines[(idx + 1) % routines.size]
}

/** Minutos estimados de una rutina: promedio de las veces anteriores, o 12 min por ejercicio. */
fun AppData.estimatedMinutes(r: Routine): Int {
    val past = workouts.filter { it.name == r.name && it.minutes > 0 }
    return if (past.isNotEmpty()) past.map { it.minutes }.average().toInt() else r.exerciseIds.size * 12
}

fun shortName(name: String): String = name.substringBefore(" (").trim()

// ---------- Hábitos (construir) ----------

/**
 * Racha de un hábito: días seguidos cumplidos hasta hoy, con 1 día de gracia por semana (lunes a domingo).
 * Si hoy aún no lo marcas, no rompe la racha: se cuenta desde ayer.
 */
fun habitStreak(h: Habit, t: Long = today()): Int {
    var d = if (t in h.doneDays) t else t - 1
    var count = 0
    val missesPerWeek = HashMap<Long, Int>()
    while (d >= h.createdDay) {
        if (d in h.doneDays) count++
        else {
            val w = mondayOf(d)
            val m = (missesPerWeek[w] ?: 0) + 1
            if (m > 1) break
            missesPerWeek[w] = m
        }
        d--
    }
    return count
}

/** ¿Queda día de gracia esta semana? */
fun graceLeft(h: Habit, t: Long = today()): Boolean {
    val monday = mondayOf(t)
    val missed = (monday until t).count { it >= h.createdDay && it !in h.doneDays }
    return missed == 0
}

fun habitBest(h: Habit): Int {
    if (h.doneDays.isEmpty()) return 0
    return h.doneDays.maxOf { habitStreak(h, it) }
}

// ---------- Sobrecarga progresiva y récords ----------

data class Target(val weight: Double, val reps: Int, val up: Boolean)

/**
 * Objetivo de hoy a partir de la última sesión: si en el peso más alto lograste las repeticiones de la
 * primera serie en todas las series, sube el peso; si no, repite el peso. Sin peso (corporal): +1 repetición.
 */
fun suggestTarget(last: List<WorkSet>?): Target? {
    if (last.isNullOrEmpty()) return null
    val top = last.maxOf { it.weight }
    val topSets = last.filter { it.weight == top }
    val goal = topSets.first().reps.coerceAtLeast(1)
    if (top == 0.0) return Target(0.0, topSets.maxOf { it.reps } + 1, true)
    val completed = topSets.all { it.reps >= goal }
    val inc = if (top >= 20) 2.5 else 1.0
    return if (completed) Target(round1(top + inc), goal, true) else Target(top, goal, false)
}

data class Bests(val weight: Double, val oneRm: Double)

fun bestsFor(data: AppData, exerciseId: String): Bests? {
    val sets = data.workouts.flatMap { w -> w.entries.filter { it.exerciseId == exerciseId }.flatMap { it.sets } }
    if (sets.isEmpty()) return null
    return Bests(sets.maxOf { it.weight }, sets.maxOf { oneRm(it) })
}

fun isPr(set: WorkSet, prev: Bests?): Boolean =
    prev != null && (set.weight > prev.weight || oneRm(set) > prev.oneRm + 0.01)

// ---------- Antojos ----------

/** Antojos por franja de 3 horas (0–3, 3–6 … 21–24). */
fun cravingBuckets(list: List<Craving>): List<Int> {
    val b = IntArray(8)
    list.forEach { c ->
        val h = java.time.Instant.ofEpochMilli(c.at).atZone(java.time.ZoneId.systemDefault()).hour
        b[h / 3]++
    }
    return b.toList()
}

fun bucketLabel(i: Int): String {
    fun h(x: Int) = when { x == 0 || x == 24 -> "12 a. m."; x < 12 -> "$x a. m."; x == 12 -> "12 p. m."; else -> "${x - 12} p. m." }
    return "${h(i * 3)} – ${h(i * 3 + 3)}"
}

/** Hora (minutos) en la que conviene avisar: 30 min antes de la franja con más antojos (mínimo 5 registrados). */
fun cravingAlertTime(list: List<Craving>): Int? {
    if (list.size < 5) return null
    val b = cravingBuckets(list)
    val peak = b.indices.maxBy { b[it] }
    return ((peak * 3 * 60) - 30 + 24 * 60) % (24 * 60)
}
