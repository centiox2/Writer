package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.data.DefaultAppContainer
import com.example.ui.screens.MainScaffold
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SongwriterTheme
import com.example.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val appContainer = DefaultAppContainer(applicationContext)

    setContent {
      var currentThemeMode by remember { mutableStateOf(ThemeMode.DARK) }

      SongwriterTheme(themeMode = currentThemeMode) {
        MainScaffold(
          appContainer = appContainer,
          currentThemeMode = currentThemeMode,
          onThemeModeSelected = { currentThemeMode = it }
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
