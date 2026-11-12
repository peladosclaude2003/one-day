package com.oneday.app

import com.oneday.app.data.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class QuickParseTest {
    private val hoy = LocalDate.of(2026, 11, 12) // jueves
    private val t = hoy.toEpochDay()
    private fun p(s: String) = QuickParse.parse(s, hoy, LocalTime.of(9, 0))

    @Test fun dentista() = with(p("dentista mañana 10am !alta")) {
        assertEquals("Dentista", title); assertEquals(t + 1, date); assertEquals(600, time); assertEquals(2, priority)
    }
    @Test fun viernes() = with(p("pagar luz viernes")) { assertEquals("Pagar luz", title); assertEquals(t + 1, date); assertEquals(null, time) }
    @Test fun diario() = with(p("leer cada día 9pm")) { assertEquals("Leer", title); assertEquals(true, daily); assertEquals(21 * 60, time) }
    @Test fun enDias() = with(p("gym en 3 días a las 7")) { assertEquals("Gym", title); assertEquals(t + 3, date); assertEquals(19 * 60, time) }
    @Test fun fechaLarga() = with(p("cita 15 de diciembre 4:30pm")) {
        assertEquals("Cita", title); assertEquals(LocalDate.of(2026, 12, 15).toEpochDay(), date); assertEquals(16 * 60 + 30, time)
    }
    @Test fun noConfundeNumeros() = with(p("leer 3 libros")) { assertEquals("Leer 3 libros", title); assertEquals(null, date); assertEquals(null, time) }
    @Test fun deLaNoche() = with(p("llamar a mamá a las 8 de la noche")) { assertEquals("Llamar a mamá", title); assertEquals(20 * 60, time); assertEquals(t, date) }
    @Test fun mananaEnLaManana() = with(p("correr mañana 6 de la mañana")) { assertEquals(t + 1, date); assertEquals(360, time); assertEquals("Correr", title) }
    @Test fun barra() = with(p("pagar renta 1/12 !!")) { assertEquals(LocalDate.of(2026, 12, 1).toEpochDay(), date); assertEquals(2, priority); assertEquals("Pagar renta", title) }
    @Test fun proximoJueves() = with(p("junta el próximo jueves !baja")) { assertEquals(t + 7, date); assertEquals(0, priority); assertEquals("Junta", title) }

    @Test fun sobrecarga() {
        assertEquals(Target(62.5, 10, true), suggestTarget(listOf(WorkSet(60.0, 10), WorkSet(60.0, 10), WorkSet(60.0, 10))))
        assertEquals(Target(60.0, 10, false), suggestTarget(listOf(WorkSet(60.0, 10), WorkSet(60.0, 8))))
        assertEquals(Target(0.0, 13, true), suggestTarget(listOf(WorkSet(0.0, 12))))
    }

    @Test fun rachaHabitoConGracia() {
        // Hábito creado hace 10 días; cumplido todos menos uno por semana.
        val h = Habit(name = "Agua", createdDay = t - 10, doneDays = ((t - 10)..t).toSet() - (t - 2))
        assertEquals(10, habitStreak(h, t))
        val h2 = Habit(name = "Agua", createdDay = t - 10, doneDays = ((t - 10)..t).toSet() - (t - 2) - (t - 1))
        assertEquals(1, habitStreak(h2, t)) // dos fallos la misma semana rompen la racha
    }
}
