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

package org.neotech.app.abysner.presentation.screens.planner.diveconfiguration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.neotech.app.abysner.domain.core.model.DiveMode
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanInputModel
import org.neotech.app.abysner.presentation.component.RadioCardGroup
import org.neotech.app.abysner.presentation.component.RadioCardItem
import org.neotech.app.abysner.presentation.component.bottomsheet.BottomSheetHeader
import org.neotech.app.abysner.presentation.component.bottomsheet.ModalBottomSheetScaffold
import org.neotech.app.abysner.presentation.component.textfield.DurationInputField
import org.neotech.app.abysner.presentation.utilities.ModalTarget
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private val diveModeItems = persistentListOf(
    RadioCardItem(
        title = "Open Circuit",
        description = "Recreational and multi-gas technical diving, with reserve gas planning.",
    ),
    RadioCardItem(
        title = "Closed Circuit Rebreather",
        description = "Rebreather diving with configurable high/low setpoints and bail-out planning.",
    ),
)

private fun DiveMode.toSelectionIndex() = when (this) {
    DiveMode.OPEN_CIRCUIT -> 0
    DiveMode.CLOSED_CIRCUIT -> 1
}

private fun Int.toDiveMode() = when (this) {
    1 -> DiveMode.CLOSED_CIRCUIT
    else -> DiveMode.OPEN_CIRCUIT
}

/**
 * A bottom sheet for configuring a dive: the dive mode (OC/CCR) and optionally the surface
 * interval for follow-up dives.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiveConfigurationBottomSheetHost(
    show: ModalTarget<Int>?,
    dives: List<DivePlanInputModel>,
    onAddDive: (Duration) -> Unit,
    onUpdateSurfaceInterval: (Int, Duration) -> Unit,
    onRemoveDive: (Int) -> Unit,
    onDiveModeChanged: (DiveMode) -> Unit,
    onDismiss: () -> Unit,
) {
    if (show != null) {
        val editIndex = (show as? ModalTarget.Edit)?.value
        DiveConfigurationBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            title = "Dive ${(editIndex ?: dives.size) + 1}",
            initialSurfaceInterval = when (show) {
                is ModalTarget.Add -> 60.minutes
                is ModalTarget.Edit -> dives.getOrNull(show.value)?.surfaceIntervalBefore
            },
            initialDiveMode = when (show) {
                is ModalTarget.Add -> DiveMode.OPEN_CIRCUIT
                is ModalTarget.Edit -> dives.getOrNull(show.value)?.diveMode ?: DiveMode.OPEN_CIRCUIT
            },
            onConfirm = { duration, diveMode ->
                if (editIndex != null) {
                    // duration is null for the first dive (no surface interval field).
                    if (duration != null) {
                        onUpdateSurfaceInterval(editIndex, duration)
                    }
                } else {
                    onAddDive(duration ?: 60.minutes)
                }
                onDiveModeChanged(diveMode)
            },
            onDismiss = onDismiss,
            onDelete = editIndex?.takeIf { dives.size > 1 }?.let { { onRemoveDive(it) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiveConfigurationBottomSheet(
    sheetState: SheetState = rememberStandardBottomSheetState(),
    title: String,
    initialSurfaceInterval: Duration? = 60.minutes,
    initialDiveMode: DiveMode = DiveMode.OPEN_CIRCUIT,
    onConfirm: (Duration?, DiveMode) -> Unit = { _, _ -> },
    onDismiss: () -> Unit = {},
    onDelete: (() -> Unit)? = null,
) {
    ModalBottomSheet(
        dragHandle = {},
        sheetState = sheetState,
        onDismissRequest = onDismiss,
    ) {
        DiveConfigurationBottomSheetContent(
            sheetState = sheetState,
            title = title,
            initialSurfaceInterval = initialSurfaceInterval,
            initialDiveMode = initialDiveMode,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            onDelete = onDelete,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiveConfigurationBottomSheetContent(
    sheetState: SheetState,
    title: String = "Dive 1",
    initialSurfaceInterval: Duration? = 60.minutes,
    initialDiveMode: DiveMode = DiveMode.OPEN_CIRCUIT,
    onConfirm: (Duration?, DiveMode) -> Unit = { _, _ -> },
    onDismiss: () -> Unit = {},
    onDelete: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    var surfaceInterval: Duration? by remember(initialSurfaceInterval) { mutableStateOf(initialSurfaceInterval) }
    val isConfirmEnabled = remember(initialSurfaceInterval) { mutableStateOf(true) }
    var selectedDiveModeIndex by remember(initialDiveMode) { mutableIntStateOf(initialDiveMode.toSelectionIndex()) }

    val dismiss: () -> Unit = {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }

    ModalBottomSheetScaffold(
        header = {
            BottomSheetHeader(
                title = title,
                primaryLabel = "Confirm",
                primaryEnabled = isConfirmEnabled.value,
                onClose = dismiss,
                onPrimary = {
                    val effectiveDuration = if (initialSurfaceInterval != null) {
                        surfaceInterval ?: initialSurfaceInterval
                    } else {
                        null
                    }
                    onConfirm(effectiveDuration, selectedDiveModeIndex.toDiveMode())
                    dismiss()
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            RadioCardGroup(
                modifier = Modifier.padding(bottom = 16.dp),
                items = diveModeItems,
                selectedIndex = selectedDiveModeIndex,
                onSelectionChanged = { selectedDiveModeIndex = it },
            )

            if (initialSurfaceInterval == null) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    text = "Surface intervals can only be set for follow-up dives. The first dive always starts with fully off-gassed tissues, meaning the planner assumes no residual nitrogen or helium from prior dives.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                DurationInputField(
                    modifier = Modifier.fillMaxWidth(),
                    initialValue = initialSurfaceInterval,
                    label = "Surface interval",
                    isValid = isConfirmEnabled,
                    onChanged = { surfaceInterval = it },
                )
            }

            if (onDelete != null) {
                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    onClick = {
                        onDelete()
                        dismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete dive")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun DiveConfigurationBottomSheetAddPreview() {
    DiveConfigurationBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        title = "Dive 1",
        initialSurfaceInterval = 60.minutes,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun DiveConfigurationBottomSheetEditPreview() {
    DiveConfigurationBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        title = "Dive 2",
        initialSurfaceInterval = 90.minutes,
        onDelete = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun DiveConfigurationBottomSheetFirstDivePreview() {
    DiveConfigurationBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        title = "Dive 1",
        initialSurfaceInterval = null,
        onDelete = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun DiveConfigurationBottomSheetCcrPreview() {
    DiveConfigurationBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        title = "Dive 1",
        initialSurfaceInterval = null,
        initialDiveMode = DiveMode.CLOSED_CIRCUIT,
    )
}
