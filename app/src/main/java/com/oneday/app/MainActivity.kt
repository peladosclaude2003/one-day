package com.oneday.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oneday.app.data.Repo
import com.oneday.app.notify.Notifier
import com.oneday.app.ui.*

class MainActivity : ComponentActivity() {
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Repo.init(this)
        Notifier.createChannel(this)
        if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)

        setContent {
            OneDayTheme {
                val data by Repo.state.collectAsStateWithLifecycle()
                var tab by rememberSaveable { mutableIntStateOf(0) }
                var profile by rememberSaveable { mutableStateOf(false) }
                // Inicio animado: solo al abrir la app en frío y con animaciones activas
                var splash by rememberSaveable { mutableStateOf(savedInstanceState == null && data.settings.animationsOn) }
                val rise by androidx.compose.animation.core.animateFloatAsState(if (splash) 1f else 0f, androidx.compose.animation.core.tween(420), label = "rise")

                // Cada vez que cambian tareas, rachas o ajustes, se reprograman los recordatorios.
                LaunchedEffect(data.tasks, data.settings, data.streaks.size, data.habits, data.cravings.size) { Notifier.rescheduleAll(this@MainActivity) }
                // Medallas nuevas de rachas y hábitos (al abrir y cada minuto)
                val nowTick = rememberNow()
                LaunchedEffect(nowTick, data.habits) { checkNewMedals() }
                // Pasos: al abrir, cada ~3 min con la app abierta y cuando se pide
                val hasSteps = data.habits.any { it.source == "steps" }
                LaunchedEffect(nowTick / 180_000, UiState.syncSteps, hasSteps) {
                    if (hasSteps) runCatching { com.oneday.app.steps.Steps.sync(this@MainActivity) }
                }
                val view = LocalView.current
                DisposableEffect(view) { Fx.view = view; onDispose { Fx.view = null } }

                // Bienvenida: al instalar (sin datos) o cuando la pides desde Ajustes.
                if (!data.settings.onboarded) {
                    Onboarding { tab = 0 }
                    return@OneDayTheme
                }

                Box(Modifier.fillMaxSize().background(Bg)) {
                    Box(Modifier.fillMaxSize().blur(backgroundBlur()).statusBarsPadding().navigationBarsPadding()
                        .graphicsLayer { translationY = rise * 60.dp.toPx(); alpha = 1f - rise }) {
                        // Las pestañas se deslizan hacia el lado de la pestaña elegida
                        AnimatedContent(
                            targetState = tab,
                            transitionSpec = {
                                if (!data.settings.animationsOn) (fadeIn(androidx.compose.animation.core.snap()) togetherWith fadeOut(androidx.compose.animation.core.snap()))
                                else {
                                    val dir = if (targetState > initialState) 1 else -1
                                    (slideInHorizontally(androidx.compose.animation.core.tween(280)) { it / 4 * dir } + fadeIn(androidx.compose.animation.core.tween(280))) togetherWith
                                        (slideOutHorizontally(androidx.compose.animation.core.tween(220)) { -it / 4 * dir } + fadeOut(androidx.compose.animation.core.tween(180)))
                                }
                            }, label = "tabs",
                        ) { t ->
                            when (t) {
                                0 -> TodayScreen(data, onOpenSettings = { profile = true }, onGoTab = { tab = it })
                                1 -> TasksScreen(data)
                                2 -> StreaksScreen(data)
                                else -> GymScreen(data)
                            }
                        }
                    }
                    UndoBar(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 92.dp))
                    MedalToast(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp))
                    AnimatedVisibility(
                        visible = !(UiState.hideNav && tab == 3) && UiState.sosStreak == null && !profile,
                        enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it },
                        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp),
                    ) {
                        FloatingNav(tab, { tab = it })
                    }
                }
                UiState.sosStreak?.let { id -> SosOverlay(id) { UiState.sosStreak = null } }
                UiState.share?.let { spec -> ShareSheet(spec) { UiState.share = null } }
                AnimatedVisibility(splash, exit = fadeOut(androidx.compose.animation.core.tween(220))) { SplashOverlay { splash = false } }
                AnimatedVisibility(profile, enter = slideInHorizontally { it } + fadeIn(), exit = slideOutHorizontally { it } + fadeOut()) {
                    ProfileScreen(data) { profile = false }
                }
            }
        }
    }
}
