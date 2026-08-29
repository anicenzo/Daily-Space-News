package com.ani.dailyspacenews.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ani.dailyspacenews.HomeViewModel
import com.ani.dailyspacenews.SpaceEvent
import com.ani.dailyspacenews.ui.components.ObservatoryCard
import com.ani.dailyspacenews.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EventsScreen(
    homeViewModel: HomeViewModel
) {
    val events by homeViewModel.eventsList
    val now = remember { System.currentTimeMillis() }

    val sdf = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    // Identify upcoming vs past
    val upcomingEvents = remember(events) {
        events.filter { event ->
            val time = try { sdf.parse(event.date)?.time ?: 0L } catch (_: Exception) { 0L }
            time >= (now - 24 * 60 * 60 * 1000) // Include today
        }.sortedBy { it.date }
    }

    val pastEvents = remember(events) {
        events.filter { event ->
            val time = try { sdf.parse(event.date)?.time ?: 0L } catch (_: Exception) { 0L }
            time < (now - 24 * 60 * 60 * 1000)
        }.sortedByDescending { it.date }
    }

    val nextUpEvent = upcomingEvents.firstOrNull()

    // Group upcoming events by Month Year (e.g., "MARCH 2026")
    val monthYearFormat = remember {
        SimpleDateFormat("MMMM yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    val groupedUpcoming = remember(upcomingEvents) {
        upcomingEvents.groupBy { event ->
            try {
                val parsed = sdf.parse(event.date)
                if (parsed != null) monthYearFormat.format(parsed).uppercase() else "UPCOMING"
            } catch (_: Exception) {
                "UPCOMING"
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBase),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)
    ) {
        // Screen Header
        item {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Text(
                    text = "MISSION LOG",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Celestial Events & Astronomical Transmissions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // 1. Next Up Featured Card
        nextUpEvent?.let { nextEvent ->
            item {
                Text(
                    text = "NEXT UP",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ObservatoryCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    backgroundColor = BgElevated,
                    borderColor = BorderHairline
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = nextEvent.type?.name?.uppercase() ?: "EVENT",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentAmberDim,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = nextEvent.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Telemetry Countdown
                        val countdownText = calculateCountdown(nextEvent.date, now)
                        Surface(
                            color = BgElevated2,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderHairline)
                        ) {
                            Text(
                                text = countdownText,
                                style = TelemetryMonoStyle,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    nextEvent.description?.let { desc ->
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formatFullDate(nextEvent.date),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // 2. Grouped Upcoming Events List
        groupedUpcoming.forEach { (monthHeader, monthEvents) ->
            item {
                Text(
                    text = monthHeader,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
            }

            itemsIndexed(monthEvents) { index, event ->
                EventRowItem(event = event, isPast = false, showDivider = index < monthEvents.size - 1)
            }
        }

        // 3. Past Events (if present)
        if (pastEvents.isNotEmpty()) {
            item {
                Text(
                    text = "PAST OBSERVATIONS",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
            }

            itemsIndexed(pastEvents) { index, event ->
                EventRowItem(event = event, isPast = true, showDivider = index < pastEvents.size - 1)
            }
        }
    }
}

@Composable
private fun EventRowItem(event: SpaceEvent, isPast: Boolean, showDivider: Boolean) {
    val alpha = if (isPast) 0.5f else 1f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Functional Event Line Icon in outline style
            val icon = getEventIcon(event.type?.name)
            Surface(
                modifier = Modifier.size(36.dp),
                color = BgElevated,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderHairline)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isPast) TextTertiary else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Content Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary.copy(alpha = alpha),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatShortDate(event.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPast) TextTertiary else TextSecondary.copy(alpha = alpha),
                        fontWeight = FontWeight.Bold
                    )
                }

                event.description?.let { desc ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary.copy(alpha = alpha),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (showDivider) {
            HorizontalDivider(
                color = BorderHairline.copy(alpha = 0.6f),
                thickness = 1.dp,
                modifier = Modifier.padding(start = 48.dp, top = 4.dp, bottom = 4.dp)
            )
        }
    }
}

private fun getEventIcon(typeName: String?): ImageVector {
    val lower = typeName?.lowercase() ?: ""
    return when {
        lower.contains("eclipse") -> Icons.Outlined.Brightness2
        lower.contains("meteor") -> Icons.Outlined.AutoAwesome
        lower.contains("launch") || lower.contains("orbit") -> Icons.Outlined.RocketLaunch
        lower.contains("align") || lower.contains("planet") -> Icons.Outlined.Public
        else -> Icons.Outlined.Event
    }
}

private fun calculateCountdown(dateStr: String, now: Long): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val targetTime = parser.parse(dateStr)?.time ?: return "TBA"
        val diff = targetTime - now
        val days = diff / (1000 * 60 * 60 * 24)
        when {
            days > 0 -> "T-${days}d"
            days == 0L -> "T-TODAY"
            else -> "OBSERVED"
        }
    } catch (_: Exception) {
        "TBA"
    }
}

private fun formatFullDate(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatter = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
        val date = parser.parse(dateStr)
        if (date != null) formatter.format(date) else dateStr
    } catch (_: Exception) {
        dateStr
    }
}

private fun formatShortDate(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatter = SimpleDateFormat("MMM d", Locale.US)
        val date = parser.parse(dateStr)
        if (date != null) formatter.format(date) else dateStr
    } catch (_: Exception) {
        dateStr
    }
}
