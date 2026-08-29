package com.ani.dailyspacenews.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
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
import coil.imageLoader
import coil.request.ImageRequest
import com.ani.dailyspacenews.R
import com.ani.dailyspacenews.util.dataStore
import kotlinx.coroutines.flow.first

object ApodWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val preferences = context.dataStore.data.first()
        val apodTitle = preferences[ApodData.APOD_TITLE]
        val apodUrl = preferences[ApodData.APOD_URL]

        val bitmap = apodUrl?.let { url ->
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false) // Important for widgets
                .build()
            (context.imageLoader.execute(request).drawable as? BitmapDrawable)?.bitmap
        }

        provideContent {
            ApodWidgetContent(apodTitle, bitmap)
        }
    }
}

@Composable
private fun ApodWidgetContent(title: String?, image: Bitmap?) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(R.color.dark_grey))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Picture of the Day",
            style = TextStyle(
                color = ColorProvider(R.color.neon_cyan),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        )
        if (image != null) {
            androidx.glance.Image(
                provider = ImageProvider(image),
                contentDescription = title,
                modifier = GlanceModifier.fillMaxSize().padding(top = 8.dp)
            )
        }
        Text(
            text = title ?: "N/A",
            style = TextStyle(fontSize = 14.sp, color = ColorProvider(android.R.color.white))
        )
    }
}
