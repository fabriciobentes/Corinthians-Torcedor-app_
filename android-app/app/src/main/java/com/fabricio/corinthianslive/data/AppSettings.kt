package com.fabricio.corinthianslive.data

import android.content.Context

object AppSettings {
    private const val PREFERENCES = "corinthians_app_settings"
    private const val HIDE_FRIENDLIES = "hide_friendlies"

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun hideFriendlies(context: Context): Boolean =
        preferences(context).getBoolean(HIDE_FRIENDLIES, true)

    fun setHideFriendlies(context: Context, hidden: Boolean) {
        preferences(context).edit().putBoolean(HIDE_FRIENDLIES, hidden).apply()
    }
}
