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

package org.neotech.app.abysner.presentation.screens.planner

import abysner.composeapp.generated.resources.Res
import abysner.composeapp.generated.resources.ic_outline_settings_24
import abysner.composeapp.generated.resources.ic_outline_tune_24
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableStateFlow
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.painterResource
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.PlanningRepository
import org.neotech.app.abysner.domain.settings.SettingsRepository
import org.neotech.app.abysner.domain.settings.model.SettingsModel
import org.neotech.app.abysner.presentation.Destinations
import org.neotech.app.abysner.presentation.screens.planner.cylinders.CylinderSelectionCardComponent
import org.neotech.app.abysner.presentation.screens.planner.decoplan.DecoPlanCardComponent
import org.neotech.app.abysner.presentation.screens.planner.cylinders.CylinderPickerBottomSheet
import org.neotech.app.abysner.presentation.screens.planner.gasplan.GasPlanCardComponent
import org.neotech.app.abysner.presentation.screens.planner.plan.PlanPickerBottomSheet
import org.neotech.app.abysner.presentation.screens.planner.plan.PlanSelectionCardComponent
import org.neotech.app.abysner.presentation.theme.AbysnerTheme

typealias PlannerScreen = @Composable (navController: NavHostController) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Inject
@Composable
fun PlannerScreen(
    planningRepository: PlanningRepository,
    settingsRepository: SettingsRepository,
    viewModelCreator: () -> PlanScreenViewModel,
    @Assisted navController: NavHostController = rememberNavController()
) {

    val viewModel = viewModel {
        viewModelCreator()
    }

    val viewState: PlanScreenViewModel.ViewState by viewModel.uiState.collectAsState()

    AbysnerTheme {

        val cylinderPickerBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var showCylinderPickerBottomSheet by remember { mutableStateOf(false) }
        var cylinderBeingEdited: Cylinder? by remember { mutableStateOf(null) }

        val segmentPickerBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var segmentBeingEdited: Int? by remember { mutableStateOf(null) }
        var showSegmentPickerBottomSheet by remember { mutableStateOf(false) }

        val configuration by planningRepository.configuration.collectAsState()
        val settings by settingsRepository.settings.collectAsState()

        Scaffold(
            topBar = {
                Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.background) {
                    TopAppBar(
                        title = {
                            Column {
                                Text("Abysner")
                                Text(
                                    style = MaterialTheme.typography.labelSmall,
                                    text = "The open-source dive planner"
                                )
                            }
                        },
                        actions = {
                            var showMenu by remember { mutableStateOf(false) }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Dive configuration")},
                                    onClick = {
                                        showMenu = false
                                        navController.navigate(Destinations.DIVE_CONFIGURATION.destinationName) },
                                    leadingIcon = { Icon(painter = painterResource(resource = Res.drawable.ic_outline_tune_24), contentDescription = "Dive configuration") }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings")},
                                    onClick = {
                                        showMenu = false
                                        navController.navigate(Destinations.APP_CONFIGURATION.destinationName)
                                              },
                                    leadingIcon = { Icon(painter = painterResource(resource = Res.drawable.ic_outline_settings_24), contentDescription = "Settings") }
                                )
                                DropdownMenuItem(
                                    text = { Text("About")},
                                    onClick = {
                                        showMenu = false
                                        navController.navigate(Destinations.ABOUT.destinationName) },
                                    leadingIcon = { Icon(imageVector = Icons.Outlined.Info, contentDescription = "About") }
                                )
                            }
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "More"
                                )
                            }
                        })
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(
                        androidx.compose.foundation.layout.WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal
                        )
                    )
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                CylinderSelectionCardComponent(
                    gases = viewState.availableGas,
                    onAddCylinder = {
                        showCylinderPickerBottomSheet = true
                    },
                    onRemoveCylinder = { gas ->
                        viewModel.removeCylinder(gas)
                    },
                    onCylinderChecked = { gas, isChecked ->
                        viewModel.toggleCylinder(gas, isChecked)
                        // TODO check if toggle is possible
                    },
                    onEditCylinder = { gas ->
                        cylinderBeingEdited = gas
                        showCylinderPickerBottomSheet = true
                    }
                )

                PlanSelectionCardComponent(
                    segments = viewState.segments,
                    addAllowed = viewState.availableGas.any { it.isChecked },
                    onAddSegment = {
                        segmentBeingEdited = null
                        showSegmentPickerBottomSheet = true
                    },
                    onRemoveSegment = { index, _ ->
                        viewModel.removeSegment(index)
                    },
                    onEditSegment = { index, _ ->
                        segmentBeingEdited = index
                        showSegmentPickerBottomSheet = true
                    },
                )

                DecoPlanCardComponent(
                    divePlanSet = viewState.divePlanSet.getOrNull(),
                    settings = settings,
                    planningException = viewState.divePlanSet.exceptionOrNull(),
                    isLoading = viewState.isLoading
                )
                GasPlanCardComponent(
                    isLoading = viewState.isLoading,
                    divePlanSet = viewState.divePlanSet.getOrNull(),
                    planningException = viewState.divePlanSet.exceptionOrNull(),
                )
            }
        }

        if (showCylinderPickerBottomSheet) {
            CylinderPickerBottomSheet(
                sheetState = cylinderPickerBottomSheetState,
                isAdd = cylinderBeingEdited == null,
                initialValue = cylinderBeingEdited,
                environment = configuration.environment,
                maxPPO2 = configuration.maxPPO2Deco,
                onAddOrUpdateCylinder = {
                    if(cylinderBeingEdited != null) {
                        viewModel.updateCylinder(it)
                    } else {
                        viewModel.addCylinder(it)
                    }
                }
            ) {
                showCylinderPickerBottomSheet = false
                cylinderBeingEdited = null
            }
        }

        if (showSegmentPickerBottomSheet) {

            val initial = segmentBeingEdited?.let { viewState.segments[it] }

            PlanPickerBottomSheet(
                sheetState = segmentPickerBottomSheetState,
                isAdd = initial == null,
                initialValue = initial,
                maxPPO2 = configuration.maxPPO2,
                maxDensity = Gas.MAX_GAS_DENSITY,
                environment = configuration.environment,
                cylinders = viewState.availableGas.filter { it.isChecked }.map { it.cylinder },
                onAddOrUpdateDiveSegment = {
                    if(segmentBeingEdited != null) {
                        viewModel.updateSegment(segmentBeingEdited!!, it)
                    } else {
                        viewModel.addSegment(it)
                    }
                }
            ) {
                showSegmentPickerBottomSheet = false
                segmentBeingEdited = null
            }
        }
    }
}

@Preview
@Composable
private fun PlannerScreenPreview() {

    val planningRepository = object : PlanningRepository {
        override val configuration = MutableStateFlow(Configuration())

        override fun updateConfiguration(updateBlock: (Configuration) -> Configuration) = Unit
    }

    val settingsRepository = object : SettingsRepository {
        override val settings = MutableStateFlow(SettingsModel())

        override suspend fun setTermsAndConditionsAccepted(accepted: Boolean) {
        }

        override suspend fun getSettings(): SettingsModel = settings.value

        override fun updateSettings(updateBlock: (SettingsModel) -> SettingsModel) = Unit
    }

    PlannerScreen(
        planningRepository = planningRepository,
        settingsRepository = settingsRepository,
        viewModelCreator = {
            PlanScreenViewModel(planningRepository)
        }
    )
}
