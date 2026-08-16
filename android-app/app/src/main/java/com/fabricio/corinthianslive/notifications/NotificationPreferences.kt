package com.fabricio.corinthianslive.notifications

import android.content.Context

object NotificationPreferences {
    private const val PREFERENCES = "corinthians_notification_settings"
    private const val ENABLED = "game_day_enabled"
    private const val PERMISSION_ASKED = "permission_asked"

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        preferences(context).getBoolean(ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(ENABLED, enabled).apply()
    }

    fun wasPermissionAsked(context: Context): Boolean =
        preferences(context).getBoolean(PERMISSION_ASKED, false)

    fun markPermissionAsked(context: Context) {
        preferences(context).edit().putBoolean(PERMISSION_ASKED, true).apply()
    }

    fun wasAlertSent(context: Context, matchId: Long, alert: String): Boolean =
        preferences(context).getBoolean("alert_" + matchId + "_" + alert, false)

    fun markAlertSent(context: Context, matchId: Long, alert: String) {
        preferences(context).edit().putBoolean("alert_" + matchId + "_" + alert, true).apply()
    }

    fun wasEventSent(context: Context, matchId: Long, eventId: String): Boolean =
        preferences(context).getStringSet("events_" + matchId, emptySet()).orEmpty().contains(eventId)

    fun markEventSent(context: Context, matchId: Long, eventId: String) {
        val key = "events_" + matchId
        val updated = preferences(context).getStringSet(key, emptySet()).orEmpty().toMutableSet()
        updated += eventId
        preferences(context).edit().putStringSet(key, updated).apply()
    }
}
