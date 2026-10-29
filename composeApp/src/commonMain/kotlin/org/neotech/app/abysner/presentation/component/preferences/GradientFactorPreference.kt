/*
 * Abysner - Dive planner
 * Copyright (C) 2025-2026 Neotech
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogCustomContent
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.neotech.app.abysner.presentation.component.textfield.number.rememberIntegerInputState
import org.neotech.app.abysner.presentation.component.textfield.OutlinedInputField
import org.neotech.app.abysner.presentation.component.textfield.SuffixVisualTransformation
import org.neotech.app.abysner.presentation.theme.AbysnerTheme

@Composable
fun GradientFactorPreference(
    modifier: Modifier = Modifier,
    label: String,
    description: String,
    gfLow: Int,
    gfHigh: Int,
    onValueChanged: (Int, Int) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        GradientFactorPreferenceDialog(
            title = label,
            gfLow = gfLow,
            gfHigh = gfHigh,
            onConfirmButtonClicked = { gfLowNew, gfHighNew ->
                if (gfLow != gfLowNew || gfHigh != gfHighNew) {
                    onValueChanged(gfLowNew, gfHighNew)
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
        value = "$gfLow/$gfHigh"
    ){
        showDialog = true
    }
}

@Composable
private fun GradientFactorPreferenceDialog(
    title: String,
    confirmButtonText: String = "OK",
    cancelButtonText: String = "Cancel",
    onConfirmButtonClicked: (gfLow: Int, gfHigh: Int) -> Unit = { _, _ -> },
    onCancelButtonClicked: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
    gfLow: Int,
    gfHigh: Int,
) {
    val gfLowInput = rememberIntegerInputState(gfLow, minValue = 10, maxValue = 100)
    val gfHighInput = rememberIntegerInputState(gfHigh, minValue = 10, maxValue = 100)

    AlertDialogCustomContent(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                enabled = gfLowInput.isValid && gfHighInput.isValid,
                onClick = {
                    onConfirmButtonClicked(gfLowInput.value, gfHighInput.value)
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
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedInputField(
                    modifier = Modifier.padding(start = 24.dp).weight(1f),
                    state = gfLowInput,
                    visualTransformation = SuffixVisualTransformation(" low"),
                )
                OutlinedInputField(
                    modifier = Modifier.padding(end = 24.dp).weight(1f),
                    state = gfHighInput,
                    visualTransformation = SuffixVisualTransformation(" high"),
                )
            }
        }
    )
}

@Preview
@Composable
fun GradientFactorPreferenceDialogPreview() {
    AbysnerTheme {
        GradientFactorPreferenceDialog(
            title = "Gradient factor",
            gfLow = 30,
            gfHigh = 70
        )
    }
}
