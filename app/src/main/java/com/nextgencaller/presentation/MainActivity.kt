package com.nextgencaller.presentation

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.nextgencaller.presentation.call.CallScreen
import com.nextgencaller.presentation.contacts.ContactsScreen
import com.nextgencaller.presentation.history.CallHistoryScreen
import com.nextgencaller.presentation.home.HomeScreen
import com.nextgencaller.presentation.theme.NextGenCallerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NextGenCallerTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val permissionsList = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.MODIFY_AUDIO_SETTINGS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val permissionsState = rememberMultiplePermissionsState(permissions = permissionsList)

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    Scaffold(
        bottomBar = {
            if (currentDestination?.route in listOf(
                    Screen.Home.route,
                    Screen.Contacts.route,
                    Screen.History.route
                )
            ) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
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
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToCall = {
                        navController.navigate(Screen.Call.route)
                    },
                    onNavigateToContacts = {
                        navController.navigate(Screen.Contacts.route)
                    },
                    onNavigateToHistory = {
                        navController.navigate(Screen.History.route)
                    }
                )
            }

            composable(Screen.Contacts.route) {
                ContactsScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onCallContact = { contact ->
                        navController.navigate(Screen.Call.route)
                    }
                )
            }

            composable(Screen.History.route) {
                CallHistoryScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onCallBack = { callLog ->
                        navController.navigate(Screen.Call.route)
                    }
                )
            }

            composable(Screen.Call.route) {
                CallScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Contacts : Screen("contacts", "Contacts", Icons.Default.Contacts)
    object History : Screen("history", "History", Icons.Default.History)
    object Call : Screen("call", "Call", Icons.Default.Call)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Contacts,
    Screen.History
)