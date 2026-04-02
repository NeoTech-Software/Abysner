/*
 * Abysner - Dive planner
 * Copyright (C) 2024-2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.utilities

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

interface DestinationDefinition {
    val destinationName: String
}

@Composable
fun NavHost(
    navController: NavHostController,
    startDestination: DestinationDefinition,
    builder: NavGraphBuilder.() -> Unit
) {
    key(LocalWindowInfo.current) {
        androidx.navigation.compose.NavHost(
            navController = navController,
            startDestination = startDestination.destinationName,
            builder = builder
        )
    }
}


fun NavGraphBuilder.composable(
    route: DestinationDefinition,
    enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
    exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = null,
    popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = enterTransition,
    popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = exitTransition,
    content: @Composable() (AnimatedContentScope.(NavBackStackEntry) -> Unit)
) =
    composable(
        route = route.destinationName,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition,
        content = content
    )

/**
 * Adds a destination to the nav graph with standard horizontal slide transitions: slides in from
 * the right on forward navigation and slides back out to the right on back navigation.
 */
fun NavGraphBuilder.slideComposable(
    route: DestinationDefinition,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) = composable(
    route = route,
    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween()) },
    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween()) },
    popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween()) },
    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween()) },
    content = content
)

/**
 * Adds a destination to the nav graph suited for a root/home screen: fades in on enter (since
 * there is no prior screen to slide from), slides normally on forward and back navigation, and
 * fades out when removed from the back stack.
 */
fun NavGraphBuilder.rootComposable(
    route: DestinationDefinition,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) = composable(
    route = route,
    enterTransition = { fadeIn() },
    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween()) },
    popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween()) },
    popExitTransition = { fadeOut() },
    content = content
)

/**
 * Adds a destination to the nav graph that fades in and out in all cases.
 */
fun NavGraphBuilder.fadeComposable(
    route: DestinationDefinition,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) = composable(
    route = route,
    enterTransition = { fadeIn() },
    exitTransition = { fadeOut() },
    content = content
)


expect fun closeApp()
