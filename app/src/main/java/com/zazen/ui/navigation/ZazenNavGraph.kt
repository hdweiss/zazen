package com.zazen.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zazen.ui.screens.settings.SettingsScreen
import com.zazen.ui.screens.stats.StatsScreen
import com.zazen.ui.screens.timer.TimerScreen

/**
 * The timer screen is home: it shows the setup dial while idle and the sit
 * itself while running, so a sit never navigates. Only the side pages
 * (history, settings) are separate destinations, reachable while idle.
 */
@Composable
fun ZazenNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Timer.route) {
        composable(Screen.Timer.route) {
            TimerScreen(
                onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
            )
        }
        composable(Screen.Stats.route) {
            StatsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) { entry ->
            // Share the idle screen's ViewModel so Start sees changes made here.
            val timerEntry = remember(entry) { navController.getBackStackEntry(Screen.Timer.route) }
            SettingsScreen(
                viewModel = hiltViewModel(timerEntry),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
