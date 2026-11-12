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
)

/** Racha: días sin un vicio. `start` = momento (millis) desde el que se cuenta. */
@Serializable
data class Streak(
    val id: String = newId(),
    val name: String,
    val emoji: String = "🚭",
    val start: Long,
    val bestDays: Long = 0,
    val resets: Int = 0,
)

@Serializable
data class Exercise(val id: String = newId(), val name: String, val muscle: String)

@Serializable
data class Routine(val id: String = newId(), val name: String, val exerciseIds: List<String>)

@Serializable
data class WorkSet(val weight: Double, val reps: Int)

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
)

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
        ex("ex_plancha", "Plancha (segundos)", "Abdomen"),
        ex("ex_crunch", "Crunch", "Abdomen"),
        ex("ex_caminadora", "Caminadora (minutos)", "Cardio"),
    )
    val routines = listOf(
        Routine("r_push", "Empuje (pecho, hombro, tríceps)", listOf("ex_banca", "ex_inclinado", "ex_militar", "ex_laterales", "ex_triceps_polea")),
        Routine("r_pull", "Jalón (espalda, bíceps)", listOf("ex_dominadas", "ex_remo", "ex_jalon", "ex_face_pull", "ex_curl_barra")),
        Routine("r_legs", "Pierna", listOf("ex_sentadilla", "ex_prensa", "ex_femoral", "ex_extension", "ex_pantorrilla")),
    )
}
