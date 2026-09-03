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

package org.neotech.app.abysner.domain.diveplanning.model

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.decompression.model.DiveSegment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class DivePlanTest {

    private val environment = Environment.Default
    private val configuration = Configuration()
    private val airCylinder = Cylinder.steel12Liter(Gas.Air)
    private val nitroxCylinder = Cylinder.aluminium80Cuft(Gas.Nitrox50)

    @Test
    fun isEmpty_trueForEmptySegments() {
        val plan = divePlan(segments = emptyList())
        assertTrue(plan.isEmpty)
    }

    @Test
    fun isEmpty_falseWhenSegmentsExist() {
        val plan = divePlan(segments = listOf(flatSegment(start = 0, depth = 20.0, duration = 10)))
        assertFalse(plan.isEmpty)
    }

    @Test
    fun maximumDepth_returnsDeepestSegmentDepth() {
        val plan = divePlan(
            segments = listOf(
                travelSegment(start = 0, startDepth = 0.0, endDepth = 30.0, duration = 3),
                flatSegment(start = 3, depth = 30.0, duration = 20),
                travelSegment(start = 23, startDepth = 30.0, endDepth = 0.0, duration = 6),
            )
        )
        assertEquals(30.0, plan.maximumDepth)
    }

    @Test
    fun runtime_returnsSumOfAllSegmentDurations() {
        val plan = divePlan(
            segments = listOf(
                travelSegment(start = 0, startDepth = 0.0, endDepth = 20.0, duration = 4),
                flatSegment(start = 4, depth = 20.0, duration = 16),
                travelSegment(start = 20, startDepth = 20.0, endDepth = 0.0, duration = 4),
            )
        )
        assertEquals(24, plan.runtime)
    }

    @Test
    fun firstDeco_returnsMinuteOfFirstDecoSegment() {
        val plan = divePlan(
            segments = listOf(
                travelSegment(start = 0, startDepth = 0.0, endDepth = 40.0, duration = 4),
                flatSegment(start = 4, depth = 40.0, duration = 20),
                travelSegment(start = 24, startDepth = 40.0, endDepth = 6.0, duration = 7),
                flatSegment(start = 31, depth = 6.0, duration = 5, type = DiveSegment.Type.DECO_STOP, gfCeilingAtEnd = 3.0),
            )
        )
        // The first segment with gfCeilingAtEnd > 0 starts at minute 31
        assertEquals(31, plan.firstDeco)
    }

    @Test
    fun firstDeco_returnsMinusOneWhenNoDecoExists() {
        val plan = divePlan(
            segments = listOf(
                travelSegment(start = 0, startDepth = 0.0, endDepth = 18.0, duration = 2),
                flatSegment(start = 2, depth = 18.0, duration = 30),
                travelSegment(start = 32, startDepth = 18.0, endDepth = 0.0, duration = 4),
            )
        )
        assertEquals(-1, plan.firstDeco)
    }

    @Test
    fun totalDeco_returnsSumOfDecoStopDurations() {
        val plan = divePlan(
            segments = listOf(
                flatSegment(start = 0, depth = 40.0, duration = 20),
                flatSegment(start = 20, depth = 6.0, duration = 3, type = DiveSegment.Type.DECO_STOP),
                flatSegment(start = 23, depth = 3.0, duration = 7, type = DiveSegment.Type.DECO_STOP),
            )
        )
        assertEquals(10, plan.totalDeco)
    }

    @Test
    fun averageDepth_computesWeightedAverage() {
        val plan = divePlan(
            segments = listOf(
                // 4 min linear descent 0-20m (average depth 10m)
                travelSegment(start = 0, startDepth = 0.0, endDepth = 20.0, duration = 4),
                // 16 min at 20m (average depth 20m)
                flatSegment(start = 4, depth = 20.0, duration = 16),
                // 4 min linear ascent 20-0m (average 10m)
                travelSegment(start = 20, startDepth = 20.0, endDepth = 0.0, duration = 4),
            )
        )
        val expected = (10.0 * 4 + 20.0 * 16 + 10.0 * 4) / (4 + 16 + 4)
        assertEquals(expected, plan.averageDepth, 0.001)
    }

    @Test
    fun maxTimeToSurface_returnsSegmentWithHighestTts() {
        val plan = divePlan(
            segments = listOf(
                flatSegment(start = 0, depth = 40.0, duration = 10, ttsAfter = 15),
                flatSegment(start = 10, depth = 40.0, duration = 10, ttsAfter = 25),
                flatSegment(start = 20, depth = 30.0, duration = 5, ttsAfter = 10),
            )
        )
        assertEquals(25, plan.maxTimeToSurface?.ttsAfter)
        assertEquals(10, plan.maxTimeToSurface?.start)
    }

    @Test
    fun maxTimeToSurface_nullWhenNoTtsCalculated() {
        val plan = divePlan(
            segments = listOf(
                flatSegment(start = 0, depth = 20.0, duration = 30),
            )
        )
        assertNull(plan.maxTimeToSurface)
    }

    @Test
    fun maxTimeToSurfaceBailout_returnsSegmentWithHighestBailoutTts() {
        val plan = divePlan(
            segments = listOf(
                flatSegment(start = 0, depth = 40.0, duration = 10, ttsAfter = 10, ttsBailoutAfter = 20),
                flatSegment(start = 10, depth = 40.0, duration = 10, ttsAfter = 15, ttsBailoutAfter = 35),
            )
        )
        assertEquals(35, plan.maxTimeToSurfaceBailout?.ttsBailoutAfter)
    }

    @Test
    fun maximumGasDensities_returnsOneEntryPerCylinder() {
        val plan = divePlan(
            segments = listOf(
                travelSegment(start = 0, startDepth = 0.0, endDepth = 30.0, duration = 3),
                flatSegment(start = 3, depth = 30.0, duration = 20),
                travelSegment(start = 23, startDepth = 30.0, endDepth = 21.0, duration = 3),
                flatSegment(start = 26, depth = 21.0, duration = 5, cylinder = nitroxCylinder),
            ),
            cylinders = listOf(AssignedCylinder(airCylinder), AssignedCylinder(nitroxCylinder)),
        )
        val densities = plan.maximumGasDensities
        assertEquals(2, densities.size)
    }

    private fun flatSegment(
        start: Int,
        depth: Double,
        duration: Int,
        cylinder: Cylinder = airCylinder,
        type: DiveSegment.Type = DiveSegment.Type.FLAT,
        gfCeilingAtEnd: Double = 0.0,
        ttsAfter: Int? = null,
        ttsBailoutAfter: Int? = null,
    ) = DiveSegment.fromMeters(
        start = start,
        duration = duration,
        startDepth = depth,
        endDepth = depth,
        cylinder = cylinder,
        gfCeilingAtEnd = gfCeilingAtEnd,
        type = type,
        environment = environment,
    ).copy(ttsAfter = ttsAfter, ttsBailoutAfter = ttsBailoutAfter)

    private fun travelSegment(
        start: Int,
        startDepth: Double,
        endDepth: Double,
        duration: Int,
        cylinder: Cylinder = airCylinder,
    ) = DiveSegment.fromMeters(
        start = start,
        duration = duration,
        startDepth = startDepth,
        endDepth = endDepth,
        cylinder = cylinder,
        gfCeilingAtEnd = 0.0,
        type = if (startDepth < endDepth) DiveSegment.Type.DECENT else DiveSegment.Type.ASCENT,
        environment = environment,
    )

    private fun divePlan(
        segments: List<DiveSegment>,
        cylinders: List<AssignedCylinder> = listOf(AssignedCylinder(airCylinder)),
        totalCns: Double = 0.0,
        totalOtu: Double = 0.0,
    ) = DivePlan(
        segments = persistentListOf(*segments.toTypedArray()),
        alternativeAccents = persistentMapOf(),
        cylinders = persistentListOf(*cylinders.toTypedArray()),
        configuration = configuration,
        totalCns = totalCns,
        totalOtu = totalOtu,
    )
}
