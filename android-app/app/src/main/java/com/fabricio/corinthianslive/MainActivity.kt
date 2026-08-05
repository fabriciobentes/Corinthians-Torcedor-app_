package com.fabricio.corinthianslive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.fabricio.corinthianslive.ui.navigation.AppNavigation
import com.fabricio.corinthianslive.ui.theme.CorinthiansTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            CorinthiansTheme {
                AppNavigation()
            }
        }
    }
}