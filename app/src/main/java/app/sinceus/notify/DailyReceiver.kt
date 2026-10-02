package app.sinceus.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.sinceus.data.AutoBackup
import app.sinceus.data.LoveRepository
import app.sinceus.update.UpdateChecker
import app.sinceus.widget.LoveWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Empfängt die täglichen Wecker sowie Neustart, Zeitzonen- und App-Updates. */
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
                    val due = Notifier.dueToday(app, settings, today)
                    if (due != null && repo.markNotified(today)) {
                        Notifier.show(app, due.first, due.second)
                    }
                }
                LoveWidget.refresh(app)
                // Wöchentliche automatische Sicherung, falls ein Ordner gewählt ist
                if (settings.onboardingDone) runCatching { AutoBackup.runIfDue(app) }
                if (intent.action == DailyScheduler.ACTION_MIDNIGHT) {
                    runCatching { UpdateChecker.checkIfDue(app) }
                }
                // Wecker sind einmalig, also den nächsten Tag neu planen
                DailyScheduler.schedule(app, settings)
            } finally {
                pending.finish()
            }
        }
    }
}
