package com.fabricio.corinthianslive.notifications

import android.content.Context

object NotificationPreferences {
    private const val PREFERENCES = "corinthians_notification_settings"
    private const val ENABLED = "game_day_enabled"
    private const val PERMISSION_ASKED = "permission_asked"
    private const val LAST_NOTIFIED_MATCH = "last_notified_match"

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = preferences(context).getBoolean(ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(ENABLED, enabled).apply()
    }

    fun wasPermissionAsked(context: Context): Boolean =
        preferences(context).getBoolean(PERMISSION_ASKED, false)

    fun markPermissionAsked(context: Context) {
        preferences(context).edit().putBoolean(PERMISSION_ASKED, true).apply()
    }

    fun lastNotifiedMatch(context: Context): Long =
        preferences(context).getLong(LAST_NOTIFIED_MATCH, Long.MIN_VALUE)

    fun markMatchNotified(context: Context, matchId: Long) {
        preferences(context).edit().putLong(LAST_NOTIFIED_MATCH, matchId).apply()
    }
}
