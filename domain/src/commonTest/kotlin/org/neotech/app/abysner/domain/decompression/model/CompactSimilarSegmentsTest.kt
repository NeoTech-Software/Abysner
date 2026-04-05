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

package org.neotech.app.abysner.domain.decompression.model

import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.assertSegment
import kotlin.test.Test
import kotlin.test.assertEquals

class CompactSimilarSegmentsTest {

    private val airCylinder = Cylinder.steel12Liter(Gas.Air)
    private val nitroxCylinder = Cylinder.aluminium80Cuft(Gas.Nitrox50)

    private fun flatSegment(
        start: Int,
        depth: Double,
        duration: Int,
        type: DiveSegment.Type = DiveSegment.Type.FLAT,
        cylinder: Cylinder = airCylinder,
        gfCeilingAtEnd: Double = 0.0,
    ) = DiveSegment(
        start = start,
        duration = duration,
        startDepth = depth,
        endDepth = depth,
        cylinder = cylinder,
        gfCeilingAtEnd = gfCeilingAtEnd,
        type = type,
    )

    private fun travelSegment(
        start: Int,
        startDepth: Double,
        endDepth: Double,
        duration: Int,
        cylinder: Cylinder = airCylinder,
        gfCeilingAtEnd: Double = 0.0,
    ) = DiveSegment(
        start = start,
        duration = duration,
        startDepth = startDepth,
        endDepth = endDepth,
        cylinder = cylinder,
        gfCeilingAtEnd = gfCeilingAtEnd,
        type = if (startDepth < endDepth) DiveSegment.Type.DECENT else DiveSegment.Type.ASCENT,
    )

    @Test
    fun compactSimilarSegments_mergesFlatSegmentsOfSameTypeAndGas() {
        val segments = mutableListOf(
            flatSegment(start = 0, depth = 9.0, duration = 3),
            flatSegment(start = 3, depth = 9.0, duration = 2),
        )
        val result = segments.compactSimilarSegments()
        assertEquals(1, result.size, "Expected segments to be merged")
        result.assertSegment(0, DiveSegment.Type.FLAT, startDepth = 9.0, endDepth = 9.0, duration = 5, gas = airCylinder)
    }

    @Test
    fun compactSimilarSegments_doesNotMergeFlatSegmentsOfDifferentType() {
        val segments = mutableListOf(
            flatSegment(start = 0, depth = 9.0, duration = 3, type = DiveSegment.Type.FLAT),
            flatSegment(start = 3, depth = 9.0, duration = 2, type = DiveSegment.Type.DECO_STOP),
        )
        val result = segments.compactSimilarSegments()
        assertEquals(2, result.size, "Expected segments with different types to remain separate")
    }

    @Test
    fun compactSimilarSegments_doesNotMergeFlatSegmentsOfDifferentGas() {
        val segments = mutableListOf(
            flatSegment(start = 0, depth = 9.0, duration = 3, cylinder = airCylinder),
            flatSegment(start = 3, depth = 9.0, duration = 2, cylinder = nitroxCylinder),
        )
        val result = segments.compactSimilarSegments()
        assertEquals(2, result.size, "Expected segments with different gas to remain separate")
    }

    @Test
    fun compactSimilarSegments_mergesTravelSegmentsWithSameSpeed() {
        // Both ascents at 3m/min: 9m→6m in 1min, 6m→3m in 1min
        val segments = mutableListOf(
            travelSegment(start = 0, startDepth = 9.0, endDepth = 6.0, duration = 1),
            travelSegment(start = 1, startDepth = 6.0, endDepth = 3.0, duration = 1),
        )
        val result = segments.compactSimilarSegments()
        assertEquals(1, result.size, "Expected travel segments to be merged")
        result.assertSegment(0, DiveSegment.Type.ASCENT, startDepth = 9.0, endDepth = 3.0, duration = 2, gas = airCylinder)
    }

    @Test
    fun compactSimilarSegments_doesNotMergeTravelSegmentsWithDifferentSpeed() {
        // First ascent: 9m→6m in 1min = 3m/min, second: 6m→0m in 1min = 6m/min
        val segments = mutableListOf(
            travelSegment(start = 0, startDepth = 9.0, endDepth = 6.0, duration = 1),
            travelSegment(start = 1, startDepth = 6.0, endDepth = 0.0, duration = 1),
        )
        val result = segments.compactSimilarSegments()
        assertEquals(2, result.size, "Expected travel segments with different speeds to remain separate")
    }

    @Test
    fun compactSimilarSegments_mergesMultipleAdjacentSimilarSegments() {
        val segments = mutableListOf(
            flatSegment(start = 0, depth = 9.0, duration = 1),
            flatSegment(start = 1, depth = 9.0, duration = 1),
            flatSegment(start = 2, depth = 9.0, duration = 1),
        )
        val result = segments.compactSimilarSegments()
        assertEquals(1, result.size, "Expected all three segments to be merged into one")
        assertEquals(3, result[0].duration)
    }

    @Test
    fun compactSimilarSegments_gfCeilingAtEndTakenFromLastMergedSegment() {
        val segments = mutableListOf(
            flatSegment(start = 0, depth = 9.0, duration = 3, gfCeilingAtEnd = 5.0),
            flatSegment(start = 3, depth = 9.0, duration = 2, gfCeilingAtEnd = 8.0),
        )
        val result = segments.compactSimilarSegments()
        assertEquals(8.0, result[0].gfCeilingAtEnd, "gfCeilingAtEnd should come from the last merged segment")
    }

    @Test
    fun compactSimilarSegments_mergesAscentBetweenDecoStopsWhenEnabled() {
        val segments = mutableListOf(
            flatSegment(start = 0,  depth = 9.0, duration = 5,  type = DiveSegment.Type.DECO_STOP),
            travelSegment(start = 5, startDepth = 9.0, endDepth = 6.0, duration = 1),
            flatSegment(start = 6,  depth = 6.0, duration = 10, type = DiveSegment.Type.DECO_STOP),
        )
        val result = segments.compactSimilarSegments(compactAscentsAndStops = true)
        assertEquals(2, result.size, "Expected ascent to be merged into the shallower deco stop")
        result.assertSegment(1, DiveSegment.Type.DECO_STOP, startDepth = 6.0, endDepth = 6.0, duration = 11, gas = airCylinder)
    }

    @Test
    fun compactSimilarSegments_doesNotMergeAscentBetweenDecoStopsWhenDisabled() {
        val segments = mutableListOf(
            flatSegment(start = 0, depth = 9.0, duration = 5, type = DiveSegment.Type.DECO_STOP),
            travelSegment(start = 5, startDepth = 9.0, endDepth = 6.0, duration = 1),
            flatSegment(start = 6, depth = 6.0, duration = 10, type = DiveSegment.Type.DECO_STOP),
        )
        val result = segments.compactSimilarSegments(compactAscentsAndStops = false)
        assertEquals(3, result.size, "Expected segments to remain separate when compacting is disabled")
    }

    @Test
    fun compactSimilarSegments_mergesGasSwitchIntoFollowingDecoStopWhenEnabled() {
        val segments = mutableListOf(
            flatSegment(start = 0, depth = 9.0, duration = 1, type = DiveSegment.Type.GAS_SWITCH, cylinder = airCylinder),
            flatSegment(start = 1, depth = 9.0, duration = 5, type = DiveSegment.Type.DECO_STOP, cylinder = nitroxCylinder),
        )
        val result = segments.compactSimilarSegments(compactAscentsAndStops = true)
        assertEquals(1, result.size, "Expected gas switch and deco stop to be merged")
        result.assertSegment(0, DiveSegment.Type.GAS_SWITCH, startDepth = 9.0, endDepth = 9.0, duration = 6, gas = airCylinder)
    }

    @Test
    fun compactSimilarSegments_doesNotMergeGasSwitchIntoDecoStopWhenDisabled() {
        val segments = mutableListOf(
            flatSegment(start = 0, depth = 9.0, duration = 1, type = DiveSegment.Type.GAS_SWITCH, cylinder = airCylinder),
            flatSegment(start = 1, depth = 9.0, duration = 5, type = DiveSegment.Type.DECO_STOP, cylinder = nitroxCylinder),
        )
        val result = segments.compactSimilarSegments(compactAscentsAndStops = false)
        assertEquals(2, result.size, "Expected gas switch and deco stop to remain separate when compacting is disabled")
    }

    @Test
    fun compactSimilarSegments_mergesAscentBetweenGasSwitchAndDecoStop() {
        val segments = mutableListOf(
            flatSegment(start = 0, depth = 9.0, duration = 1, type = DiveSegment.Type.GAS_SWITCH, cylinder = airCylinder),
            travelSegment(start = 1, startDepth = 9.0, endDepth = 6.0, duration = 1, cylinder = airCylinder),
            flatSegment(start = 2, depth = 6.0, duration = 10, type = DiveSegment.Type.DECO_STOP,  cylinder = nitroxCylinder),
        )
        val result = segments.compactSimilarSegments(compactAscentsAndStops = true)
        assertEquals(2, result.size, "Expected ascent to be folded into the shallower deco stop")
        result.assertSegment(0, DiveSegment.Type.GAS_SWITCH, startDepth = 9.0, endDepth = 9.0, duration = 1,  gas = airCylinder)
        result.assertSegment(1, DiveSegment.Type.DECO_STOP,  startDepth = 6.0, endDepth = 6.0, duration = 11, gas = nitroxCylinder)
    }

    @Test
    fun compactSimilarSegments_mergesAscentBetweenDecoStopAndGasSwitch() {
        val segments = mutableListOf(
            flatSegment(start = 0, depth = 9.0, duration = 5, type = DiveSegment.Type.DECO_STOP, cylinder = airCylinder),
            travelSegment(start = 5, startDepth = 9.0, endDepth = 6.0, duration = 1, cylinder = airCylinder),
            flatSegment(start = 6, depth = 6.0, duration = 1, type = DiveSegment.Type.GAS_SWITCH, cylinder = nitroxCylinder),
        )
        val result = segments.compactSimilarSegments(compactAscentsAndStops = true)
        assertEquals(2, result.size, "Expected ascent to be folded into the shallower gas switch")
        result.assertSegment(0, DiveSegment.Type.DECO_STOP,  startDepth = 9.0, endDepth = 9.0, duration = 5, gas = airCylinder)
        result.assertSegment(1, DiveSegment.Type.GAS_SWITCH, startDepth = 6.0, endDepth = 6.0, duration = 2, gas = nitroxCylinder)
    }
}

