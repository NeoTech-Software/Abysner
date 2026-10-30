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

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Decides how an [OutlinedInputField] handles every change to its text. A behavior can accept a
 * change, adjust it, or reject it. It also converts between the text and a value of type [T],
 * allowing for strictly typed text editing.
 */
interface InputBehavior<T> {

    val keyboardOptions: KeyboardOptions

    /**
     * Returns the text to show after [previousValue] changed into [newValue]. Return
     * [previousValue] to reject the change.
     */
    fun transformInput(previousValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue

    /**
     * Returns the value of [text], or null if [text] does not have one.
     */
    fun parse(text: String): T?

    /**
     * Returns the text to show for [value].
     */
    fun format(value: T): String
}
