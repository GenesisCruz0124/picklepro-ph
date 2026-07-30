package com.gentech.picklepro.core.navigation

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
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
import com.gentech.picklepro.organizer.bracket.BracketSetupScreen
import com.gentech.picklepro.organizer.bracket.BracketSetupViewModel
import com.gentech.picklepro.organizer.bracket.BracketSetupViewModelFactory
import com.gentech.picklepro.organizer.bracket.BracketViewScreen
import com.gentech.picklepro.organizer.bracket.BracketViewViewModel
import com.gentech.picklepro.organizer.bracket.BracketViewViewModelFactory
import com.gentech.picklepro.organizer.certificates.CertificatesScreen
import com.gentech.picklepro.organizer.certificates.CertificatesViewModel
import com.gentech.picklepro.organizer.certificates.CertificatesViewModelFactory
import com.gentech.picklepro.organizer.dashboard.DashboardScreen
import com.gentech.picklepro.organizer.dashboard.DashboardViewModel
import com.gentech.picklepro.organizer.dashboard.DashboardViewModelFactory
import com.gentech.picklepro.organizer.division.DivisionSetupScreen
import com.gentech.picklepro.organizer.division.DivisionSetupViewModel
import com.gentech.picklepro.organizer.division.DivisionSetupViewModelFactory
import com.gentech.picklepro.organizer.registration.ManualAddScreen
import com.gentech.picklepro.organizer.registration.ManualAddViewModel
import com.gentech.picklepro.organizer.registration.ManualAddViewModelFactory
import com.gentech.picklepro.organizer.registration.PairingScreen
import com.gentech.picklepro.organizer.registration.PairingViewModel
import com.gentech.picklepro.organizer.registration.PairingViewModelFactory
import com.gentech.picklepro.organizer.registration.QrScanScreen
import com.gentech.picklepro.organizer.registration.QrScanViewModel
import com.gentech.picklepro.organizer.registration.QrScanViewModelFactory
import com.gentech.picklepro.organizer.registration.RegistrationListScreen
import com.gentech.picklepro.organizer.registration.RegistrationListViewModel
import com.gentech.picklepro.organizer.registration.RegistrationListViewModelFactory
import com.gentech.picklepro.organizer.results.ResultsScreen
import com.gentech.picklepro.organizer.results.ResultsViewModel
import com.gentech.picklepro.organizer.results.ResultsViewModelFactory
import com.gentech.picklepro.organizer.scorer.LiveScorerScreen
import com.gentech.picklepro.organizer.scorer.LiveScorerViewModel
import com.gentech.picklepro.organizer.scorer.LiveScorerViewModelFactory
import com.gentech.picklepro.organizer.scoreboard.ScoreboardScreen
import com.gentech.picklepro.organizer.scoreboard.ScoreboardViewModel
import com.gentech.picklepro.organizer.scoreboard.ScoreboardViewModelFactory
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
import com.gentech.picklepro.player.tournaments.PlayerTournamentDetailScreen
import com.gentech.picklepro.player.tournaments.PlayerTournamentDetailViewModel
import com.gentech.picklepro.player.tournaments.PlayerTournamentDetailViewModelFactory
import com.gentech.picklepro.player.tournaments.TournamentsScreen
import com.gentech.picklepro.player.tournaments.TournamentsViewModel
import com.gentech.picklepro.player.tournaments.TournamentsViewModelFactory

private object Routes {
    const val AUTH = "auth"
    const val HOME = "home"
    const val BECOME_ORGANIZER = "become-organizer"
    const val MATCH_SCORE_PATTERN = "match/{matchId}/score"
    const val MATCH_SCOREBOARD_PATTERN = "match/{matchId}/scoreboard"

    fun matchScore(id: String) = "match/$id/score"
    fun matchScoreboard(id: String) = "match/$id/scoreboard"
}

private enum class HomeTab(val route: String) {
    PROFILE("profile"),
    QR("qr"),
    TOURNAMENTS("tournaments"),
    ORGANIZER("organizer"),
}

private object PlayerRoutes {
    const val TOURNAMENTS_LIST = "tournaments/list"
    const val TOURNAMENT_DETAIL_PATTERN = "tournaments/{tournamentId}"

    fun tournamentDetail(id: String) = "tournaments/$id"
}

private object OrganizerRoutes {
    const val DASHBOARD = "organizer/dashboard"
    const val WALLET = "organizer/wallet"
    const val NEW_TOURNAMENT = "organizer/tournament/new"
    const val TOURNAMENT_DETAIL_PATTERN = "organizer/tournament/{tournamentId}"
    const val DIVISIONS_PATTERN = "organizer/tournament/{tournamentId}/divisions"
    const val REGISTRATIONS_PATTERN = "organizer/division/{divisionId}/registrations"
    const val REGISTRATIONS_SCAN_PATTERN = "organizer/division/{divisionId}/registrations/scan"
    const val REGISTRATIONS_MANUAL_PATTERN = "organizer/division/{divisionId}/registrations/manual"
    const val REGISTRATIONS_PAIRING_PATTERN = "organizer/division/{divisionId}/registrations/pairing"
    const val BRACKET_SETUP_PATTERN = "organizer/division/{divisionId}/bracket/setup"
    const val BRACKET_VIEW_PATTERN = "organizer/division/{divisionId}/bracket/view"
    const val RESULTS_PATTERN = "organizer/division/{divisionId}/results"
    const val CERTIFICATES_PATTERN = "organizer/division/{divisionId}/certificates"

    fun tournamentDetail(id: String) = "organizer/tournament/$id"
    fun divisions(id: String) = "organizer/tournament/$id/divisions"
    fun registrations(id: String) = "organizer/division/$id/registrations"
    fun registrationsScan(id: String) = "organizer/division/$id/registrations/scan"
    fun registrationsManual(id: String) = "organizer/division/$id/registrations/manual"
    fun registrationsPairing(id: String) = "organizer/division/$id/registrations/pairing"
    fun bracketSetup(id: String) = "organizer/division/$id/bracket/setup"
    fun bracketView(id: String) = "organizer/division/$id/bracket/view"
    fun results(id: String) = "organizer/division/$id/results"
    fun certificates(id: String) = "organizer/division/$id/certificates"
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
                onOpenMatchScore = { matchId -> navController.navigate(Routes.matchScore(matchId)) },
            )
        }
        composable(Routes.BECOME_ORGANIZER) {
            val viewModel: BecomeOrganizerViewModel = viewModel(factory = BecomeOrganizerViewModelFactory(appContext))
            BecomeOrganizerScreen(
                viewModel = viewModel,
                onRedeemed = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        // Top-level, not nested under HomeScaffold's Scaffold: the scoreboard is spec'd as a
        // true fullscreen display (spec §5.7), and the live scorer benefits from the same
        // freedom from bottom-nav chrome while a match is in progress.
        composable(
            Routes.MATCH_SCORE_PATTERN,
            arguments = listOf(navArgument("matchId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId").orEmpty()
            val viewModel: LiveScorerViewModel = viewModel(factory = LiveScorerViewModelFactory(appContext, matchId))
            LiveScorerScreen(
                viewModel = viewModel,
                onOpenScoreboard = { navController.navigate(Routes.matchScoreboard(matchId)) },
                onOpenBracket = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            Routes.MATCH_SCOREBOARD_PATTERN,
            arguments = listOf(navArgument("matchId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId").orEmpty()
            val viewModel: ScoreboardViewModel = viewModel(factory = ScoreboardViewModelFactory(appContext, matchId))
            ScoreboardScreen(viewModel = viewModel)
        }
    }
}

@Composable
private fun HomeScaffold(
    appContext: Context,
    onBecomeOrganizerClick: () -> Unit,
    onOpenMatchScore: (matchId: String) -> Unit,
) {
    val tabNavController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(appContext))
    val isOrganizer by homeViewModel.isOrganizer.collectAsState()
    val visibleTabs = if (isOrganizer) {
        HomeTab.entries.toList()
    } else {
        listOf(HomeTab.PROFILE, HomeTab.QR, HomeTab.TOURNAMENTS)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by tabNavController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination

                visibleTabs.forEach { tab ->
                    val (icon, labelRes) = when (tab) {
                        HomeTab.PROFILE -> Icons.Filled.Person to R.string.nav_profile
                        HomeTab.QR -> Icons.Filled.QrCode to R.string.nav_qr
                        HomeTab.TOURNAMENTS -> Icons.Filled.Groups to R.string.nav_tournaments
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

            navigation(startDestination = PlayerRoutes.TOURNAMENTS_LIST, route = HomeTab.TOURNAMENTS.route) {
                composable(PlayerRoutes.TOURNAMENTS_LIST) {
                    val viewModel: TournamentsViewModel = viewModel(factory = TournamentsViewModelFactory(appContext))
                    TournamentsScreen(
                        viewModel = viewModel,
                        onOpenTournament = { id -> tabNavController.navigate(PlayerRoutes.tournamentDetail(id)) },
                    )
                }
                composable(
                    PlayerRoutes.TOURNAMENT_DETAIL_PATTERN,
                    arguments = listOf(navArgument("tournamentId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val tournamentId = backStackEntry.arguments?.getString("tournamentId").orEmpty()
                    val viewModel: PlayerTournamentDetailViewModel = viewModel(
                        factory = PlayerTournamentDetailViewModelFactory(appContext, tournamentId),
                    )
                    PlayerTournamentDetailScreen(viewModel = viewModel, onBack = { tabNavController.popBackStack() })
                }
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
                    WalletScreen(
                        viewModel = viewModel,
                        onRedeemAnother = onBecomeOrganizerClick,
                        onBack = { tabNavController.popBackStack() },
                    )
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
                        onBack = { tabNavController.popBackStack() },
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
                        onBack = { tabNavController.popBackStack() },
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
                    DivisionSetupScreen(
                        viewModel = viewModel,
                        onOpenRegistrations = { divisionId -> tabNavController.navigate(OrganizerRoutes.registrations(divisionId)) },
                        onOpenBracket = { divisionId, alreadyGenerated ->
                            val destination = if (alreadyGenerated) {
                                OrganizerRoutes.bracketView(divisionId)
                            } else {
                                OrganizerRoutes.bracketSetup(divisionId)
                            }
                            tabNavController.navigate(destination)
                        },
                        onOpenResults = { divisionId -> tabNavController.navigate(OrganizerRoutes.results(divisionId)) },
                        onBack = { tabNavController.popBackStack() },
                    )
                }

                composable(
                    OrganizerRoutes.REGISTRATIONS_PATTERN,
                    arguments = listOf(navArgument("divisionId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val divisionId = backStackEntry.arguments?.getString("divisionId").orEmpty()
                    val viewModel: RegistrationListViewModel = viewModel(
                        factory = RegistrationListViewModelFactory(appContext, divisionId),
                    )
                    RegistrationListScreen(
                        viewModel = viewModel,
                        onScanQr = { tabNavController.navigate(OrganizerRoutes.registrationsScan(divisionId)) },
                        onManualAdd = { tabNavController.navigate(OrganizerRoutes.registrationsManual(divisionId)) },
                        onPairing = { tabNavController.navigate(OrganizerRoutes.registrationsPairing(divisionId)) },
                        onBack = { tabNavController.popBackStack() },
                    )
                }
                composable(
                    OrganizerRoutes.REGISTRATIONS_SCAN_PATTERN,
                    arguments = listOf(navArgument("divisionId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val divisionId = backStackEntry.arguments?.getString("divisionId").orEmpty()
                    val viewModel: QrScanViewModel = viewModel(factory = QrScanViewModelFactory(appContext, divisionId))
                    QrScanScreen(viewModel = viewModel, onBack = { tabNavController.popBackStack() })
                }
                composable(
                    OrganizerRoutes.REGISTRATIONS_MANUAL_PATTERN,
                    arguments = listOf(navArgument("divisionId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val divisionId = backStackEntry.arguments?.getString("divisionId").orEmpty()
                    val viewModel: ManualAddViewModel = viewModel(factory = ManualAddViewModelFactory(appContext, divisionId))
                    ManualAddScreen(viewModel = viewModel, onBack = { tabNavController.popBackStack() })
                }
                composable(
                    OrganizerRoutes.REGISTRATIONS_PAIRING_PATTERN,
                    arguments = listOf(navArgument("divisionId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val divisionId = backStackEntry.arguments?.getString("divisionId").orEmpty()
                    val viewModel: PairingViewModel = viewModel(factory = PairingViewModelFactory(appContext, divisionId))
                    PairingScreen(viewModel = viewModel, onBack = { tabNavController.popBackStack() })
                }

                composable(
                    OrganizerRoutes.BRACKET_SETUP_PATTERN,
                    arguments = listOf(navArgument("divisionId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val divisionId = backStackEntry.arguments?.getString("divisionId").orEmpty()
                    val viewModel: BracketSetupViewModel = viewModel(
                        factory = BracketSetupViewModelFactory(appContext, divisionId),
                    )
                    BracketSetupScreen(
                        viewModel = viewModel,
                        onGenerated = {
                            tabNavController.navigate(OrganizerRoutes.bracketView(divisionId)) {
                                popUpTo(OrganizerRoutes.bracketSetup(divisionId)) { inclusive = true }
                            }
                        },
                        onViewExistingBracket = { tabNavController.navigate(OrganizerRoutes.bracketView(divisionId)) },
                        onBack = { tabNavController.popBackStack() },
                    )
                }
                composable(
                    OrganizerRoutes.BRACKET_VIEW_PATTERN,
                    arguments = listOf(navArgument("divisionId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val divisionId = backStackEntry.arguments?.getString("divisionId").orEmpty()
                    val viewModel: BracketViewViewModel = viewModel(
                        factory = BracketViewViewModelFactory(appContext, divisionId),
                    )
                    BracketViewScreen(
                        viewModel = viewModel,
                        onOpenMatch = onOpenMatchScore,
                        onBack = { tabNavController.popBackStack() },
                    )
                }

                composable(
                    OrganizerRoutes.RESULTS_PATTERN,
                    arguments = listOf(navArgument("divisionId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val divisionId = backStackEntry.arguments?.getString("divisionId").orEmpty()
                    val viewModel: ResultsViewModel = viewModel(
                        factory = ResultsViewModelFactory(appContext, divisionId),
                    )
                    ResultsScreen(
                        viewModel = viewModel,
                        onOpenCertificates = { tabNavController.navigate(OrganizerRoutes.certificates(divisionId)) },
                        onBack = { tabNavController.popBackStack() },
                    )
                }
                composable(
                    OrganizerRoutes.CERTIFICATES_PATTERN,
                    arguments = listOf(navArgument("divisionId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val divisionId = backStackEntry.arguments?.getString("divisionId").orEmpty()
                    val viewModel: CertificatesViewModel = viewModel(
                        factory = CertificatesViewModelFactory(appContext, divisionId),
                    )
                    CertificatesScreen(viewModel = viewModel, onBack = { tabNavController.popBackStack() })
                }
            }
        }
    }
}
