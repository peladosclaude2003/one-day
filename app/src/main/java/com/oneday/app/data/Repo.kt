package com.oneday.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Guarda todos los datos de la app en un archivo JSON dentro del teléfono. */
object Repo {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var file: File? = null
    private val _state = MutableStateFlow(AppData())
    val state: StateFlow<AppData> get() = _state

    fun init(ctx: Context) = synchronized(this) {
        if (file != null) return@synchronized
        val f = File(ctx.applicationContext.filesDir, "data.json")
        file = f
        if (f.exists()) {
            runCatching { json.decodeFromString<AppData>(f.readText()) }
                .onSuccess { d ->
                    // Quien ya usaba la app antes de la bienvenida no tiene por qué verla.
                    val hadData = d.tasks.isNotEmpty() || d.streaks.isNotEmpty() || d.workouts.isNotEmpty()
                    _state.value = if (!d.settings.onboarded && hadData) d.copy(settings = d.settings.copy(onboarded = true)) else d
                }
                .onFailure { f.copyTo(File(f.parentFile, "data-corrupto.json"), overwrite = true) }
        }
    }

    val data: AppData get() = _state.value

    fun update(change: (AppData) -> AppData) = synchronized(this) {
        val next = change(_state.value)
        _state.value = next
        file?.let { f ->
            val tmp = File(f.parentFile, "data.tmp")
            tmp.writeText(json.encodeToString(next))
            if (!tmp.renameTo(f)) { f.writeText(tmp.readText()); tmp.delete() }
        }
    }

    fun exportJson(): String = json.encodeToString(_state.value)

    fun importJson(text: String): Boolean {
        val parsed = runCatching { json.decodeFromString<AppData>(text) }.getOrNull() ?: return false
        update { parsed }
        return true
    }
}
