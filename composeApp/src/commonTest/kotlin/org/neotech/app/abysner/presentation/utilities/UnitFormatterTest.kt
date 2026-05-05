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

package org.neotech.app.abysner.presentation.utilities

import org.neotech.app.abysner.domain.core.model.UnitSystem
import kotlin.test.Test
import kotlin.test.assertEquals

class UnitFormatterTest {

    @Test
    fun formatDisplayDepth_roundsAndAppendsUnit() {
        assertEquals("30 m", 30.0.formatDisplayDepth(UnitSystem.METRIC))
        assertEquals("100 ft", 100.0.formatDisplayDepth(UnitSystem.IMPERIAL))
    }

    @Test
    fun formatDisplayDepth_respectsDecimalParameter() {
        assertEquals("30.5 m", 30.48.formatDisplayDepth(UnitSystem.METRIC, decimals = 1))
        assertEquals("100.3 ft", 100.3.formatDisplayDepth(UnitSystem.IMPERIAL, decimals = 1))
    }

    @Test
    fun formatDepth_convertsMetersToDisplayUnit() {
        assertEquals("30 m", 30.48.formatDepth(UnitSystem.METRIC))
        assertEquals("100 ft", 30.48.formatDepth(UnitSystem.IMPERIAL))
    }


    @Test
    fun formatPressure_convertsBarToDisplayUnit() {
        assertEquals("200 bar", 200.0.formatPressure(UnitSystem.METRIC))
        assertEquals("2901 psi", 200.0.formatPressure(UnitSystem.IMPERIAL))
    }

    @Test
    fun formatVolume_convertsLitersToDisplayUnit() {
        assertEquals("2400 L", 2400.0.formatVolume(UnitSystem.METRIC))
        assertEquals("100 ft³", 2831.6846592.formatVolume(UnitSystem.IMPERIAL))
    }

    @Test
    fun formatVolume_respectsDecimalParameter() {
        assertEquals("12.0 L", 12.0.formatVolume(UnitSystem.METRIC, decimals = 1))
        assertEquals("1.0 ft³", 28.316846592.formatVolume(UnitSystem.IMPERIAL, decimals = 1))
    }

    @Test
    fun formatVolume_supportsUnitOverride() {
        assertEquals("20 L/min", 20.0.formatVolume(UnitSystem.METRIC, unit = UnitSystem.METRIC.sacRateUnitLabel))
        assertEquals("1 ft³/min", 28.316846592.formatVolume(UnitSystem.IMPERIAL, unit = UnitSystem.IMPERIAL.sacRateUnitLabel))
    }
}
