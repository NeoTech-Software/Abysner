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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * State of an [OutlinedInputField]. The [behavior] decides how the text changes, and the value is
 * only updated when the text is valid. When editing ends with invalid text, the value from before
 * editing is restored.
 */
@Stable
abstract class InputState<V>(
    protected val behavior: InputBehavior<V>,
    initialValue: V,
) {

    val keyboardOptions: KeyboardOptions
        get() = behavior.keyboardOptions

    var text by mutableStateOf(behavior.format(initialValue).let { TextFieldValue(it, TextRange(it.length)) })
        protected set

    protected var currentValue by mutableStateOf(initialValue)
        private set

    private var valueBeforeEditing = initialValue

    val isValid: Boolean
        get() = behavior.parse(text.text)?.let { isValid(it) } ?: false

    open fun errorMessage(label: String?): String? = null

    protected abstract fun isValid(value: V): Boolean

    fun onTextChanged(newValue: TextFieldValue) {
        text = behavior.transformInput(text, newValue)
        val parsed = behavior.parse(text.text)
        if (parsed != null && isValid(parsed)) {
            currentValue = parsed
        }
    }

    fun onEditingStarted() {
        valueBeforeEditing = currentValue
    }

    fun onEditingEnded() {
        if (!isValid) {
            currentValue = valueBeforeEditing
        }
        replaceText(behavior.format(currentValue))
    }

    protected fun replaceValue(newValue: V) {
        currentValue = newValue
        valueBeforeEditing = newValue
        replaceText(behavior.format(newValue))
    }

    private fun replaceText(newText: String) {
        if (newText != text.text) {
            text = TextFieldValue(newText, TextRange(newText.length))
        }
    }
}
