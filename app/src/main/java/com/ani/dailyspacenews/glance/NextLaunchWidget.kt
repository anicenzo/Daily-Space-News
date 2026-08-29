package com.ani.dailyspacenews.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ani.dailyspacenews.R
import com.ani.dailyspacenews.util.dataStore
import kotlinx.coroutines.flow.first

object NextLaunchWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val preferences = context.dataStore.data.first()
        val nextLaunchName = preferences[NextLaunchData.NEXT_LAUNCH_NAME]
        val nextLaunchTime = preferences[NextLaunchData.NEXT_LAUNCH_TIME]

        provideContent {
            NextLaunchWidgetContent(nextLaunchName, nextLaunchTime)
        }
    }
}

@Composable
private fun NextLaunchWidgetContent(name: String?, time: String?) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(R.color.dark_grey))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Next Launch",
            style = TextStyle(
                color = ColorProvider(R.color.neon_cyan),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = name ?: "N/A",
            style = TextStyle(fontSize = 22.sp, color = ColorProvider(android.R.color.white))
        )
        Text(
            text = time ?: "",
            style = TextStyle(fontSize = 14.sp, color = ColorProvider(android.R.color.darker_gray))
        )
    }
}
