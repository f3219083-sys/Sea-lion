package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.GameScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
  private lateinit var gameViewModel: GameViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        // Use the Jetpack lifecycles ViewModel provider which auto-injects standard Application
        gameViewModel = viewModel()
        GameScreen(
            viewModel = gameViewModel,
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    if (::gameViewModel.isInitialized) {
      gameViewModel.onAppResume()
    }
  }

  override fun onPause() {
    super.onPause()
    if (::gameViewModel.isInitialized) {
      gameViewModel.onAppPause()
    }
  }
}
