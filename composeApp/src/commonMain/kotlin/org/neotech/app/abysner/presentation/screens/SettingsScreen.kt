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

package org.neotech.app.abysner.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import org.neotech.app.abysner.domain.settings.SettingsRepository
import org.neotech.app.abysner.domain.settings.model.SettingsModel
import org.neotech.app.abysner.domain.settings.model.ThemeMode
import org.neotech.app.abysner.presentation.component.preferences.SettingsSubTitle
import org.neotech.app.abysner.presentation.component.preferences.SingleChoicePreference
import org.neotech.app.abysner.presentation.component.preferences.SwitchPreference
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import kotlinx.collections.immutable.toImmutableList

typealias SettingsScreen = @Composable (navController: NavHostController) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Inject
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    @Assisted navController: NavHostController = rememberNavController()
) {
    val settings by settingsRepository.settings.collectAsState()
    SettingsScreen(
        navController = navController,
        settings = settings,
        updateSettings = settingsRepository::updateSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController = rememberNavController(),
    settings: SettingsModel,
    updateSettings: ((SettingsModel) -> SettingsModel) -> Unit,
) {
    AbysnerTheme {
        Scaffold(
            topBar = {
                Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.background) {
                    TopAppBar(
                        title = { Text("Preferences") },
                        navigationIcon = {

                            val currentBackStackEntry by navController.currentBackStackEntryAsState()
                            if (currentBackStackEntry != null || LocalInspectionMode.current) {
                                IconButton(onClick = {
                                    navController.navigateUp()
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        },
                    )

                }
            }
        ) { scaffoldPadding ->
            Box(
                Modifier
                    .verticalScroll(rememberScrollState())
            ) {

                Column(modifier = Modifier.padding(scaffoldPadding)) {
                    SettingsSubTitle(subTitle = "Appearance")

                    SingleChoicePreference(
                        label = "Theme",
                        description = "Change the app's overall appearance to dark, light or follow the system.",
                        items = ThemeMode.entries.toImmutableList(),
                        selectedItemIndex = ThemeMode.entries.indexOf(settings.themeMode),
                        itemToStringMapper = { it.humanReadableName },
                        onItemPicked = { picked ->
                            updateSettings { it.copy(themeMode = picked) }
                        },
                    )

                    SettingsSubTitle(subTitle = "Deco plan")

                    SwitchPreference(
                        label = "Simple deco table",
                        value = "Display a simpler deco plan, by removing less important details such as ascents between deco stops.",
                        isChecked = settings.showBasicDecoTable,
                        onCheckedChanged = { checked ->
                            updateSettings { it.copy(showBasicDecoTable = checked) }
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        settings = SettingsModel(),
        updateSettings = {}
    )
}
