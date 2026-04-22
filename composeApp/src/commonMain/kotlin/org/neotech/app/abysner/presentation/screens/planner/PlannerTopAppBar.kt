/*
 * Abysner - Dive planner
 * Copyright (C) 2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.screens.planner

import abysner.composeapp.generated.resources.Res
import abysner.composeapp.generated.resources.ic_outline_settings_24
import abysner.composeapp.generated.resources.ic_outline_tune_24
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.neotech.app.abysner.presentation.Destinations
import org.neotech.app.abysner.presentation.screens.ShareImage
import org.neotech.app.abysner.presentation.theme.IconSet
import org.neotech.app.abysner.presentation.component.LocalBitmapRenderController
import org.neotech.app.abysner.presentation.utilities.shareImageBitmap
import androidx.navigation.NavHostController
import org.neotech.app.abysner.domain.settings.model.SettingsModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlannerTopAppBar(
    uiState: PlanScreenViewModel.UiState,
    navController: NavHostController,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.background) {
        Column {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Abysner")
                        Text(
                            style = MaterialTheme.typography.labelSmall,
                            text = "The open-source dive planner"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Unspecified,
                    scrolledContainerColor = Color.Unspecified,
                ),
                actions = {
                    AppBarActions(uiState, navController, uiState.settingsModel)
                },
            )
            content()
        }
    }
}

@Composable
private fun RowScope.AppBarActions(
    uiState: PlanScreenViewModel.UiState,
    navController: NavHostController,
    settings: SettingsModel,
) {
    val coroutineScope = rememberCoroutineScope()
    val bitmapRenderController = LocalBitmapRenderController.current

    val plan = uiState.selectedDivePlanSet.getOrNull()
    if (plan != null && plan.isEmpty.not()) {
        IconButton(onClick = {
            coroutineScope.launch {
                bitmapRenderController.renderBitmap(
                    width = 1024,
                    height = null,
                    onRendered = {
                        shareImageBitmap(it)
                    }
                ) {
                    ShareImage(
                        divePlan = plan,
                        diveNumber = uiState.selectedDiveIndex + 1,
                        surfaceInterval = uiState.dives.getOrNull(uiState.selectedDiveIndex)?.surfaceIntervalBefore,
                        settingsModel = settings,
                    )
                }
            }
        }) {
            Icon(
                imageVector = IconSet.share,
                contentDescription = "Share"
            )
        }
    }

    IconButton(onClick = { navController.navigate(Destinations.DIVE_CONFIGURATION.destinationName) }) {
        Icon(
            painter = painterResource(resource = Res.drawable.ic_outline_tune_24),
            contentDescription = "Dive configuration"
        )
    }

    var showMenu by remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("Preferences") },
            onClick = {
                showMenu = false
                navController.navigate(Destinations.APP_CONFIGURATION.destinationName)
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(resource = Res.drawable.ic_outline_settings_24),
                    contentDescription = "Preferences"
                )
            }
        )
        DropdownMenuItem(
            text = { Text("About") },
            onClick = {
                showMenu = false
                navController.navigate(Destinations.ABOUT.destinationName)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "About"
                )
            }
        )
    }
    IconButton(onClick = { showMenu = true }) {
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = "More"
        )
    }
}

