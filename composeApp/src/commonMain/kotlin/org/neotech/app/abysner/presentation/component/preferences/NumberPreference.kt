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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.neotech.app.abysner.presentation.component.textfield.OutlinedNumberInputField

@Preview
@Composable
private fun NumberPreferencePreview() {
    Surface {

        var value by mutableIntStateOf(0)

        NumberPreference(
            label = "Salinity",
            description = "The type of water. Saltier water is heavier and increases pressure at depth.",
            onValueChanged = {
                value = it
            },
        )
    }
}

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

    BasicPreference(
        modifier = modifier.clickable {
            showDialog = true
        },
        label = label,
        value = description,
        hideDivider = true,
        action = {
            Text(
                text = valueFormatter(initialValue),
                style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    )
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
    val numberValue: MutableState<Int> = remember(initialValue) { mutableIntStateOf(initialValue) }

    val isValid = remember { mutableStateOf(false) }

    AlertDialogCustomContent(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                enabled = isValid.value,
                onClick = {
                onConfirmButtonClicked(numberValue.value)
            }) {
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
            OutlinedNumberInputField(
                modifier = Modifier.padding(horizontal = 24.dp),
                minValue = minValue,
                maxValue = maxValue,
                initialValue = initialValue,
                isValid = isValid,
                visualTransformation = visualTransformation
            ) {
                // Empty is not valid, thus don't accept it to start with.
                if(it != null) {
                    numberValue.value = it
                }
            }
        }

    )
}

@Preview
@Composable
fun NumberPreferenceDialogPreview() {
    NumberPreferenceDialog(
        title = "Salinity",
    )
}
