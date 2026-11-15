package com.oneday.app.data

import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

/** Tarea o pendiente. `date` = día (epochDay), `time` = minutos desde medianoche. */
@Serializable
data class Task(
    val id: String = newId(),
    val title: String,
    val notes: String = "",
    val date: Long? = null,
    val time: Int? = null,
    val priority: Int = 1, // 0 baja, 1 media, 2 alta
    val done: Boolean = false,
    val remind: Boolean = false,
    val doneAt: Long? = null,
    val area: String? = null,
)

/** Racha: días sin un vicio. `start` = momento (millis) desde el que se cuenta. */
@Serializable
data class Streak(
    val id: String = newId(),
    val name: String,
    val emoji: String = "fumar", // clave del ícono (antes guardaba un emoji)
    val start: Long,
    val bestDays: Long = 0,
    val resets: Int = 0,
    val reason: String = "", // "Tu motivo"
)

/** Recaída registrada al reiniciar una racha. */
@Serializable
data class Relapse(val streakId: String, val at: Long, val trigger: String, val note: String = "", val days: Long = 0)

val TRIGGERS = listOf("Estrés", "Social o fiesta", "Aburrimiento", "Cansancio", "Después de comer", "Otro")

/** kind: 0 = peso × repeticiones · 1 = solo tiempo · 2 = tiempo y distancia. `note` = nota fija del ejercicio. */
@Serializable
data class Exercise(val id: String = newId(), val name: String, val muscle: String, val kind: Int = 0, val note: String = "")

const val SET_NORMAL = 0
const val SET_WARMUP = 1
const val SET_FAILURE = 2
const val SET_DROP = 3

@Serializable
data class Routine(val id: String = newId(), val name: String, val exerciseIds: List<String>)

/** kind = tipo de serie (normal, calentamiento, al fallo, drop). rpe = esfuerzo 1–10. seconds/km para ejercicios de tiempo o distancia. */
@Serializable
data class WorkSet(
    val weight: Double,
    val reps: Int,
    val kind: Int = SET_NORMAL,
    val rpe: Int? = null,
    val seconds: Int? = null,
    val km: Double? = null,
    val done: Boolean = true, // false = serie planeada que aún no haces
)

@Serializable
data class WorkEntry(val exerciseId: String, val sets: List<WorkSet> = emptyList())

@Serializable
data class Workout(
    val id: String = newId(),
    val date: Long, // epochDay
    val name: String,
    val entries: List<WorkEntry> = emptyList(),
    val startedAt: Long = System.currentTimeMillis(),
    val minutes: Int = 0,
    val note: String = "",
)

/** Hábito por construir: se marca una vez al día. `doneDays` = días (epochDay) cumplidos. */
@Serializable
data class Habit(
    val id: String = newId(),
    val name: String,
    val emoji: String = "agua", // clave del ícono (antes guardaba un emoji)
    val createdDay: Long,
    val doneDays: Set<Long> = emptySet(),
    val remindAt: Int? = null,
    val target: Int? = null,          // meta diaria (null = sí/no)
    val unit: String = "",            // vasos, min, páginas…
    val step: Int = 1,                // cuánto suma cada toque
    val counts: Map<Long, Int> = emptyMap(), // día → cantidad
)

/** Antojo superado con el botón SOS. */
@Serializable
data class Craving(val streakId: String, val at: Long, val intensity: Int)

@Serializable
data class BodyWeight(val date: Long, val kg: Double)

@Serializable
data class Settings(
    val summaryOn: Boolean = true,
    val summaryTime: Int = 8 * 60,
    val gymOn: Boolean = false,
    val gymTime: Int = 18 * 60,
    val gymDays: Set<Int> = setOf(1, 2, 3, 4, 5), // DayOfWeek: 1 = lunes … 7 = domingo
    val milestonesOn: Boolean = true,
    val gymGoal: Int = 4, // días por semana
    val weightGoal: Double? = null,
    val userName: String = "",
    val onboarded: Boolean = false,
    // Microinteracciones: todo se puede apagar.
    val animationsOn: Boolean = true,
    val rollingNumbersOn: Boolean = true,
    val celebrationsOn: Boolean = true,
    val hapticsOn: Boolean = true,
    val hapticStrong: Boolean = false,
    val cravingAlertOn: Boolean = false,
    val areas: List<String> = listOf("Trabajo", "Casa", "Salud", "Personal"),
    val barKg: Double = 20.0,
    val plates: List<Double> = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25),
    val seenMedals: Set<String> = emptySet(),
)

@Serializable
data class AppData(
    val tasks: List<Task> = emptyList(),
    val streaks: List<Streak> = emptyList(),
    val exercises: List<Exercise> = Defaults.exercises,
    val routines: List<Routine> = Defaults.routines,
    val workouts: List<Workout> = emptyList(),
    val activeWorkout: Workout? = null,
    val bodyWeights: List<BodyWeight> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val cravings: List<Craving> = emptyList(),
    val relapses: List<Relapse> = emptyList(),
    val settings: Settings = Settings(),
)

val MUSCLES = listOf("Pecho", "Espalda", "Pierna", "Hombro", "Bíceps", "Tríceps", "Abdomen", "Cardio", "Otro")

object Defaults {
    private fun ex(id: String, name: String, muscle: String) = Exercise(id, name, muscle)
    val exercises = listOf(
        ex("ex_banca", "Press de banca", "Pecho"),
        ex("ex_inclinado", "Press inclinado con mancuernas", "Pecho"),
        ex("ex_aperturas", "Aperturas", "Pecho"),
        ex("ex_fondos", "Fondos", "Pecho"),
        ex("ex_dominadas", "Dominadas", "Espalda"),
        ex("ex_jalon", "Jalón al pecho", "Espalda"),
        ex("ex_remo", "Remo con barra", "Espalda"),
        ex("ex_remo_polea", "Remo en polea", "Espalda"),
        ex("ex_muerto", "Peso muerto", "Espalda"),
        ex("ex_sentadilla", "Sentadilla", "Pierna"),
        ex("ex_prensa", "Prensa", "Pierna"),
        ex("ex_extension", "Extensión de cuádriceps", "Pierna"),
        ex("ex_femoral", "Curl femoral", "Pierna"),
        ex("ex_zancadas", "Zancadas", "Pierna"),
        ex("ex_pantorrilla", "Elevación de pantorrilla", "Pierna"),
        ex("ex_militar", "Press militar", "Hombro"),
        ex("ex_laterales", "Elevaciones laterales", "Hombro"),
        ex("ex_face_pull", "Face pull", "Hombro"),
        ex("ex_curl_barra", "Curl con barra", "Bíceps"),
        ex("ex_martillo", "Curl martillo", "Bíceps"),
        ex("ex_triceps_polea", "Extensión de tríceps en polea", "Tríceps"),
        ex("ex_frances", "Press francés", "Tríceps"),
        Exercise("ex_plancha", "Plancha", "Abdomen", kind = 1),
        ex("ex_crunch", "Crunch", "Abdomen"),
        Exercise("ex_caminadora", "Caminadora", "Cardio", kind = 2),
        Exercise("ex_bici", "Bicicleta", "Cardio", kind = 2),
    )
    val routines = listOf(
        Routine("r_push", "Empuje (pecho, hombro, tríceps)", listOf("ex_banca", "ex_inclinado", "ex_militar", "ex_laterales", "ex_triceps_polea")),
        Routine("r_pull", "Jalón (espalda, bíceps)", listOf("ex_dominadas", "ex_remo", "ex_jalon", "ex_face_pull", "ex_curl_barra")),
        Routine("r_legs", "Pierna", listOf("ex_sentadilla", "ex_prensa", "ex_femoral", "ex_extension", "ex_pantorrilla")),
    )
}
