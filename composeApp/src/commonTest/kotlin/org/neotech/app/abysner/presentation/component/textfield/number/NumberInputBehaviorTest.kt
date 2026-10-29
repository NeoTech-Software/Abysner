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

import org.neotech.app.abysner.domain.utilities.FormatLocale
import org.neotech.app.abysner.presentation.component.textfield.ime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NumberInputBehaviorTest {

    @Test
    fun type_acceptsNumbersUpToFractionDigits() {
        behavior(fractionDigits = 2).ime("")
            .type("1").assert("1")
            .type(".").assert("1.")
            .type("5").assert("1.5")
            .type("0").assert("1.50")
        behavior(fractionDigits = 2).ime("").type(".5").assert(".5")
    }

    @Test
    fun type_dropsFractionDigitsBeyondFractionDigits() {
        behavior(fractionDigits = 2).ime("1.50").type("5").assert("1.50")
        behavior(fractionDigits = 1).ime("1.5", cursor = 2).type("2").assert("1.2").assert(3)
        behavior(fractionDigits = 1).ime("120", cursor = 1).type(".").assert("1.2").assert(2)
    }

    @Test
    fun type_ignoresSecondSeparator() {
        behavior(fractionDigits = 2).ime("12.5").type(".").assert("12.5").assert(4)
    }

    @Test
    fun type_ignoresSeparatorWithoutFractionDigits() {
        behavior(fractionDigits = 0).ime("1").type(".").assert("1").assert(1)
    }

    @Test
    fun type_rejectsTooManyIntegerDigits() {
        behavior(fractionDigits = 0).ime("123456789").type("0").assert("123456789")
    }

    @Test
    fun type_replacesSeparatorWithLocaleSeparator() {
        behavior(fractionDigits = 2).ime("1").type(",").assert("1.")
        behavior(fractionDigits = 2, locale = FormatLocale.forLanguageTag("nl-NL")).ime("1").type(".").assert("1,")
    }

    @Test
    fun backspace_neverAddsCharacters() {
        behavior(fractionDigits = 2).ime("0.50")
            .backspace().assert("0.5")
            .backspace().assert("0.")
            .backspace().assert("0")
            .backspace().assert("")
    }

    @Test
    fun paste_keepsOnlyDigitsAndSeparator() {
        behavior(fractionDigits = 2).ime("").paste(" 1 500 ").assert("1500").assert(4)
        behavior(fractionDigits = 2).ime("").paste("15 m").assert("15")
        behavior(fractionDigits = 2).ime("").paste("€ 12,5").assert("12.5")
        behavior(fractionDigits = 2).ime("7").selectAll().paste("abc").assert("")
    }

    @Test
    fun paste_removesSeparatorsWithoutFractionDigits() {
        behavior(fractionDigits = 0).ime("").paste("1.5").assert("15")
        behavior(fractionDigits = 0).ime("").paste("1,500").assert("1500")
    }

    @Test
    fun paste_rejectsMoreThanOneSeparator() {
        behavior(fractionDigits = 2).ime("12.45").select(3, 5).paste("12.43").assert("12.45")
        behavior(fractionDigits = 2).ime("7").selectAll().paste("1,234.5").assert("7")
    }

    @Test
    fun parse_readsScaledValue() {
        assertEquals(232L, behavior(fractionDigits = 0).parse("232"))
        assertEquals(15L, behavior(fractionDigits = 1).parse("1.5"))
        assertEquals(150L, behavior(fractionDigits = 2).parse("1.5"))
        assertEquals(100L, behavior(fractionDigits = 2).parse("1."))
        assertEquals(50L, behavior(fractionDigits = 2).parse(".5"))
        assertEquals(23200L, behavior(fractionDigits = 2).parse("232"))
        assertEquals(450L, behavior(fractionDigits = 4).parse("0.045"))
        assertEquals(5000L, behavior(fractionDigits = 4).parse(".5"))
    }

    @Test
    fun parse_returnsNullWithoutDigits() {
        assertNull(behavior(fractionDigits = 2).parse(""))
        assertNull(behavior(fractionDigits = 2).parse("."))
    }

    @Test
    fun format_showsOneToFractionDigitsDecimals() {
        assertEquals("232", behavior(fractionDigits = 0).format(232))
        assertEquals("12.0", behavior(fractionDigits = 1).format(120))
        assertEquals("232.0", behavior(fractionDigits = 2).format(23200))
        assertEquals("1.5", behavior(fractionDigits = 2).format(150))
        assertEquals("1.25", behavior(fractionDigits = 2).format(125))
        assertEquals("0.045", behavior(fractionDigits = 4).format(450))
    }

    @Test
    fun format_usesAsciiDigitsInLocalesWithOtherDigits() {
        assertEquals("50", behavior(fractionDigits = 0, locale = FormatLocale.forLanguageTag("ar-EG")).format(50))
        assertEquals("50", behavior(fractionDigits = 0, locale = FormatLocale.forLanguageTag("fa-IR")).format(50))
    }

    private fun behavior(
        fractionDigits: Int,
        locale: FormatLocale = FormatLocale.forLanguageTag("en-US"),
    ) = NumberInputBehavior(fractionDigits, locale)
}
