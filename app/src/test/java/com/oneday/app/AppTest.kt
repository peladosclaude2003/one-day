package com.oneday.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.oneday.app.data.*
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

    @Test fun recorreLaApp() {
        val t = today()
        val now = System.currentTimeMillis()
        Repo.update {
            AppData(
                tasks = listOf(
                    Task(title = "Llamar al dentista", date = t, time = 10 * 60, priority = 2, remind = true),
                    Task(title = "Comprar proteína", date = t, time = 17 * 60, done = true),
                    Task(title = "Pagar luz", date = t - 1, priority = 1),
                    Task(title = "Leer 20 páginas", priority = 0),
                ),
                streaks = listOf(
                    Streak(name = "Fumar", emoji = "🚭", start = now - 12 * DAY_MS - 5 * 3600_000L, bestDays = 20, resets = 1),
                    Streak(name = "Refresco", emoji = "🥤", start = now - 3 * DAY_MS),
                ),
                workouts = (0 until 6).map { i ->
                    Workout(date = t - 12 + i * 2, name = "Empuje (pecho, hombro, tríceps)", startedAt = now - (12 - i * 2) * DAY_MS, minutes = 55,
                        entries = listOf(WorkEntry("ex_banca", listOf(WorkSet(60.0 + i * 2.5, 10), WorkSet(60.0 + i * 2.5, 8)))))
                },
            )
        }
        rule.mainClock.autoAdvance = false
        shot("1_hoy")
        rule.onNodeWithText("Pendientes").performClick(); shot("2_pendientes")
        rule.onAllNodes(isToggleable())[0].performClick() // marcar "Pagar luz" como hecho
        shot("3_pendientes_marcado")
        check(Repo.data.tasks.first { it.title == "Pagar luz" }.done) { "No se marcó la tarea" }
        rule.onNodeWithText("Rachas").performClick(); shot("4_rachas")
        rule.onNodeWithText("Gym").performClick(); shot("5_gym")
        rule.onNodeWithText("Historial").performClick(); shot("6_historial")
        rule.onNodeWithText("Progreso").performClick(); shot("7_progreso")
        rule.onNodeWithText("Rutinas").performClick(); shot("8_rutinas")
        rule.onNodeWithText("Entrenar").performClick(); rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithText("Pierna").performClick(); shot("9_entrenando")
        Repo.update { d -> d.copy(activeWorkout = d.activeWorkout!!.let { w ->
            w.copy(entries = w.entries.mapIndexed { i, e -> if (i == 0) e.copy(sets = listOf(WorkSet(80.0, 8), WorkSet(85.0, 6))) else e })
        }) }
        shot("10_entrenando_series")
        rule.onNodeWithText("Terminar ✔").performClick()
        rule.onNodeWithText("Hoy").performClick(); shot("11_hoy_final")
        check(Repo.data.workouts.any { it.name == "Pierna" }) { "No se guardó el entrenamiento" }
    }
}
