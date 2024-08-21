/*
 * Abysner - Dive planner
 * Copyright (C) 2024 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.neotech.app.abysner.di.AppScope
import org.neotech.app.abysner.presentation.screens.AboutScreen
import org.neotech.app.abysner.presentation.screens.planner.PlannerScreen
import org.neotech.app.abysner.presentation.screens.DiveConfigurationScreen
import org.neotech.app.abysner.presentation.screens.SettingsScreen
import org.neotech.app.abysner.presentation.screens.terms_and_conditions.TermsAndConditionsScreen
import org.neotech.app.abysner.presentation.utilities.DestinationDefinition
import org.neotech.app.abysner.presentation.utilities.NavHost
import org.neotech.app.abysner.presentation.utilities.composable
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.presentation.utilities.BitmapRenderRoot

enum class Destinations(override val destinationName: String) : DestinationDefinition {
    PLANNER("planner"),
    DIVE_CONFIGURATION("dive-configuration"),
    APP_CONFIGURATION("app-configuration"),
    ABOUT("about"),
    TERMS_AND_CONDITIONS("terms-and-conditions"),
    TERMS_AND_CONDITIONS_VIEW("terms-and-conditions-view")
}

typealias MainNavController = @Composable () -> Unit

@Composable
@Preview
@AppScope
@Inject
fun MainNavController(
    viewModelCreator: () -> MainNavControllerViewModel,
    plannerScreen: PlannerScreen,
    diveConfigurationScreen: DiveConfigurationScreen,
    settingsScreen: SettingsScreen,
    termsAndConditionsScreen: TermsAndConditionsScreen,
    aboutScreen: AboutScreen
) {

    val viewModel = viewModel {
        viewModelCreator()
    }

    val startDestination = when(viewModel.areTermsAndConditionsAccepted) {
        false -> Destinations.TERMS_AND_CONDITIONS
        true -> Destinations.PLANNER
    }

    val navController = rememberNavController()

    BitmapRenderRoot {

        NavHost(navController = navController, startDestination = startDestination) {
            composable(
                route = Destinations.PLANNER,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween()) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween()) },
                popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween()) },
                popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween()) }
            ) { plannerScreen(navController) }

            composable(
                route = Destinations.DIVE_CONFIGURATION,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween()) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween()) },
                popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween()) },
                popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween()) }
            ) { diveConfigurationScreen(navController) }

            composable(
                route = Destinations.APP_CONFIGURATION,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween()) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween()) },
                popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween()) },
                popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween()) }
            ) { settingsScreen(navController) }

            composable(
                route = Destinations.ABOUT,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween()) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween()) },
                popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween()) },
                popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween()) }
            ) { aboutScreen(navController) }

            composable(
                route = Destinations.TERMS_AND_CONDITIONS,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
                popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween()) },
                popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween()) }
            ) { termsAndConditionsScreen(navController) }

            composable(
                route = Destinations.TERMS_AND_CONDITIONS_VIEW,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween()) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween()) },
                popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween()) },
                popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween()) }
            ) { termsAndConditionsScreen(navController) }
        }
    }
}
