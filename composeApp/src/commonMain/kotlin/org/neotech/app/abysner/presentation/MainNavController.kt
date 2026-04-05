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

package org.neotech.app.abysner.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import me.tatarka.inject.annotations.Inject
import androidx.compose.ui.tooling.preview.Preview
import org.neotech.app.abysner.di.AppScope
import org.neotech.app.abysner.presentation.screens.AboutScreen
import org.neotech.app.abysner.presentation.screens.planner.PlannerScreen
import org.neotech.app.abysner.presentation.screens.DiveConfigurationScreen
import org.neotech.app.abysner.presentation.screens.SettingsScreen
import org.neotech.app.abysner.presentation.screens.terms_and_conditions.TermsAndConditionsScreen
import org.neotech.app.abysner.presentation.theme.LocalThemeMode
import org.neotech.app.abysner.presentation.utilities.DestinationDefinition
import org.neotech.app.abysner.presentation.utilities.NavHost
import org.neotech.app.abysner.presentation.utilities.fadeComposable
import org.neotech.app.abysner.presentation.utilities.rootComposable
import org.neotech.app.abysner.presentation.utilities.slideComposable
import androidx.compose.foundation.layout.Box
import org.neotech.app.abysner.presentation.component.BitmapRenderRoot

enum class Destinations(override val destinationName: String) : DestinationDefinition {
    PLANNER("planner"),
    DIVE_CONFIGURATION("dive-configuration"),
    APP_CONFIGURATION("app-configuration"),
    ABOUT("about"),
    TERMS_AND_CONDITIONS("terms-and-conditions"),
    TERMS_AND_CONDITIONS_INITIAL("terms-and-conditions-initial")
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

    val startDestination = when (viewModel.settings.value.termsAndConditionsAccepted) {
        false -> Destinations.TERMS_AND_CONDITIONS_INITIAL
        true -> Destinations.PLANNER
    }

    val navController = rememberNavController()

    val settings by viewModel.settings.collectAsState()

    CompositionLocalProvider(LocalThemeMode provides settings.themeMode) {
        Box {
            BitmapRenderRoot {
                NavHost(navController = navController, startDestination = startDestination) {
                    rootComposable(Destinations.PLANNER) { plannerScreen(navController) }

                    slideComposable(Destinations.DIVE_CONFIGURATION) { diveConfigurationScreen(navController) }
                    slideComposable(Destinations.APP_CONFIGURATION) { settingsScreen(navController) }
                    slideComposable(Destinations.ABOUT) { aboutScreen(navController) }
                    slideComposable(Destinations.TERMS_AND_CONDITIONS) { termsAndConditionsScreen(navController) }

                    fadeComposable(Destinations.TERMS_AND_CONDITIONS_INITIAL) { termsAndConditionsScreen(navController) }
                }
            }
        }
    }
}
