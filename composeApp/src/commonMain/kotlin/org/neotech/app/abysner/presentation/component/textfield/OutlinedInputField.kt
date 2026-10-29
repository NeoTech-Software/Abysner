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

package org.neotech.app.abysner.presentation.component.textfield

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * A single line [OutlinedTextField] for an [InputState].
 */
@Composable
fun OutlinedInputField(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    state: InputState<*>,
    label: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    supportingText: (@Composable (message: String?) -> Unit)? = { ErrorSupportingText(it) },
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        textAlign = TextAlign.Center,
        fontSize = 24.sp,
    ),
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    OutlinedTextField(
        modifier = modifier.onFocusChanged { focusState ->
            if (!isFocused && focusState.isFocused) {
                state.onEditingStarted()
            } else if (isFocused && !focusState.isFocused) {
                state.onEditingEnded()
            }
            isFocused = focusState.isFocused
        },
        enabled = enabled,
        singleLine = true,
        value = state.text,
        onValueChange = state::onTextChanged,
        keyboardOptions = state.keyboardOptions,
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        visualTransformation = visualTransformation,
        label = defaultInputFieldLabel(label),
        isError = !state.isValid,
        supportingText = { supportingText?.invoke(state.errorMessage(label)) },
        colors = OutlinedTextFieldDefaults.colors().copy(errorTextColor = Color.Red),
        textStyle = textStyle,
    )
}
