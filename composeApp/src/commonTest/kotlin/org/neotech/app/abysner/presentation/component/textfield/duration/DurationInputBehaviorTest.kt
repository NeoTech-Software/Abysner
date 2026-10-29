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

import org.neotech.app.abysner.presentation.component.textfield.ime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class DurationInputBehaviorTest {

    private val behavior = DurationInputBehavior()

    @Test
    fun type_fourDigitsFromEmptyFillsField() {
        behavior.ime("00:00", 0)
            .type("1").assert("10:00")
            .type("1").assert("11:00")
            .type("2").assert("11:20")
            .type("2").assert("11:22")
            .type("5").assert("11:22")
            .assert(5)
    }

    @Test
    fun type_atEndOfHoursOverflowsIntoMinutes() {
        behavior.ime("12:34", 2).type("1").assert("12:14").assert(4)
    }

    @Test
    fun type_inMiddleOfHoursDoesNotOverflowIntoMinutes() {
        behavior.ime("12:34", 1).type("5").assert("15:34").assert(2)
    }

    @Test
    fun type_atStartOfHoursDoesNotOverflowIntoMinutes() {
        behavior.ime("12:34", 0).type("5").assert("51:34").assert(1)
    }

    @Test
    fun type_nonAsciiDigitIsFiltered() {
        behavior.ime("01:23", 2).type("\u0663").assert("01:23").assert(2)
    }

    @Test
    fun backspace_onSingleHourDigitReplacesWithZero() {
        behavior.ime("09:12", 2).backspace().assert("00:12").assert(1)
    }

    @Test
    fun backspace_onFirstHourDigitReplacesWithZero() {
        behavior.ime("12:34", 1).backspace().assert("02:34").assert(0)
    }

    @Test
    fun backspace_onSecondHourDigitReplacesWithZero() {
        behavior.ime("12:34", 2).backspace().assert("10:34").assert(1)
    }

    @Test
    fun backspace_onFirstMinuteDigitReplacesWithZero() {
        behavior.ime("01:34", 4).backspace().assert("01:04").assert(3)
    }

    @Test
    fun backspace_onSecondMinuteDigitReplacesWithZero() {
        behavior.ime("01:34", 5).backspace().assert("01:30").assert(4)
    }

    @Test
    fun backspace_beforeColonDeletesSecondHourDigit() {
        behavior.ime("12:34", 2).backspace().assert("10:34").assert(1)
    }

    @Test
    fun backspace_afterColonHopsBackOverColon() {
        behavior.ime("12:34", 3).backspace().assert("12:34").assert(2)
    }

    @Test
    fun selectAllAndPaste_oneDigitStartsFilling() {
        behavior.ime("12:34").selectAll().paste("5").assert("05:00").assert(2)
    }

    @Test
    fun selectAllAndPaste_fourDigitsFillsField() {
        behavior.ime("12:34").selectAll().paste("9876").assert("98:76").assert(5)
    }

    @Test
    fun selectAllAndPaste_emptyInputResetsToZero() {
        behavior.ime("12:34").selectAll().paste("").assert("00:00").assert(0)
    }

    @Test
    fun delete_beforeColonDoesNotRemoveColon() {
        behavior.ime("01:23", 2).delete().assert("01:23").assert(2)
    }

    @Test
    fun format_showsHoursAndMinutes() {
        assertEquals("00:00", behavior.format(Duration.ZERO))
        assertEquals("01:30", behavior.format(1.hours + 30.minutes))
    }

    @Test
    fun format_clampsAt99Hours59Minutes() {
        assertEquals("99:59", behavior.format(100.hours))
    }

    @Test
    fun parse_convertsHoursAndMinuteString() {
        assertEquals(1.hours + 30.minutes, behavior.parse("01:30"))
    }

    @Test
    fun parse_handlesDoubleDigitHours() {
        assertEquals(12.hours + 5.minutes, behavior.parse("12:05"))
    }

    @Test
    fun parse_zeroBothFieldsReturnsZeroDuration() {
        assertEquals(Duration.ZERO, behavior.parse("00:00"))
    }

    @Test
    fun parse_emptyHoursReturnsNull() {
        assertNull(behavior.parse(":30"))
    }

    @Test
    fun parse_emptyMinutesReturnsNull() {
        assertNull(behavior.parse("12:"))
    }

    @Test
    fun parse_minutesOver59ReturnsNull() {
        assertNull(behavior.parse("01:60"))
    }

    @Test
    fun parse_hoursOver99ReturnsNull() {
        assertNull(behavior.parse("100:00"))
    }

    @Test
    fun parse_nonNumericInputReturnsNull() {
        assertNull(behavior.parse("ab:cd"))
    }
}
