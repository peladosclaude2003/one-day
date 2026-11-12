package com.oneday.app

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.oneday.app.data.*
import com.oneday.app.ui.*
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
class MuscleMapTest {
    @get:Rule val rule = createComposeRule()

    @Test fun mapa() {
        val t = today()
        val data = AppData(workouts = listOf(
            Workout(date = t, name = "Mix", entries = listOf(
                WorkEntry("ex_banca", List(10) { WorkSet(60.0, 8) }),
                WorkEntry("ex_remo", List(6) { WorkSet(50.0, 10) }),
                WorkEntry("ex_sentadilla", List(14) { WorkSet(80.0, 8) }),
                WorkEntry("ex_militar", List(4) { WorkSet(30.0, 10) }),
                WorkEntry("ex_curl_barra", List(2) { WorkSet(20.0, 12) }),
                WorkEntry("ex_frances", List(5) { WorkSet(20.0, 12) }),
                WorkEntry("ex_crunch", List(8) { WorkSet(0.0, 20) }),
            ))))
        rule.setContent { OneDayTheme { Box(Modifier.fillMaxSize().background(Bg).padding(16.dp)) { MuscleCard(data) } } }
        rule.waitForIdle()
        val v = Class.forName("android.view.WindowManagerGlobal").let { c ->
            val inst = c.getMethod("getInstance").invoke(null)
            (c.getDeclaredField("mViews").apply { isAccessible = true }.get(inst) as List<android.view.View>).first()
        }
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(android.graphics.Canvas(bmp))
        File("build/shots").mkdirs()
        File("build/shots/12_mapa_muscular.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
