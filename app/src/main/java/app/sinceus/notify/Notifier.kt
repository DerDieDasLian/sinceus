package app.sinceus.notify

import app.sinceus.data.Texts
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import app.sinceus.BuildConfig
import app.sinceus.update.UpdateChecker
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import android.graphics.Bitmap
import app.sinceus.MainActivity
import app.sinceus.R
import app.sinceus.data.LoveSettings
import app.sinceus.data.BirthdayMath
import app.sinceus.data.LoveMath
import app.sinceus.data.MomentMath
import app.sinceus.data.Names
import app.sinceus.data.Relationship
import java.time.LocalDate

object Notifier {
    /** Öffnet die App und startet dort Download und Installation des Updates */
    const val ACTION_INSTALL_UPDATE = "app.sinceus.action.INSTALL_UPDATE"

    private const val CHANNEL_ID = "special_days"
    private const val NOTIFICATION_ID = 1
    private const val UPDATE_CHANNEL_ID = "updates"
    private const val UPDATE_NOTIFICATION_ID = 2

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.channel_description) }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        if (BuildConfig.UPDATE_CHECK) {
            manager.createNotificationChannel(
                NotificationChannel(
                    UPDATE_CHANNEL_ID,
                    context.getString(R.string.update_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    fun showUpdate(context: Context, release: UpdateChecker.Release) {
        if (!canNotify(context)) return
        val open = PendingIntent.getActivity(
            context, 1,
            Intent(context, MainActivity::class.java)
                .setAction(ACTION_INSTALL_UPDATE)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.wine))
            .setContentTitle(context.getString(R.string.update_available, release.version))
            .setContentText(context.getString(R.string.update_tap_to_open))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Berechtigung wurde zwischenzeitlich entzogen
        }
    }

    fun canNotify(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Titel und Zeilen der Mitteilung für [today], null = heute ist nichts Besonderes */
    fun dueToday(context: Context, settings: LoveSettings, today: LocalDate): Pair<String, List<String>>? {
        val multi = settings.relationships.size > 1
        fun shown(r: Relationship) = Names.join(settings.shownNames(r))
        val involved = mutableSetOf<Relationship>()
        val lines = mutableListOf<String>()
        settings.relationships.filter { it.notify }.forEach { r ->
            LoveMath.milestonesOn(r.startDate, today).forEach { m ->
                involved += r
                lines += (if (multi) "${shown(r)}: " else "") + Texts.milestoneMessage(context, m)
            }
        }
        if (settings.showMoments) {
            MomentMath.remindersOn(settings.moments, today).forEach { (m, years) ->
                val r = settings.relationships.firstOrNull { it.id == m.relationshipId }
                if (r != null && !r.notify) return@forEach
                r?.let { involved += it }
                lines += (if (multi && r != null) "${shown(r)}: " else "") + Texts.momentMessage(context, m, years)
            }
        }
        // Geburtstage, außer alle Beziehungen dieses Menschen haben die Mitteilungen aus
        val celebrating = settings.people.filter { p ->
            settings.relationships.none { p.id in it.members } || settings.relationships.any { it.notify && p.id in it.members }
        }
        BirthdayMath.milestonesOn(celebrating, today) { if (settings.discreet) Names.initial(it.name) else it.name }
            .forEach { lines += Texts.milestoneMessage(context, it) }
        if (lines.isEmpty()) return null
        val title = when {
            involved.size == 1 -> shown(involved.first())
            multi -> Names.join(settings.people.map { if (settings.discreet) Names.initial(it.name) else it.name })
            else -> shown(settings.relationships.first())
        }
        return title to lines
    }

    fun show(context: Context, title: String, lines: List<String>) {
        if (lines.isEmpty() || !canNotify(context)) return
        post(context, title, lines.first(), lines.joinToString("\n"))
    }

    fun showTest(context: Context, settings: LoveSettings) {
        if (!canNotify(context)) return
        post(
            context,
            Names.join(settings.shownNames()),
            context.getString(R.string.test_notification),
            null,
        )
    }

    private fun heartIcon(context: Context): Bitmap? =
        ContextCompat.getDrawable(context, R.drawable.ic_heart_small)?.toBitmap(HEART_PX, HEART_PX)

    private const val HEART_PX = 128

    private fun post(context: Context, title: String, text: String, big: String?) {
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.wine))
            .setContentTitle(title)
            // Gezeichnetes rosa Herz rechts in der Mitteilung (Emojis sehen je nach Handy unschön aus)
            .apply { heartIcon(context)?.let { setLargeIcon(it) } }
            .setContentText(text)
            .apply { if (big != null) setStyle(NotificationCompat.BigTextStyle().bigText(big)) }
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Berechtigung wurde zwischenzeitlich entzogen
        }
    }
}
