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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.presentation.screens.planner.cylinders.CylinderSelectionCardComponent
import org.neotech.app.abysner.presentation.screens.planner.decoplan.DecoPlanCardComponent
import org.neotech.app.abysner.presentation.screens.planner.gasplan.GasPlanCardComponent
import org.neotech.app.abysner.presentation.screens.planner.segments.SegmentsCardComponent

@Composable
internal fun PlanScreenContent(
    isLoading: Boolean,
    uiState: PlanScreenViewModel.UiState,
    modifier: Modifier = Modifier,
    onAddCylinder: () -> Unit,
    onEditCylinder: (Cylinder) -> Unit,
    onRemoveCylinder: (Cylinder) -> Unit,
    onToggleCylinder: (Cylinder, Boolean) -> Unit,
    onAddSegment: () -> Unit,
    onEditSegment: (Int) -> Unit,
    onRemoveSegment: (Int) -> Unit,
    onContingencyInputChanged: (Boolean, Boolean) -> Unit,
) {
    AnimatedVisibility(!isLoading, enter = fadeIn(), exit = fadeOut()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                // The cards are currently not designed to become super wide, so we limit the
                // maximum width in the future show them perhaps in two columns on very wide screens?
                .widthIn(max = 500.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CylinderSelectionCardComponent(
                gases = uiState.availableGas,
                onAddCylinder = onAddCylinder,
                onRemoveCylinder = onRemoveCylinder,
                onCylinderChecked = onToggleCylinder,
                onEditCylinder = onEditCylinder,
            )
            SegmentsCardComponent(
                segments = uiState.segments,
                addAllowed = uiState.availableGas.isNotEmpty(),
                onAddSegment = onAddSegment,
                onRemoveSegment = { index, _ -> onRemoveSegment(index) },
                onEditSegment = { index, _ -> onEditSegment(index) },
            )
            DecoPlanCardComponent(
                divePlanSet = uiState.selectedDivePlanSet.getOrNull(),
                settings = uiState.settingsModel,
                planningException = uiState.selectedDivePlanSet.exceptionOrNull() ?: uiState.multiDivePlanSet.exceptionOrNull(),
                isLoading = uiState.isCalculatingDivePlan,
                onContingencyInputChanged = onContingencyInputChanged,
            )
            GasPlanCardComponent(
                isLoading = uiState.isCalculatingDivePlan,
                divePlanSet = uiState.selectedDivePlanSet.getOrNull(),
                planningException = uiState.selectedDivePlanSet.exceptionOrNull(),
            )
        }
    }
}

