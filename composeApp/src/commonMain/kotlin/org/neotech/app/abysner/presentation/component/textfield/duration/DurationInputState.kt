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

package org.neotech.app.abysner.presentation.component.textfield.duration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextRange
import org.neotech.app.abysner.presentation.component.textfield.InputState
import kotlin.time.Duration

@Stable
class DurationInputState(initialValue: Duration) : InputState<Duration>(
    behavior = DurationInputBehavior(),
    initialValue = initialValue
) {

    init {
        // Starts with the cursor at the start for zero, so typing fills the field from the left.
        if (initialValue == Duration.ZERO) {
            text = text.copy(selection = TextRange.Zero)
        }
    }

    val value: Duration
        get() = currentValue

    override fun isValid(value: Duration): Boolean = value > Duration.ZERO
}

@Composable
fun rememberDurationInputState(initialValue: Duration): DurationInputState = remember(initialValue) {
    DurationInputState(initialValue)
}
