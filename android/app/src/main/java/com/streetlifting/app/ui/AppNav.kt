package com.streetlifting.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object Routes {
    const val PLAN = "plan"
    const val DAY = "day/{semana}/{diaIndex}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    fun day(semana: Int, diaIndex: Int) = "day/$semana/$diaIndex"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val vm: AppViewModel = viewModel()

    NavHost(navController = nav, startDestination = Routes.PLAN) {
        composable(Routes.PLAN) {
            PlanScreen(
                vm = vm,
                onOpenDay = { semana, dia -> nav.navigate(Routes.day(semana, dia)) },
                onOpenHistory = { nav.navigate(Routes.HISTORY) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            Routes.DAY,
            arguments = listOf(
                navArgument("semana") { type = NavType.IntType },
                navArgument("diaIndex") { type = NavType.IntType },
            ),
        ) { backStack ->
            val semana = backStack.arguments?.getInt("semana") ?: 1
            val diaIndex = backStack.arguments?.getInt("diaIndex") ?: 0
            DayScreen(
                vm = vm,
                semana = semana,
                diaIndex = diaIndex,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(vm = vm, onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
        }
    }
}
