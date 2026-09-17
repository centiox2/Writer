package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.audio.BeatPlayer
import com.example.audio.mixer.MultiTrackAudioMixer
import com.example.data.AppContainer
import com.example.ui.components.MultiTrackMixerScreen
import com.example.ui.components.NewItemBottomSheet
import com.example.ui.navigation.Screen
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodels.AlbumsViewModel
import com.example.ui.viewmodels.LyricEditorViewModel
import com.example.ui.viewmodels.MultiTrackMixerViewModel
import com.example.ui.viewmodels.SongsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScaffold(
  appContainer: AppContainer,
  currentThemeMode: ThemeMode,
  onThemeModeSelected: (ThemeMode) -> Unit,
  modifier: Modifier = Modifier
) {
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Songs.route
  val isFullScreen = currentRoute.startsWith("lyric_editor") || currentRoute.startsWith("mixer")
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  var showCreateSheet by remember { mutableStateOf(false) }

  val songsViewModel = remember(appContainer) {
    SongsViewModel(appContainer.songRepository, appContainer.albumRepository)
  }

  val albumsViewModel = remember(appContainer) {
    AlbumsViewModel(appContainer.albumRepository, appContainer.songRepository)
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isExpandedLayout = maxWidth >= 600.dp

    if (isExpandedLayout) {
      // Tablet / Landscape Layout: Navigation Rail on the left
      Row(modifier = Modifier.fillMaxSize()) {
        if (!isFullScreen) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            header = {
              Spacer(modifier = Modifier.height(8.dp))
              FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = StudioBlue,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("tablet_fab_new_project")
              ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create New Project")
              }
            },
            modifier = Modifier.testTag("navigation_rail")
          ) {
            Spacer(modifier = Modifier.height(16.dp))
            Screen.bottomNavItems.forEach { screen ->
              val selected = currentRoute == screen.route
              NavigationRailItem(
                selected = selected,
                onClick = {
                  navController.navigate(screen.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                      saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                icon = {
                  Icon(
                    imageVector = screen.icon,
                    contentDescription = screen.title
                  )
                },
                label = { Text(screen.title) },
                modifier = Modifier.testTag("nav_rail_item_${screen.route}"),
                colors = NavigationRailItemDefaults.colors(
                  selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                  selectedTextColor = StudioBlue,
                  indicatorColor = StudioBlue
                )
              )
            }
          }
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        ) {
          NavHost(
            navController = navController,
            startDestination = Screen.Songs.route,
            modifier = Modifier.fillMaxSize()
          ) {
            composable(Screen.Songs.route) {
              SongsScreen(
                viewModel = songsViewModel,
                onOpenSong = { songId ->
                  navController.navigate(Screen.LyricEditor.createRoute(songId))
                },
                onNewSong = { showCreateSheet = true }
              )
            }
            composable(Screen.Albums.route) {
              AlbumsScreen(
                viewModel = albumsViewModel,
                onOpenSong = { songId ->
                  navController.navigate(Screen.LyricEditor.createRoute(songId))
                }
              )
            }
            composable(Screen.Recordings.route) {
              RecordingsScreen(
                audioRepository = appContainer.audioRepository
              )
            }
            composable(Screen.Settings.route) {
              SettingsScreen(
                currentThemeMode = currentThemeMode,
                onThemeModeSelected = onThemeModeSelected
              )
            }
            composable(
              route = Screen.LyricEditor.route,
              arguments = listOf(navArgument("songId") { type = NavType.StringType })
            ) { backStackEntry ->
              val songId = backStackEntry.arguments?.getString("songId") ?: ""
              val context = LocalContext.current
              val lyricEditorViewModel = remember(songId) {
                val beatPlayer = BeatPlayer(context)
                LyricEditorViewModel(
                  songId = songId,
                  songRepository = appContainer.songRepository,
                  audioRepository = appContainer.audioRepository,
                  audioFileManager = appContainer.audioFileManager,
                  waveformAnalyzer = appContainer.waveformAnalyzer,
                  beatPlayer = beatPlayer
                )
              }
              LyricEditorScreen(
                viewModel = lyricEditorViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMixer = { targetSongId ->
                  navController.navigate(Screen.MultiTrackMixer.createRoute(targetSongId))
                }
              )
            }
            composable(
              route = Screen.MultiTrackMixer.route,
              arguments = listOf(navArgument("songId") { type = NavType.StringType })
            ) { backStackEntry ->
              val songId = backStackEntry.arguments?.getString("songId") ?: ""
              val context = LocalContext.current
              val mixerViewModel = remember(songId) {
                val mixer = MultiTrackAudioMixer(context)
                MultiTrackMixerViewModel(
                  songId = songId,
                  songRepository = appContainer.songRepository,
                  audioRepository = appContainer.audioRepository,
                  audioFileManager = appContainer.audioFileManager,
                  waveformAnalyzer = appContainer.waveformAnalyzer,
                  mixer = mixer
                )
              }
              MultiTrackMixerScreen(
                viewModel = mixerViewModel,
                onNavigateBack = { navController.popBackStack() }
              )
            }
          }

          SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(16.dp)
          )
        }
      }
    } else {
      // Phone / Portrait Layout: Bottom Navigation Bar + FAB
      Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
          if (!isFullScreen) {
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              contentColor = MaterialTheme.colorScheme.onSurface,
              tonalElevation = 8.dp,
              modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
              Screen.bottomNavItems.forEach { screen ->
                val selected = currentRoute == screen.route
                NavigationBarItem(
                  selected = selected,
                  onClick = {
                    navController.navigate(screen.route) {
                      popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                      }
                      launchSingleTop = true
                      restoreState = true
                    }
                  },
                  icon = {
                    Icon(
                      imageVector = screen.icon,
                      contentDescription = screen.title
                    )
                  },
                  label = { Text(screen.title) },
                  modifier = Modifier.testTag("nav_item_${screen.route}"),
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = StudioBlue,
                    indicatorColor = StudioBlue
                  )
                )
              }
            }
          }
        },
        floatingActionButton = {
          if (!isFullScreen) {
            FloatingActionButton(
              onClick = { showCreateSheet = true },
              containerColor = StudioBlue,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              elevation = FloatingActionButtonDefaults.elevation(6.dp),
              shape = CircleShape,
              modifier = Modifier.testTag("fab_create_new")
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create New Project",
                modifier = Modifier.size(28.dp)
              )
            }
          }
        }
      ) { innerPadding ->
        NavHost(
          navController = navController,
          startDestination = Screen.Songs.route,
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
          composable(Screen.Songs.route) {
            SongsScreen(
              viewModel = songsViewModel,
              onOpenSong = { songId ->
                navController.navigate(Screen.LyricEditor.createRoute(songId))
              },
              onNewSong = { showCreateSheet = true }
            )
          }
          composable(Screen.Albums.route) {
            AlbumsScreen(
              viewModel = albumsViewModel,
              onOpenSong = { songId ->
                navController.navigate(Screen.LyricEditor.createRoute(songId))
              }
            )
          }
          composable(Screen.Recordings.route) {
            RecordingsScreen(
              audioRepository = appContainer.audioRepository
            )
          }
          composable(Screen.Settings.route) {
            SettingsScreen(
              currentThemeMode = currentThemeMode,
              onThemeModeSelected = onThemeModeSelected
            )
          }
          composable(
            route = Screen.LyricEditor.route,
            arguments = listOf(navArgument("songId") { type = NavType.StringType })
          ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId") ?: ""
            val context = LocalContext.current
            val lyricEditorViewModel = remember(songId) {
              val beatPlayer = BeatPlayer(context)
              LyricEditorViewModel(
                songId = songId,
                songRepository = appContainer.songRepository,
                audioRepository = appContainer.audioRepository,
                audioFileManager = appContainer.audioFileManager,
                waveformAnalyzer = appContainer.waveformAnalyzer,
                beatPlayer = beatPlayer
              )
            }
            LyricEditorScreen(
              viewModel = lyricEditorViewModel,
              onNavigateBack = { navController.popBackStack() },
              onNavigateToMixer = { targetSongId ->
                navController.navigate(Screen.MultiTrackMixer.createRoute(targetSongId))
              }
            )
          }
          composable(
            route = Screen.MultiTrackMixer.route,
            arguments = listOf(navArgument("songId") { type = NavType.StringType })
          ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId") ?: ""
            val context = LocalContext.current
            val mixerViewModel = remember(songId) {
              val mixer = MultiTrackAudioMixer(context)
              MultiTrackMixerViewModel(
                songId = songId,
                songRepository = appContainer.songRepository,
                audioRepository = appContainer.audioRepository,
                audioFileManager = appContainer.audioFileManager,
                waveformAnalyzer = appContainer.waveformAnalyzer,
                mixer = mixer
              )
            }
            MultiTrackMixerScreen(
              viewModel = mixerViewModel,
              onNavigateBack = { navController.popBackStack() }
            )
          }
        }
      }
    }
  }

  if (showCreateSheet) {
    NewItemBottomSheet(
      onDismiss = { showCreateSheet = false },
      onCreateSong = { title ->
        songsViewModel.createSong(title = title) { newSongId ->
          navController.navigate(Screen.LyricEditor.createRoute(newSongId))
        }
      },
      onCreateAlbum = { name, description, artworkUri ->
        albumsViewModel.createAlbum(name = name, description = description, artworkUri = artworkUri)
        scope.launch {
          snackbarHostState.showSnackbar("Created album: $name")
        }
      },
      onCreateVoiceIdea = {
        val dateStr = SimpleDateFormat("MMM d • h:mm a", Locale.getDefault()).format(Date())
        val ideaTitle = "Idea - $dateStr"
        songsViewModel.createSong(title = ideaTitle) { newSongId ->
          navController.navigate(Screen.LyricEditor.createRoute(newSongId))
        }
      }
    )
  }
}
