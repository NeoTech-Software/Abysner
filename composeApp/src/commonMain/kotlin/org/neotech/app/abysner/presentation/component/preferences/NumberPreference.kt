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

package org.neotech.app.abysner.presentation.component.preferences

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogCustomContent
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.neotech.app.abysner.presentation.component.textfield.number.rememberIntegerInputState
import org.neotech.app.abysner.presentation.component.textfield.OutlinedInputField
import org.neotech.app.abysner.presentation.component.textfield.SuffixVisualTransformation
import org.neotech.app.abysner.presentation.theme.AbysnerTheme

@Composable
fun NumberPreference(
    modifier: Modifier = Modifier,
    label: String,
    description: String,
    initialValue: Int = 0,
    minValue: Int = Int.MIN_VALUE,
    maxValue: Int = Int.MAX_VALUE,
    textFieldVisualTransformation: VisualTransformation = VisualTransformation.None,
    valueFormatter: (Int) -> String = { it.toString() },
    onValueChanged: (Int) -> Unit,
) {

    var showDialog by remember {
        mutableStateOf(false)
    }

    if (showDialog) {
        NumberPreferenceDialog(
            title = label,
            initialValue = initialValue,
            minValue = minValue,
            maxValue = maxValue,
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
fun NumberPreferenceDialog(
    title: String,
    confirmButtonText: String = "OK",
    cancelButtonText: String = "Cancel",
    onConfirmButtonClicked: (value: Int) -> Unit = { },
    onCancelButtonClicked: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
    maxValue: Int = Int.MAX_VALUE,
    minValue: Int = Int.MIN_VALUE,
    initialValue: Int = 0,
) {
    val numberInput = rememberIntegerInputState(initialValue, minValue, maxValue)

    AlertDialogCustomContent(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                enabled = numberInput.isValid,
                onClick = { onConfirmButtonClicked(numberInput.value) }
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
            OutlinedInputField(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                state = numberInput,
                visualTransformation = visualTransformation,
            )
        }

    )
}

@Preview
@Composable
private fun NumberPreferencePreview() {
    Surface {
        NumberPreference(
            label = "Altitude",
            description = "The altitude of the water surface at which the dive is taking place, in most cases this will be 0 meter (sea level).",
            initialValue = 0,
            minValue = -450,
            maxValue = 3000,
            valueFormatter = { "$it m"},
            textFieldVisualTransformation = SuffixVisualTransformation(" m"),
            onValueChanged = {},
        )
    }
}

@Preview
@Composable
fun NumberPreferenceDialogPreview() {
    AbysnerTheme {
        NumberPreferenceDialog(
            title = "Altitude",
            visualTransformation = SuffixVisualTransformation(" m"),
            minValue = -450,
            maxValue = 3000,
            initialValue = 0
        )
    }
}
