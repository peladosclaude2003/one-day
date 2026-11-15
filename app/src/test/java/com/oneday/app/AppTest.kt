package com.oneday.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.oneday.app.data.*
import com.oneday.app.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class AppTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun shot(name: String) {
        rule.mainClock.advanceTimeBy(1000)
        val views = Class.forName("android.view.WindowManagerGlobal").let { c ->
            val inst = c.getMethod("getInstance").invoke(null)
            c.getDeclaredField("mViews").apply { isAccessible = true }.get(inst) as List<android.view.View>
        }
        val base = views.first()
        val bmp = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        views.forEachIndexed { i, v ->
            if (i > 0) {
                canvas.drawColor(0x99000000.toInt())
                canvas.save(); canvas.translate((base.width - v.width) / 2f, (base.height - v.height) / 2f)
                v.draw(canvas); canvas.restore()
            } else v.draw(canvas)
        }
        val dir = File(System.getProperty("shots.dir") ?: "build/shots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun nav(desc: String) { rule.onNodeWithContentDescription(desc, useUnmergedTree = false).performClick(); rule.mainClock.advanceTimeBy(800) }
    private fun tap(text: String) { rule.onNodeWithText(text).performClick(); rule.mainClock.advanceTimeBy(800) }

    @Test fun recorreLaApp() {
        val t = today()
        val now = System.currentTimeMillis()
        val rutinas = listOf(
            "Empuje (pecho, hombro, tríceps)" to listOf("ex_banca", "ex_militar", "ex_triceps_polea"),
            "Jalón (espalda, bíceps)" to listOf("ex_dominadas", "ex_remo", "ex_curl_barra"),
            "Pierna" to listOf("ex_sentadilla", "ex_prensa", "ex_femoral"),
        )
        // ~20 entrenamientos en los últimos 30 días, rotando rutinas, con pesos que suben poco a poco.
        val dias = (29 downTo 1).filter { java.time.LocalDate.ofEpochDay(t - it).dayOfWeek.value <= 5 && it % 7 != 3 }
        val workouts = dias.mapIndexed { i, ago ->
            val (name, ex) = rutinas[i % 3]
            val prog = i * 1.25
            Workout(date = t - ago, name = name, startedAt = now - ago * DAY_MS, minutes = 50 + (i * 7) % 25,
                entries = ex.mapIndexed { j, id -> WorkEntry(id, List(3) { k -> WorkSet(40.0 + j * 15 + prog, 12 - k * 2) }) })
        }
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(1000)
        shot("00_bienvenida")
        // Ícono de la app
        run {
            val d = rule.activity.getDrawable(R.mipmap.ic_launcher)!!
            val bmp = Bitmap.createBitmap(432, 432, Bitmap.Config.ARGB_8888)
            d.setBounds(0, 0, 432, 432); d.draw(android.graphics.Canvas(bmp))
            File(File(System.getProperty("shots.dir") ?: "build/shots").apply { mkdirs() }, "00_icono.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        val fumarId = "racha_fumar"
        Repo.update {
            AppData(
                tasks = listOf(
                    Task(title = "Llamar al dentista", date = t, time = 10 * 60, priority = 2, remind = true, area = "Salud"),
                    Task(title = "Comprar proteína", date = t, time = 17 * 60, done = true),
                    Task(title = "Estudiar inglés 30 min", date = t, time = 20 * 60, priority = 1, area = "Personal"),
                    Task(title = "Enviar reporte mensual", date = t + 1, priority = 2, area = "Trabajo"),
                    Task(title = "Pagar luz", date = t - 1, priority = 1),
                    Task(title = "Leer 20 páginas", priority = 0),
                ),
                streaks = listOf(
                    Streak(id = fumarId, name = "Fumar", emoji = "🚭", start = now - 12 * DAY_MS - 5 * 3600_000L, bestDays = 20, resets = 1, reason = "Por mi hija"),
                    Streak(name = "Refresco", emoji = "🥤", start = now - 3 * DAY_MS),
                ),
                workouts = workouts,
                bodyWeights = (0 until 20).map { BodyWeight(t - 38 + it * 2, 74.0 - it * 0.15 + (if (it % 3 == 0) 0.6 else 0.0)) },
                relapses = listOf(Relapse(fumarId, now - 30 * DAY_MS, "Estrés", days = 20), Relapse(fumarId, now - 50 * DAY_MS, "Social o fiesta", days = 6), Relapse(fumarId, now - 60 * DAY_MS, "Estrés", days = 3)),
                settings = Settings(weightGoal = 70.0, gymGoal = 5, onboarded = true, userName = "Alex"),
                habits = listOf(
                    Habit(name = "Tomar 2 L de agua", emoji = "💧", createdDay = t - 20, doneDays = ((t - 20)..(t - 1)).toSet() - (t - 9), remindAt = 10 * 60),
                    Habit(name = "Leer 20 minutos", emoji = "📖", createdDay = t - 6, doneDays = setOf(t - 6, t - 5, t - 3, t - 2, t - 1, t)),
                    Habit(name = "Vasos de agua", emoji = "💧", createdDay = t - 3, target = 8, unit = "vasos", doneDays = setOf(t - 3, t - 2, t - 1), counts = mapOf(t to 5)),
                ),
                cravings = List(9) { i -> Craving(fumarId, now - i * DAY_MS / 2 - (i % 3) * 3600_000L, 2 + i % 4) },
            )
        }
        rule.mainClock.advanceTimeBy(1000)
        shot("01_hoy")
        rule.onRoot().performTouchInput { swipeUp(startY = bottom * 0.85f, endY = top + 50f) }
        rule.mainClock.advanceTimeBy(1500); shot("01b_hoy_abajo")
        nav("Pendientes"); shot("02_pendientes")
        // Deslizar a la derecha = hecho, con barra de Deshacer
        rule.onNodeWithText("Pagar luz").performTouchInput { swipeRight(startX = left + 20f, endX = right - 10f, durationMillis = 300) }
        rule.mainClock.advanceTimeBy(1200); shot("02b_deslizar_deshacer")
        check(Repo.data.tasks.first { it.title == "Pagar luz" }.done) { "Deslizar no marcó la tarea" }
        tap("Deshacer")
        check(!Repo.data.tasks.first { it.title == "Pagar luz" }.done) { "Deshacer no funcionó" }
        rule.onNodeWithText("Llamar al dentista").performTouchInput { swipeLeft(startX = right - 20f, endX = left + 10f, durationMillis = 300) }
        rule.mainClock.advanceTimeBy(1200)
        check(Repo.data.tasks.first { it.title == "Llamar al dentista" }.date == t + 1) { "Deslizar a la izquierda no pasó a mañana" }
        tap("Trabajo · 1"); shot("02c_area_trabajo")
        rule.onAllNodes(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Checkbox))[0].performClick()
        rule.mainClock.advanceTimeBy(800)
        check(Repo.data.tasks.first { it.title == "Enviar reporte mensual" }.done) { "No se marcó la tarea" }
        UiState.undo = null
        nav("Rachas"); shot("03_rachas")
        tap("Construir"); shot("03b_habitos")
        rule.onRoot().performTouchInput { swipeUp(startY = bottom * 0.9f, endY = bottom * 0.2f) }
        rule.mainClock.advanceTimeBy(1500); shot("03b2_habito_cantidad")
        tap("Sumar 1")
        check(Repo.data.habits.first { it.target != null }.countToday() == 6) { "No sumó al hábito" }
        rule.onRoot().performTouchInput { swipeDown(startY = bottom * 0.2f, endY = bottom * 0.9f) }
        rule.mainClock.advanceTimeBy(1500)
        tap("Marcar hecho hoy")
        check(today() in Repo.data.habits.first().doneDays) { "No se marcó el hábito" }
        tap("Dejar")
        rule.onAllNodesWithText("Tengo ganas")[0].performClick(); rule.mainClock.advanceTimeBy(2500); shot("03c_sos_respira")
        tap("Ya estoy mejor"); shot("03d_sos_califica")
        tap("4"); shot("03e_sos_listo")
        tap("Volver")
        check(Repo.data.cravings.size == 10) { "No se guardó el antojo" }
        nav("Gym"); shot("04_gym_resumen")
        rule.onRoot().performTouchInput { swipeUp(startY = bottom * 0.9f, endY = bottom * 0.15f) }
        rule.mainClock.advanceTimeBy(1500); shot("05_gym_resumen_abajo")
        rule.onRoot().performTouchInput { swipeUp(startY = bottom * 0.8f, endY = bottom * 0.45f) }
        rule.mainClock.advanceTimeBy(1500); shot("05b_gym_musculos")
        tap("Historial"); shot("06_historial")
        tap("Progreso"); shot("07_progreso")
        tap("Rutinas"); shot("08_rutinas")
        tap("Resumen")
        tap("Empezar entrenamiento"); shot("09_entrenando")
        Repo.update { d -> d.copy(activeWorkout = d.activeWorkout!!.let { w ->
            w.copy(entries = w.entries.mapIndexed { i, e -> if (i == 0) e.copy(sets = listOf(WorkSet(40.0, 10, SET_WARMUP), WorkSet(60.0, 10, rpe = 7), WorkSet(150.0, 6, SET_FAILURE, rpe = 10))) else e } +
                WorkEntry("ex_plancha", listOf(WorkSet(0.0, 0, seconds = 75))))
        }) }
        shot("10_entrenando_series")
        tap("Terminar entrenamiento")
        check(Repo.data.activeWorkout == null && Repo.data.workouts.size == workouts.size + 1) { "No se guardó el entrenamiento" }
        nav("Hoy"); shot("11_hoy_final")
    }
}
