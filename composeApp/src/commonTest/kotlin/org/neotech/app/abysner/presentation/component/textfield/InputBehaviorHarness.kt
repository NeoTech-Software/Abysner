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

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.assertEquals

/**
 * Starts an [InputBehaviorHarness] for this behavior on [text], with the cursor at [cursor].
 */
internal fun InputBehavior<*>.ime(text: String, cursor: Int = text.length) =
    InputBehaviorHarness(this, TextFieldValue(text, TextRange(cursor)))

/**
 * Test harness for an [InputBehavior]. Makes edits the way an input method would, and allows to
 * assert results.
 */
internal class InputBehaviorHarness(private val behavior: InputBehavior<*>, private val value: TextFieldValue) {

    fun type(chars: String): InputBehaviorHarness = chars.fold(this) { field, char -> field.paste(char.toString()) }

    fun paste(text: String): InputBehaviorHarness {
        val start = value.selection.min
        val newText = value.text.replaceRange(start, value.selection.max, text)
        return change(TextFieldValue(newText, TextRange(start + text.length)))
    }

    fun backspace(): InputBehaviorHarness {
        val cursor = value.selection.start
        if (cursor == 0) {
            return change(value)
        }
        return change(TextFieldValue(value.text.removeRange(cursor - 1, cursor), TextRange(cursor - 1)))
    }

    fun delete(): InputBehaviorHarness {
        val cursor = value.selection.start
        if (cursor >= value.text.length) {
            return change(value)
        }
        return change(TextFieldValue(value.text.removeRange(cursor, cursor + 1), TextRange(cursor)))
    }

    fun select(start: Int, end: Int) = InputBehaviorHarness(behavior, value.copy(selection = TextRange(start, end)))

    fun selectAll() = select(0, value.text.length)

    fun assert(expected: String): InputBehaviorHarness {
        assertEquals(expected, value.text)
        return this
    }

    fun assert(expectedCursor: Int): InputBehaviorHarness {
        assertEquals(TextRange(expectedCursor), value.selection)
        return this
    }

    private fun change(newValue: TextFieldValue) = InputBehaviorHarness(behavior, behavior.transformInput(value, newValue))
}
