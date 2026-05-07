/*
 * Abysner - Dive planner
 * Copyright (C) 2024-2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.domain.core.model

import org.neotech.app.abysner.domain.core.physics.asBarToPsi
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CylinderTest {

    @Test
    fun capacityAt_returnsCorrectCapacity() {
        val cylinder = Cylinder(Gas.Air, 232.0, 12.0)
        assertEquals(2316.0, cylinder.capacityAt(pressure = 200.0), 1.0)
    }

    @Test
    fun pressureAt_returnsCorrectPressure() {
        val cylinder = Cylinder(Gas.Air, 232.0, 12.0)
        assertEquals(99.0, cylinder.pressureAt(volume = 1200.0), 1.0)
    }

    @Test
    fun ratedCapacity_returnsCorrectRatedCapacity() {
        assertEquals(2206.0, Cylinder.AL80.ratedCapacity(), 1.0)
        assertEquals(2633.0, Cylinder.STEEL_12L.ratedCapacity(), 1.0)
    }

    @Test
    fun workingPressure_roundTripsToExactPsiForImperialCylinders() {
        assertEquals(3000, Cylinder.AL80.workingPressure.asBarToPsi().roundToInt())
        assertEquals(3300, Cylinder.AL100.workingPressure.asBarToPsi().roundToInt())
        assertEquals(3442, Cylinder.HP100.workingPressure.asBarToPsi().roundToInt())
    }

    @Test
    fun pressureAfter_returnsReducedPressure() {
        val cylinder = Cylinder(Gas.Air, 200.0, 10.0)
        val pressure = cylinder.pressureAfter(volumeUsage = 1500.0)
        // Not asserting on exact value, to avoid coupling to the equation of state model.
        assertNotNull(pressure)
        assertTrue(pressure < 100.0)
        assertTrue(pressure > 0.0)
    }

    @Test
    fun pressureAfter_returnsNullWhenVolumeExceedsCapacity() {
        val cylinder = Cylinder(Gas.Air, 200.0, 10.0)
        val pressure = cylinder.pressureAfter(volumeUsage = 20000.0)
        assertNull(pressure)
    }

    @Test
    fun sizeFindMatching_returnsMatchingStandardSize() {
        val found = Cylinder.Size.findMatching(waterVolume = 12.0, workingPressure = 232.0)
        assertNotNull(found)
        assertEquals("12L", found.name)
    }

    @Test
    fun sizeFindMatching_matchesWithinTolerance() {
        // Tolerance is 0.05L and 0.5 bar
        val found = Cylinder.Size.findMatching(waterVolume = 12.04, workingPressure = 231.6)
        assertNotNull(found)
        assertEquals("12L", found.name)
    }

    @Test
    fun sizeFindMatching_returnsNullForDeviationBeyondTolerance() {
        val found = Cylinder.Size.findMatching(waterVolume = 12.1, workingPressure = 232.0)
        assertNull(found)
    }

    @Test
    fun sizeFill_createsCylinderWithWorkingPressure() {
        val cylinder = Cylinder.STEEL_12L.fill(Gas.Air)
        assertEquals(Cylinder.STEEL_12L.workingPressure, cylinder.pressure)
        assertEquals(Cylinder.STEEL_12L.waterVolume, cylinder.waterVolume)
        assertEquals(Gas.Air, cylinder.gas)
    }
}
