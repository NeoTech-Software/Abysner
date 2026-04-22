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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogCustomContent
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.neotech.app.abysner.presentation.component.textfield.OutlinedDecimalInputField
import org.neotech.app.abysner.presentation.component.textfield.SuffixVisualTransformation

@Composable
fun DecimalNumberPreference(
    modifier: Modifier = Modifier,
    label: String,
    description: String,
    initialValue: Double = 0.0,
    minValue: Double = 0.0,
    maxValue: Double = Double.MAX_VALUE,
    fractionDigits: Int = 1,
    textFieldVisualTransformation: VisualTransformation = VisualTransformation.None,
    valueFormatter: (Double) -> String = { it.toString() },
    onValueChanged: (Double) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        DecimalNumberPreferenceDialog(
            title = label,
            initialValue = initialValue,
            minValue = minValue,
            maxValue = maxValue,
            fractionDigits = fractionDigits,
            visualTransformation = textFieldVisualTransformation,
            onConfirmButtonClicked = { newValue ->
                if (newValue != initialValue) {
                    onValueChanged(newValue)
                }
                showDialog = false
            },
            onCancelButtonClicked = { showDialog = false },
            onDismissRequest = { showDialog = false },
        )
    }

    BaseTextPreference(
        modifier = modifier,
        label = label,
        description = description,
        value = valueFormatter(initialValue)
    ) {
        showDialog = true
    }
}

@Composable
fun DecimalNumberPreferenceDialog(
    title: String,
    confirmButtonText: String = "OK",
    cancelButtonText: String = "Cancel",
    onConfirmButtonClicked: (value: Double) -> Unit = {},
    onCancelButtonClicked: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
    fractionDigits: Int = 1,
    minValue: Double = 0.0,
    maxValue: Double = Double.MAX_VALUE,
    initialValue: Double = 0.0,
) {
    val numberValue: MutableState<Double> = remember(initialValue) { mutableStateOf(initialValue) }
    val isValid = remember { mutableStateOf(false) }

    AlertDialogCustomContent(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                enabled = isValid.value,
                onClick = { onConfirmButtonClicked(numberValue.value) }
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
            OutlinedDecimalInputField(
                modifier = Modifier.padding(horizontal = 24.dp),
                minValue = minValue,
                maxValue = maxValue,
                fractionDigits = fractionDigits,
                initialValue = initialValue,
                isValid = isValid,
                visualTransformation = visualTransformation,
            ) {
                if (it != null) {
                    numberValue.value = it
                }
            }
        }
    )
}

@Preview
@Composable
private fun DecimalNumberPreferencePreview() {
    Surface {
        DecimalNumberPreference(
            label = "Metabolic oxygen rate",
            description = "Oxygen consumption rate in liters per minute. Used to calculate oxygen usage for CCR dives.",
            initialValue = 0.8,
            minValue = 0.1,
            maxValue = 5.0,
            valueFormatter = { "$it L/min" },
            onValueChanged = {},
        )
    }
}

@Preview
@Composable
fun DecimalNumberPreferenceDialogPreview() {
    DecimalNumberPreferenceDialog(
        title = "Metabolic oxygen rate",
        visualTransformation = SuffixVisualTransformation(" L/min"),
        minValue = 0.1,
        maxValue = 5.0,
        initialValue = 0.8
    )
}
