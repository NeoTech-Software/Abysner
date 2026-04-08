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

package org.neotech.app.abysner.presentation.screens.planner

import abysner.composeapp.generated.resources.Res
import abysner.composeapp.generated.resources.ic_outline_settings_24
import abysner.composeapp.generated.resources.ic_outline_tune_24
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.painterResource
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.diveplanning.model.MultiDivePlanSet
import org.neotech.app.abysner.domain.settings.model.SettingsModel
import org.neotech.app.abysner.presentation.Destinations
import org.neotech.app.abysner.presentation.component.DefaultPathwayButtonItem
import org.neotech.app.abysner.presentation.screens.ShareImage
import org.neotech.app.abysner.presentation.screens.planner.cylinders.CylinderPickerBottomSheet
import org.neotech.app.abysner.presentation.screens.planner.cylinders.CylinderSelectionCardComponent
import org.neotech.app.abysner.presentation.screens.planner.decoplan.DecoPlanCardComponent
import org.neotech.app.abysner.presentation.screens.planner.gasplan.GasPlanCardComponent
import org.neotech.app.abysner.presentation.screens.planner.segments.SegmentPickerBottomSheet
import org.neotech.app.abysner.presentation.screens.planner.segments.SegmentsCardComponent
import org.neotech.app.abysner.presentation.screens.planner.surfaceinterval.DiveConfigurationBottomSheet
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.presentation.theme.IconSet
import org.neotech.app.abysner.presentation.component.BitmapRenderController
import org.neotech.app.abysner.presentation.component.LocalBitmapRenderController
import org.neotech.app.abysner.presentation.component.PathwayButtonsComponent
import org.neotech.app.abysner.presentation.component.appbar.UnboundedBox
import org.neotech.app.abysner.presentation.component.core.toPx
import org.neotech.app.abysner.presentation.preview.DEVICE_PHONE_MAX_HEIGHT
import org.neotech.app.abysner.presentation.preview.PreviewData
import org.neotech.app.abysner.presentation.utilities.shareImageBitmap
import kotlin.time.Duration

typealias PlannerScreen = @Composable (navController: NavHostController) -> Unit

@Inject
@Composable
fun PlannerScreen(
    viewModelCreator: () -> PlanScreenViewModel,
    @Assisted navController: NavHostController = rememberNavController()
) {
    val viewModel = viewModel {
        viewModelCreator()
    }

    val uiState: PlanScreenViewModel.UiState by viewModel.uiState.collectAsState()

    PlannerScreen(
        uiState = uiState,
        navController = navController,
        onAddCylinder = { viewModel.addCylinder(it) },
        onUpdateCylinder = { viewModel.updateCylinder(it) },
        onRemoveCylinder = { viewModel.removeCylinder(it) },
        onToggleCylinder = { cylinder, isChecked -> viewModel.toggleCylinder(cylinder, isChecked) },
        onAddSegment = { viewModel.addSegment(it) },
        onUpdateSegment = { index, segment -> viewModel.updateSegment(index, segment) },
        onRemoveSegment = { viewModel.removeSegment(it) },
        onContingencyInputChanged = { deeper, longer -> viewModel.setContingency(deeper, longer) },
        onSelectDive = { viewModel.selectDive(it) },
        onAddDive = { viewModel.addDive(it) },
        onRemoveDive = { viewModel.removeDive(it) },
        onUpdateSurfaceInterval = { i, d -> viewModel.updateSurfaceInterval(i, d) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    uiState: PlanScreenViewModel.UiState,
    navController: NavHostController = rememberNavController(),
    onAddCylinder: (Cylinder) -> Unit = {},
    onUpdateCylinder: (Cylinder) -> Unit = {},
    onRemoveCylinder: (Cylinder) -> Unit = {},
    onToggleCylinder: (Cylinder, Boolean) -> Unit = { _, _ -> },
    onAddSegment: (DiveProfileSection) -> Unit = {},
    onUpdateSegment: (Int, DiveProfileSection) -> Unit = { _, _ -> },
    onRemoveSegment: (Int) -> Unit = {},
    onContingencyInputChanged: (Boolean, Boolean) -> Unit = { _, _ -> },
    onSelectDive: (Int) -> Unit = {},
    onAddDive: (Duration) -> Unit = {},
    onRemoveDive: (Int) -> Unit = {},
    onUpdateSurfaceInterval: (Int, Duration) -> Unit = { _, _ -> },
) {

    AbysnerTheme {

        val showCylinderPickerBottomSheet = remember { mutableStateOf(false) }
        val cylinderBeingEdited: MutableState<Cylinder?> = remember { mutableStateOf(null) }

        val segmentBeingEdited: MutableState<Int?> = remember { mutableStateOf(null) }
        val showSegmentPickerBottomSheet = remember { mutableStateOf(false) }

        var showSurfaceIntervalPicker by remember { mutableStateOf(false) }
        var diveBeingEdited: Int? by remember { mutableStateOf(null) }

        val scrollState = rememberScrollState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.background) {
                    Column {
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
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Unspecified,
                                scrolledContainerColor = Color.Unspecified
                            ),
                            actions = {
                                AppBarActions(uiState, navController, uiState.settingsModel)
                            },
                        )

                        val height = 64.dp.toPx()

                        SideEffect {
                            if (scrollBehavior.state.heightOffsetLimit != -height) {
                                scrollBehavior.state.heightOffsetLimit = -height
                            }
                        }

                        val offset = with(LocalDensity.current) { scrollBehavior.state.heightOffset.toDp() }

                        // Allow children to be unbounded, so they take up as much space as they
                        // need, so that when the box itself shrinks the children do not chance size.
                        UnboundedBox(
                            modifier = Modifier.clipToBounds().height(64.dp + offset).alpha(1f - scrollBehavior.state.collapsedFraction),
                            constrainChildrenVertical = false,
                            alignment = Alignment.BottomStart
                        ) {
                            PathwayButtonsComponent(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                selectedButton = uiState.selectedDiveIndex,
                                buttonLabels = List(uiState.diveCount) { i ->
                                    DefaultPathwayButtonItem(
                                        buttonLabel = "Dive ${i + 1}",
                                        nextConnectorLabel = uiState.surfaceIntervals.getOrNull(i)?.toHHMM() ?: "",
                                    )
                                },
                                onClick = { index, _ ->
                                    if (index == uiState.selectedDiveIndex) {
                                        // Tapping the already-selected dive opens the edit sheet
                                        // (same condition as before: there must be something to edit).
                                        if (index > 0 || uiState.diveCount > 1) {
                                            diveBeingEdited = index
                                        }
                                    } else {
                                        onSelectDive(index)
                                    }
                                },
                                onAddClicked = { showSurfaceIntervalPicker = true },
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            AnimatedVisibility(!uiState.isLoading, enter = fadeIn(), exit = fadeOut()) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
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
                        gases = uiState.availableGas,
                        onAddCylinder = {
                            showCylinderPickerBottomSheet.value = true
                        },
                        onRemoveCylinder = { gas ->
                            onRemoveCylinder(gas)
                        },
                        onCylinderChecked = { gas, isChecked ->
                            onToggleCylinder(gas, isChecked)
                        },
                        onEditCylinder = { gas ->
                            cylinderBeingEdited.value = gas
                            showCylinderPickerBottomSheet.value = true
                        }
                    )

                    SegmentsCardComponent(
                        segments = uiState.segments,
                        addAllowed = uiState.availableGas.isNotEmpty(),
                        onAddSegment = {
                            segmentBeingEdited.value = null
                            showSegmentPickerBottomSheet.value = true
                        },
                        onRemoveSegment = { index, _ ->
                            onRemoveSegment(index)
                        },
                        onEditSegment = { index, _ ->
                            segmentBeingEdited.value = index
                            showSegmentPickerBottomSheet.value = true
                        },
                    )

                    DecoPlanCardComponent(
                        divePlanSet = uiState.selectedDivePlanSet.getOrNull(),
                        settings = uiState.settingsModel,
                        planningException = uiState.selectedDivePlanSet.exceptionOrNull()
                            ?: uiState.multiDivePlanSet.exceptionOrNull(),
                        isLoading = uiState.isCalculatingDivePlan,
                        onContingencyInputChanged = { deeper, longer ->
                            onContingencyInputChanged(deeper, longer)
                        }
                    )
                    GasPlanCardComponent(
                        isLoading = uiState.isCalculatingDivePlan,
                        divePlanSet = uiState.selectedDivePlanSet.getOrNull(),
                        planningException = uiState.selectedDivePlanSet.exceptionOrNull(),
                    )
                }
            }

            ShowCylinderPickerBottomSheet(
                show = showCylinderPickerBottomSheet,
                configuration = uiState.configuration,
                cylinderBeingEdited = cylinderBeingEdited,
                onAddCylinder = onAddCylinder,
                onUpdateCylinder = onUpdateCylinder,
            )

            ShowSegmentPickerBottomSheet(
                show = showSegmentPickerBottomSheet,
                configuration = uiState.configuration,
                segmentBeingEdited = segmentBeingEdited,
                uiState = uiState,
                onAddSegment = onAddSegment,
                onUpdateSegment = onUpdateSegment,
            )

            if (showSurfaceIntervalPicker) {
                DiveConfigurationBottomSheet(
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    title = "Dive ${uiState.diveCount + 1}",
                    onConfirm = { duration: Duration -> onAddDive(duration) },
                    onDismiss = { showSurfaceIntervalPicker = false },
                )
            }

            val editIndex = diveBeingEdited
            if (editIndex != null) {
                val currentInterval = if (editIndex > 0) {
                    uiState.surfaceIntervals.getOrNull(editIndex - 1)
                } else {
                    null
                }
                DiveConfigurationBottomSheet(
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    title = "Dive ${editIndex + 1}",
                    initialValue = currentInterval,
                    onConfirm = { duration: Duration -> onUpdateSurfaceInterval(editIndex, duration) },
                    onDismiss = { diveBeingEdited = null },
                    onDelete = { onRemoveDive(editIndex) },
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowSegmentPickerBottomSheet(
    show: MutableState<Boolean>,
    uiState: PlanScreenViewModel.UiState,
    segmentBeingEdited: MutableState<Int?>,
    configuration: Configuration,
    onAddSegment: (DiveProfileSection) -> Unit,
    onUpdateSegment: (Int, DiveProfileSection) -> Unit,
) {
    if (show.value) {

        val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        val initial = segmentBeingEdited.value?.let { uiState.segments[it] }
        val previousIndex = (segmentBeingEdited.value ?: (uiState.segments.size)) - 1


        SegmentPickerBottomSheet(
            sheetState = bottomSheetState,
            isAdd = initial == null,
            initialValue = initial,
            maxPPO2 = configuration.maxPPO2,
            maxDensity = Gas.MAX_GAS_DENSITY,
            environment = configuration.environment,
            cylinders = uiState.availableGas.map { it.cylinder }.toImmutableList(),
            previousDepth = uiState.segments.getOrNull(previousIndex)?.depth?.toDouble() ?: 0.0,
            configuration = configuration,
            onAddOrUpdateDiveSegment = {
                if (segmentBeingEdited.value != null) {
                    onUpdateSegment(segmentBeingEdited.value!!, it)
                } else {
                    onAddSegment(it)
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
    onAddCylinder: (Cylinder) -> Unit,
    onUpdateCylinder: (Cylinder) -> Unit,
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
                    onUpdateCylinder(it)
                } else {
                    onAddCylinder(it)
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

private fun Duration.toHHMM(): String {
    val h = inWholeHours
    val m = inWholeMinutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

@Preview(device = DEVICE_PHONE_MAX_HEIGHT)
@Composable
fun PlannerScreenPreview() {    CompositionLocalProvider(
        LocalBitmapRenderController provides remember { BitmapRenderController() },
    ) {
        PlannerScreen(
            uiState = PlanScreenViewModel.UiState(
                isLoading = false,
                isCalculatingDivePlan = false,
                segments = PreviewData.divePlan1Segments,
                availableGas = PreviewData.divePlan1Cylinders,
                configuration = PreviewData.divePlan1.configuration,
                selectedDivePlanSet = Result.success(PreviewData.divePlan1),
                multiDivePlanSet = Result.success(MultiDivePlanSet(divePlanSets = listOf(PreviewData.divePlan1)))
            )
        )
    }
}

/**
 * Preview that shows the planner screen with a dive plan designed to trigger as many warnings
 * as possible. See [PreviewData.divePlan2] for details.
 */
@Preview(device = DEVICE_PHONE_MAX_HEIGHT)
@Composable
fun PlannerScreenWithWarningsPreview() {
    CompositionLocalProvider(
        LocalBitmapRenderController provides remember { BitmapRenderController() },
    ) {
        PlannerScreen(
            uiState = PlanScreenViewModel.UiState(
                isLoading = false,
                isCalculatingDivePlan = false,
                segments = PreviewData.divePlan2Segments,
                availableGas = PreviewData.divePlan2Cylinders,
                configuration = PreviewData.divePlan2.configuration,
                selectedDivePlanSet = Result.success(PreviewData.divePlan2),
                multiDivePlanSet = Result.success(MultiDivePlanSet(divePlanSets = listOf(PreviewData.divePlan2))),
                settingsModel = SettingsModel(showBasicDecoTable = true)
            )
        )
    }
}
