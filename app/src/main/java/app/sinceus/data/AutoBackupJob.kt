package app.sinceus.data

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Führt die wöchentliche automatische Sicherung als eigene Hintergrundaufgabe aus. Im täglichen
 * Wecker selbst bleibt dafür nur wenig Zeit, und eine Sicherung mit vielen Fotos braucht länger.
 * Android gibt einer solchen Aufgabe bis zu zehn Minuten und startet sie neu, falls sie unterbrochen wird.
 */
class AutoBackupJob : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters): Boolean {
        scope.launch {
            try {
                AutoBackup.runIfDue(applicationContext)
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    /** Unterbrochen (z. B. Speicher wird knapp): abgebrochene Datei räumt [AutoBackup.run] weg, später neu versuchen */
    override fun onStopJob(params: JobParameters): Boolean {
        scope.cancel()
        return true
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** Weit weg von den Nummern, die WorkManager (für die Widgets) von 0 an vergibt */
        private const val JOB_ID = 0x5153_0001

        /** Plant die Sicherung; sie startet, sobald genug Speicher frei ist (meist sofort) */
        fun schedule(context: Context) {
            val job = JobInfo.Builder(JOB_ID, ComponentName(context, AutoBackupJob::class.java))
                .setRequiresStorageNotLow(true)
                .build()
            runCatching { context.getSystemService(JobScheduler::class.java).schedule(job) }
        }

        /** Nimmt eine geplante Sicherung zurück, z. B. wenn die automatische Sicherung ausgeschaltet wird */
        fun cancel(context: Context) {
            runCatching { context.getSystemService(JobScheduler::class.java).cancel(JOB_ID) }
        }
    }
}
