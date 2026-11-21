package com.oneday.app

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.oneday.app.data.*
import com.oneday.app.ui.*
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ShareRenderTest {
    @Test fun dibujaImagenes() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val spec = ShareSpec("90", "DÍAS", Tier.Pleno, "90 días\nsin fumar", "Por mi hija", listOf("RÉCORD" to "90 d", "ANTOJOS SUPERADOS" to "41", "FECHA" to "21 nov 2026"))
        val story = renderShare(ctx, spec, square = false, withQuote = true)
        val sq = renderShare(ctx, ShareSpec("PR", "RÉCORD", Tier.Naranja, "Récord en\npress de banca", null, listOf("SERIE" to "65 kg × 6", "1RM ESTIMADO" to "78 kg", "FECHA" to "21 nov 2026")), square = true, withQuote = false)
        assertEquals(1920, story.height); assertEquals(1080, sq.height)
        File("build/shots").mkdirs()
        File("build/shots/13_compartir_historia.png").outputStream().use { story.compress(Bitmap.CompressFormat.PNG, 90, it) }
        File("build/shots/13b_compartir_cuadrado.png").outputStream().use { sq.compress(Bitmap.CompressFormat.PNG, 90, it) }
    }

    @Test fun progresionDeRutina() {
        val t = today()
        val d = AppData(workouts = listOf(Workout(date = t - 2, name = "x", entries = listOf(WorkEntry("ex_banca", listOf(WorkSet(60.0, 10), WorkSet(60.0, 10), WorkSet(60.0, 10)))))))
        // Llegó al máximo (10) en todas: sube el peso y vuelve al mínimo
        assertEquals(List(4) { WorkSet(62.5, 8, done = false) }, plannedFor(d, RoutineItem("ex_banca", sets = 4, repsMin = 8, repsMax = 10)))
        // Rango hasta 12: mismo peso, una repetición más
        assertEquals(List(3) { WorkSet(60.0, 11, done = false) }, plannedFor(d, RoutineItem("ex_banca", sets = 3, repsMin = 8, repsMax = 12)))
    }
}
