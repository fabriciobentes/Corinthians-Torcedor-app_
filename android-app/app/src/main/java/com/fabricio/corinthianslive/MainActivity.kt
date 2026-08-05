package com.fabricio.corinthianslive

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.fabricio.corinthianslive.notifications.NotificationPreferences
import com.fabricio.corinthianslive.ui.navigation.AppNavigation
import com.fabricio.corinthianslive.ui.theme.CorinthiansTheme

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            CorinthiansTheme {
                AppNavigation()
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
