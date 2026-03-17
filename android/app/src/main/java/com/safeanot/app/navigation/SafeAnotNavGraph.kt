/**
 * Navigation graph for Safe Anot? with bottom navigation bar and all screen routes.
 */
package com.safeanot.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.safeanot.app.feature.alerts.AlertsScreen
import com.safeanot.app.feature.check.CheckScreen
import com.safeanot.app.feature.check.CheckViewModel
import com.safeanot.app.feature.fix.FixScreen
import com.safeanot.app.feature.profile.ProfileScreen
import com.safeanot.app.feature.shield.ShieldScreen

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Shield, "Shield", Icons.Default.Shield),
    BottomNavItem(Screen.Check, "Check", Icons.Default.Link),
    BottomNavItem(Screen.Alerts, "Alerts", Icons.Default.Notifications),
    BottomNavItem(Screen.Profile, "Profile", Icons.Default.Person),
)

@Composable
fun SafeAnotNavGraph(pendingUrl: String? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Track whether we have consumed the pending URL
    var consumed by rememberSaveable { mutableStateOf(false) }

    // Navigate to Check screen when a pending URL is provided
    LaunchedEffect(pendingUrl) {
        if (pendingUrl != null && !consumed) {
            consumed = true
            navController.navigate(Screen.Check.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Hide bottom bar on the Fix flow
    val showBottomBar = currentDestination?.route?.startsWith("fix/") != true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
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
            startDestination = Screen.Shield.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Shield.route) {
                ShieldScreen(
                    onFixClick = { packageName, appName ->
                        navController.navigate(Screen.Fix.createRoute(packageName, appName))
                    }
                )
            }

            composable(Screen.Check.route) {
                val viewModel: CheckViewModel = hiltViewModel()

                // Pre-fill URL from intent if present
                LaunchedEffect(pendingUrl) {
                    if (pendingUrl != null) {
                        viewModel.prefillUrl(pendingUrl)
                    }
                }

                CheckScreen(viewModel = viewModel)
            }

            composable(Screen.Alerts.route) {
                AlertsScreen()
            }

            composable(Screen.Profile.route) {
                ProfileScreen()
            }

            composable(
                route = Screen.Fix.route,
                arguments = listOf(
                    navArgument("packageName") { type = NavType.StringType },
                    navArgument("appName") { type = NavType.StringType },
                )
            ) {
                FixScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
