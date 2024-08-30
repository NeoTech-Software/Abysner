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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.painterResource
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.PlanningRepository
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanInputModel
import org.neotech.app.abysner.domain.settings.SettingsRepository
import org.neotech.app.abysner.domain.settings.model.SettingsModel
import org.neotech.app.abysner.presentation.Destinations
import org.neotech.app.abysner.presentation.screens.ShareImage
import org.neotech.app.abysner.presentation.screens.planner.cylinders.CylinderPickerBottomSheet
import org.neotech.app.abysner.presentation.screens.planner.cylinders.CylinderSelectionCardComponent
import org.neotech.app.abysner.presentation.screens.planner.decoplan.DecoPlanCardComponent
import org.neotech.app.abysner.presentation.screens.planner.gasplan.GasPlanCardComponent
import org.neotech.app.abysner.presentation.screens.planner.segments.SegmentPickerBottomSheet
import org.neotech.app.abysner.presentation.screens.planner.segments.SegmentsCardComponent
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.presentation.theme.IconSet
import org.neotech.app.abysner.presentation.component.LocalBitmapRenderController
import org.neotech.app.abysner.presentation.utilities.shareImageBitmap

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

        val showCylinderPickerBottomSheet = remember { mutableStateOf(false) }
        val cylinderBeingEdited: MutableState<Cylinder?> = remember { mutableStateOf(null) }

        val segmentBeingEdited: MutableState<Int?> = remember { mutableStateOf(null) }
        val showSegmentPickerBottomSheet = remember { mutableStateOf(false) }

        // TODO move these into the ViewState and the collection to the ViewModel
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
                            AppBarActions(viewState, navController, settings)
                        })
                }
            }
        ) { paddingValues ->
            AnimatedVisibility(!viewState.isLoading, enter = fadeIn(), exit = fadeOut()) {
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
                            showCylinderPickerBottomSheet.value = true
                        },
                        onRemoveCylinder = { gas ->
                            viewModel.removeCylinder(gas)
                        },
                        onCylinderChecked = { gas, isChecked ->
                            viewModel.toggleCylinder(gas, isChecked)
                        },
                        onEditCylinder = { gas ->
                            cylinderBeingEdited.value = gas
                            showCylinderPickerBottomSheet.value = true
                        }
                    )

                    SegmentsCardComponent(
                        segments = viewState.segments,
                        addAllowed = viewState.availableGas.any { it.isChecked },
                        onAddSegment = {
                            segmentBeingEdited.value = null
                            showSegmentPickerBottomSheet.value = true
                        },
                        onRemoveSegment = { index, _ ->
                            viewModel.removeSegment(index)
                        },
                        onEditSegment = { index, _ ->
                            segmentBeingEdited.value = index
                            showSegmentPickerBottomSheet.value = true
                        },
                    )

                    DecoPlanCardComponent(
                        divePlanSet = viewState.divePlanSet.getOrNull(),
                        settings = settings,
                        planningException = viewState.divePlanSet.exceptionOrNull(),
                        isLoading = viewState.isCalculatingDivePlan,
                        onContingencyInputChanged = { deeper, longer ->
                            viewModel.setContingency(deeper, longer)
                        }
                    )
                    GasPlanCardComponent(
                        isLoading = viewState.isCalculatingDivePlan,
                        divePlanSet = viewState.divePlanSet.getOrNull(),
                        planningException = viewState.divePlanSet.exceptionOrNull(),
                    )
                }
            }

            ShowCylinderPickerBottomSheet(
                show = showCylinderPickerBottomSheet,
                configuration = configuration,
                cylinderBeingEdited = cylinderBeingEdited,
                viewModel = viewModel,
            )

            ShowSegmentPickerBottomSheet(
                show = showSegmentPickerBottomSheet,
                configuration = configuration,
                segmentBeingEdited = segmentBeingEdited,
                viewModel = viewModel,
                viewState = viewState
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowSegmentPickerBottomSheet(
    show: MutableState<Boolean>,
    viewState: PlanScreenViewModel.ViewState,
    segmentBeingEdited: MutableState<Int?>,
    configuration: Configuration,
    viewModel: PlanScreenViewModel,
) {
    if (show.value) {

        val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        val initial = segmentBeingEdited.value?.let {  viewState.segments[it] }
        val previousIndex = (segmentBeingEdited.value ?: ( viewState.segments.size)) - 1


        SegmentPickerBottomSheet(
            sheetState = bottomSheetState,
            isAdd = initial == null,
            initialValue = initial,
            maxPPO2 = configuration.maxPPO2,
            maxDensity = Gas.MAX_GAS_DENSITY,
            environment = configuration.environment,
            cylinders = viewState.availableGas.filter { it.isChecked }.map { it.cylinder },
            previousDepth =  viewState.segments.getOrNull(previousIndex)?.depth?.toDouble() ?: 0.0,
            configuration = configuration,
            onAddOrUpdateDiveSegment = {
                if (segmentBeingEdited.value != null) {
                    viewModel.updateSegment(segmentBeingEdited.value!!, it)
                } else {
                    viewModel.addSegment(it)
                }
            }
        ) {
            show.value = false
            segmentBeingEdited.value = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowCylinderPickerBottomSheet(
    show: MutableState<Boolean>,
    configuration: Configuration,
    cylinderBeingEdited: MutableState<Cylinder?>,
    viewModel: PlanScreenViewModel,
) {
    if (show.value) {

        val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        CylinderPickerBottomSheet(
            sheetState = bottomSheetState,
            isAdd = cylinderBeingEdited.value == null,
            initialValue = cylinderBeingEdited.value,
            environment = configuration.environment,
            maxPPO2 = configuration.maxPPO2,
            maxPPO2Secondary = configuration.maxPPO2Deco,
            onAddOrUpdateCylinder = {
                if (cylinderBeingEdited.value != null) {
                    viewModel.updateCylinder(it)
                } else {
                    viewModel.addCylinder(it)
                }
            }
        ) {
            show.value = false
            cylinderBeingEdited.value = null
        }
    }
}

@Composable
private fun RowScope.AppBarActions(
    viewState: PlanScreenViewModel.ViewState,
    navController: NavHostController,
    settings: SettingsModel,
) {
    val coroutineScope = rememberCoroutineScope()
    val bitmapRenderController = LocalBitmapRenderController.current

    val plan = viewState.divePlanSet.getOrNull()
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

    var showMenu by remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("Dive configuration") },
            onClick = {
                showMenu = false
                navController.navigate(Destinations.DIVE_CONFIGURATION.destinationName)
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(resource = Res.drawable.ic_outline_tune_24),
                    contentDescription = "Dive configuration"
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Settings") },
            onClick = {
                showMenu = false
                navController.navigate(Destinations.APP_CONFIGURATION.destinationName)
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(resource = Res.drawable.ic_outline_settings_24),
                    contentDescription = "Settings"
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

@Preview
@Composable
private fun PlannerScreenPreview() {

    val planningRepository = object : PlanningRepository {
        override val configuration = MutableStateFlow(Configuration())

        override fun updateConfiguration(updateBlock: (Configuration) -> Configuration) = Unit

        override suspend fun getDivePlanInput(): DivePlanInputModel? = null

        override fun setDivePlanInput(divePlanInputModel: DivePlanInputModel) = Unit
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
