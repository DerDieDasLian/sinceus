package app.sinceus

import android.app.Application
import app.sinceus.data.LoveRepository
import app.sinceus.notify.DailyScheduler
import app.sinceus.notify.Notifier
import app.sinceus.update.UpdateChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LoveApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannel(this)
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            DailyScheduler.schedule(this@LoveApp, LoveRepository(this@LoveApp).current())
            runCatching { UpdateChecker.checkIfDue(this@LoveApp) }
        }
    }
}
