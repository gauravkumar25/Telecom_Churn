package com.studybuddy.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.studybuddy.app.ui.screens.chat.ChatScreen
import com.studybuddy.app.ui.screens.exams.ExamsScreen
import com.studybuddy.app.ui.screens.home.HomeScreen
import com.studybuddy.app.ui.screens.knowledge.KnowledgeScreen
import com.studybuddy.app.ui.screens.settings.SettingsScreen
import com.studybuddy.app.ui.screens.subjects.SubjectsScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Chat : Screen("chat/{sessionId}", "Chat", Icons.Default.Chat) {
        fun createRoute(sessionId: String) = "chat/$sessionId"
    }
    object Subjects : Screen("subjects", "Subjects", Icons.Default.Book)
    object Exams : Screen("exams", "Exams", Icons.Default.CalendarMonth)
    object Knowledge : Screen("knowledge", "Knowledge", Icons.Default.Psychology)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Home, Screen.Subjects, Screen.Exams, Screen.Knowledge, Screen.Settings
)

@Composable
fun StudyBuddyNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route?.startsWith("chat/") != true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300))
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300))
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(onOpenChat = { sessionId ->
                    navController.navigate(Screen.Chat.createRoute(sessionId))
                })
            }
            composable(
                route = Screen.Chat.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
                ChatScreen(
                    sessionId = sessionId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Subjects.route) { SubjectsScreen() }
            composable(Screen.Exams.route) { ExamsScreen() }
            composable(Screen.Knowledge.route) { KnowledgeScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
