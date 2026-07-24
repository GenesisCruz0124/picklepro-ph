package com.gentech.picklepro.core.navigation

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gentech.picklepro.R
import com.gentech.picklepro.auth.AuthScreen
import com.gentech.picklepro.auth.AuthViewModel
import com.gentech.picklepro.auth.AuthViewModelFactory
import com.gentech.picklepro.data.repository.AuthRepository
import com.gentech.picklepro.data.repository.AuthState
import com.gentech.picklepro.organizer.activation.BecomeOrganizerScreen
import com.gentech.picklepro.organizer.activation.BecomeOrganizerViewModel
import com.gentech.picklepro.organizer.activation.BecomeOrganizerViewModelFactory
import com.gentech.picklepro.organizer.dashboard.DashboardScreen
import com.gentech.picklepro.organizer.dashboard.DashboardViewModel
import com.gentech.picklepro.organizer.dashboard.DashboardViewModelFactory
import com.gentech.picklepro.organizer.division.DivisionSetupScreen
import com.gentech.picklepro.organizer.division.DivisionSetupViewModel
import com.gentech.picklepro.organizer.division.DivisionSetupViewModelFactory
import com.gentech.picklepro.organizer.tournament.NewTournamentScreen
import com.gentech.picklepro.organizer.tournament.NewTournamentViewModel
import com.gentech.picklepro.organizer.tournament.NewTournamentViewModelFactory
import com.gentech.picklepro.organizer.tournament.TournamentDetailScreen
import com.gentech.picklepro.organizer.tournament.TournamentDetailViewModel
import com.gentech.picklepro.organizer.tournament.TournamentDetailViewModelFactory
import com.gentech.picklepro.organizer.wallet.WalletScreen
import com.gentech.picklepro.organizer.wallet.WalletViewModel
import com.gentech.picklepro.organizer.wallet.WalletViewModelFactory
import com.gentech.picklepro.player.profile.ProfileScreen
import com.gentech.picklepro.player.profile.ProfileViewModel
import com.gentech.picklepro.player.profile.ProfileViewModelFactory
import com.gentech.picklepro.player.qr.QrScreen
import com.gentech.picklepro.player.qr.QrViewModel
import com.gentech.picklepro.player.qr.QrViewModelFactory

private object Routes {
    const val AUTH = "auth"
    const val HOME = "home"
    const val BECOME_ORGANIZER = "become-organizer"
}

private enum class HomeTab(val route: String) {
    PROFILE("profile"),
    QR("qr"),
    ORGANIZER("organizer"),
}

private object OrganizerRoutes {
    const val DASHBOARD = "organizer/dashboard"
    const val WALLET = "organizer/wallet"
    const val NEW_TOURNAMENT = "organizer/tournament/new"
    const val TOURNAMENT_DETAIL_PATTERN = "organizer/tournament/{tournamentId}"
    const val DIVISIONS_PATTERN = "organizer/tournament/{tournamentId}/divisions"
    fun tournamentDetail(id: String) = "organizer/tournament/$id"
    fun divisions(id: String) = "organizer/tournament/$id/divisions"
}

/**
 * Single-APK, role-based root navigation (spec §8). The Organizer tab only
 * appears once the signed-in profile's cached role is organizer/admin
 * (spec §5.1) — before that, "Become an Organizer" is reached from a CTA
 * on the Profile tab as a top-level route, not a tab.
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
            HomeScaffold(
                appContext = appContext,
                onBecomeOrganizerClick = { navController.navigate(Routes.BECOME_ORGANIZER) },
            )
        }
        composable(Routes.BECOME_ORGANIZER) {
            val viewModel: BecomeOrganizerViewModel = viewModel(factory = BecomeOrganizerViewModelFactory(appContext))
            BecomeOrganizerScreen(viewModel = viewModel, onRedeemed = { navController.popBackStack() })
        }
    }
}

@Composable
private fun HomeScaffold(appContext: Context, onBecomeOrganizerClick: () -> Unit) {
    val tabNavController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(appContext))
    val isOrganizer by homeViewModel.isOrganizer.collectAsState()
    val visibleTabs = if (isOrganizer) HomeTab.entries.toList() else listOf(HomeTab.PROFILE, HomeTab.QR)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by tabNavController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination

                visibleTabs.forEach { tab ->
                    val (icon, labelRes) = when (tab) {
                        HomeTab.PROFILE -> Icons.Filled.Person to R.string.nav_profile
                        HomeTab.QR -> Icons.Filled.QrCode to R.string.nav_qr
                        HomeTab.ORGANIZER -> Icons.Filled.EmojiEvents to R.string.nav_organizer
                    }
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
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
                ProfileScreen(viewModel = viewModel, onBecomeOrganizerClick = onBecomeOrganizerClick)
            }
            composable(HomeTab.QR.route) {
                val viewModel: QrViewModel = viewModel(factory = QrViewModelFactory(appContext))
                QrScreen(viewModel = viewModel)
            }

            navigation(startDestination = OrganizerRoutes.DASHBOARD, route = HomeTab.ORGANIZER.route) {
                composable(OrganizerRoutes.DASHBOARD) {
                    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModelFactory(appContext))
                    DashboardScreen(
                        viewModel = viewModel,
                        onNewTournament = { tabNavController.navigate(OrganizerRoutes.NEW_TOURNAMENT) },
                        onOpenTournament = { id -> tabNavController.navigate(OrganizerRoutes.tournamentDetail(id)) },
                        onOpenWallet = { tabNavController.navigate(OrganizerRoutes.WALLET) },
                    )
                }
                composable(OrganizerRoutes.WALLET) {
                    val viewModel: WalletViewModel = viewModel(factory = WalletViewModelFactory(appContext))
                    WalletScreen(viewModel = viewModel, onRedeemAnother = onBecomeOrganizerClick)
                }
                composable(OrganizerRoutes.NEW_TOURNAMENT) {
                    val viewModel: NewTournamentViewModel = viewModel(factory = NewTournamentViewModelFactory(appContext))
                    NewTournamentScreen(
                        viewModel = viewModel,
                        onCreated = { id ->
                            tabNavController.navigate(OrganizerRoutes.tournamentDetail(id)) {
                                popUpTo(OrganizerRoutes.NEW_TOURNAMENT) { inclusive = true }
                            }
                        },
                    )
                }
                composable(
                    OrganizerRoutes.TOURNAMENT_DETAIL_PATTERN,
                    arguments = listOf(navArgument("tournamentId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val tournamentId = backStackEntry.arguments?.getString("tournamentId").orEmpty()
                    val viewModel: TournamentDetailViewModel = viewModel(
                        factory = TournamentDetailViewModelFactory(appContext, tournamentId),
                    )
                    TournamentDetailScreen(
                        viewModel = viewModel,
                        onOpenDivisions = { tabNavController.navigate(OrganizerRoutes.divisions(tournamentId)) },
                    )
                }
                composable(
                    OrganizerRoutes.DIVISIONS_PATTERN,
                    arguments = listOf(navArgument("tournamentId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val tournamentId = backStackEntry.arguments?.getString("tournamentId").orEmpty()
                    val viewModel: DivisionSetupViewModel = viewModel(
                        factory = DivisionSetupViewModelFactory(appContext, tournamentId),
                    )
                    DivisionSetupScreen(viewModel = viewModel)
                }
            }
        }
    }
}
