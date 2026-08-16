package com.fabricio.corinthianslive

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.fabricio.corinthianslive.data.AppSettings
import com.fabricio.corinthianslive.data.AppThemeMode
import com.fabricio.corinthianslive.notifications.NotificationPreferences
import com.fabricio.corinthianslive.ui.navigation.AppNavigation
import com.fabricio.corinthianslive.ui.theme.CorinthiansTheme

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            var themeMode by remember { mutableStateOf(AppSettings.themeMode(applicationContext)) }
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                AppThemeMode.System -> systemDark
                AppThemeMode.Light -> false
                AppThemeMode.Dark -> true
            }
            SideEffect {
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !useDarkTheme
                    isAppearanceLightNavigationBars = !useDarkTheme
                }
            }
            CorinthiansTheme(darkTheme = useDarkTheme) {
                AppNavigation(
                    themeMode = themeMode,
                    onThemeModeChanged = { selected ->
                        AppSettings.setThemeMode(applicationContext, selected)
                        themeMode = selected
                    }
                )
            }
        }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            NotificationPreferences.isEnabled(this) &&
            !NotificationPreferences.wasPermissionAsked(this) &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            NotificationPreferences.markPermissionAsked(this)
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
