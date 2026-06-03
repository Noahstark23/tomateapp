package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.DashboardRepository
import com.example.ui.DashboardScreen
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
        DashboardScreen(viewModel = viewModel)
      }
    }
  }
}
