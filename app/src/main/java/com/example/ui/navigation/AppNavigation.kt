package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.ui.components.AppBottomBar
import com.example.ui.components.KaamKaajFeedbackSnackbar
import com.example.ui.components.UpdateDialog
import com.example.ui.screens.*
import com.example.ui.viewmodel.KaamKaajViewModel
import com.example.update.AppUpdateManager
import com.example.update.UpdateInfo

object Routes {
    const val HOME = "home"
    const val DOCUMENTS = "documents"
    const val PAYMENTS = "payments"
    const val REMINDERS = "reminders"
    const val MORE = "more"
    const val WARRANTIES = "warranties"
    const val SEARCH = "search"
    const val BACKUP = "backup"
    const val SETTINGS = "settings"
}

private val FluidTabEasing = CubicBezierEasing(0.20f, 0.98f, 0.32f, 1.0f)

private fun getTabOrder(route: String?): Int {
    return when (route) {
        Routes.HOME -> 0
        Routes.DOCUMENTS -> 1
        Routes.PAYMENTS -> 2
        Routes.REMINDERS -> 3
        Routes.MORE -> 4
        else -> -1
    }
}

@Composable
fun KaamKaajApp(
    viewModel: KaamKaajViewModel,
    targetRoute: String? = null,
    onRouteConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.HOME

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val appLockEnabled by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()

    var updateInfoState by remember { mutableStateOf<UpdateInfo?>(null) }

    // Lightweight internet reminder for update check & automatic background update-check
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        val isOnline = AppUpdateManager.isNetworkAvailable(context)
        if (!isOnline) {
            if (!AppUpdateManager.hasShownOfflineReminderThisSession) {
                AppUpdateManager.hasShownOfflineReminderThisSession = true
                snackbarHostState.showSnackbar(
                    message = "Please turn on your internet for a moment so KaamKaaj can check for the latest update.",
                    duration = SnackbarDuration.Long
                )
            }
        } else {
            kotlinx.coroutines.delay(5500)
            val update = AppUpdateManager.checkForUpdate()
            if (update != null) {
                updateInfoState = update
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(targetRoute) {
        targetRoute?.let { route ->
            val destination = when (route) {
                "documents", Routes.DOCUMENTS -> Routes.DOCUMENTS
                "payments", Routes.PAYMENTS -> Routes.PAYMENTS
                "reminders", Routes.REMINDERS -> Routes.REMINDERS
                "warranties", Routes.WARRANTIES -> Routes.WARRANTIES
                "more", Routes.MORE -> Routes.MORE
                "settings", Routes.SETTINGS -> Routes.SETTINGS
                "backup", Routes.BACKUP -> Routes.BACKUP
                "search", Routes.SEARCH -> Routes.SEARCH
                else -> Routes.HOME
            }
            if (destination in listOf(Routes.HOME, Routes.DOCUMENTS, Routes.PAYMENTS, Routes.REMINDERS, Routes.MORE)) {
                navController.navigate(destination) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            } else {
                navController.navigate(destination)
            }
            onRouteConsumed()
        }
    }

    val isTopLevelRoute = currentRoute in listOf(
        Routes.HOME, Routes.DOCUMENTS, Routes.PAYMENTS, Routes.REMINDERS, Routes.MORE
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = if (isTopLevelRoute) 94.dp else 16.dp
                        )
                ) { snackbarData ->
                    KaamKaajFeedbackSnackbar(snackbarData = snackbarData)
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier.fillMaxSize()
        ) { _ ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    val initialTab = getTabOrder(initialState.destination.route)
                    val targetTab = getTabOrder(targetState.destination.route)
                    if (initialTab != -1 && targetTab != -1) {
                        EnterTransition.None
                    } else {
                        slideInHorizontally(
                            initialOffsetX = { (it * 0.12f).toInt() },
                            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                        )
                    }
                },
                exitTransition = {
                    val initialTab = getTabOrder(initialState.destination.route)
                    val targetTab = getTabOrder(targetState.destination.route)
                    if (initialTab != -1 && targetTab != -1) {
                        ExitTransition.None
                    } else {
                        slideOutHorizontally(
                            targetOffsetX = { -(it * 0.08f).toInt() },
                            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                        )
                    }
                },
                popEnterTransition = {
                    val initialTab = getTabOrder(initialState.destination.route)
                    val targetTab = getTabOrder(targetState.destination.route)
                    if (initialTab != -1 && targetTab != -1) {
                        EnterTransition.None
                    } else {
                        slideInHorizontally(
                            initialOffsetX = { -(it * 0.08f).toInt() },
                            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                        )
                    }
                },
                popExitTransition = {
                    val initialTab = getTabOrder(initialState.destination.route)
                    val targetTab = getTabOrder(targetState.destination.route)
                    if (initialTab != -1 && targetTab != -1) {
                        ExitTransition.None
                    } else {
                        slideOutHorizontally(
                            targetOffsetX = { (it * 0.12f).toInt() },
                            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                        )
                    }
                }
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToTab = { tab ->
                            navController.navigate(tab) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToWarranties = { navController.navigate(Routes.WARRANTIES) },
                        onNavigateToSearch = { navController.navigate(Routes.SEARCH) }
                    )
                }

                composable(Routes.DOCUMENTS) {
                    DocumentsScreen(
                        viewModel = viewModel,
                        onNavigateToSearch = { navController.navigate(Routes.SEARCH) }
                    )
                }

                composable(Routes.PAYMENTS) {
                    PaymentsScreen(
                        viewModel = viewModel,
                        onNavigateToSearch = { navController.navigate(Routes.SEARCH) }
                    )
                }

                composable(Routes.REMINDERS) {
                    RemindersScreen(
                        viewModel = viewModel,
                        onNavigateToSearch = { navController.navigate(Routes.SEARCH) }
                    )
                }

                composable(Routes.MORE) {
                    MoreScreen(
                        viewModel = viewModel,
                        onNavigateToWarranties = { navController.navigate(Routes.WARRANTIES) },
                        onNavigateToSearch = { navController.navigate(Routes.SEARCH) },
                        onNavigateToBackup = { navController.navigate(Routes.BACKUP) },
                        onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
                    )
                }

                composable(Routes.WARRANTIES) {
                    WarrantiesScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToSearch = { navController.navigate(Routes.SEARCH) }
                    )
                }

                composable(Routes.SEARCH) {
                    SearchScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.BACKUP) {
                    BackupRestoreScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToBackup = { navController.navigate(Routes.BACKUP) }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isTopLevelRoute,
            enter = fadeIn(tween(180)) + slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(tween(150)) + slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(180, easing = FastOutSlowInEasing)
            ),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AppBottomBar(
                currentRoute = currentRoute,
                onNavigateToRoute = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // Update Dialog Overlay
        val updateInfo = updateInfoState
        if (updateInfo != null && !(appLockEnabled && isAppLocked)) {
            UpdateDialog(
                updateInfo = updateInfo,
                onDismiss = { updateInfoState = null }
            )
        }

        // App Lock Protection Screen
        if (appLockEnabled && isAppLocked) {
            AppLockScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
