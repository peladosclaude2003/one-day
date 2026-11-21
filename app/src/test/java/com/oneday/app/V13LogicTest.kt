package com.oneday.app

import com.oneday.app.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class V13LogicTest {
    private val plates = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

    @Test fun discos() {
        assertEquals(listOf(25.0, 5.0, 1.25) to 0.0, platesFor(82.5, 20.0, plates))
        assertEquals(listOf(20.0) to 0.0, platesFor(60.0, 20.0, plates))
        assertEquals(emptyList<Double>() to 0.0, platesFor(20.0, 20.0, plates))
        // Sin discos de 1.25: sobran 2.5 kg que no se pueden poner
        assertEquals(listOf(25.0, 5.0) to 2.5, platesFor(82.5, 20.0, plates - 1.25))
    }

    @Test fun mediaDeSieteDias() {
        val ws = (0..13).map { BodyWeight(100L + it, 80.0 - it * 0.1) }
        val avg = movingAvg7(ws)
        assertEquals(80.0, avg.first().second, 1e-9)
        assertEquals((7..13).map { 80.0 - it * 0.1 }.average(), avg.last().second, 1e-9)
        assertEquals(-0.7, weeklyChange(ws)!!, 1e-6)
    }

    @Test fun habitoConCantidad() {
        var h = Habit(name = "Agua", createdDay = 0, target = 8, unit = "vasos")
        h = h.addAmount(5, t = 10)
        assertEquals(5, h.countToday(10)); assertFalse(10L in h.doneDays)
        h = h.addAmount(3, t = 10)
        assertTrue(10L in h.doneDays)
        h = h.addAmount(-1, t = 10)
        assertFalse(10L in h.doneDays)
    }

    @Test fun calentamientoNoCuenta() {
        val w = Workout(date = 0, name = "x", entries = listOf(WorkEntry("ex_banca", listOf(WorkSet(100.0, 5, SET_WARMUP), WorkSet(60.0, 10)))))
        assertEquals(600.0, w.volume(), 1e-9)
        assertEquals(1, w.setCount())
        val data = AppData(workouts = listOf(w))
        assertEquals(60.0, bestsFor(data, "ex_banca")!!.weight, 1e-9)
        assertFalse(isPr(WorkSet(200.0, 5, SET_WARMUP), bestsFor(data, "ex_banca")))
        assertTrue(isPr(WorkSet(62.5, 10), bestsFor(data, "ex_banca")))
    }

    @Test fun esfuerzoAltoNoSubePeso() {
        assertEquals(Target(60.0, 10, false), suggestTarget(listOf(WorkSet(60.0, 10, rpe = 9), WorkSet(60.0, 10))))
    }

    @Test fun recordsDeTiempoYDistancia() {
        val data = AppData(workouts = listOf(Workout(date = 0, name = "x", entries = listOf(
            WorkEntry("ex_plancha", listOf(WorkSet(0.0, 0, seconds = 60))),
            WorkEntry("ex_caminadora", listOf(WorkSet(0.0, 0, seconds = 1200, km = 2.5))),
        ))))
        assertTrue(isPr(WorkSet(0.0, 0, seconds = 75), bestsFor(data, "ex_plancha")))
        assertFalse(isPr(WorkSet(0.0, 0, seconds = 50), bestsFor(data, "ex_plancha")))
        assertTrue(isPr(WorkSet(0.0, 0, seconds = 1500, km = 3.0), bestsFor(data, "ex_caminadora")))
        assertEquals("2.5 km · 20:00", fmtSet(WorkSet(0.0, 0, seconds = 1200, km = 2.5)))
    }

    @Test fun fechasDeRepeticion() {
        val jue = LocalDate.of(2026, 11, 12).toEpochDay()
        assertEquals(jue + 1, nextDate(Repeat("day"), jue, jue))
        assertEquals(jue + 4, nextDate(Repeat("week", days = setOf(1)), jue, jue)) // siguiente lunes
        assertEquals(LocalDate.of(2026, 12, 12).toEpochDay(), nextDate(Repeat("month"), jue, jue))
        // Si estaba atrasado, salta hasta hoy o después
        assertEquals(jue, nextDate(Repeat("day"), jue - 5, jue))
    }

    @Test fun completarRepetidoCreaElSiguienteYReabrirLoQuita() {
        val t0 = today()
        val d0 = AppData(tasks = listOf(Task(id = "a", title = "Basura", date = t0, repeat = Repeat("week", days = setOf(java.time.LocalDate.ofEpochDay(t0).dayOfWeek.value)))))
        val d1 = d0.completeTask("a")
        assertEquals(2, d1.tasks.size)
        val next = d1.tasks.first { it.id != "a" }
        assertEquals(t0 + 7, next.date); assertFalse(next.done)
        val d2 = d1.reopenTask("a")
        assertEquals(1, d2.tasks.size); assertFalse(d2.tasks.first().done)
    }

    @Test fun progresoPorGrupo() {
        val t0 = today()
        val ws = (0 until 6).map { i ->
            Workout(date = t0 - 50 + i * 10, name = "Pierna", startedAt = i.toLong(),
                entries = listOf(WorkEntry("ex_sentadilla", listOf(WorkSet(60.0 + i * 5, 8))), WorkEntry("ex_curl_barra", listOf(WorkSet(20.0, 10)))))
        }
        val d = AppData(workouts = ws)
        assertEquals(oneRm(WorkSet(85.0, 8)) / oneRm(WorkSet(60.0, 8)) * 100, d.strengthIndex("Piernas")!!, 1e-6)
        assertEquals(100.0, d.strengthIndex("Brazos")!!, 1e-6)
        assertEquals(null, d.strengthIndex("Pecho"))
        assertEquals("Brazos", groupOf("Tríceps"))
        assertTrue(d.groupWeeks("Piernas", 12).sumOf { it.sets } >= 5)
    }

    @Test fun formatoDePeso() {
        assertEquals("56.25", fmtWeight(56.25)); assertEquals("82.5", fmtWeight(82.5)); assertEquals("60", fmtWeight(60.0)); assertEquals("1.25", fmtWeight(1.25))
    }

    @Test fun areaEnCapturaRapida() {
        val p = QuickParse.parse("enviar reporte mañana #trabajo", LocalDate.of(2026, 11, 15), LocalTime.of(9, 0), listOf("Trabajo", "Casa"))
        assertEquals("Trabajo", p.area); assertEquals("Enviar reporte", p.title)
    }
}
