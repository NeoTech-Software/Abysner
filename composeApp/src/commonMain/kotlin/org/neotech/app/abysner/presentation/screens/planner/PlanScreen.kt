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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.collections.immutable.toImmutableList
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.Inject
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.DiveMode
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.diveplanning.model.MultiDivePlanSet
import org.neotech.app.abysner.domain.settings.model.SettingsModel
import org.neotech.app.abysner.presentation.component.BitmapRenderController
import org.neotech.app.abysner.presentation.screens.planner.cylinders.CylinderPickerBottomSheetHost
import org.neotech.app.abysner.presentation.screens.planner.segments.SegmentPickerBottomSheetHost
import org.neotech.app.abysner.presentation.screens.planner.diveconfiguration.DiveConfigurationBottomSheetHost
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.presentation.component.DefaultPathwayButtonItem
import org.neotech.app.abysner.presentation.component.LocalBitmapRenderController
import org.neotech.app.abysner.presentation.component.PathwayButtonsComponent
import org.neotech.app.abysner.presentation.component.appbar.UnboundedBox
import org.neotech.app.abysner.presentation.component.core.toPx
import org.neotech.app.abysner.presentation.utilities.ModalTarget
import org.neotech.app.abysner.presentation.utilities.rememberModalTarget
import org.neotech.app.abysner.presentation.formatting.toHHMM
import org.neotech.app.abysner.presentation.preview.DEVICE_FOLDABLE_MAX_HEIGHT
import org.neotech.app.abysner.presentation.preview.DEVICE_PHONE_MAX_HEIGHT
import org.neotech.app.abysner.presentation.preview.PreviewData
import kotlin.time.Duration


@Inject
@Composable
fun PlannerScreen(
    viewModelCreator: () -> PlanScreenViewModel,
    @Assisted navController: NavHostController,
) {
    val viewModel = viewModel { viewModelCreator() }
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
        onContingencyInputChanged = { deeper, longer, bailout ->
            viewModel.setContingency(
                deeper,
                longer,
                bailout
            )
        },
        onSelectDive = { viewModel.selectDive(it) },
        onAddDive = { viewModel.addDive(it) },
        onRemoveDive = { viewModel.removeDive(it) },
        onUpdateSurfaceInterval = { i, d -> viewModel.updateSurfaceInterval(i, d) },
        onDiveModeChanged = { viewModel.setDiveMode(it) },
        onAvailableForBailoutChanged = { cylinder, value ->
            viewModel.toggleAvailableForBailout(
                cylinder,
                value
            )
        },
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
    onContingencyInputChanged: (Boolean, Boolean, Boolean) -> Unit = { _, _, _ -> },
    onSelectDive: (Int) -> Unit = {},
    onAddDive: (Duration) -> Unit = {},
    onRemoveDive: (Int) -> Unit = {},
    onUpdateSurfaceInterval: (Int, Duration) -> Unit = { _, _ -> },
    onDiveModeChanged: (DiveMode) -> Unit = {},
    onAvailableForBailoutChanged: (Cylinder, Boolean) -> Unit = { _, _ -> },
) {
    AbysnerTheme {

        var cylinderSheet: ModalTarget<Cylinder>? by rememberModalTarget()
        var segmentSheet: ModalTarget<Int>? by rememberModalTarget()
        var diveSheet: ModalTarget<Int>? by rememberModalTarget()

        val scrollState = rememberScrollState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

        val diveButtonLabels = List(uiState.dives.size) { i ->
            DefaultPathwayButtonItem(
                buttonLabel = "Dive ${i + 1}",
                nextConnectorLabel = uiState.dives.getOrNull(i + 1)?.surfaceIntervalBefore?.toHHMM() ?: "",
            )
        }
        val onDiveButtonClick: (Int, DefaultPathwayButtonItem) -> Unit = { index, _ ->
            if (index == uiState.selectedDiveIndex) {
                diveSheet = ModalTarget.Edit(index)
            } else {
                onSelectDive(index)
            }
        }

        BoxWithConstraints {
            val isWide = maxWidth >= 600.dp

            if (isWide) {
                Scaffold(
                    // In the wide two-panels mode the panels manage their insets per-panel
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = { PlannerTopAppBar(uiState, navController) },
                ) { paddingValues ->
                    Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        Surface(
                            modifier = Modifier.width(240.dp).fillMaxHeight(),
                            shadowElevation = 4.dp,
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            PathwayButtonsComponent(
                                vertical = true,
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    // End safe paddings are not required, the right panel takes care of those (top is taken care of by the app bar)
                                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Bottom))
                                    .padding(vertical = 16.dp, horizontal = 16.dp),
                                selectedButton = uiState.selectedDiveIndex,
                                buttonLabels = diveButtonLabels,
                                onClick = onDiveButtonClick,
                                onAddClicked = { diveSheet = ModalTarget.Add },
                                addButtonLabel = "Add dive",
                                limit = MAX_DIVES,
                                limitTooltipText = MAX_DIVES_TOOLTIP,
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            PlanScreenContent(
                                isLoading = uiState.isLoading,
                                uiState = uiState,
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End + WindowInsetsSides.Bottom))
                                    .padding(16.dp),
                                onAddCylinder = { cylinderSheet = ModalTarget.Add },
                                onEditCylinder = { cylinderSheet = ModalTarget.Edit(it) },
                                onRemoveCylinder = onRemoveCylinder,
                                onToggleCylinder = onToggleCylinder,
                                onAddSegment = { segmentSheet = ModalTarget.Add },
                                onEditSegment = { segmentSheet = ModalTarget.Edit(it) },
                                onRemoveSegment = onRemoveSegment,
                                onContingencyInputChanged = onContingencyInputChanged,
                            )
                        }
                    }
                }
            } else {
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        PlannerTopAppBar(uiState, navController) {
                            val height = 64.dp.toPx()
                            SideEffect {
                                if (scrollBehavior.state.heightOffsetLimit != -height) {
                                    scrollBehavior.state.heightOffsetLimit = -height
                                }
                            }
                            val offset = with(LocalDensity.current) { scrollBehavior.state.heightOffset.toDp() }
                            UnboundedBox(
                                modifier = Modifier
                                    .clipToBounds()
                                    .height(64.dp + offset)
                                    .alpha(1f - scrollBehavior.state.collapsedFraction),
                                constrainChildrenVertical = false,
                                alignment = Alignment.BottomStart,
                            ) {
                                PathwayButtonsComponent(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                                    selectedButton = uiState.selectedDiveIndex,
                                    buttonLabels = diveButtonLabels,
                                    onClick = onDiveButtonClick,
                                    onAddClicked = { diveSheet = ModalTarget.Add },
                                    addButtonLabel = "Add dive",
                                    limit = MAX_DIVES,
                                    limitTooltipText = MAX_DIVES_TOOLTIP,
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    PlanScreenContent(
                        isLoading = uiState.isLoading,
                        uiState = uiState,
                        modifier = Modifier
                            .verticalScroll(scrollState)
                            .padding(paddingValues)
                            .padding(16.dp),
                        onAddCylinder = { cylinderSheet = ModalTarget.Add },
                        onEditCylinder = { cylinderSheet = ModalTarget.Edit(it) },
                        onRemoveCylinder = onRemoveCylinder,
                        onToggleCylinder = onToggleCylinder,
                        onAddSegment = { segmentSheet = ModalTarget.Add },
                        onEditSegment = { segmentSheet = ModalTarget.Edit(it) },
                        onRemoveSegment = onRemoveSegment,
                        onContingencyInputChanged = onContingencyInputChanged,
                    )
                }
            }

            CylinderPickerBottomSheetHost(
                show = cylinderSheet,
                configuration = uiState.configuration,
                diveMode = uiState.diveMode,
                cylinders = uiState.availableGas,
                segments = uiState.segments,
                onDismiss = { cylinderSheet = null },
                onAddCylinder = onAddCylinder,
                onUpdateCylinder = onUpdateCylinder,
                onAvailableForBailoutChanged = onAvailableForBailoutChanged,
            )

            SegmentPickerBottomSheetHost(
                show = segmentSheet,
                configuration = uiState.configuration,
                segments = uiState.segments,
                diveMode = uiState.diveMode,
                cylinders = uiState.availableGas.toImmutableList(),
                onDismiss = { segmentSheet = null },
                onAddSegment = onAddSegment,
                onUpdateSegment = onUpdateSegment,
            )

            DiveConfigurationBottomSheetHost(
                show = diveSheet,
                dives = uiState.dives,
                onAddDive = onAddDive,
                onUpdateSurfaceInterval = onUpdateSurfaceInterval,
                onRemoveDive = onRemoveDive,
                onDiveModeChanged = onDiveModeChanged,
                onDismiss = { diveSheet = null },
            )
        }
    }
}

private const val MAX_DIVES = 10
private const val MAX_DIVES_TOOLTIP = "Easy there, Cousteau!\nPlans are limited to $MAX_DIVES dives."

@Preview(device = DEVICE_PHONE_MAX_HEIGHT)
@Composable
fun PlannerScreenPreview() {
    CompositionLocalProvider(
        LocalBitmapRenderController provides remember { BitmapRenderController() },
    ) {
        PlannerScreen(
            uiState = PlanScreenViewModel.UiState(
                isLoading = false,
                isCalculatingDivePlan = false,
                segments = PreviewData.diveProfile30Meters,
                availableGas = PreviewData.divePlan1Cylinders,
                configuration = PreviewData.divePlan1.configuration,
                selectedDivePlanSet = Result.success(PreviewData.divePlan1),
                multiDivePlanSet = Result.success(MultiDivePlanSet(divePlanSets = listOf(PreviewData.divePlan1)))
            )
        )
    }
}

/**
 * Preview that shows the planner screen with a dive plan designed to trigger as many warnings as
 * possible. See [PreviewData.divePlan2] for details.
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

@Preview(device = DEVICE_FOLDABLE_MAX_HEIGHT)
@Composable
fun PlannerScreenFoldablePreview() {
    CompositionLocalProvider(
        LocalBitmapRenderController provides remember { BitmapRenderController() },
    ) {
        PlannerScreen(
            uiState = PlanScreenViewModel.UiState(
                isLoading = false,
                isCalculatingDivePlan = false,
                segments = PreviewData.diveProfile30Meters,
                availableGas = PreviewData.divePlan1Cylinders,
                configuration = PreviewData.divePlan1.configuration,
                selectedDivePlanSet = Result.success(PreviewData.divePlan1),
                multiDivePlanSet = Result.success(MultiDivePlanSet(divePlanSets = listOf(PreviewData.divePlan1)))
            )
        )
    }
}
