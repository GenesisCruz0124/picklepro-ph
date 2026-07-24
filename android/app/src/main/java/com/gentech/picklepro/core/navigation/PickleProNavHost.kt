package com.gentech.picklepro.core.navigation

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gentech.picklepro.R
import com.gentech.picklepro.auth.AuthScreen
import com.gentech.picklepro.auth.AuthViewModel
import com.gentech.picklepro.auth.AuthViewModelFactory
import com.gentech.picklepro.data.repository.AuthRepository
import com.gentech.picklepro.data.repository.AuthState
import com.gentech.picklepro.player.profile.ProfileScreen
import com.gentech.picklepro.player.profile.ProfileViewModel
import com.gentech.picklepro.player.profile.ProfileViewModelFactory
import com.gentech.picklepro.player.qr.QrScreen
import com.gentech.picklepro.player.qr.QrViewModel
import com.gentech.picklepro.player.qr.QrViewModelFactory

private object Routes {
    const val AUTH = "auth"
    const val HOME = "home"
}

private enum class HomeTab(val route: String) {
    PROFILE("profile"),
    QR("qr"),
}

/**
 * Single-APK, role-based root navigation (spec §8). M2 only has the player
 * surface (Profile, My QR); the organizer tab is added in M3 once role
 * upgrade + tournament setup exist, gated on the signed-in profile's role.
 */
@Composable
fun PickleProNavHost() {
    val appContext = LocalContext.current.applicationContext
    val navController = rememberNavController()
    val authRepository = remember { AuthRepository(appContext) }
    val authState by authRepository.authState.collectAsState(initial = AuthState.Loading)

    LaunchedEffect(authState) {
        val currentRoute = navController.currentDestination?.route
        when (authState) {
            is AuthState.SignedIn -> if (currentRoute != Routes.HOME) {
                navController.navigate(Routes.HOME) { popUpTo(0) }
            }
            is AuthState.SignedOut -> if (currentRoute != Routes.AUTH) {
                navController.navigate(Routes.AUTH) { popUpTo(0) }
            }
            is AuthState.Loading -> Unit
        }
    }

    NavHost(navController = navController, startDestination = Routes.AUTH) {
        composable(Routes.AUTH) {
            val viewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(appContext))
            AuthScreen(viewModel = viewModel)
        }
        composable(Routes.HOME) {
            HomeScaffold(appContext)
        }
    }
}

@Composable
private fun HomeScaffold(appContext: Context) {
    val tabNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by tabNavController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                HomeTab.entries.forEach { tab ->
                    val (icon, labelRes) = when (tab) {
                        HomeTab.PROFILE -> Icons.Filled.Person to R.string.nav_profile
                        HomeTab.QR -> Icons.Filled.QrCode to R.string.nav_qr
                    }
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            tabNavController.navigate(tab.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(stringResource(labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = tabNavController,
            startDestination = HomeTab.PROFILE.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(HomeTab.PROFILE.route) {
                val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(appContext))
                ProfileScreen(viewModel = viewModel)
            }
            composable(HomeTab.QR.route) {
                val viewModel: QrViewModel = viewModel(factory = QrViewModelFactory(appContext))
                QrScreen(viewModel = viewModel)
            }
        }
    }
}
