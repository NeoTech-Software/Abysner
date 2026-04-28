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

package org.neotech.app.abysner.domain.decompression

import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.physics.feetToAmbientPressure
import org.neotech.app.abysner.domain.core.physics.feetToHydrostaticPressure
import org.neotech.app.abysner.domain.core.physics.metersToAmbientPressure
import org.neotech.app.abysner.domain.core.physics.metersToHydrostaticPressure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DecoGridTest {

    private val environment = Environment.SeaLevelFresh
    private val surfacePressure = environment.atmosphericPressure

    @Test
    fun init_rejectsDecoStepNotMultipleOfDisplayUnit() {
        assertFailsWith<IllegalArgumentException> {
            DecoGrid(
                surfacePressure = surfacePressure,
                decoStepSizePressureDelta = metersToHydrostaticPressure(2.0, environment).value,
                displayUnitPressureDelta = metersToHydrostaticPressure(3.0, environment).value,
                lastDecoStopAmbientPressure = metersToAmbientPressure(2.0, environment).value,
            )
        }
    }

    @Test
    fun init_acceptsDecoStepThatIsMultipleOfDisplayUnit() {
        DecoGrid(
            surfacePressure = surfacePressure,
            decoStepSizePressureDelta = metersToHydrostaticPressure(3.0, environment).value,
            displayUnitPressureDelta = metersToHydrostaticPressure(1.0, environment).value,
            lastDecoStopAmbientPressure = metersToAmbientPressure(3.0, environment).value,
        )
    }

    @Test
    fun init_rejectsLastDecoStopNotOnDisplayUnitGrid() {
        assertFailsWith<IllegalArgumentException> {
            DecoGrid(
                surfacePressure = surfacePressure,
                decoStepSizePressureDelta = metersToHydrostaticPressure(3.0, environment).value,
                displayUnitPressureDelta = metersToHydrostaticPressure(1.0, environment).value,
                lastDecoStopAmbientPressure = metersToAmbientPressure(2.5, environment).value,
            )
        }
    }

    @Test
    fun init_acceptsLastDecoStopOnDisplayUnitGrid() {
        DecoGrid(
            surfacePressure = surfacePressure,
            decoStepSizePressureDelta = metersToHydrostaticPressure(3.0, environment).value,
            displayUnitPressureDelta = metersToHydrostaticPressure(1.0, environment).value,
            lastDecoStopAmbientPressure = metersToAmbientPressure(6.0, environment).value,
        )
    }

    @Test
    fun init_acceptsImperialAlignedStepSize() {
        DecoGrid(
            surfacePressure = surfacePressure,
            decoStepSizePressureDelta = feetToHydrostaticPressure(10.0, environment).value,
            displayUnitPressureDelta = feetToHydrostaticPressure(1.0, environment).value,
            lastDecoStopAmbientPressure = feetToAmbientPressure(20.0, environment).value,
        )
    }

    @Test
    fun snapCeilingToDecoGrid_returnsSurfaceWhenCeilingAtOrAboveSurface() {
        val grid = metricGrid()
        assertEquals(surfacePressure, grid.snapCeilingToDecoGrid(surfacePressure))
        assertEquals(surfacePressure, grid.snapCeilingToDecoGrid(surfacePressure - 0.1))
    }

    @Test
    fun snapCeilingToDecoGrid_snapsUpToNearestGridPoint() {
        val grid = metricGrid()
        val rawCeiling = metersToAmbientPressure(4.0, environment).value
        val expectedGridCeiling = metersToAmbientPressure(6.0, environment).value
        assertEquals(expectedGridCeiling, grid.snapCeilingToDecoGrid(rawCeiling))
    }

    @Test
    fun snapCeilingToDecoGrid_clampsToLastDecoStop() {
        val grid = metricGrid(decoStepMeters = 3.0, lastDecoStopMeters = 6.0)
        val rawCeiling = metersToAmbientPressure(2.0, environment).value
        val expectedGridCeiling = metersToAmbientPressure(6.0, environment).value
        assertEquals(expectedGridCeiling, grid.snapCeilingToDecoGrid(rawCeiling))
    }

    @Test
    fun findNextDecoStopPressure_returnsSurfaceFromLastDecoStop() {
        val grid = metricGrid()
        val currentStop = metersToAmbientPressure(3.0, environment).value
        assertEquals(surfacePressure, grid.findNextDecoStopPressure(currentStop))
    }

    @Test
    fun findNextDecoStopPressure_returnsNextShallowerGridPoint() {
        val grid = metricGrid()
        val currentStop = metersToAmbientPressure(9.0, environment).value
        val expectedNextStop = metersToAmbientPressure(6.0, environment).value
        assertEquals(expectedNextStop, grid.findNextDecoStopPressure(currentStop))
    }

    @Test
    fun findNextDecoStopPressure_betweenGridPointsReturnsShallowerGridPoint() {
        val grid = metricGrid()
        // This is a bit of a weird stop location (should in production code not happen, but the
        // grid code should be able to handle it). Let's just pretend the diver is at 7 meter, then
        // the next shallower stop to reach should be at 6 meters if we want to get back on the
        // grid.
        val currentStop = metersToAmbientPressure(7.0, environment).value
        val expectedNextStop = metersToAmbientPressure(6.0, environment).value
        assertEquals(expectedNextStop, grid.findNextDecoStopPressure(currentStop))
    }

    @Test
    fun findNextDecoStopPressure_returnsSurfaceWhenNextStopShallowerThanLastDecoStop() {
        val grid = metricGrid(decoStepMeters = 3.0, lastDecoStopMeters = 6.0)
        val currentStop = metersToAmbientPressure(6.0, environment).value
        assertEquals(surfacePressure, grid.findNextDecoStopPressure(currentStop))
    }

    @Test
    fun findNextDecoStopPressure_returnsSurfaceAtSurface() {
        val grid = metricGrid()
        assertEquals(surfacePressure, grid.findNextDecoStopPressure(surfacePressure))
    }

    @Test
    fun isAtDecoStop_returnsTrueForGridPoint() {
        val grid = metricGrid()
        val pointOnGrid = metersToAmbientPressure(6.0, environment).value
        assertTrue(grid.isAtDecoStop(pointOnGrid))
    }

    @Test
    fun isAtDecoStop_returnsFalseForNonGridPoint() {
        val grid = metricGrid()
        val pointNotOnGrid = metersToAmbientPressure(5.0, environment).value
        assertFalse(grid.isAtDecoStop(pointNotOnGrid))
    }

    @Test
    fun isAtDecoStop_returnsTrueAtSurface() {
        val grid = metricGrid()
        assertTrue(grid.isAtDecoStop(surfacePressure))
    }

    private fun metricGrid(
        decoStepMeters: Double = 3.0,
        lastDecoStopMeters: Double = 3.0,
    ) = DecoGrid(
        surfacePressure = surfacePressure,
        decoStepSizePressureDelta = metersToHydrostaticPressure(decoStepMeters, environment).value,
        lastDecoStopAmbientPressure = metersToAmbientPressure(
            lastDecoStopMeters,
            environment
        ).value,
        displayUnitPressureDelta = metersToHydrostaticPressure(1.0, environment).value,
    )
}
