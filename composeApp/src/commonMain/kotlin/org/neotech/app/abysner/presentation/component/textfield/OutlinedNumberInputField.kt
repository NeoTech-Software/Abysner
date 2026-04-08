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

package org.neotech.app.abysner.presentation.component.textfield

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import org.neotech.app.abysner.presentation.component.textfield.behavior.DecimalInputBehavior
import org.neotech.app.abysner.presentation.component.textfield.behavior.NumberInputBehavior


@Composable
fun OutlinedDecimalInputField(
    modifier: Modifier = Modifier,
    initialValue: Double?,
    label: String? = null,
    fractionDigits: Int = 2,
    minValue: Double = Double.MIN_VALUE,
    maxValue: Double = Double.MAX_VALUE,
    isValid: MutableState<Boolean> = remember { mutableStateOf(false) },
    visualTransformation: VisualTransformation = VisualTransformation.None,
    errorMessage: MutableState<String?> = remember { mutableStateOf(null) },
    supportingText: (@Composable (message: String?) -> Unit)? = { ErrorSupportingText(it) },
    onNumberChanged: (Double?) -> Unit,
) {

    val behavior = remember {
        DecimalInputBehavior(fractionDigits = fractionDigits, visualTransformation = visualTransformation)
    }

    fun updateErrorMessage(number: Double?) {
        errorMessage.value = if(number == null) {
            isValid.value = false
            "${label ?: "Value"} must be between ${behavior.toString(behavior.fromDecimal(minValue))} and ${behavior.toString(behavior.fromDecimal(maxValue))}."
        } else if(number > maxValue) {
            isValid.value = false
            "${label ?: "Value"} must not be higher then ${behavior.toString(behavior.fromDecimal(maxValue))}."
        } else if(number < minValue) {
            isValid.value = false
            "${label ?: "Value"} must not be lower then ${behavior.toString(behavior.fromDecimal(minValue))}."
        } else {
            isValid.value = true
            null
        }
    }

    val numberValue: MutableState<Long?> = remember(initialValue) {
        updateErrorMessage(initialValue)
        mutableStateOf(behavior.fromDecimal(initialValue))
    }

    OutlinedGenericInputField(
        modifier = modifier,
        behavior = behavior,
        initialValue = numberValue.value,
        supportingText = { supportingText?.invoke(errorMessage.value) },
        label = defaultInputFieldLabel(label),
        isError = errorMessage.value != null,
        colors = OutlinedTextFieldDefaults.colors().copy(errorTextColor = Color.Red),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            textAlign = TextAlign.Center,
            fontSize = 24.sp
        ),
        errorMessage = errorMessage.value,
        onValueChanged = {

            val decimalNumber = behavior.toDecimal(it)
            updateErrorMessage(decimalNumber)
            onNumberChanged(decimalNumber)
            numberValue.value = it
        }
    )
}

@Composable
fun OutlinedNumberInputField(
    modifier: Modifier = Modifier,
    initialValue: Int?,
    label: String? = null,
    minValue: Int = Int.MIN_VALUE,
    maxValue: Int = Int.MAX_VALUE,
    isValid: MutableState<Boolean> = remember { mutableStateOf(false) },
    visualTransformation: VisualTransformation = VisualTransformation.None,
    errorMessage: MutableState<String?> = remember { mutableStateOf(null) },
    supportingText: (@Composable (message: String?) -> Unit)? = { ErrorSupportingText(it) },
    onNumberChanged: (Int?) -> Unit,
) {

    val behavior = remember {
        NumberInputBehavior(visualTransformation = visualTransformation)
    }

    fun updateErrorMessage(number: Int?) {
        errorMessage.value = if (number == null) {
            isValid.value = false
            "${label ?: "Value"} must be between $minValue and $maxValue."
        } else if (number > maxValue) {
            isValid.value = false
            "${label ?: "Value"} must not be higher then $maxValue."
        } else if (number < minValue) {
            isValid.value = false
            "${label ?: "Value"} must not be lower then $minValue."
        } else {
            isValid.value = true
            null
        }
    }

    val numberValue: MutableState<Int?> = remember(initialValue) {
        updateErrorMessage(initialValue)
        mutableStateOf(initialValue)
    }

    OutlinedGenericInputField(
        modifier = modifier,
        behavior = behavior,
        initialValue = numberValue.value?.toLong(),
        supportingText = { supportingText?.invoke(errorMessage.value) },
        label = defaultInputFieldLabel(label),
        isError = errorMessage.value != null,
        colors = OutlinedTextFieldDefaults.colors().copy(errorTextColor = Color.Red),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            textAlign = TextAlign.Center,
            fontSize = 24.sp
        ),
        errorMessage = errorMessage.value,
        onValueChanged = {
            val number = it?.toInt()
            updateErrorMessage(number)
            onNumberChanged(number)
            numberValue.value = number
        }
    )
}
