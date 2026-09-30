package de.loveapp.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.loveapp.data.LoveMath
import de.loveapp.data.LoveRepository
import de.loveapp.widget.LoveWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Empfaengt die taeglichen Wecker sowie Neustart, Zeitzonen- und App-Updates. */
class DailyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val repo = LoveRepository(app)
                val settings = repo.current()
                if (intent.action == DailyScheduler.ACTION_CHECK && settings.notificationsEnabled) {
                    val today = LocalDate.now()
                    val milestones = LoveMath.milestonesOn(settings.startDate, today)
                    if (milestones.isNotEmpty() && repo.markNotified(today)) {
                        Notifier.show(app, settings, milestones)
                    }
                }
                LoveWidget.refresh(app)
                // Wecker sind einmalig, also den naechsten Tag neu planen
                DailyScheduler.schedule(app, settings)
            } finally {
                pending.finish()
            }
        }
    }
}
