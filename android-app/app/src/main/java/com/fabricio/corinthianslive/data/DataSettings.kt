package com.fabricio.corinthianslive.data

import android.content.Context

object DataSettings {
    private const val PREFERENCES = "corinthians_live_settings"
    private const val BASE_URL = "data_base_url"

    fun getBaseUrl(context: Context): String =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(BASE_URL, "").orEmpty().trim().trimEnd('/')

    fun setBaseUrl(context: Context, value: String) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putString(BASE_URL, value.trim().trimEnd('/')).apply()
    }
}
