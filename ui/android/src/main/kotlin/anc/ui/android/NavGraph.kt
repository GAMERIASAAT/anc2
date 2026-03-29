package anc.ui.android

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import anc.ui.android.screens.ConsoleScreen
import anc.ui.android.screens.ModulesScreen
import anc.ui.android.screens.SessionsScreen
import anc.ui.android.viewmodel.FrameworkViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Console : Screen("console", "Console", Icons.Outlined.Terminal)
    object Modules : Screen("modules", "Modules", Icons.Default.Search)
    object Sessions : Screen("sessions", "Sessions", Icons.Default.Share)

    companion object {
        val all = listOf(Console, Modules, Sessions)
    }
}

@Composable
fun AncKitNavGraph(viewModel: FrameworkViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Console.route
        ) {
            composable(Screen.Console.route) {
                ConsoleScreen(viewModel)
            }
            composable(Screen.Modules.route) {
                ModulesScreen(viewModel) { modulePath ->
                    viewModel.executeCommand("use $modulePath")
                    navController.navigate(Screen.Console.route) {
                        popUpTo(Screen.Console.route) { inclusive = true }
                    }
                }
            }
            composable(Screen.Sessions.route) {
                SessionsScreen(viewModel)
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    NavigationBar {
        Screen.all.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) }
            )
        }
    }
}
