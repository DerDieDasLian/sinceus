package app.sinceus.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.sinceus.data.AutoBackup
import app.sinceus.data.FolderSync
import app.sinceus.data.LoveRepository
import app.sinceus.update.UpdateChecker
import app.sinceus.widget.LoveWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Empfängt die täglichen Wecker sowie Neustart, Zeitzonen- und App-Updates. */
class DailyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val repo = LoveRepository(app)
                val settings = repo.current()
                // Nicht nur beim Wecker zur gewählten Uhrzeit: Auch nach einem Neustart des Handys oder
                // einer Zeitumstellung wird eine verpasste Mitteilung für heute nachgeholt.
                Notifier.notifyIfDue(app, repo, settings)
                LoveWidget.refresh(app)
                // Wöchentliche automatische Sicherung, falls ein Ordner gewählt ist. Sie läuft als eigene
                // Hintergrundaufgabe, weil der Wecker nur wenig Zeit hat und viele Fotos dauern können.
                if (settings.onboardingDone) runCatching { AutoBackup.scheduleIfDue(app) }
                // Abgleich über den Ordner auch dann, wenn die App länger nicht geöffnet wurde
                if (settings.onboardingDone) runCatching { FolderSync.schedule(app) }
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
