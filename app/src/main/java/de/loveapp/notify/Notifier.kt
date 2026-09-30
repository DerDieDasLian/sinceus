package de.loveapp.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import de.loveapp.MainActivity
import de.loveapp.R
import de.loveapp.data.LoveSettings
import de.loveapp.data.Milestone

object Notifier {
    private const val CHANNEL_ID = "special_days"
    private const val NOTIFICATION_ID = 1

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun show(context: Context, settings: LoveSettings, milestones: List<Milestone>) {
        if (milestones.isEmpty() || !canNotify(context)) return
        val title = "${settings.names} ❤"
        val text = milestones.first().message
        val big = milestones.joinToString("\n") { it.message }
        post(context, title, text, big)
    }

    fun showTest(context: Context, settings: LoveSettings) {
        if (!canNotify(context)) return
        post(
            context,
            "${settings.names} ❤",
            "So sehen eure Mitteilungen an besonderen Tagen aus.",
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
