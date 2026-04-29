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

import org.neotech.app.abysner.domain.core.physics.ATMOSPHERIC_PRESSURE_AT_SEA_LEVEL
import org.neotech.app.abysner.domain.core.physics.METERS_PER_FOOT
import org.neotech.app.abysner.domain.core.physics.metersToAmbientPressure
import kotlin.test.Test
import kotlin.test.assertEquals

class UnitSystemTest {

    private val environment = Environment(Salinity.WATER_EN13319, ATMOSPHERIC_PRESSURE_AT_SEA_LEVEL)

    @Test
    fun snapMetersToDisplayUnit_metricRoundsToNearestMeter() {
        assertEquals(10.0, UnitSystem.METRIC.snapMetersToDisplayUnit(10.0))
        assertEquals(11.0, UnitSystem.METRIC.snapMetersToDisplayUnit(10.6))
    }

    @Test
    fun snapMetersToDisplayUnit_imperialRoundsToNearestFoot() {
        assertEquals(100.0 * METERS_PER_FOOT, UnitSystem.IMPERIAL.snapMetersToDisplayUnit(100.0 * METERS_PER_FOOT))
        assertEquals(98.0 * METERS_PER_FOOT, UnitSystem.IMPERIAL.snapMetersToDisplayUnit(98.2 * METERS_PER_FOOT))
    }

    @Test
    fun displayDepthToMeters_convertsDisplayUnitsToMeters() {
        assertEquals(30.0, UnitSystem.METRIC.displayDepthToMeters(30.0))
        assertEquals(30.48, UnitSystem.IMPERIAL.displayDepthToMeters(100.0), 1e-10)
    }

    @Test
    fun metersToDisplayDepth_metricRoundsToNearestMeter() {
        assertEquals(30.0, UnitSystem.METRIC.metersToDisplayDepth(30.48))
        assertEquals(31.0, UnitSystem.METRIC.metersToDisplayDepth(30.6))
    }

    @Test
    fun metersToDisplayDepth_imperialConvertsToNearestFoot() {
        assertEquals(100.0, UnitSystem.IMPERIAL.metersToDisplayDepth(30.48))
        assertEquals(98.0, UnitSystem.IMPERIAL.metersToDisplayDepth(30.0))
    }

    @Test
    fun depthToAmbientPressure_convertsDisplayDepthToPressure() {
        val expectedMetric = metersToAmbientPressure(30.0, environment)
        assertEquals(expectedMetric.value, UnitSystem.METRIC.depthToAmbientPressure(30.0, environment).value)

        val expectedImperial = metersToAmbientPressure(30.48, environment)
        assertEquals(expectedImperial.value, UnitSystem.IMPERIAL.depthToAmbientPressure(100.0, environment).value)
    }
}
