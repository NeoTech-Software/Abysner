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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.neotech.app.abysner.presentation.component.bottomsheet.BottomSheetButtonRow
import org.neotech.app.abysner.presentation.component.bottomsheet.ModalBottomSheetScaffold
import org.neotech.app.abysner.presentation.component.textfield.DurationInputField
import org.neotech.app.abysner.presentation.utilities.ModalTarget
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanInputModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * A bottom sheet for configuring a dive, currently allows editing the surface interval and
 * optionally deleting it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiveConfigurationBottomSheetHost(
    show: ModalTarget<Int>?,
    dives: List<DivePlanInputModel>,
    onAddDive: (Duration) -> Unit,
    onUpdateSurfaceInterval: (Int, Duration) -> Unit,
    onRemoveDive: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    if (show != null) {
        val editIndex = (show as? ModalTarget.Edit)?.value
        DiveConfigurationBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            title = "Dive ${(editIndex ?: dives.size) + 1}",
            initialValue = when (show) {
                is ModalTarget.Add -> 60.minutes
                is ModalTarget.Edit -> dives.getOrNull(show.value)?.surfaceIntervalBefore
            },
            onConfirm = { duration ->
                if (editIndex != null) {
                    onUpdateSurfaceInterval(editIndex, duration)
                } else {
                    onAddDive(duration)
                }
            },
            onDismiss = onDismiss,
            onDelete = editIndex?.let { { onRemoveDive(it) } },
        )
    }
}


/**
 * @param initialValue Pre-filled surface interval duration. `null` indicates the first dive, the
 *                     interval field is hidden and a clean-tissues explanation is shown.
 * @param onDelete     If not null a "Delete" button is shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiveConfigurationBottomSheet(
    sheetState: SheetState = rememberStandardBottomSheetState(),
    title: String,
    initialValue: Duration? = 60.minutes,
    onConfirm: (Duration) -> Unit = {},
    onDismiss: () -> Unit = {},
    onDelete: (() -> Unit)? = null,
) {
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
    ) {
        DiveConfigurationBottomSheetContent(
            sheetState = sheetState,
            title = title,
            initialValue = initialValue,
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
    title: String = "Surface interval",
    initialValue: Duration? = 60.minutes,
    onConfirm: (Duration) -> Unit = {},
    onDismiss: () -> Unit = {},
    onDelete: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    var confirmedDuration: Duration? by remember(initialValue) { mutableStateOf(initialValue) }
    val isConfirmEnabled = remember(initialValue) { mutableStateOf(initialValue != null) }

    ModalBottomSheetScaffold {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    style = MaterialTheme.typography.headlineSmall,
                    text = title,
                )
                if (onDelete != null) {
                    TextButton(
                        onClick = {
                            onDelete()
                            scope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text("Delete")
                    }
                }
            }

            if (initialValue == null) {
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
                    initialValue = initialValue,
                    label = "Surface interval",
                    isValid = isConfirmEnabled,
                    onChanged = { confirmedDuration = it },
                )
            }

            BottomSheetButtonRow(
                modifier = Modifier.padding(top = 16.dp),
                secondaryLabel = if (initialValue != null) { "Cancel" } else { null },
                primaryLabel = if (initialValue != null) { "Confirm" } else { "Close" },
                primaryEnabled = if (initialValue != null) { isConfirmEnabled.value } else { true },
                onSecondary = {
                    scope.launch {
                        sheetState.hide()
                        onDismiss()
                    }
                },
                onPrimary = {
                    if (initialValue != null) {
                        onConfirm(confirmedDuration ?: initialValue)
                    }
                    scope.launch {
                        sheetState.hide()
                        onDismiss()
                    }
                },
            )
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
        initialValue = 60.minutes,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun DiveConfigurationBottomSheetEditPreview() {
    DiveConfigurationBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        title = "Dive 2",
        initialValue = 90.minutes,
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
        initialValue = null,
        onDelete = {},
    )
}
