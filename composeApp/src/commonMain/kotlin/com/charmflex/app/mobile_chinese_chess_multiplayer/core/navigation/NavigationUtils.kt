package com.charmflex.app.mobile_chinese_chess_multiplayer.core.navigation

import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import androidx.savedstate.write
import kotlinx.serialization.json.Json

inline fun <reified T: Any> customNavType() = object : NavType<T>(isNullableAllowed = true) {
    override fun put(bundle: SavedState, key: String, value: T) {
        bundle.write { putSavedState(key, encodeToSavedState(value)) }
    }

    override fun get(bundle: SavedState, key: String): T? {
        return bundle.read { decodeFromSavedState(getSavedState(key)) as? T }
    }

    override fun serializeAsValue(value: T): String {
        return Json.encodeToString(value)
    }

    override fun parseValue(value: String): T {
        return Json.decodeFromString(value)
    }
}


internal fun NavController.navigateTo(navigationRoute: NavigationRoute) {
//    args?.let {
//        this.currentBackStackEntry?.savedStateHandle?.let { savedStateHandler ->
//            for (arg in args) {
//                savedStateHandler[arg.key] = arg.value
//            }
//        }
//    }
    navigate(navigationRoute) {
        launchSingleTop = true
    }
}

internal fun NavController.navigateAndPopUpTo(route: NavigationRoute, popUpToRouteInclusive: NavigationRoute? = null) {
    navigate(route) {
        launchSingleTop = true

        if (popUpToRouteInclusive != null) {
            popUpTo(popUpToRouteInclusive) {
                inclusive = true
            }
        }

    }
}

fun NavController.popWithArgs(data: Map<String, Any>) {
    data.let { args ->
        this.previousBackStackEntry?.savedStateHandle?.let { savedStateHandler ->
            for (arg in args) {
                savedStateHandler[arg.key] = arg.value
            }
        }
    }

    popBackStack()
}


