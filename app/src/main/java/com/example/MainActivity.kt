package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.data.DefaultAppContainer
import com.example.data.settings.AppSettings
import com.example.ui.screens.MainScaffold
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SongwriterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val appContainer = DefaultAppContainer(applicationContext)

    setContent {
      val scope = rememberCoroutineScope()
      val appSettings by appContainer.settingsRepository.settings.collectAsState(initial = AppSettings())

      SongwriterTheme(themeMode = appSettings.themeMode) {
        MainScaffold(
          appContainer = appContainer,
          currentThemeMode = appSettings.themeMode,
          onThemeModeSelected = { mode ->
            scope.launch { appContainer.settingsRepository.setThemeMode(mode) }
          }
        )
      }
    }
  }
}

// Retained for test harness & preview compatibility
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { Greeting("Android") }
}
