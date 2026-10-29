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

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class DurationInputStateTest {

    @Test
    fun text_showsInitialValueWithCursorAtEnd() {
        val text = DurationInputState(90.minutes).text

        assertEquals("01:30", text.text)
        assertEquals(TextRange(5), text.selection)
    }

    @Test
    fun text_startsWithCursorAtStartForZero() {
        val text = DurationInputState(Duration.ZERO).text

        assertEquals("00:00", text.text)
        assertEquals(TextRange(0), text.selection)
    }

    @Test
    fun onTextChanged_updatesValueOnlyWhenDurationIsValid() {
        val state = DurationInputState(60.minutes)

        state.type("00:00")
        assertFalse(state.isValid)
        assertEquals(60.minutes, state.value)
        state.type("02:15")
        assertTrue(state.isValid)
        assertEquals(135.minutes, state.value)
    }

    @Test
    fun onEditingEnded_restoresValueFromBeforeEditingWhenTextIsInvalid() {
        val state = DurationInputState(60.minutes)
        state.onEditingStarted()
        state.type("00:00")

        state.onEditingEnded()

        assertEquals("01:00", state.text.text)
        assertTrue(state.isValid)
    }

    private fun DurationInputState.type(text: String) =
        onTextChanged(TextFieldValue(text, TextRange(text.length)))
}
