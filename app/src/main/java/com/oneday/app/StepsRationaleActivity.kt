package com.oneday.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneday.app.ui.*

/** Pantalla que Health Connect muestra cuando preguntas por qué One Day lee tus pasos. */
class StepsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OneDayTheme {
                Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Glyph("pasos", Accent, 40.dp)
                    Text("Tus pasos en One Day", fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Ink)
                    Text("One Day solo LEE el total de pasos de cada día para llenar tu hábito de caminar y tu meta diaria.", color = Ink2, style = MaterialTheme.typography.bodyLarge)
                    Text("No escribe nada en Health Connect, no comparte tus datos y todo se guarda únicamente en tu teléfono. Puedes quitar el permiso cuando quieras desde los ajustes de Health Connect.", color = Muted, style = MaterialTheme.typography.bodyMedium)
                    PrimaryButton("Entendido") { finish() }
                }
            }
        }
    }
}
