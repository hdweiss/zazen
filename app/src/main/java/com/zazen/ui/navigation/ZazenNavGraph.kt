package com.zazen.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zazen.data.model.TimerState
import com.zazen.service.TimerService
import com.zazen.ui.screens.settings.SettingsScreen
import com.zazen.ui.screens.setup.SetupScreen
import com.zazen.ui.screens.stats.StatsScreen
import com.zazen.ui.screens.timer.TimerScreen

@Composable
fun ZazenNavGraph() {
    val navController = rememberNavController()

    // If the timer is active when the app opens, go straight to the timer screen.
    // Read once: a changing startDestination rebuilds the graph and resets the
    // back stack, recreating TimerScreen mid-sit (and the outgoing copy's
    // onDispose re-shows the system bars).
    val startDest = remember {
        if (TimerService.timerState.value !is TimerState.Idle) Screen.Timer.route else Screen.Setup.route
    }

    NavHost(navController = navController, startDestination = startDest) {
        composable(Screen.Setup.route) {
            SetupScreen(
                onStartTimer = {
                    // Screens stay composed (and clickable / observing state)
                    // through their exit transition, so a second trigger from the
                    // outgoing screen would push a duplicate destination.
                    if (navController.currentDestination?.route == Screen.Setup.route) {
                        navController.navigate(Screen.Timer.route) {
                            popUpTo(Screen.Setup.route) { inclusive = true }
                        }
                    }
                },
                onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
            )
        }
        composable(Screen.Timer.route) {
            TimerScreen(
                onTimerDone = {
                    if (navController.currentDestination?.route == Screen.Timer.route) {
                        navController.navigate(Screen.Setup.route) {
                            popUpTo(Screen.Timer.route) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(Screen.Stats.route) {
            StatsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) { entry ->
            // Share the setup screen's ViewModel so Start sees changes made here.
            val setupEntry = remember(entry) { navController.getBackStackEntry(Screen.Setup.route) }
            SettingsScreen(
                viewModel = hiltViewModel(setupEntry),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
