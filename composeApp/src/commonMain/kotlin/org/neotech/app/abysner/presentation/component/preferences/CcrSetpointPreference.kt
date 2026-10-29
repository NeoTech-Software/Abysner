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

package org.neotech.app.abysner.presentation.component.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogCustomContent
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.neotech.app.abysner.presentation.component.textfield.number.rememberDecimalInputState
import org.neotech.app.abysner.presentation.component.textfield.number.rememberIntegerInputState
import org.neotech.app.abysner.presentation.component.textfield.OutlinedInputField
import org.neotech.app.abysner.presentation.component.textfield.SuffixVisualTransformation
import org.neotech.app.abysner.presentation.theme.AbysnerTheme

@Composable
fun CcrSetpointPreference(
    modifier: Modifier = Modifier,
    label: String,
    description: String,
    switchDepthDescription: String,
    setpoint: Double,
    switchDepth: Int?,
    onValueChanged: (setpoint: Double, switchDepth: Int?) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        CcrSetpointPreferenceDialog(
            title = label,
            setpoint = setpoint,
            switchDepth = switchDepth,
            switchDepthDescription = switchDepthDescription,
            onConfirmButtonClicked = { newSetpoint, newSwitchDepth ->
                if (newSetpoint != setpoint || newSwitchDepth != switchDepth) {
                    onValueChanged(newSetpoint, newSwitchDepth)
                }
                showDialog = false
            },
            onCancelButtonClicked = { showDialog = false },
            onDismissRequest = { showDialog = false },
        )
    }

    val summary = if (switchDepth != null) {
        "$setpoint bar\ntill $switchDepth m"
    } else {
        "$setpoint bar"
    }

    BaseTextPreference(
        modifier = modifier,
        label = label,
        description = description,
        value = summary,
    ) {
        showDialog = true
    }
}

@Composable
private fun CcrSetpointPreferenceDialog(
    title: String,
    confirmButtonText: String = "OK",
    cancelButtonText: String = "Cancel",
    onConfirmButtonClicked: (setpoint: Double, switchDepth: Int?) -> Unit = { _, _ -> },
    onCancelButtonClicked: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
    setpoint: Double,
    switchDepth: Int?,
    switchDepthDescription: String = "",
) {
    val initialSwitchDepth = switchDepth ?: 6
    var switchEnabled by remember(switchDepth) { mutableStateOf(switchDepth != null) }

    val setpointInput = rememberDecimalInputState(setpoint, fractionDigits = 1, minValue = 0.1, maxValue = 1.6)
    val switchDepthInput = rememberIntegerInputState(initialSwitchDepth, minValue = 1, maxValue = 150)

    AlertDialogCustomContent(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                enabled = setpointInput.isValid && (!switchEnabled || switchDepthInput.isValid),
                onClick = {
                    val effectiveSwitchDepth = if (switchEnabled) { switchDepthInput.value } else { null }
                    onConfirmButtonClicked(setpointInput.value, effectiveSwitchDepth)
                }
            ) {
                Text(text = confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelButtonClicked) {
                Text(text = cancelButtonText)
            }
        },
        title = { Text(title) },
        content = {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedInputField(
                    modifier = Modifier.fillMaxWidth(),
                    state = setpointInput,
                    label = "Setpoint",
                    visualTransformation = SuffixVisualTransformation(" bar"),
                    supportingText = null
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Checkbox(
                        checked = switchEnabled,
                        onCheckedChange = { checked ->
                            switchEnabled = checked
                            if (checked) {
                                switchDepthInput.value = initialSwitchDepth
                            }
                        }
                    )

                    OutlinedInputField(
                        modifier = Modifier.weight(1f),
                        enabled = switchEnabled,
                        state = switchDepthInput,
                        label = "Auto-switch depth",
                        visualTransformation = SuffixVisualTransformation(" m"),
                        supportingText = null,
                    )
                }

                if (switchDepthDescription.isNotEmpty()) {
                    Text(
                        text = switchDepthDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    )
}

@Preview
@Composable
private fun CcrSetpointPreferencePreview() {
    Surface {
        Column {
            CcrSetpointPreference(
                label = "Low setpoint",
                description = "The CCR setpoint used during descent, with optional auto-switch depth to the high setpoint.",
                switchDepthDescription = "Auto-switch depth: during descent, switch to the high setpoint at this depth.",
                setpoint = 0.7,
                switchDepth = 6,
                onValueChanged = { _, _ -> },
            )
            CcrSetpointPreference(
                label = "High setpoint",
                description = "The CCR setpoint used during bottom time and ascent, with optional auto-switch depth to the low setpoint.",
                switchDepthDescription = "Auto-switch depth: during ascent, switch to the low setpoint at this depth.",
                setpoint = 1.2,
                switchDepth = null,
                onValueChanged = { _, _ -> },
            )
        }
    }
}

@Preview
@Composable
fun CcrSetpointPreferenceDialogPreview() {
    AbysnerTheme {
        CcrSetpointPreferenceDialog(
            title = "Low setpoint",
            setpoint = 1.2,
            switchDepth = 6,
            switchDepthDescription = "Auto-switch depth: during descent, switch to the high setpoint at this depth.",
        )
    }
}
