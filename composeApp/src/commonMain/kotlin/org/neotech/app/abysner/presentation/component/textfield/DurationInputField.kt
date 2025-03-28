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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import org.neotech.app.abysner.presentation.component.textfield.behavior.DurationInputBehavior
import kotlin.time.Duration

/**
 * A text field that displays and accepts a [Duration] in `HH:MM` format.
 */
@Composable
fun DurationInputField(
    modifier: Modifier = Modifier,
    label: String? = null,
    initialValue: Duration? = null,
    isValid: MutableState<Boolean> = remember { mutableStateOf(false) },
    onChanged: (Duration?) -> Unit = {},
) {
    val behavior = remember { DurationInputBehavior() }
    var fieldValue by remember(initialValue) {
        mutableStateOf(DurationInputBehavior.fromDuration(initialValue))
    }

    val currentDuration = DurationInputBehavior.toDuration(fieldValue)
    isValid.value = currentDuration != null && currentDuration > Duration.ZERO

    OutlinedGenericInputField(
        modifier = modifier,
        initialValue = fieldValue,
        label = defaultInputFieldLabel(label),
        behavior = behavior,
        isError = !isValid.value,
        colors = OutlinedTextFieldDefaults.colors().copy(errorTextColor = Color.Red),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
        ),
        onValueChanged = {
            fieldValue = it
            onChanged(DurationInputBehavior.toDuration(it))
        },
    )
}
