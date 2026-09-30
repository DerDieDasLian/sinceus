package de.loveapp.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import de.loveapp.MainActivity
import de.loveapp.data.LoveMath
import de.loveapp.data.LoveRepository
import de.loveapp.data.formatNumber
import java.time.LocalDate

class LoveWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = LoveRepository(context).current()
        val days = LoveMath.together(settings.startDate, LocalDate.now()).totalDays
        val future = settings.startDate.isAfter(LocalDate.now())
        val white = ColorProvider(Color.White)
        val soft = ColorProvider(Color(0xFFF6D6DC))

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(24.dp)
                    .background(Color(0xFF7A1A2C))
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    settings.names,
                    style = TextStyle(color = soft, fontSize = 13.sp, fontFamily = FontFamily.Serif),
                    maxLines = 1,
                )
                Text(
                    formatNumber(days),
                    style = TextStyle(color = white, fontSize = 40.sp, fontWeight = FontWeight.Bold),
                )
                Text(
                    if (future) "Tage bis es losgeht ❤" else if (days == 1L) "Tag zusammen ❤" else "Tage zusammen ❤",
                    style = TextStyle(color = soft, fontSize = 12.sp),
                )
            }
        }
    }

    companion object {
        suspend fun refresh(context: Context) = LoveWidget().updateAll(context)
    }
}

class LoveWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LoveWidget()
}
