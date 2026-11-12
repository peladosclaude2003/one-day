package com.oneday.app.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.oneday.app.MainActivity
import com.oneday.app.R
import com.oneday.app.data.Repo
import com.oneday.app.data.atMillis
import com.oneday.app.data.MILESTONES
import com.oneday.app.data.streakDays
import com.oneday.app.data.today
import com.oneday.app.data.habitStreak
import com.oneday.app.data.cravingAlertTime
import com.oneday.app.data.bucketLabel
import java.time.LocalDate

object Notifier {
    private const val CHANNEL = "one_day_reminders"
    private const val REQ_SUMMARY = 1001
    private const val REQ_GYM = 1002
    private const val REQ_MILESTONE = 1003
    private const val REQ_CRAVING = 1004
    const val MILESTONE_CHECK_TIME = 9 * 60

    fun createChannel(ctx: Context) {
        val ch = NotificationChannel(CHANNEL, "Recordatorios", NotificationManager.IMPORTANCE_HIGH)
        ch.description = "Tareas, gimnasio y logros de rachas"
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    fun show(ctx: Context, id: Int, title: String, text: String) {
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(id, n) }
    }

    private fun intent(ctx: Context, type: String, id: String? = null) =
        Intent(ctx, AlarmReceiver::class.java).setAction("oneday.$type").putExtra("type", type).putExtra("id", id)

    private fun pending(ctx: Context, req: Int, i: Intent) =
        PendingIntent.getBroadcast(ctx, req, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun set(ctx: Context, at: Long, pi: PendingIntent) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    private fun cancel(ctx: Context, req: Int, i: Intent) =
        ctx.getSystemService(AlarmManager::class.java).cancel(pending(ctx, req, i))

    /** Próximo momento (millis) para una hora diaria. */
    private fun nextDaily(minutes: Int): Long {
        val now = System.currentTimeMillis()
        val t = atMillis(today(), minutes)
        return if (t > now + 5_000) t else atMillis(today() + 1, minutes)
    }

    /** Vuelve a programar todas las alarmas según los datos actuales. */
    fun rescheduleAll(ctx: Context) {
        Repo.init(ctx)
        val d = Repo.data
        val s = d.settings
        val now = System.currentTimeMillis()
        for (t in d.tasks) {
            val i = intent(ctx, "task", t.id)
            val req = t.id.hashCode()
            if (t.remind && !t.done && t.date != null && t.time != null) {
                val at = atMillis(t.date, t.time)
                if (at > now) set(ctx, at, pending(ctx, req, i)) else cancel(ctx, req, i)
            } else cancel(ctx, req, i)
        }
        scheduleDaily(ctx, "summary", REQ_SUMMARY, s.summaryOn, s.summaryTime)
        scheduleDaily(ctx, "gym", REQ_GYM, s.gymOn && s.gymDays.isNotEmpty(), s.gymTime)
        scheduleDaily(ctx, "milestone", REQ_MILESTONE, s.milestonesOn && d.streaks.isNotEmpty(), MILESTONE_CHECK_TIME)
        val alert = cravingAlertTime(d.cravings)
        scheduleDaily(ctx, "craving", REQ_CRAVING, s.cravingAlertOn && alert != null, alert ?: 0)
        for (h in d.habits) {
            val i = intent(ctx, "habit", h.id)
            val req = ("h" + h.id).hashCode()
            if (h.remindAt != null) set(ctx, nextDaily(h.remindAt), pending(ctx, req, i)) else cancel(ctx, req, i)
        }
    }

    private fun scheduleDaily(ctx: Context, type: String, req: Int, on: Boolean, minutes: Int) {
        val i = intent(ctx, type)
        if (on) set(ctx, nextDaily(minutes), pending(ctx, req, i)) else cancel(ctx, req, i)
    }

    fun handle(ctx: Context, type: String?, id: String?) {
        Repo.init(ctx)
        val d = Repo.data
        val t = today()
        when (type) {
            "task" -> {
                val task = d.tasks.firstOrNull { it.id == id } ?: return
                if (!task.done) show(ctx, task.id.hashCode(), "⏰ ${task.title}", task.notes.ifBlank { "Es hora de tu pendiente." })
            }
            "summary" -> {
                val todays = d.tasks.filter { !it.done && it.date == t }
                val overdue = d.tasks.count { !it.done && it.date != null && it.date < t }
                if (todays.isNotEmpty() || overdue > 0) {
                    val list = todays.take(4).joinToString("\n") { "• ${it.title}" }
                    val extra = if (overdue > 0) "\n⚠️ $overdue pendiente(s) atrasado(s)" else ""
                    show(ctx, REQ_SUMMARY, "☀️ Tu día: ${todays.size} pendiente(s) para hoy", (list + extra).trim())
                } else {
                    show(ctx, REQ_SUMMARY, "☀️ Buen día", "No tienes pendientes para hoy. ¡Aprovecha para planear tu día!")
                }
            }
            "gym" -> {
                val dow = LocalDate.now().dayOfWeek.value
                val trainedToday = d.workouts.any { it.date == t }
                if (dow in d.settings.gymDays && !trainedToday)
                    show(ctx, REQ_GYM, "🏋️ Hora del gimnasio", "Hoy toca entrenar. ¡Un día a la vez!")
            }
            "habit" -> {
                val h = d.habits.firstOrNull { it.id == id } ?: return
                if (t !in h.doneDays) {
                    val streak = habitStreak(h)
                    show(ctx, ("h" + h.id).hashCode(), "${h.emoji} ${h.name}",
                        if (streak > 0) "Llevas $streak día(s) seguidos. Márcalo hoy para no romper la racha." else "Hoy es buen día para empezar.")
                }
            }
            "craving" -> {
                val alert = cravingAlertTime(d.cravings) ?: return
                val peak = (alert + 30) / 180
                show(ctx, REQ_CRAVING, "🫁 Se acerca tu hora difícil", "Sueles tener antojos entre ${bucketLabel(peak % 8)}. Si llegan, abre One Day y respira 60 segundos.")
            }
            "milestone" -> {
                d.streaks.forEach { s ->
                    val days = streakDays(s)
                    if (days in MILESTONES)
                        show(ctx, s.id.hashCode(), "🔥 ¡$days día(s) sin ${s.name.lowercase()}!", "Llevas $days día(s). Sigue así, vas muy bien.")
                }
            }
        }
        rescheduleAll(ctx)
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        Notifier.handle(ctx, i.getStringExtra("type"), i.getStringExtra("id"))
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) = Notifier.rescheduleAll(ctx)
}
