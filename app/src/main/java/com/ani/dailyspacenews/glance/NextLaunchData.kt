package com.ani.dailyspacenews.glance

import androidx.datastore.preferences.core.stringPreferencesKey

object NextLaunchData {
    val NEXT_LAUNCH_NAME = stringPreferencesKey("next_launch_name")
    val NEXT_LAUNCH_TIME = stringPreferencesKey("next_launch_time")
}