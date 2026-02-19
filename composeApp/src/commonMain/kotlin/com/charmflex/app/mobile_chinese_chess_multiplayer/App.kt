package com.charmflex.app.mobile_chinese_chess_multiplayer

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.navigation.DestinationBuilder
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.navigation.RouteNavigator
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.navigation.RouteNavigatorListener
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.XiangqiMasterTheme
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui.SGSnackBar
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui.SnackBarType
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui.ToastManager
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui.ToastState
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui.ToastType
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui.showSnackBarImmediately
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.destination.AuthDestinationBuilder
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.route.AuthRoute
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.destination.GameDestination
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.destination.HomeDestinationBuilder

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App(
    routeNavigator: RouteNavigator,
    toastManager: ToastManager,
    onBack: () -> Unit
) {
    val navController = rememberNavController()
    val state by toastManager.state.collectAsState()

    BackHandler {
        if (navController.popBackStack().not()) {
            onBack()
        }
    }

    RouteNavigatorListener(routeNavigator = routeNavigator, navController = navController)

    XiangqiMasterTheme {
        NavHost(navController, startDestination = AuthRoute.Login) {
            createDestinations(navController).forEach {
                with(it) { buildGraph() }
            }
        }

        SnackBarView(state) {
            toastManager.reset()
        }
    }
}

@Composable
internal fun SnackBarView(toastState: ToastState?, onReset: () -> Unit) {
    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastState) {
        if (toastState != null) {
            snackBarHostState.showSnackBarImmediately(toastState.message)
            onReset()
        }
    }

    val snackBarType = when (toastState?.toastType) {
        ToastType.SUCCESS, ToastType.NEUTRAL -> SnackBarType.Success
        else -> SnackBarType.Error
    }
    SGSnackBar(snackBarHostState = snackBarHostState, snackBarType = snackBarType)
}

private fun createDestinations(navController: NavController): List<DestinationBuilder> {
    return listOf(
        AuthDestinationBuilder(),
        HomeDestinationBuilder(),
        GameDestination()
    )
}
