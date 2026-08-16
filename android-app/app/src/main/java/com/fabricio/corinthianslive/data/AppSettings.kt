package com.fabricio.corinthianslive.data

import android.content.Context

enum class AppThemeMode {
    System,
    Light,
    Dark
}

object AppSettings {
    private const val PREFERENCES = "corinthians_app_settings"
    private const val HIDE_FRIENDLIES = "hide_friendlies"
    private const val THEME_MODE = "theme_mode"

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun hideFriendlies(context: Context): Boolean =
        preferences(context).getBoolean(HIDE_FRIENDLIES, true)

    fun setHideFriendlies(context: Context, hidden: Boolean) {
        preferences(context).edit().putBoolean(HIDE_FRIENDLIES, hidden).apply()
    }

    fun themeMode(context: Context): AppThemeMode = runCatching {
        AppThemeMode.valueOf(
            preferences(context).getString(THEME_MODE, AppThemeMode.System.name)
                ?: AppThemeMode.System.name
        )
    }.getOrDefault(AppThemeMode.System)

    fun setThemeMode(context: Context, mode: AppThemeMode) {
        preferences(context).edit().putString(THEME_MODE, mode.name).apply()
    }
}
