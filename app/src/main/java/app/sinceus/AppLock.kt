package app.sinceus

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager.Authenticators
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.os.SystemClock
import android.view.WindowManager

/**
 * App-Sperre: Beim Öffnen fragt die App nach Fingerabdruck, Gesicht oder der PIN des Handys.
 * Die Einstellung gehört zu diesem Handy und wird deshalb nicht gesichert oder abgeglichen.
 */
object AppLock {
    private const val PREFS = "app_lock"

    /** So lange darf die App im Hintergrund sein (z. B. für die Fotoauswahl), ohne neu zu sperren */
    const val GRACE_MS = 60_000L

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean("on", false)

    fun setEnabled(context: Context, on: Boolean) = prefs(context).edit().putBoolean("on", on).apply()

    /** Ohne Displaysperre am Handy gibt es nichts, womit man entsperren könnte */
    fun canLock(context: Context): Boolean = context.getSystemService(KeyguardManager::class.java).isDeviceSecure

    /** Muss nach so langer Abwesenheit neu entsperrt werden? */
    fun shouldLock(leftAt: Long, now: Long = SystemClock.elapsedRealtime()): Boolean = leftAt == 0L || now - leftAt >= GRACE_MS

    /** Inhalt in "Letzte Apps" verbergen, solange die Sperre an ist */
    fun hideInRecents(activity: Activity, hide: Boolean) {
        if (Build.VERSION.SDK_INT >= 33) {
            activity.setRecentsScreenshotEnabled(!hide)
        } else if (hide) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    /**
     * Fragt ab Android 11 mit dem Systemdialog nach Fingerabdruck, Gesicht oder PIN.
     * false = nicht möglich, dann [Activity] selbst mit [credentialIntent] fragen.
     */
    fun prompt(activity: Activity, onSuccess: () -> Unit): Boolean {
        if (Build.VERSION.SDK_INT < 30) return false
        // Klappt der Systemdialog nicht, fragt die App über die Displaysperre, statt abzustürzen
        return runCatching { biometric(activity, onSuccess) }.isSuccess
    }

    private fun biometric(activity: Activity, onSuccess: () -> Unit) {
        BiometricPrompt.Builder(activity)
            .setTitle(activity.getString(R.string.lock_prompt))
            .setAllowedAuthenticators(Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL)
            .build()
            .authenticate(
                CancellationSignal(),
                activity.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) = onSuccess()
                },
            )
    }

    /** Bis Android 10: Abfrage der Displaysperre des Handys (PIN, Muster, Passwort oder Fingerabdruck) */
    @Suppress("DEPRECATION")
    fun credentialIntent(activity: Activity) = runCatching {
        activity.getSystemService(KeyguardManager::class.java)
            .createConfirmDeviceCredentialIntent(activity.getString(R.string.lock_prompt), null)
    }.getOrNull()
}
