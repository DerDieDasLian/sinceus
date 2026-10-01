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
import app.sinceus.MainActivity
import app.sinceus.R
import app.sinceus.data.LoveSettings
import app.sinceus.data.Milestone
import app.sinceus.data.Moment

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

    fun show(
        context: Context,
        settings: LoveSettings,
        milestones: List<Milestone>,
        moments: List<Pair<Moment, Int>> = emptyList(),
    ) {
        val lines = milestones.map { Texts.milestoneMessage(context, it) } +
            moments.map { (m, years) -> Texts.momentMessage(context, m, years) }
        if (lines.isEmpty() || !canNotify(context)) return
        post(context, settings.names, lines.first(), lines.joinToString("\n"))
    }

    fun showTest(context: Context, settings: LoveSettings) {
        if (!canNotify(context)) return
        post(
            context,
            settings.names,
            context.getString(R.string.test_notification),
            null,
        )
    }

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
