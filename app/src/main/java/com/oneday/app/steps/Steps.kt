package com.oneday.app.steps

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.oneday.app.data.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import kotlin.coroutines.resume

/**
 * Pasos del día. Fuente principal: Health Connect (en un Samsung, lo que cuenta Samsung Health).
 * Respaldo: el sensor de pasos del teléfono, que la app revisa cada ~30 min.
 * Por cada día se usa el mayor de los dos, para no contar doble.
 */
object Steps {
    val HC_PERMS = setOf(HealthPermission.getReadPermission(StepsRecord::class))
    const val GOAL_DEFAULT = 10_000

    fun hcAvailable(ctx: Context) = runCatching { HealthConnectClient.getSdkStatus(ctx) == HealthConnectClient.SDK_AVAILABLE }.getOrDefault(false)

    suspend fun hcGranted(ctx: Context): Boolean = runCatching {
        hcAvailable(ctx) && HealthConnectClient.getOrCreate(ctx).permissionController.getGrantedPermissions().containsAll(HC_PERMS)
    }.getOrDefault(false)

    fun sensorAvailable(ctx: Context) = ctx.getSystemService(SensorManager::class.java)?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    fun sensorGranted(ctx: Context) = Build.VERSION.SDK_INT < 29 ||
        ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    /** Pasos por día de los últimos [days] días según Health Connect. */
    private suspend fun readHc(ctx: Context, days: Int): Map<Long, Int> = runCatching {
        val client = HealthConnectClient.getOrCreate(ctx)
        val start = LocalDate.now().minusDays(days.toLong() - 1).atStartOfDay()
        val res = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, LocalDateTime.now()),
                timeRangeSlicer = Period.ofDays(1),
            )
        )
        res.associate { it.startTime.toLocalDate().toEpochDay() to (it.result[StepsRecord.COUNT_TOTAL] ?: 0L).toInt() }
    }.getOrDefault(emptyMap())

    /** Lee una vez el contador del sensor (pasos desde que se encendió el teléfono). */
    private suspend fun readSensor(ctx: Context): Float? {
        val sm = ctx.getSystemService(SensorManager::class.java) ?: return null
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return null
        if (!sensorGranted(ctx)) return null
        return withTimeoutOrNull(4000) {
            suspendCancellableCoroutine { cont ->
                val l = object : SensorEventListener {
                    override fun onSensorChanged(e: SensorEvent) { sm.unregisterListener(this); if (cont.isActive) cont.resume(e.values[0]) }
                    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
                }
                sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                cont.invokeOnCancellation { sm.unregisterListener(l) }
            }
        }
    }

    /** Convierte el contador del sensor en "pasos de hoy" guardando el punto de partida de cada día. */
    private fun sensorToday(ctx: Context, counter: Float): Int {
        val p = ctx.getSharedPreferences("steps", Context.MODE_PRIVATE)
        val t = today()
        val day = p.getLong("day", -1); val last = p.getFloat("last", -1f); var acc = p.getInt("acc", 0)
        if (day != t) acc = 0
        else if (last >= 0) acc += (if (counter >= last) counter - last else counter).toInt() // si se reinició el teléfono, el contador vuelve a 0
        p.edit().putLong("day", t).putFloat("last", counter).putInt("acc", acc).apply()
        return acc
    }

    /** Sincroniza: guarda los pasos en el hábito de pasos (hoy y, con Health Connect, los últimos 30 días). */
    suspend fun sync(ctx: Context) {
        Repo.init(ctx)
        val h = Repo.data.habits.firstOrNull { it.source == "steps" } ?: return
        val perDay = HashMap<Long, Int>()
        if (hcGranted(ctx)) perDay.putAll(readHc(ctx, 30))
        readSensor(ctx)?.let { c -> val t = today(); perDay[t] = maxOf(perDay[t] ?: 0, sensorToday(ctx, c)) }
        if (perDay.isEmpty()) return
        val goal = h.target ?: GOAL_DEFAULT
        Repo.update { d ->
            d.copy(habits = d.habits.map { x ->
                if (x.id != h.id) x else {
                    val counts = x.counts.toMutableMap(); val done = x.doneDays.toMutableSet()
                    perDay.forEach { (day, n) ->
                        val v = maxOf(counts[day] ?: 0, n)
                        counts[day] = v
                        if (v >= goal) done += day else done -= day
                    }
                    val firstDay = counts.filterValues { it > 0 }.keys.minOrNull() ?: x.createdDay
                    x.copy(counts = counts, doneDays = done, createdDay = minOf(x.createdDay, firstDay))
                }
            })
        }
        ctx.getSharedPreferences("steps", Context.MODE_PRIVATE).edit().putLong("synced", System.currentTimeMillis()).apply()
    }

    fun lastSync(ctx: Context) = ctx.getSharedPreferences("steps", Context.MODE_PRIVATE).getLong("synced", 0)

    /** Crea el hábito de pasos (una sola vez). */
    fun createHabit(goal: Int = GOAL_DEFAULT) = Repo.update { d ->
        if (d.habits.any { it.source == "steps" }) d
        else d.copy(
            habits = d.habits + Habit(name = "Caminar", emoji = "pasos", createdDay = today(), target = goal, unit = "pasos", step = 0, source = "steps"),
            settings = d.settings.copy(stepsAsked = true),
        )
    }

    /** ¿Lleva 3 semanas superando la meta? Devuelve la nueva meta sugerida (+500). */
    fun suggestedGoal(h: Habit): Int? {
        val goal = h.target ?: return null
        val t = today()
        val ok = (1..21).all { (h.counts[t - it] ?: 0) >= goal }
        return if (ok) goal + 500 else null
    }
}
