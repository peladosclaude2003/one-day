package com.oneday.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneday.app.data.*

private val QUIT = listOf("🚭" to "Fumar", "🍺" to "Alcohol", "🥤" to "Refresco", "📱" to "Redes sociales", "🍬" to "Azúcar")
private val BUILD = listOf("💧" to "Tomar 2 L de agua", "📖" to "Leer 20 minutos", "🧘" to "Meditar", "🚶" to "Caminar 30 minutos", "😴" to "Dormir 8 horas")

/** Bienvenida de 3 pasos: nombre, gimnasio y qué quieres cambiar. */
@Composable
fun Onboarding(onDone: () -> Unit) {
    val s0 = Repo.data.settings
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(s0.userName) }
    var days by remember { mutableStateOf(s0.gymDays) }
    var goal by rememberSaveable { mutableIntStateOf(s0.gymGoal) }
    val quit = remember { mutableStateListOf<String>() }
    val build = remember { mutableStateListOf<String>() }

    fun finish() {
        val now = System.currentTimeMillis()
        Repo.update { d ->
            d.copy(
                settings = d.settings.copy(userName = name.trim(), gymDays = days, gymGoal = goal, gymOn = days.isNotEmpty() || d.settings.gymOn, onboarded = true),
                streaks = d.streaks + QUIT.filter { it.second in quit && d.streaks.none { s -> s.name == it.second } }.map { Streak(name = it.second, emoji = it.first, start = now) },
                habits = d.habits + BUILD.filter { it.second in build && d.habits.none { h -> h.name == it.second } }.map { Habit(name = it.second, emoji = it.first, createdDay = today()) },
            )
        }
        Fx.success()
        onDone()
    }

    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding().imePadding().padding(24.dp)) {
        // Progreso: 3 píldoras, la actual en naranja
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
            (0..2).forEach { i ->
                Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50)).background(when { i == step -> Accent; i < step -> Ink2; else -> CardHigh }))
            }
        }
        Spacer(Modifier.height(32.dp))
        AnimatedContent(
            targetState = step, modifier = Modifier.weight(1f),
            transitionSpec = { (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut()) }, label = "onb",
        ) { st ->
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when (st) {
                    0 -> {
                        Text("Bienvenido a", color = Muted, style = MaterialTheme.typography.titleMedium)
                        DashedBox(Modifier.padding(top = 8.dp)) { Text("One Day", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Ink) }
                        Spacer(Modifier.height(16.dp))
                        Text("Organiza tu día, deja lo que te hace daño y mide tu progreso en el gym. Un día a la vez.", color = Ink2, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(28.dp))
                        OutlinedTextField(name, { name = it }, label = { Text("¿Cómo te llamas?") }, singleLine = true, shape = RoundedCornerShape(20.dp),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth())
                        Text("Lo usamos solo para saludarte.", color = Faint, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
                    }
                    1 -> {
                        Text("Tu gimnasio", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("¿Qué días piensas entrenar? Te aviso esos días si aún no has ido.", color = Ink2)
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("L", "M", "X", "J", "V", "S", "D").forEachIndexed { i, l ->
                                val d = i + 1
                                ChoicePill(l, d in days, Modifier.weight(1f)) { days = if (d in days) days - d else days + d }
                            }
                        }
                        Spacer(Modifier.height(28.dp))
                        Text("Meta: $goal día(s) por semana", color = Ink, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..7).forEach { n -> ChoicePill("$n", goal == n, Modifier.weight(1f)) { goal = n } }
                        }
                    }
                    else -> {
                        Text("¿Qué quieres cambiar?", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Elige las que quieras; puedes agregar más después.", color = Ink2)
                        Spacer(Modifier.height(20.dp))
                        Text("DEJAR", color = Faint, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.2.sp)
                        Spacer(Modifier.height(8.dp))
                        QUIT.forEach { (e, n) -> ChoicePill("$e  $n", n in quit, Modifier.fillMaxWidth().padding(vertical = 3.dp)) { if (n in quit) quit.remove(n) else quit.add(n) } }
                        Spacer(Modifier.height(18.dp))
                        Text("CONSTRUIR", color = Faint, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.2.sp)
                        Spacer(Modifier.height(8.dp))
                        BUILD.forEach { (e, n) -> ChoicePill("$e  $n", n in build, Modifier.fillMaxWidth().padding(vertical = 3.dp)) { if (n in build) build.remove(n) else build.add(n) } }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(if (step < 2) "Siguiente" else "Empezar") { Fx.tick(); if (step < 2) step++ else finish() }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { if (step > 0) step-- }, enabled = step > 0) { Text(if (step > 0) "Atrás" else "", color = Muted) }
            TextButton(onClick = { finish() }) { Text("Saltar", color = Muted) }
        }
    }
}
