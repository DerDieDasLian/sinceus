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
import app.sinceus.data.Milestone
import app.sinceus.data.MilestoneKind
import app.sinceus.data.Moment
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

    /**
     * Titel und Zeilen der Mitteilung für [today], null = heute ist nichts Besonderes.
     * Mit Vorab-Erinnerung kommen die Anlässe in 1 oder 7 Tagen als eigene Zeilen dazu.
     */
    fun dueToday(context: Context, settings: LoveSettings, today: LocalDate): Pair<String, List<String>>? {
        val multi = settings.relationships.size > 1
        fun shown(r: Relationship) = Names.join(settings.shownNames(r))
        val involved = mutableSetOf<Relationship>()
        fun line(r: Relationship?, text: String): String {
            r?.let { involved += it }
            return (if (multi && r != null) "${shown(r)}: " else "") + text
        }
        val lines = eventsOn(settings, today).map { (r, m) -> line(r, Texts.milestoneMessage(context, m)) }.toMutableList()
        val before = settings.remindBefore
        if (before > 0) {
            val res = if (before == 1) R.string.remind_tomorrow else R.string.remind_in_week
            eventsOn(settings, today.plusDays(before.toLong())).forEach { (r, m) ->
                lines += line(r, context.getString(res, Texts.milestoneTitle(context, m)))
            }
        }
        if (lines.isEmpty()) return null
        val title = when {
            involved.size == 1 -> shown(involved.first())
            multi -> Names.join(settings.people.map { if (settings.discreet) Names.initial(it.name) else it.name })
            else -> shown(settings.relationships.first())
        }
        return title to lines
    }

    /** Alle Anlässe mit Mitteilung an [day], jeweils mit ihrer Beziehung (null = alle bzw. ein Mensch) */
    private fun eventsOn(settings: LoveSettings, day: LocalDate): List<Pair<Relationship?, Milestone>> = buildList {
        settings.relationships.filter { it.notify }.forEach { r ->
            LoveMath.milestonesOn(r.startDate, day).forEach { add(r to it) }
        }
        if (settings.showMoments) {
            fun relationOf(m: Moment) = settings.relationships.firstOrNull { it.id == m.relationshipId }
            fun allowed(m: Moment) = relationOf(m)?.notify != false
            MomentMath.remindersOn(settings.moments, day).filter { allowed(it.first) }.forEach { (m, years) ->
                add(relationOf(m) to Milestone(MilestoneKind.MOMENT, years.toLong(), day, m.title))
            }
            MomentMath.plannedOn(settings.moments, day).filter(::allowed).forEach { m ->
                add(relationOf(m) to Milestone(MilestoneKind.PLANNED, 0, day, m.title))
            }
        }
        // Geburtstage, außer alle Beziehungen dieses Menschen haben die Mitteilungen aus
        val celebrating = settings.people.filter { p ->
            settings.relationships.none { p.id in it.members } || settings.relationships.any { it.notify && p.id in it.members }
        }
        BirthdayMath.milestonesOn(celebrating, day) { if (settings.discreet) Names.initial(it.name) else it.name }
            .forEach { add(null to it) }
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
