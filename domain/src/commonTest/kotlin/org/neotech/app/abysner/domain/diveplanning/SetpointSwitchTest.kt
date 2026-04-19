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

package org.neotech.app.abysner.domain.diveplanning

import org.neotech.app.abysner.domain.core.model.BreathingMode
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Configuration.Algorithm
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.DiveMode
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.model.Salinity
import org.neotech.app.abysner.domain.decompression.model.DiveSegment
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.diveplanning.model.assign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CcrSetpointSwitchTest {

    private val lowSetpoint = 0.7
    private val highSetpoint = 1.2
    private val lowMode = BreathingMode.ClosedCircuit(lowSetpoint)
    private val highMode = BreathingMode.ClosedCircuit(highSetpoint)

    private fun createPlanner(
        switchDepthDescend: Int? = 6,
        switchDepthAscend: Int? = 6,
    ): DivePlanner = DivePlanner(
        Configuration(
            maxAscentRate = 9.0,
            maxDescentRate = 18.0,
            gfLow = 0.3,
            gfHigh = 0.7,
            salinity = Salinity.WATER_SALT,
            algorithm = Algorithm.BUHLMANN_ZH16C,
            decoStepSize = 3,
            lastDecoStopDepth = 3,
            ccrLowSetpoint = lowSetpoint,
            ccrHighSetpoint = highSetpoint,
            ccrToHighSetpointSwitchDepth = switchDepthDescend,
            ccrToLowSetpointSwitchDepth = switchDepthAscend,
        )
    )

    @Test
    fun ccrDive_descentSwitchDepthSetsBreathingModeAtEnd() {
        val diluent = Cylinder.aluminium80Cuft(Gas.Air)
        val planner = createPlanner(switchDepthDescend = 6, switchDepthAscend = null)

        val plan = planner.addDive(
            listOf(DiveProfileSection(duration = 20, depth = 36, cylinder = diluent)),
            listOf(diluent).assign(),
            diveMode = DiveMode.CLOSED_CIRCUIT
        )

        // The descent from 0 to 18 meters crosses the 6 meters switch point. At 18 meters per
        // minute the switch will be in the first segment/minute.
        val segments = plan.segments
        val firstSegment = segments.first()

        assertEquals(DiveSegment.Type.DECENT, firstSegment.type)
        assertEquals(lowMode, firstSegment.breathingMode)
        assertEquals(highMode, firstSegment.breathingModeAtEnd)
    }

    @Test
    fun ccrDive_ascentSwitchDepthSetsBreathingModeAtEnd() {
        val diluent = Cylinder.aluminium80Cuft(Gas.Air)
        val planner = createPlanner(switchDepthDescend = null, switchDepthAscend = 6)

        val plan = planner.addDive(
            listOf(DiveProfileSection(duration = 10, depth = 18, cylinder = diluent)),
            listOf(diluent).assign(),
            diveMode = DiveMode.CLOSED_CIRCUIT
        )

        val segments = plan.segments

        // The ascent from 18 to 0 meters crosses the 6 meters switch point. At 9 meters per
        // minute the switch will be in the second (and last) segment/minute.
        val switchSegment = segments.last { it.type == DiveSegment.Type.ASCENT }

        assertEquals(highMode, switchSegment.breathingMode)
        assertEquals(lowMode, switchSegment.breathingModeAtEnd)
    }

    @Test
    fun ccrDive_ascentSwitchDepthWithDecoStopSwitchesAtStopDepth() {
        val diluent = Cylinder.aluminium80Cuft(Gas.Air)
        val planner = createPlanner(switchDepthDescend = null, switchDepthAscend = 6)

        val plan = planner.addDive(
            listOf(DiveProfileSection(duration = 30, depth = 30, cylinder = diluent)),
            listOf(diluent).assign(),
            diveMode = DiveMode.CLOSED_CIRCUIT
        )

        val segments = plan.segments

        // With deco stops the switch depth aligns with a deco stop at 6 meters. The planner
        // transitions to the low setpoint at the deco stop level exactly (no sub-minute switch).

        // All segments at or above 6 meter should use the low setpoint.
        val segmentsAtOrAboveSwitchDepth = segments.filter {
            it.startDepth <= 6.0 && it.type != DiveSegment.Type.DECENT
        }
        segmentsAtOrAboveSwitchDepth.forEach {
            assertEquals(lowMode, it.breathingMode)
        }

        // All segments below 6 meter should use the high setpoint.
        val deepAscentSegments = segments.filter {
            it.type == DiveSegment.Type.ASCENT && it.startDepth > 6.0
        }
        deepAscentSegments.forEach {
            assertEquals(highMode, it.breathingMode)
        }
    }

    @Test
    fun ccrDive_switchDepthDisabledUsesDefaultBehavior() {
        val diluent = Cylinder.aluminium80Cuft(Gas.Air)
        val planner = createPlanner(switchDepthDescend = null, switchDepthAscend = null)

        val plan = planner.addDive(
            listOf(DiveProfileSection(duration = 30, depth = 30, cylinder = diluent)),
            listOf(diluent).assign(),
            diveMode = DiveMode.CLOSED_CIRCUIT
        )

        val segments = plan.segments

        // No segment should have breathingModeAtEnd set, since the switches happen between
        // sections with default behavior.
        val switchSegments = segments.filter { it.breathingModeAtEnd != null }
        assertEquals(0, switchSegments.size)

        // All descents should use low setpoint
        segments.filter { it.type == DiveSegment.Type.DECENT }.forEach {
            assertEquals(lowMode, it.breathingMode)
        }
        // All ascents and flat segments should use high setpoint
        segments.filter { it.type != DiveSegment.Type.DECENT }.forEach {
            assertEquals(highMode, it.breathingMode)
        }
    }
}
