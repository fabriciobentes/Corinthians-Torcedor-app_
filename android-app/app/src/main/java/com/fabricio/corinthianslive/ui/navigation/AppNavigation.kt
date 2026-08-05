package com.fabricio.corinthianslive.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.*
import com.fabricio.corinthianslive.ui.screens.AoVivoScreen
import com.fabricio.corinthianslive.ui.screens.ClassificacaoScreen
import com.fabricio.corinthianslive.ui.screens.JogosScreen
import com.fabricio.corinthianslive.ui.screens.ConfiguracoesScreen
import com.fabricio.corinthianslive.ui.screens.EstatisticasScreen
import com.fabricio.corinthianslive.ui.theme.CorinthiansColors

private sealed class BottomDest(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
) {
    data object Jogos : BottomDest(
        route = "jogos",
        label = "Jogos",
        icon = { Icon(Icons.Filled.SportsSoccer, contentDescription = null) }
    )

    data object AoVivo : BottomDest(
        route = "ao_vivo",
        label = "Ao vivo",
        icon = { Icon(Icons.Filled.LiveTv, contentDescription = null) }
    )

    data object Tabela : BottomDest(
        route = "tabela",
        label = "Tabela",
        icon = { Icon(Icons.Filled.Leaderboard, contentDescription = null) }
    )

    data object Estatisticas : BottomDest(
        route = "estatisticas",
        label = "Estat.",
        icon = { Icon(Icons.Filled.QueryStats, contentDescription = null) }
    )

    data object Configuracoes : BottomDest(
        route = "configuracoes",
        label = "Ajustes",
        icon = { Icon(Icons.Filled.Settings, contentDescription = null) }
    )
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val items = listOf(BottomDest.Jogos, BottomDest.AoVivo, BottomDest.Tabela, BottomDest.Estatisticas, BottomDest.Configuracoes)

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                items.forEach { dest ->
                    val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = dest.icon,
                        label = { Text(dest.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CorinthiansColors.Red,
                            selectedTextColor = CorinthiansColors.Red,
                            indicatorColor = CorinthiansColors.Red.copy(alpha = 0.14f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomDest.Jogos.route
        ) {
            composable(BottomDest.Jogos.route) {
                JogosScreen(contentPadding = innerPadding)
            }
            composable(BottomDest.AoVivo.route) {
                AoVivoScreen(contentPadding = innerPadding)
            }
            composable(BottomDest.Tabela.route) {
                ClassificacaoScreen(contentPadding = innerPadding)
            }
            composable(BottomDest.Estatisticas.route) {
                EstatisticasScreen(contentPadding = innerPadding)
            }
            composable(BottomDest.Configuracoes.route) {
                ConfiguracoesScreen(contentPadding = innerPadding)
            }
        }
    }
}
