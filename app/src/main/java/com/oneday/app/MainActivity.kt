package com.oneday.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
                var settings by remember { mutableStateOf(false) }

                // Cada vez que cambian tareas, rachas o ajustes, se reprograman los recordatorios.
                LaunchedEffect(data.tasks, data.settings, data.streaks.size, data.habits, data.cravings.size) { Notifier.rescheduleAll(this@MainActivity) }
                val view = LocalView.current
                DisposableEffect(view) { Fx.view = view; onDispose { Fx.view = null } }

                // Bienvenida: al instalar (sin datos) o cuando la pides desde Ajustes.
                if (!data.settings.onboarded) {
                    Onboarding { tab = 0 }
                    return@OneDayTheme
                }

                Box(Modifier.fillMaxSize().background(Bg)) {
                    Box(Modifier.fillMaxSize().blur(backgroundBlur()).statusBarsPadding().navigationBarsPadding()) {
                        when (tab) {
                            0 -> TodayScreen(data, onOpenSettings = { settings = true }, onGoTab = { tab = it })
                            1 -> TasksScreen(data)
                            2 -> StreaksScreen(data)
                            else -> GymScreen(data)
                        }
                    }
                    AnimatedVisibility(
                        visible = !(UiState.hideNav && tab == 3) && UiState.sosStreak == null,
                        enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it },
                        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp),
                    ) {
                        FloatingNav(tab, { tab = it })
                    }
                }
                UiState.sosStreak?.let { id -> SosOverlay(id) { UiState.sosStreak = null } }
                if (settings) SettingsDialog(data) { settings = false }
            }
        }
    }
}
