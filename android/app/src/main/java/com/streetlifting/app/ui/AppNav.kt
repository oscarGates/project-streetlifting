package com.streetlifting.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object Routes {
    const val HOY = "hoy"
    const val PLAN = "plan"
    const val DAY = "day/{semana}/{diaIndex}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    fun day(semana: Int, diaIndex: Int) = "day/$semana/$diaIndex"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOY, "Hoy", Icons.Filled.FitnessCenter),
    Tab(Routes.PLAN, "Plan", Icons.Filled.CalendarMonth),
    Tab(Routes.HISTORY, "Historial", Icons.Filled.ShowChart),
    Tab(Routes.SETTINGS, "Ajustes", Icons.Filled.Settings),
)

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val vm: AppViewModel = viewModel()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val openDay: (Int, Int) -> Unit = { semana, dia -> nav.navigate(Routes.day(semana, dia)) }

    Scaffold(
        // La sesión del día ocupa toda la pantalla (sin barra inferior).
        bottomBar = {
            if (TABS.any { it.route == route }) {
                NavigationBar {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
        // Cada pantalla gestiona sus insets (barra de estado / navegación).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOY,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOY) { TodayScreen(vm = vm, onOpenDay = openDay) }
            composable(Routes.PLAN) { PlanScreen(vm = vm, onOpenDay = openDay) }
            composable(
                Routes.DAY,
                arguments = listOf(
                    navArgument("semana") { type = NavType.IntType },
                    navArgument("diaIndex") { type = NavType.IntType },
                ),
            ) { entry ->
                DayScreen(
                    vm = vm,
                    semana = entry.arguments?.getInt("semana") ?: 1,
                    diaIndex = entry.arguments?.getInt("diaIndex") ?: 0,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.HISTORY) { HistoryScreen(vm = vm) }
            composable(Routes.SETTINGS) { SettingsScreen(vm = vm) }
        }
    }
}
