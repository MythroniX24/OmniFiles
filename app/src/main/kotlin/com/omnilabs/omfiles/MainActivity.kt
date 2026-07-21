package com.omnilabs.omfiles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.omnilabs.omfiles.core.theme.OmniFilesTheme
import com.omnilabs.omfiles.domain.model.ThemeMode
import com.omnilabs.omfiles.domain.repository.SettingsRepository
import com.omnilabs.omfiles.ui.components.OmniFilesBottomBar
import com.omnilabs.omfiles.ui.navigation.NavRoutes
import com.omnilabs.omfiles.ui.navigation.OmniFilesNavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val dynamicColors by settingsRepository.dynamicColors.collectAsState(initial = true)

            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            OmniFilesTheme(
                darkTheme = isDarkTheme,
                dynamicColor = dynamicColors
            ) {
                MainScreen()
            }
        }
    }
}

@Composable
private fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val showBottomBar = currentRoute in listOf(
                NavRoutes.HOME,
                NavRoutes.SEARCH,
                NavRoutes.SETTINGS
            ) || currentRoute?.startsWith("files") == true

            if (showBottomBar) {
                OmniFilesBottomBar(
                    currentRoute = currentRoute,
                    onItemSelected = { route ->
                        if (route != currentRoute) {
                            when (route) {
                                "home" -> {
                                    navController.navigate(NavRoutes.HOME) {
                                        popUpTo(NavRoutes.HOME) { inclusive = true }
                                    }
                                }
                                "files" -> {
                                    navController.navigate(NavRoutes.files("/storage/emulated/0")) {
                                        popUpTo(NavRoutes.HOME)
                                    }
                                }
                                "search" -> {
                                    navController.navigate(NavRoutes.SEARCH) {
                                        popUpTo(NavRoutes.HOME)
                                    }
                                }
                                "settings" -> {
                                    navController.navigate(NavRoutes.SETTINGS) {
                                        popUpTo(NavRoutes.HOME)
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        OmniFilesNavGraph(
            navController = navController,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
