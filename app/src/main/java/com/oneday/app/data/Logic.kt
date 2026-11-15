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

fun fmtWeight(w: Double): String {
    val r2 = Math.round(w * 100) / 100.0
    return when {
        r2 % 1.0 == 0.0 -> r2.toLong().toString()
        Math.round(r2 * 10) / 10.0 == r2 -> "%.1f".format(java.util.Locale.US, r2)
        else -> "%.2f".format(java.util.Locale.US, r2)
    }
}

fun millisToEpochDay(ms: Long): Long = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

/** Momento (millis) de una fecha + hora local. */
fun atMillis(epochDay: Long, minutes: Int): Long =
    LocalDate.ofEpochDay(epochDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + minutes * 60_000L

/** Series que cuentan (el calentamiento no suma a volumen, repeticiones ni récords). */
val WorkSet.counts get() = kind != SET_WARMUP && done
fun Workout.volume(): Double = entries.sumOf { e -> e.sets.filter { it.counts }.sumOf { it.weight * it.reps } }
fun Workout.setCount(): Int = entries.sumOf { e -> e.sets.count { it.counts } }

/** 1RM estimado (fórmula de Epley). */
fun oneRm(s: WorkSet): Double = if (s.reps <= 1) s.weight else s.weight * (1 + s.reps / 30.0)

fun lastSetsFor(data: AppData, exerciseId: String): List<WorkSet>? =
    data.workouts.sortedByDescending { it.startedAt }
        .firstNotNullOfOrNull { w -> w.entries.firstOrNull { it.exerciseId == exerciseId && it.sets.any { s -> s.counts } }?.sets?.filter { it.counts } }

fun round1(d: Double) = (d * 10).roundToInt() / 10.0

// ---------- Estadísticas del gimnasio ----------

fun fmtCompact(v: Double): String = when {
    v >= 1_000_000 -> "%.1fM".format(v / 1_000_000)
    v >= 10_000 -> "%.0fk".format(v / 1000)
    v >= 1_000 -> "%.1fk".format(v / 1000)
    else -> fmtWeight(round1(v))
}

fun Workout.reps(): Int = entries.sumOf { e -> e.sets.filter { it.counts }.sumOf { it.reps } }

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
    // Si se sintió muy pesado (esfuerzo 9 o 10), no sube el peso.
    val completed = topSets.all { it.reps >= goal } && topSets.none { (it.rpe ?: 0) >= 9 }
    val inc = if (top >= 20) 2.5 else 1.0
    return if (completed) Target(round1(top + inc), goal, true) else Target(top, goal, false)
}

data class Bests(val weight: Double, val oneRm: Double, val seconds: Int = 0, val km: Double = 0.0)

fun bestsFor(data: AppData, exerciseId: String): Bests? {
    val sets = data.workouts.flatMap { w -> w.entries.filter { it.exerciseId == exerciseId }.flatMap { it.sets } }.filter { it.counts }
    if (sets.isEmpty()) return null
    return Bests(sets.maxOf { it.weight }, sets.maxOf { oneRm(it) }, sets.maxOf { it.seconds ?: 0 }, sets.maxOf { it.km ?: 0.0 })
}

fun isPr(set: WorkSet, prev: Bests?): Boolean = prev != null && set.counts && when {
    set.km != null -> set.km > prev.km + 0.001
    set.seconds != null -> set.seconds > prev.seconds
    else -> set.weight > prev.weight || oneRm(set) > prev.oneRm + 0.01
}

fun fmtSecs(s: Int): String = "%d:%02d".format(s / 60, s % 60)

/** Texto de una serie según el tipo de ejercicio. */
fun fmtSet(s: WorkSet): String = when {
    s.km != null -> "${fmtWeight(round1(s.km))} km · ${fmtSecs(s.seconds ?: 0)}"
    s.seconds != null -> fmtSecs(s.seconds)
    else -> "${fmtWeight(s.weight)}×${s.reps}"
}

// ---------- Calculadora de discos ----------

/** Discos por lado para llegar a `total` con la barra dada. Devuelve los discos y lo que no se pudo cubrir. */
fun platesFor(total: Double, bar: Double, plates: List<Double>): Pair<List<Double>, Double> {
    var side = (total - bar) / 2
    if (side <= 0) return emptyList<Double>() to 0.0
    val out = mutableListOf<Double>()
    for (p in plates.sortedDescending()) while (side + 1e-6 >= p) { out += p; side -= p }
    return out to round1(side * 2 * 100) / 100
}

// ---------- Peso corporal: media de 7 días ----------

/** Media móvil de 7 días en cada registro (incluye los registros de los 6 días anteriores). */
fun movingAvg7(list: List<BodyWeight>): List<Pair<Long, Double>> {
    val sorted = list.sortedBy { it.date }
    return sorted.map { w -> w.date to sorted.filter { it.date in (w.date - 6)..w.date }.map { it.kg }.average() }
}

/** Cambio de la media en los últimos 7 días (kg/semana), si hay datos suficientes. */
fun weeklyChange(list: List<BodyWeight>): Double? {
    val avg = movingAvg7(list)
    if (avg.size < 2) return null
    val last = avg.last()
    val ref = avg.lastOrNull { it.first <= last.first - 7 } ?: return null
    val weeks = (last.first - ref.first) / 7.0
    return (last.second - ref.second) / weeks
}

// ---------- Hábitos con cantidad ----------

fun Habit.countToday(t: Long = today()) = counts[t] ?: 0

/** Suma (o resta) cantidad hoy y marca el día como hecho al llegar a la meta. */
fun Habit.addAmount(delta: Int, t: Long = today()): Habit {
    val n = (countToday(t) + delta).coerceAtLeast(0)
    val goal = target ?: return this
    return copy(counts = counts + (t to n), doneDays = if (n >= goal) doneDays + t else doneDays - t)
}

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


/** Series planeadas para hoy a partir de la última sesión (el objetivo sube el peso si toca). */
fun plannedSets(data: AppData, exerciseId: String): List<WorkSet> {
    val last = lastSetsFor(data, exerciseId) ?: return emptyList()
    val kind = data.exercises.firstOrNull { it.id == exerciseId }?.kind ?: 0
    if (kind != 0) return last.map { it.copy(done = false, rpe = null) }
    val top = last.maxOf { it.weight }
    val target = suggestTarget(last)
    return last.map { it.copy(weight = if (it.weight == top && target != null) target.weight else it.weight, done = false, rpe = null, kind = SET_NORMAL) }
}
