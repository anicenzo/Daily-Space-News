package com.ani.dailyspacenews.widget

import androidx.datastore.preferences.core.stringPreferencesKey

object ApodData {
    val APOD_TITLE = stringPreferencesKey("apod_title")
    val APOD_URL = stringPreferencesKey("apod_url")
}