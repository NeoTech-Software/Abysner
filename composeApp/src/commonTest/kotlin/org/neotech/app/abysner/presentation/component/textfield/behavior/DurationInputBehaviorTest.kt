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

package org.neotech.app.abysner.presentation.component.textfield.behavior

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class DurationInputBehaviorTest {

    private val behavior = DurationInputBehavior()

    @Test
    fun typingFourDigitsFromEmptyFillsField() {
        textField("00:00", 0)
            .type("1").assert("10:00")
            .type("1").assert("11:00")
            .type("2").assert("11:20")
            .type("2").assert("11:22")
            .type("5").assert("11:22")
            .assert(5)
    }

    @Test
    fun typeAtEndOfHoursOverflowsIntoMinutes() {
        textField("12:34", 2).type("1").assert("12:14").assert(4)
    }

    @Test
    fun typeInMiddleOfHoursDoesNotOverflowIntoMinutes() {
        textField("12:34", 1).type("5").assert("15:34").assert(2)
    }

    @Test
    fun typeAtStartOfHoursDoesNotOverflowIntoMinutes() {
        textField("12:34", 0).type("5").assert("51:34").assert(1)
    }

    @Test
    fun typeNonAsciiDigitIsFiltered() {
        textField("01:23", 2).type("\u0663").assert("01:23").assert(2)
    }

    @Test
    fun backspaceOnSingleHourDigitReplacesWithZero() {
        textField("09:12", 2).backspace().assert("00:12").assert(1)
    }

    @Test
    fun backspaceOnFirstHourDigitReplacesWithZero() {
        textField("12:34", 1).backspace().assert("02:34").assert(0)
    }

    @Test
    fun backspaceOnSecondHourDigitReplacesWithZero() {
        textField("12:34", 2).backspace().assert("10:34").assert(1)
    }

    @Test
    fun backspaceOnFirstMinuteDigitReplacesWithZero() {
        textField("01:34", 4).backspace().assert("01:04").assert(3)
    }

    @Test
    fun backspaceOnSecondMinuteDigitReplacesWithZero() {
        textField("01:34", 5).moveCursor(5).backspace().assert("01:30").assert(4)
    }

    @Test
    fun backspaceBeforeColonDeletesSecondHourDigit() {
        textField("12:34", 2).backspace().assert("10:34").assert(1)
    }

    @Test
    fun backspaceAfterColonHopsBackOverColon() {
        textField("12:34", 3).backspace().assert("12:34").assert(2)
    }

    @Test
    fun selectAllAndTypeOneDigit() {
        textField("12:34").selectAllAndType("5").assert("05:00").assert(2)
    }

    @Test
    fun selectAllAndTypeFourDigits() {
        textField("12:34").selectAllAndType("9876").assert("98:76").assert(5)
    }

    @Test
    fun selectAllAndDelete() {
        textField("12:34").selectAllAndType("").assert("00:00").assert(0)
    }

    @Test
    fun deleteBeforeColonDoesNotRemoveColon() {
        textField("01:23", 2).delete().assert("01:23").assert(2)
    }

    @Test
    fun fromDurationNullCursorAtStart() {
        DurationInputBehavior.fromDuration(null).assert("00:00").assert(0)
    }

    @Test
    fun fromDurationZeroCursorAtStart() {
        DurationInputBehavior.fromDuration(Duration.ZERO).assert("00:00").assert(0)
    }

    @Test
    fun fromDurationWithDurationCursorAtEnd() {
        DurationInputBehavior.fromDuration(1.hours + 30.minutes).assert("01:30").assert(5)
    }

    @Test
    fun fromDurationClampsAt99Hours59Minutes() {
        DurationInputBehavior.fromDuration(100.hours).assert("99:59").assert(5)
    }

    @Test
    fun toDurationNormal() {
        assertEquals(1.hours + 30.minutes, DurationInputBehavior.toDuration(textField("01:30")))
    }

    @Test
    fun toDurationTwoDigitHours() {
        assertEquals(12.hours + 5.minutes, DurationInputBehavior.toDuration(textField("12:05")))
    }

    @Test
    fun toDurationZeroReturnsZeroDuration() {
        assertEquals(Duration.ZERO, DurationInputBehavior.toDuration(textField("00:00")))
    }

    @Test
    fun toDurationEmptyHoursReturnsNull() {
        assertNull(DurationInputBehavior.toDuration(textField(":30")))
    }

    @Test
    fun toDurationEmptyMinutesReturnsNull() {
        assertNull(DurationInputBehavior.toDuration(textField("12:")))
    }

    @Test
    fun toDurationMinutesOver59ReturnsNull() {
        assertNull(DurationInputBehavior.toDuration(textField("01:60")))
    }

    @Test
    fun toDurationHoursOver99ReturnsNull() {
        assertNull(DurationInputBehavior.toDuration(textField("100:00")))
    }

    @Test
    fun toDurationNonNumericReturnsNull() {
        assertNull(DurationInputBehavior.toDuration(textField("ab:cd")))
    }

    @Test
    fun processValueWithNullPreviousValue() {
        // In our usage this should not really happen, but processValue handles it.
        val result = behavior.processValue(null, textField("01:23"))
        assertEquals("01:23", result.text)
    }

    private fun textField(text: String, cursor: Int = text.length) =
        TextFieldValue(text, TextRange(cursor))

    private fun TextFieldValue.type(chars: String): TextFieldValue {
        var current = this
        for (ch in chars) {
            val pos = current.selection.start
            val newText = current.text.substring(0, pos) + ch + current.text.substring(pos)
            val newValue = TextFieldValue(newText, TextRange(pos + 1))
            current = behavior.processValue(current, newValue)
        }
        return current
    }

    private fun TextFieldValue.backspace(): TextFieldValue {
        val pos = selection.start
        if (pos == 0) {
            return behavior.processValue(this, this)
        }
        val newText = text.removeRange(pos - 1, pos)
        val newValue = TextFieldValue(newText, TextRange(pos - 1))
        return behavior.processValue(this, newValue)
    }

    private fun TextFieldValue.delete(): TextFieldValue {
        val pos = selection.start
        if (pos >= text.length) {
            return behavior.processValue(this, this)
        }
        val newText = text.removeRange(pos, pos + 1)
        val newValue = TextFieldValue(newText, TextRange(pos))
        return behavior.processValue(this, newValue)
    }

    private fun TextFieldValue.moveCursor(position: Int): TextFieldValue {
        val clamped = position.coerceIn(0, text.length)
        return copy(selection = TextRange(clamped))
    }

    private fun TextFieldValue.selectAllAndType(replacement: String): TextFieldValue {
        val newValue = TextFieldValue(replacement, TextRange(replacement.length))
        return behavior.processValue(this, newValue)
    }

    private fun TextFieldValue.assert(expected: String): TextFieldValue {
        assertEquals(expected, text)
        return this
    }

    private fun TextFieldValue.assert(expected: Int): TextFieldValue {
        assertEquals(expected, selection.start)
        assertEquals(0, selection.length)
        return this
    }
}
