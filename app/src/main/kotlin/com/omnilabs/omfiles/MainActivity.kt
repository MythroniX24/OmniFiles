package com.omnilabs.omfiles

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.omnilabs.omfiles.core.GlobalExceptionHandler
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
                CrashErrorDialog()
                MainScreen()
            }
        }
    }
}

@Composable
private fun CrashErrorDialog() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showCrashDialog by remember { mutableStateOf(GlobalExceptionHandler.hasCachedCrash(context)) }
    var showFullStack by remember { mutableStateOf(false) }

    if (showCrashDialog) {
        val crash = GlobalExceptionHandler.getCachedCrash(context)
        if (crash != null) {
            AlertDialog(
                onDismissRequest = { },
                title = {
                    Column {
                        Text(
                            "⚠ App Crashed",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            crash.time,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "The app crashed due to an unexpected error:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            crash.message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Thread: ${crash.threadName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(onClick = { showFullStack = !showFullStack }) {
                            Text(if (showFullStack) "Hide details" else "Show full error")
                        }

                        if (showFullStack) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(8.dp)
                            ) {
                                Text(
                                    crash.stackTrace,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    maxLines = 50,
                                    overflow = TextOverflow.Visible
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // Copy crash info to clipboard
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                as ClipboardManager
                            val clip = ClipData.newPlainText(
                                "Crash Report",
                                "Time: ${crash.time}\n" +
                                "Thread: ${crash.threadName}\n" +
                                "Error: ${crash.message}\n\n" +
                                "Stack:\n${crash.stackTrace}"
                            )
                            clipboard.setPrimaryClip(clip)
                        }
                    ) {
                        Text("Copy Report")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            GlobalExceptionHandler.clearCachedCrash(context)
                            showCrashDialog = false
                        }
                    ) {
                        Text("Dismiss")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
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
