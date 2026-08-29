package com.ani.dailyspacenews.util

import android.app.Activity
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.play.core.review.ReviewManagerFactory

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object AppReviewManager {

    private val APP_OPEN_COUNT = intPreferencesKey("app_open_count")

    suspend fun incrementAndRequestReviewIfQualified(activity: Activity) {
        val newCount = incrementAppOpenCount(activity)
        if (newCount == 3) {
            requestReview(activity)
        }
    }

    private suspend fun incrementAppOpenCount(context: Context): Int {
        var newCount = 0
        context.dataStore.edit { settings ->
            val currentCount = settings[APP_OPEN_COUNT] ?: 0
            newCount = currentCount + 1
            settings[APP_OPEN_COUNT] = newCount
        }
        return newCount
    }

    private fun requestReview(activity: Activity) {
        val reviewManager = ReviewManagerFactory.create(activity)
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                reviewManager.launchReviewFlow(activity, reviewInfo)
            }
        }
    }
}