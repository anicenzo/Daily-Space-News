package com.ani.dailyspacenews.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ani.dailyspacenews.MainActivity
import com.ani.dailyspacenews.R
import java.util.Calendar

object NotificationScheduler {

    const val LAUNCH_CHANNEL_ID = "launch_notifications"
    const val APOD_CHANNEL_ID = "daily_apod_notifications"

    fun scheduleLaunchReminder(context: Context, launchName: String, launchTimeInMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val notificationIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = "ACTION_LAUNCH_REMINDER"
            putExtra("title", "Rocket Launch Telemetry")
            putExtra("message", "$launchName is scheduled to lift off in 5 minutes.")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            launchName.hashCode(),
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule 5 minutes before launch
        val triggerAtMillis = launchTimeInMillis - (5 * 60 * 1000)
        if (triggerAtMillis <= System.currentTimeMillis()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun scheduleDailyApodReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "ACTION_DAILY_APOD"
            putExtra("title", "Daily Astronomy Observation")
            putExtra("message", "Today's NASA space photograph and telemetry are now available.")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val launchChannel = NotificationChannel(
                LAUNCH_CHANNEL_ID,
                "Launch Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Countdown alerts for upcoming rocket launches"
            }

            val apodChannel = NotificationChannel(
                APOD_CHANNEL_ID,
                "Daily Space Photos",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily NASA astronomy photograph updates"
            }

            notificationManager.createNotificationChannels(listOf(launchChannel, apodChannel))
        }
    }
}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Space Transmission"
        val message = intent.getStringExtra("message") ?: "New observation available."
        val channelId = if (intent.action == "ACTION_LAUNCH_REMINDER") {
            NotificationScheduler.LAUNCH_CHANNEL_ID
        } else {
            NotificationScheduler.APOD_CHANNEL_ID
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(title.hashCode(), notification)
    }
}
