package app.sinceus.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.sinceus.data.LoveSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Plant zwei tägliche Wecker:
 * - kurz nach Mitternacht: Widget auf den neuen Tag aktualisieren
 * - zur gewählten Uhrzeit: prüfen, ob heute ein besonderer Tag ist
 *
 * Genutzt wird ein Zeitfenster-Alarm (setWindow), der keine Sonderberechtigung
 * für exakte Wecker braucht und trotzdem auf wenige Minuten genau ausgelöst wird.
 */
object DailyScheduler {
    const val ACTION_CHECK = "app.sinceus.action.CHECK"
    const val ACTION_MIDNIGHT = "app.sinceus.action.MIDNIGHT"

    private const val WINDOW_MS = 10 * 60 * 1000L

    fun schedule(context: Context, settings: LoveSettings) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val check = pending(context, ACTION_CHECK)
        alarm.cancel(check)
        if (settings.notificationsEnabled) {
            val time = LocalTime.of(settings.notifyHour, settings.notifyMinute)
            alarm.setWindow(AlarmManager.RTC_WAKEUP, nextMillis(time), WINDOW_MS, check)
        }
        alarm.setWindow(
            AlarmManager.RTC,
            nextMillis(LocalTime.of(0, 1)),
            WINDOW_MS,
            pending(context, ACTION_MIDNIGHT),
        )
    }

    private fun nextMillis(time: LocalTime): Long {
        val now = LocalDateTime.now()
        var next = LocalDate.now().atTime(time)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun pending(context: Context, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            Intent(context, DailyReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
