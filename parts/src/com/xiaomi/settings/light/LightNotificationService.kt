package com.xiaomi.settings.light

import android.content.Intent
import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.preference.PreferenceManager

class LightNotificationService : NotificationListenerService() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val masterEnabled = sharedPreferences.getBoolean("light_enable", false)
        if (!masterEnabled) return

        val enable = sharedPreferences.getBoolean("light_notifications_enable", false)
        if (!enable) return

        val appSet = sharedPreferences.getStringSet("light_notifications_apps", emptySet()) ?: emptySet()
        if (appSet.contains(sbn.packageName)) {
            val intent = Intent(this, LightService::class.java)
            intent.action = "ACTION_PULSE_NOTIFICATION"
            startService(intent)
        }
    }
}
