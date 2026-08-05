package com.fabricio.corinthianslive

import android.app.Application
import com.fabricio.corinthianslive.notifications.GameNotificationManager
import com.fabricio.corinthianslive.notifications.NotificationPreferences

class CorinthiansApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        GameNotificationManager.createChannel(this)
        if (NotificationPreferences.isEnabled(this)) {
            GameNotificationManager.scheduleChecks(this)
        }
    }
}
