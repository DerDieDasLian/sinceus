package app.sinceus

import android.app.Application
import app.sinceus.data.CacheCleanup
import app.sinceus.data.LoveRepository
import app.sinceus.notify.DailyScheduler
import app.sinceus.notify.Notifier
import app.sinceus.update.UpdateChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LoveApp : Application() {
    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(AppLanguage.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        Notifier.createChannel(this)
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            val repo = LoveRepository(this@LoveApp)
            val settings = repo.current()
            DailyScheduler.schedule(this@LoveApp, settings)
            // Verpasste Mitteilung für heute nachholen (Handy war aus oder die App wurde beendet)
            runCatching { Notifier.notifyIfDue(this@LoveApp, repo, settings) }
            runCatching { UpdateChecker.checkIfDue(this@LoveApp) }
            runCatching { CacheCleanup.run(this@LoveApp) }
        }
    }
}
