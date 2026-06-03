package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.DashboardRepository
import com.example.ui.DashboardScreen
import com.example.ui.ReportsScreen
import com.example.ui.DashboardViewModel
import com.example.ui.DashboardViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val database = AppDatabase.getDatabase(this)
    val repository = DashboardRepository(database.appDao())
    val factory = DashboardViewModelFactory(repository)

    setContent {
      MyApplicationTheme {
        val viewModel: DashboardViewModel = viewModel(factory = factory)
        val navController = rememberNavController()
        val navBackStackEntry = navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry.value?.destination?.route

        Scaffold(
          bottomBar = {
            NavigationBar {
              NavigationBarItem(
                icon = { Text("🏠") },
                label = { Text("Inicio") },
                selected = currentRoute == "dashboard" || currentRoute == null,
                onClick = {
                  navController.navigate("dashboard") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                }
              )
              NavigationBarItem(
                icon = { Text("📈") },
                label = { Text("Reportes") },
                selected = currentRoute == "reports",
                onClick = {
                  navController.navigate("reports") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                }
              )
            }
          }
        ) { padding ->
          NavHost(navController, startDestination = "dashboard", modifier = Modifier.padding(padding)) {
            composable("dashboard") { DashboardScreen(viewModel = viewModel) }
            composable("reports") { ReportsScreen(viewModel = viewModel) }
          }
        }
      }
    }
  }
}
