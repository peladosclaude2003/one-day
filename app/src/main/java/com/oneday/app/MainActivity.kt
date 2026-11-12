package com.oneday.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
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
                LaunchedEffect(data.tasks, data.settings, data.streaks.size) { Notifier.rescheduleAll(this@MainActivity) }

                Scaffold(
                    containerColor = Bg,
                    bottomBar = {
                        NavigationBar(containerColor = CardBg) {
                            listOf("📅" to "Hoy", "✅" to "Pendientes", "🔥" to "Rachas", "🏋️" to "Gym").forEachIndexed { i, (icon, label) ->
                                NavigationBarItem(
                                    selected = tab == i, onClick = { tab = i },
                                    icon = { Text(icon, fontSize = 20.sp) }, label = { Text(label) },
                                    colors = NavigationBarItemDefaults.colors(indicatorColor = Accent.copy(alpha = 0.25f), selectedTextColor = Accent),
                                )
                            }
                        }
                    },
                ) { pad ->
                    Box(Modifier.padding(pad).consumeWindowInsets(pad)) {
                        when (tab) {
                            0 -> TodayScreen(data, onOpenSettings = { settings = true }, onGoTab = { tab = it })
                            1 -> TasksScreen(data)
                            2 -> StreaksScreen(data)
                            else -> GymScreen(data)
                        }
                    }
                }
                if (settings) SettingsDialog(data) { settings = false }
            }
        }
    }
}
