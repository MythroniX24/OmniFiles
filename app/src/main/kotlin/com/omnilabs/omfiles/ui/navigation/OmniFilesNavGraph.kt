package com.omnilabs.omfiles.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.omnilabs.omfiles.ui.screens.files.FilesScreen
import com.omnilabs.omfiles.ui.screens.home.HomeScreen
import com.omnilabs.omfiles.ui.screens.search.SearchScreen
import com.omnilabs.omfiles.ui.screens.recycle.RecycleBinScreen
import com.omnilabs.omfiles.ui.screens.preview.PreviewScreen
import com.omnilabs.omfiles.ui.screens.settings.SettingsScreen
import java.net.URLDecoder

@Composable
fun OmniFilesNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(NavRoutes.HOME) {
            HomeScreen(
                onNavigateToFiles = { path ->
                    navController.navigate(NavRoutes.files(path))
                },
                onNavigateToSearch = {
                    navController.navigate(NavRoutes.SEARCH)
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.SETTINGS)
                }
            )
        }

        composable(
            route = NavRoutes.FILES,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val path = URLDecoder.decode(
                backStackEntry.arguments?.getString("path") ?: "/",
                "UTF-8"
            )
            FilesScreen(
                initialPath = path,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFolder = { folderPath ->
                    navController.navigate(NavRoutes.files(folderPath))
                },
                onNavigateToPreview = { previewPath ->
                    navController.navigate(NavRoutes.preview(previewPath))
                }
            )
        }

        composable(NavRoutes.SEARCH) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFile = { path ->
                    val parentPath = path.substringBeforeLast('/')
                    navController.navigate(NavRoutes.files(parentPath))
                }
            )
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.RECYCLE) {
            RecycleBinScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.PREVIEW,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val path = URLDecoder.decode(
                backStackEntry.arguments?.getString("path") ?: "",
                "UTF-8"
            )
            PreviewScreen(
                path = path,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
