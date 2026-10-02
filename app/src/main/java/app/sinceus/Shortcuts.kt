package app.sinceus

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import app.sinceus.data.LoveSettings

/**
 * Kleine Abkürzungen beim langen Drücken auf das App-Icon: neuer Moment und Live-Zähler.
 * Es erscheinen nur die, deren Bereich in den Einstellungen eingeschaltet ist.
 */
object Shortcuts {
    const val ACTION_ADD_MOMENT = "app.sinceus.action.ADD_MOMENT"
    const val ACTION_LIVE = "app.sinceus.action.LIVE"

    fun publish(context: Context, settings: LoveSettings) {
        val list = if (!settings.onboardingDone) emptyList() else buildList {
            if (settings.showMoments) {
                add(
                    shortcut(
                        context, "moment", ACTION_ADD_MOMENT,
                        R.string.shortcut_moment, R.string.shortcut_moment_long, R.drawable.ic_shortcut_moment,
                    ),
                )
            }
            if (settings.showLive) {
                add(
                    shortcut(
                        context, "live", ACTION_LIVE,
                        R.string.shortcut_live, R.string.shortcut_live_long, R.drawable.ic_shortcut_live,
                    ),
                )
            }
        }
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, list) }
    }

    private fun shortcut(context: Context, id: String, action: String, short: Int, long: Int, icon: Int) =
        ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(context.getString(short))
            .setLongLabel(context.getString(long))
            .setIcon(IconCompat.createWithResource(context, icon))
            .setIntent(Intent(context, MainActivity::class.java).setAction(action))
            .build()
}
