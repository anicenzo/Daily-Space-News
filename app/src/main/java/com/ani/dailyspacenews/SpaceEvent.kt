package com.ani.dailyspacenews

import com.google.gson.annotations.SerializedName

data class SpaceEvent(
    val id: Int,
    val name: String,
    val description: String?,
    val date: String,
    @SerializedName("feature_image") val featureImage: String?,
    val type: EventType?
)

data class EventType(
    val name: String?
)

/**
 * A static collection of notable space events for 2026,
 * used as fallback when the API is unreachable.
 */
object SpaceEvents2026 {
    val events = listOf(
        SpaceEvent(
            id = 900001,
            name = "Total Lunar Eclipse",
            description = "A total lunar eclipse visible across North America, the Pacific and East Asia.",
            date = "2026-03-03",
            featureImage = null,
            type = EventType("Eclipse")
        ),
        SpaceEvent(
            id = 900002,
            name = "Annular Solar Eclipse",
            description = "An annular solar eclipse visible across parts of Antarctica and southern oceans.",
            date = "2026-02-17",
            featureImage = null,
            type = EventType("Eclipse")
        ),
        SpaceEvent(
            id = 900003,
            name = "Total Solar Eclipse",
            description = "A total solar eclipse visible across parts of Arctic Russia, Greenland, Iceland and Spain.",
            date = "2026-08-12",
            featureImage = null,
            type = EventType("Eclipse")
        ),
        SpaceEvent(
            id = 900004,
            name = "Perseids Meteor Shower",
            description = "One of the best meteor showers of the year, peaking with up to 100 meteors per hour.",
            date = "2026-08-12",
            featureImage = null,
            type = EventType("Meteor Shower")
        ),
        SpaceEvent(
            id = 900005,
            name = "Geminids Meteor Shower",
            description = "The king of meteor showers, producing up to 120 multicolored meteors per hour.",
            date = "2026-12-14",
            featureImage = null,
            type = EventType("Meteor Shower")
        )
    )
}
