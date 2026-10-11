package com.example.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.GenerateScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ScannerScreen

sealed class Screen(val route: String) {
    data object Scanner : Screen("scanner")
    data object History : Screen("history")
    data object Generate : Screen("generate")
}

@Composable
fun SwiftScanNavHost(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Scanner.route) {
        composable(Screen.Scanner.route) {
            ScannerScreen(
                viewModel = viewModel,
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToGenerate = { navController.navigate(Screen.Generate.route) }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Generate.route) {
            GenerateScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
