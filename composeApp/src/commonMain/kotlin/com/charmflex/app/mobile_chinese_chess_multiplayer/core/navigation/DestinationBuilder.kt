package com.charmflex.app.mobile_chinese_chess_multiplayer.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder

interface DestinationBuilder {
    fun NavGraphBuilder.buildGraph()
}

internal val FEVerticalSlideUp: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Companion.Up,
        animationSpec = tween(300)
    )
}


internal val FEVerticalSlideDown: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?) = {
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Companion.Down,
        animationSpec = tween(300)
    )
}

internal val FEHorizontalEnterFromStart: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
        animationSpec = tween(300)
    )
}

internal val FEHorizontalEnterFromEnd: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Companion.Left,
        animationSpec = tween(300)
    )
}

internal val FEHorizontalExitToEnd: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
        animationSpec = tween(300)
    )
}

internal val FEHorizontalExitToStart: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Companion.Left,
        animationSpec = tween(300)
    )
}