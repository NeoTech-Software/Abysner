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

package org.neotech.app.abysner.presentation.screens.planner.decoplan

import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Configuration.Algorithm
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.model.Salinity
import org.neotech.app.abysner.domain.diveplanning.DivePlanner
import org.neotech.app.abysner.domain.diveplanning.model.DivePlan
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.diveplanning.model.assign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DecoPlanGraphCoordinatesTest {

    /**
     * Returns a [DivePlan] with decompression and a gas switch (gasSwitchTime=0).
     */
    private fun divePlan30m30minWithDecoGas(): DivePlan {
        val bottomGas = Cylinder.steel12Liter(Gas.Air)
        val decoGas = Cylinder.aluminium80Cuft(Gas.Nitrox50)
        val divePlanner = DivePlanner(Configuration(
            maxAscentRate = 5.0,
            maxDescentRate = 5.0,
            gfLow = 0.3, gfHigh = 0.7,
            salinity = Salinity.WATER_SALT,
            algorithm = Algorithm.BUHLMANN_ZH16C,
            altitude = 0.0,
            decoStepSize = 3,
            lastDecoStopDepth = 6,
            gasSwitchTime = 0,
        ))
        return divePlanner.addDive(
            plan = listOf(DiveProfileSection(duration = 30, 30, bottomGas)),
            cylinders = listOf(decoGas).assign(),
        )
    }

    /**
     * Returns a [DivePlan] no-decompression dive, the ceiling should stay at 0 throughout.
     */
    private fun divePlan20m20minNoDeco(): DivePlan {
        val bottomGas = Cylinder.steel12Liter(Gas.Air)
        val divePlanner = DivePlanner(Configuration(
            maxAscentRate = 5.0,
            maxDescentRate = 5.0,
            gfLow = 0.3,
            gfHigh = 0.7,
            salinity = Salinity.WATER_FRESH,
            algorithm = Algorithm.BUHLMANN_ZH16C,
            altitude = 0.0,
        ))
        return divePlanner.addDive(
            plan = listOf(DiveProfileSection(duration = 20, 20, bottomGas)),
            cylinders = emptyList(),
        )
    }

    @Test
    fun gfCeiling_hasNoDuplicateXValuesForDiveWithGasSwitch() {
        val plotPoints = buildGfCeilingPlotPoints(divePlan30m30minWithDecoGas().segments)
        val xValues = plotPoints.map { it.x }
        val duplicateXValues = xValues.groupBy { it }.filter { it.value.size > 1 }.keys
        assertTrue(duplicateXValues.isEmpty(), "Duplicate x-coordinates: $duplicateXValues")
    }

    @Test
    fun gfCeiling_hasAtMostOneLeadingZeroPoint() {
        val plotPoints = buildGfCeilingPlotPoints(divePlan30m30minWithDecoGas().segments)
        val leadingZeroCeilingPoints = plotPoints.takeWhile { it.y == 0f }
        assertTrue(leadingZeroCeilingPoints.size <= 1, "Expected at most 1 leading zero-ceiling point but found ${leadingZeroCeilingPoints.size}")
    }

    @Test
    fun gfCeiling_lastPointXMatchesRuntime() {
        val divePlan = divePlan30m30minWithDecoGas()
        val plotPoints = buildGfCeilingPlotPoints(divePlan.segments)
        assertEquals(divePlan.runtime.toFloat(), plotPoints.last().x, "Last coordinate x should equal divePlan.runtime")
    }

    @Test
    fun gfCeiling_isEmptyForNoDecoDive() {
        val plotPoints = buildGfCeilingPlotPoints(divePlan20m20minNoDeco().segments)
        val nonZeroCeilingPoints = plotPoints.filter { it.y != 0f }
        assertTrue(nonZeroCeilingPoints.isEmpty(), "No-deco dive should produce no non-zero ceiling coordinates")
    }

    @Test
    fun depthProfile_lastPointIsAtRuntime() {
        val divePlan = divePlan30m30minWithDecoGas()
        val plotPoints = buildDepthProfilePlotPoints(divePlan.segments)
        val lastSegment = divePlan.segments.last()
        val expectedX = divePlan.segments.sumOf { it.duration }.toFloat()
        assertEquals(expectedX, plotPoints.last().x, "Last x should equal total segment duration sum")
        assertEquals(-lastSegment.endDepth.toFloat(), plotPoints.last().y, "Last y should be last segment endDepth")
    }

    @Test
    fun averageDepth_lastPointXMatchesRuntime() {
        val divePlan = divePlan30m30minWithDecoGas()
        val plotPoints = buildAverageDepthPlotPoints(divePlan.segments)
        assertEquals(divePlan.runtime.toFloat(), plotPoints.last().x, "Last x should equal divePlan.runtime")
    }
}
