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

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import dev.zacsweers.metro.Inject
import org.neotech.app.abysner.presentation.component.BitmapRenderRoot
import org.neotech.app.abysner.presentation.screens.DiveConfigurationScreen
import org.neotech.app.abysner.presentation.screens.SettingsScreen
import org.neotech.app.abysner.presentation.screens.about.AboutScreen
import org.neotech.app.abysner.presentation.screens.planner.PlannerScreen
import org.neotech.app.abysner.presentation.screens.terms_and_conditions.TermsAndConditionsScreen
import org.neotech.app.abysner.presentation.theme.LocalThemeMode
import org.neotech.app.abysner.presentation.utilities.DestinationDefinition
import org.neotech.app.abysner.presentation.utilities.NavHost
import org.neotech.app.abysner.presentation.utilities.fadeComposable
import org.neotech.app.abysner.presentation.utilities.rootComposable
import org.neotech.app.abysner.presentation.utilities.slideComposable

enum class Destinations(override val destinationName: String) : DestinationDefinition {
    PLANNER("planner"),
    DIVE_CONFIGURATION("dive-configuration"),
    APP_CONFIGURATION("app-configuration"),
    ABOUT("about"),
    TERMS_AND_CONDITIONS("terms-and-conditions"),
    TERMS_AND_CONDITIONS_INITIAL("terms-and-conditions-initial")
}

// Metro supports @Inject on top-level functions, but the generated types are not resolved by the
// IDE, causing "Unresolved reference" errors. This wrapper class avoids those IDE errors.
// See: https://zacsweers.github.io/metro/latest/installation/#ide-support
@Inject
class MainNavController(
    private val viewModelCreator: () -> MainNavControllerViewModel,
    private val plannerScreen: PlannerScreen,
    private val diveConfigurationScreen: DiveConfigurationScreen,
    private val settingsScreen: SettingsScreen,
    private val termsAndConditionsScreen: TermsAndConditionsScreen,
    private val aboutScreen: AboutScreen,
) {
    @Composable
    operator fun invoke() {
        MainNavController(
            viewModel = viewModel { viewModelCreator() },
            plannerScreen = plannerScreen,
            diveConfigurationScreen = diveConfigurationScreen,
            settingsScreen = settingsScreen,
            termsAndConditionsScreen = termsAndConditionsScreen,
            aboutScreen = aboutScreen,
        )
    }
}

@Composable
fun MainNavController(
    viewModel: MainNavControllerViewModel,
    plannerScreen: PlannerScreen,
    diveConfigurationScreen: DiveConfigurationScreen,
    settingsScreen: SettingsScreen,
    termsAndConditionsScreen: TermsAndConditionsScreen,
    aboutScreen: AboutScreen,
) {

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
