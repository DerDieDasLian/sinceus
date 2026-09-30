package de.loveapp

import android.app.Application
import de.loveapp.data.LoveRepository
import de.loveapp.notify.DailyScheduler
import de.loveapp.notify.Notifier
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
        }
    }
}
