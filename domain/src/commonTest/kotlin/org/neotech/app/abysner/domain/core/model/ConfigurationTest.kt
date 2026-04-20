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

package org.neotech.app.abysner.domain.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigurationTest {

    private val configuration = Configuration(
        maxAscentRate = 5.0,
        maxDescentRate = 20.0,
    )

    @Test
    fun travelTime_returnsWholeMinutesForEvenDivision() {
        assertEquals(2, configuration.travelTime(-40.0))
        assertEquals(3, configuration.travelTime(15.0))
    }

    @Test
    fun travelTime_roundsUpToNotExceedConfiguredRate() {
        assertEquals(3, configuration.travelTime(-50.0))
        assertEquals(2, configuration.travelTime(8.0))
    }

    @Test
    fun travelTime_returnsZeroForZeroDistance() {
        assertEquals(0, configuration.travelTime(0.0))
    }
}
