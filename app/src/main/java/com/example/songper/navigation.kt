package com.example.songper


import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.songper.ViewModel.MainViewModel
import com.example.songper.screens.LoginScreen
import com.example.songper.screens.HomeScreen

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (viewModel.userIsAuthenticated) "home" else "login"
    ) {
        composable("login") {
            LoginScreen(navController, viewModel)
        }
        composable("home") {
            HomeScreen(navController, viewModel)
        }
    }
}


