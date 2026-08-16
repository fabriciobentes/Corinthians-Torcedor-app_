package com.fabricio.corinthianslive.data

import android.content.Context
import com.fabricio.corinthianslive.BuildConfig

object DataSettings {
    fun getBaseUrl(@Suppress("UNUSED_PARAMETER") context: Context): String =
        BuildConfig.DATA_BASE_URL.trim().trimEnd('/')
}
