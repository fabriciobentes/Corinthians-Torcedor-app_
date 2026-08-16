package com.fabricio.corinthianslive.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fabricio.corinthianslive.data.AppThemeMode
import com.fabricio.corinthianslive.ui.screens.AoVivoScreen
import com.fabricio.corinthianslive.ui.screens.ClassificacaoScreen
import com.fabricio.corinthianslive.ui.screens.ConfiguracoesScreen
import com.fabricio.corinthianslive.ui.screens.EstatisticasScreen
import com.fabricio.corinthianslive.ui.screens.JogosScreen
import com.fabricio.corinthianslive.ui.theme.CorinthiansColors

private sealed class BottomDest(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
) {
    data object Jogos : BottomDest("jogos", "Jogos", { Icon(Icons.Filled.SportsSoccer, null) })
    data object AoVivo : BottomDest("ao_vivo", "Ao vivo", { Icon(Icons.Filled.LiveTv, null) })
    data object Tabela : BottomDest("tabela", "Tabela", { Icon(Icons.Filled.Leaderboard, null) })
    data object Estatisticas : BottomDest("estatisticas", "Estat.", { Icon(Icons.Filled.QueryStats, null) })
    data object Configuracoes : BottomDest("configuracoes", "Ajustes", { Icon(Icons.Filled.Settings, null) })
}

@Composable
fun AppNavigation(
    themeMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit
) {
    val navController = rememberNavController()
    val colors = MaterialTheme.colorScheme
    val destinations = listOf(
        BottomDest.Jogos,
        BottomDest.AoVivo,
        BottomDest.Tabela,
        BottomDest.Estatisticas,
        BottomDest.Configuracoes
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize().background(colors.background),
        containerColor = colors.background,
        contentColor = colors.onBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Box(
                Modifier.background(colors.background)
                    .padding(horizontal = 8.dp, vertical = 7.dp)
            ) {
                NavigationBar(
                    modifier = Modifier.clip(RoundedCornerShape(26.dp))
                        .border(1.dp, colors.outlineVariant, RoundedCornerShape(26.dp)),
                    containerColor = colors.surface,
                    contentColor = colors.onSurface,
                    tonalElevation = 0.dp
                ) {
                    destinations.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = destination.icon,
                            label = { Text(destination.label, fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = CorinthiansColors.Red,
                                indicatorColor = CorinthiansColors.Red,
                                unselectedIconColor = colors.onSurfaceVariant,
                                unselectedTextColor = colors.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(colors.background, colors.surfaceVariant.copy(alpha = .34f), colors.background)
                )
            )
        ) {
            NavHost(navController = navController, startDestination = BottomDest.Jogos.route) {
                composable(BottomDest.Jogos.route) { JogosScreen(innerPadding) }
                composable(BottomDest.AoVivo.route) { AoVivoScreen(innerPadding) }
                composable(BottomDest.Tabela.route) { ClassificacaoScreen(innerPadding) }
                composable(BottomDest.Estatisticas.route) { EstatisticasScreen(innerPadding) }
                composable(BottomDest.Configuracoes.route) {
                    ConfiguracoesScreen(innerPadding, themeMode, onThemeModeChanged)
                }
            }
        }
    }
}
