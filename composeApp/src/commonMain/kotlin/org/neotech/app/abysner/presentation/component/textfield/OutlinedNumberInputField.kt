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

package org.neotech.app.abysner.presentation.component.textfield

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.neotech.app.abysner.presentation.component.modifier.ifTrue
import org.neotech.app.abysner.presentation.component.modifier.invisible

@Composable
fun OutlinedNumberInputField(
    modifier: Modifier = Modifier,
    initialValue: Int,
    label: String? = null,
    minValue: Int = Int.MIN_VALUE,
    maxValue: Int = Int.MAX_VALUE,
    isValid: MutableState<Boolean> = remember { mutableStateOf(false) },
    visualTransformation: VisualTransformation = VisualTransformation.None,
    errorMessage: MutableState<String?> = remember { mutableStateOf(null) },
    supportingText: (@Composable (message: String?) -> Unit)? = {
        Text(
            modifier = Modifier.ifTrue(errorMessage.value == null) {
                invisible()
            },
            text = errorMessage.value ?: "Dummy to avoid jumping",
            color = MaterialTheme.colorScheme.error
        )
    },
    onNumberChanged: (Int) -> Unit,
) {

    val numberValue: MutableState<String> =
        remember(initialValue) { mutableStateOf(initialValue.toString()) }

    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        modifier = modifier,
        singleLine = true,
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done, keyboardType = KeyboardType.Number),
        supportingText = { supportingText?.invoke(errorMessage.value) },
        label = {
            if(label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                )
            }
        },
        isError = errorMessage.value != null,
        colors = OutlinedTextFieldDefaults.colors().copy(errorTextColor = Color.Red),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            textAlign = TextAlign.Center,
            fontSize = 24.sp
        ),
        visualTransformation = visualTransformation,
        value = numberValue.value,
        onValueChange = { rawValue ->

            // Filter out all non 0-9 characters and only allow minus at first character
            val filtered = rawValue.trim().filterIndexed { index, c ->
                c in '0'..'9' || (c == '-' && index == 0 && minValue < 0)
            }.take(10)

            // Remove leading zeroes (if-any), this is done after initial character filtering
            // so we can correctly check if a zero is the last character or not.
            var anyNumberBeforeZero = false
            val zeroPadRemoved = filtered.filterIndexed { index, c ->
                when (c) {
                    '0' -> {
                        // Keep zero only if:
                        // - It is the last zero (and only zero)
                        // - There was at least a non-zero number in front of it.
                        index == filtered.length-1  || anyNumberBeforeZero
                    }
                    '-' -> true// Keep minus
                    else -> {
                        // Keep all other digits and mark that we have seen at least another digit.
                        anyNumberBeforeZero = true
                        true
                    }
                }
            }

            val number = zeroPadRemoved.toIntOrNull()
            errorMessage.value = if(number == null) {
                isValid.value = false
                "${label ?: "Value"} must be between $minValue and $maxValue."
            } else if(number > maxValue) {
                isValid.value = false
                "${label ?: "Value"} must not be higher then $maxValue."
            } else if(number < minValue) {
                isValid.value = false
                "${label ?: "Value"} must not be lower then $minValue."
            } else {
                isValid.value = true
                onNumberChanged(number)
                null
            }

            numberValue.value = zeroPadRemoved
        }
    )
}
