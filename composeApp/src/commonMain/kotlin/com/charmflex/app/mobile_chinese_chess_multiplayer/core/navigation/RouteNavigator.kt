package com.charmflex.app.mobile_chinese_chess_multiplayer.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Single
import org.koin.core.annotation.Singleton

@Singleton
class RouteNavigatorImpl : RouteNavigator {
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 10)
    override val navigationEvent: Flow<NavigationEvent>
        get() = _navigationEvent.asSharedFlow()

    override fun navigateTo(navigationRoute: NavigationRoute) {
        _navigationEvent.tryEmit(NavigateTo(navigationRoute))
    }

    override fun navigateAndPopUpTo(route: NavigationRoute, popUpToRouteInclusive: NavigationRoute) {
        _navigationEvent.tryEmit(NavigateAndPopUpTo(route = route, popToRouteInclusive = popUpToRouteInclusive))
    }

    override fun pop() {
        _navigationEvent.tryEmit(Pop)
    }

    override fun popWithArguments(data: Map<String, Any>) {
        _navigationEvent.tryEmit(PopWithArguments(data))
    }
}


interface RouteNavigator {
    val navigationEvent: Flow<NavigationEvent>
    fun navigateTo(navigationRoute: NavigationRoute)
    fun navigateAndPopUpTo(route: NavigationRoute, popUpToRouteInclusive: NavigationRoute)
    fun pop()
    fun popWithArguments(data: Map<String, Any>)

    companion object {
        val instance by lazy { RouteNavigatorImpl() }
    }
}


sealed interface NavigationEvent

data class NavigateTo(
    val navigationRoute: NavigationRoute
) : NavigationEvent
data class NavigateAndPopUpTo(val route: NavigationRoute, val popToRouteInclusive: NavigationRoute): NavigationEvent
object Pop : NavigationEvent
data class PopWithArguments(
    val data: Map<String, Any>
) : NavigationEvent

@Composable
fun RouteNavigatorListener(
    routeNavigator: RouteNavigator,
    navController: NavController,
) {
    LaunchedEffect(Unit) {
        routeNavigator.navigationEvent.collect {
            when (it) {
                is NavigateTo -> navController.navigateTo(it.navigationRoute)
                is NavigateAndPopUpTo -> navController.navigateAndPopUpTo(it.route, it.popToRouteInclusive)
                is Pop -> navController.popBackStack()
                is PopWithArguments -> navController.popWithArgs(it.data)
            }
        }
    }
}