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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.flow.drop
import org.neotech.app.abysner.presentation.component.textfield.number.rememberDecimalInputState

/**
 * A decimal [OutlinedInputField] that reports every valid value while typing, for inputs that take
 * effect immediately instead of on a confirm button.
 *
 * [initialValue] is only read when the field is first shown (and whenever [resetKey] changes), so
 * callers may feed in the very value they store in reaction to [onValueChanged] without the text
 * being edited getting replaced on every keystroke. Use [resetKey] for the things that do have to
 * replace the text, such as a change of unit system.
 */
@Composable
fun LiveDecimalInputField(
    modifier: Modifier = Modifier,
    initialValue: Double,
    resetKey: Any? = Unit,
    label: String? = null,
    fractionDigits: Int = 2,
    minValue: Double = 0.0,
    maxValue: Double,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    supportingText: (@Composable (message: String?) -> Unit)? = { ErrorSupportingText(it) },
    onValueChanged: (Double) -> Unit,
) {
    val state = rememberDecimalInputState(
        initialValue = remember(resetKey) { initialValue },
        fractionDigits = fractionDigits,
        minValue = minValue,
        maxValue = maxValue,
    )

    // The state itself holds the value, so changes have to be observed rather than received as a
    // callback. The value the state starts out with is dropped, as that is the value the caller
    // passed in to begin with.
    val currentOnValueChanged by rememberUpdatedState(onValueChanged)
    LaunchedEffect(state) {
        snapshotFlow { state.value }
            .drop(1)
            .collect { currentOnValueChanged(it) }
    }

    OutlinedInputField(
        modifier = modifier,
        state = state,
        label = label,
        visualTransformation = visualTransformation,
        supportingText = supportingText,
    )
}
