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

package org.neotech.app.abysner.presentation.formatting

import org.neotech.app.abysner.domain.core.model.Gas
import kotlin.test.Test
import kotlin.test.assertEquals

class GasFormatterTest {

    @Test
    fun toPaddedMixString_padsShorterMixesAtTheEnd() {
        assertEquals("8/70$FIGURE_SPACE", Gas(oxygenFraction = 0.08, heliumFraction = 0.70).toPaddedMixString(padEnd = true))
        assertEquals("32/0$FIGURE_SPACE", Gas(oxygenFraction = 0.32, heliumFraction = 0.0).toPaddedMixString(padEnd = true))
        assertEquals("8/0$FIGURE_SPACE$FIGURE_SPACE", Gas(oxygenFraction = 0.08, heliumFraction = 0.0).toPaddedMixString(padEnd = true))
    }

    @Test
    fun toPaddedMixString_padsShorterMixesAtTheStart() {
        assertEquals("${FIGURE_SPACE}8/70", Gas(oxygenFraction = 0.08, heliumFraction = 0.70).toPaddedMixString(padEnd = false))
        assertEquals("${FIGURE_SPACE}32/0", Gas(oxygenFraction = 0.32, heliumFraction = 0.0).toPaddedMixString(padEnd = false))
        assertEquals("$FIGURE_SPACE${FIGURE_SPACE}8/0", Gas(oxygenFraction = 0.08, heliumFraction = 0.0).toPaddedMixString(padEnd = false))
    }

    @Test
    fun toPaddedMixString_doesNotPadFullWidthMixes() {
        assertEquals("21/35", Gas(oxygenFraction = 0.21, heliumFraction = 0.35).toPaddedMixString(padEnd = true))
        assertEquals("100/0", Gas(oxygenFraction = 1.0, heliumFraction = 0.0).toPaddedMixString(padEnd = true))
        assertEquals("100/0", Gas(oxygenFraction = 1.0, heliumFraction = 0.0).toPaddedMixString(padEnd = false))
    }
}
