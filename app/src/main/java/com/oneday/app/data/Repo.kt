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
                    var m = if (!d.settings.onboarded && hadData) d.copy(settings = d.settings.copy(onboarded = true)) else d
                    // v1.3: plancha y caminadora pasan a ser de tiempo / distancia.
                    m = m.copy(exercises = m.exercises.map {
                        when {
                            it.id == "ex_plancha" && it.kind == 0 -> it.copy(kind = 1, name = it.name.replace(" (segundos)", ""))
                            it.id == "ex_caminadora" && it.kind == 0 -> it.copy(kind = 2, name = it.name.replace(" (minutos)", ""))
                            else -> it
                        }
                    } + Defaults.exercises.filter { def -> def.id == "ex_bici" && m.exercises.none { it.id == def.id } })
                    // v1.4: los emojis guardados pasan a ser íconos propios.
                    m = m.copy(
                        streaks = m.streaks.map { it.copy(emoji = com.oneday.app.ui.Glyphs.keyFor(it.emoji, "racha")) },
                        habits = m.habits.map { it.copy(emoji = com.oneday.app.ui.Glyphs.keyFor(it.emoji, "hecho")) },
                    )
                    _state.value = m
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
