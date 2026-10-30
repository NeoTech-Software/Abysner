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

package org.neotech.app.abysner.presentation.component.textfield.number

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.neotech.app.abysner.domain.utilities.FormatLocale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NumberInputStateTest {

    @Test
    fun text_showsInitialValueFormatted() {
        assertEquals("1.0", state(initialValue = 100, fractionDigits = 2).text.text)
        assertEquals("1.5", state(initialValue = 150, fractionDigits = 2).text.text)
    }

    @Test
    fun onTextChanged_updatesValueOnlyWhenTextIsValid() {
        val state = state(initialValue = 50, minimum = 10, maximum = 300)

        state.type("5")
        assertFalse(state.isValid)
        assertEquals(50L, state.value)
        state.type("301")
        assertEquals(50L, state.value)
        state.type("250")
        assertTrue(state.isValid)
        assertEquals(250L, state.value)
    }

    @Test
    fun onEditingEnded_restoresValueFromBeforeEditingWhenTextIsInvalid() {
        val state = state(initialValue = 150, minimum = 10, maximum = 300)
        state.onEditingStarted()
        state.type("15")
        state.type("1")

        state.onEditingEnded()

        assertEquals("150", state.text.text)
        assertEquals(150L, state.value)
        assertTrue(state.isValid)
    }

    @Test
    fun onEditingEnded_keepsValidValueAndFormatsText() {
        val state = state(initialValue = 100, fractionDigits = 2)
        state.onEditingStarted()
        state.type("2.")

        state.onEditingEnded()

        assertEquals("2.0", state.text.text)
        assertEquals(200L, state.value)
    }

    @Test
    fun value_replacesTextWhenSet() {
        val state = state(initialValue = 50)
        state.type("")

        state.value = 232

        assertEquals("232", state.text.text)
        assertTrue(state.isValid)
    }

    private fun state(
        initialValue: Long,
        fractionDigits: Int = 0,
        minimum: Long = 0,
        maximum: Long = 100_000,
    ) = NumberInputState(
        behavior = NumberInputBehavior(fractionDigits, FormatLocale.forLanguageTag("en-US")),
        initialValue = initialValue,
        minimum = minimum,
        maximum = maximum,
        fromScaled = { it },
        toScaled = { it },
    )

    private fun NumberInputState<*>.type(text: String) =
        onTextChanged(TextFieldValue(text, TextRange(text.length)))
}
