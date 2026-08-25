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

package org.neotech.app.abysner.domain.gasplanning

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import org.neotech.app.abysner.domain.core.model.BreathingMode
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.decompression.model.DiveSegment
import org.neotech.app.abysner.domain.diveplanning.DivePlanner.PlanningException
import org.neotech.app.abysner.domain.diveplanning.model.CylinderRole
import org.neotech.app.abysner.domain.diveplanning.model.DivePlan
import org.neotech.app.abysner.domain.diveplanning.model.assign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GasPlannerSacRateTest {

    private val bottomGas = Cylinder.steel12Liter(Gas.Air)
    private val diluentCylinder = Cylinder.aluminium80Cuft(Gas.Air)
    private val bailoutCylinder = Cylinder.aluminium80Cuft(Gas.Nitrox32)

    private val defaultSacRate = 25.0
    private val defaultSacRateStress = 40.0
    private val defaultSacRateDeco = 20.0

    private val defaultBottomSegment = segment(start = 0, duration = 10, type = DiveSegment.Type.FLAT, startPressure = 3.0, ttsAfter = 100)

    private val defaultCcrBottomSegment = segment(start = 0, duration = 10, type = DiveSegment.Type.FLAT, startPressure = 3.0, cylinder = diluentCylinder)
        .copy(breathingMode = BreathingMode.ccr(1.2), ttsBailoutAfter = 100)

    @Test
    fun calculateGasPlan_ocOutOfAirReserveScalesAsTwiceStressMinusNormal() {
        val configuration = Configuration(sacRate = defaultSacRate, sacRateStress = defaultSacRateStress, stressDurationMinutes = 5)
        val divePlan = ocDivePlan(
            configuration = configuration,
            ascent = listOf(segment(start = defaultBottomSegment.end, duration = 5, type = DiveSegment.Type.ASCENT, startPressure = 4.0, endPressure = 1.0)),
        )

        val gasPlan = GasPlanner().calculateGasPlan(divePlan)

        val bottomUsage = defaultBottomSegment.duration * defaultSacRate * 3.0
        assertEquals(bottomUsage, gasPlan[0].normalRequirement, 0.001)

        // The stress duration exactly covers this 5-minute ascent, so the whole reserve resolves
        // to the stress rate.
        val stressRate = 2 * defaultSacRateStress - defaultSacRate
        val reserveUsage = 5 * stressRate * (4.0 + 1.0) / 2
        assertEquals(reserveUsage, gasPlan[0].extraEmergencyRequirement, 0.001)
    }

    @Test
    fun calculateGasPlan_throwsWhenSacRateStressIsBelowSacRateOrSacRateDeco() {
        val invalidConfigurations = listOf(
            Configuration(sacRate = 40.0, sacRateStress = 10.0),
            Configuration(sacRate = 10.0, sacRateDeco = 15.0, sacRateStress = 12.0),
        )

        invalidConfigurations.forEach { configuration ->
            val divePlan = ocDivePlan(configuration = configuration, segments = listOf(defaultBottomSegment))
            assertFailsWith<PlanningException> {
                GasPlanner().calculateGasPlan(divePlan)
            }
        }
    }

    @Test
    fun calculateGasPlan_decoRateAppliesToDecoStopOnly() {
        val configuration = Configuration(sacRate = defaultSacRate, sacRateDeco = defaultSacRateDeco)
        val gasSwitchSegment = segment(start = defaultBottomSegment.end, duration = 2, type = DiveSegment.Type.GAS_SWITCH, startPressure = 2.0)
        val decoStopSegment = segment(start = gasSwitchSegment.end, duration = 3, type = DiveSegment.Type.DECO_STOP, startPressure = 2.0)
        val divePlan = ocDivePlan(
            configuration = configuration,
            segments = listOf(defaultBottomSegment, gasSwitchSegment, decoStopSegment),
        )

        val gasPlan = GasPlanner().calculateGasPlan(divePlan)

        val bottomUsage = defaultBottomSegment.duration * defaultSacRate * 3.0
        val gasSwitchUsage = gasSwitchSegment.duration * defaultSacRate * 2.0
        val decoStopUsage = decoStopSegment.duration * defaultSacRateDeco * 2.0
        assertEquals(bottomUsage + gasSwitchUsage + decoStopUsage, gasPlan[0].normalRequirement, 0.001)
        assertEquals(0.0, gasPlan[0].extraEmergencyRequirement, 0.001)
    }

    @Test
    fun calculateGasPlan_stressWindowSplitsSegmentAtBoundary() {
        val configuration = Configuration(sacRate = defaultSacRate, sacRateStress = defaultSacRateStress, stressDurationMinutes = 4)
        val divePlan = ocDivePlan(
            configuration = configuration,
            ascent = listOf(segment(start = defaultBottomSegment.end, duration = 10, type = DiveSegment.Type.ASCENT, startPressure = 4.0, endPressure = 1.0)),
        )

        val gasPlan = GasPlanner().calculateGasPlan(divePlan)

        val stressRate = 2 * defaultSacRateStress - defaultSacRate
        val boundaryPressure = 4.0 - (4.0 - 1.0) / 10 * 4
        val stressedPart = 4 * stressRate * (4.0 + boundaryPressure) / 2
        val calmPart = 6 * defaultSacRate * (boundaryPressure + 1.0) / 2
        assertEquals(stressedPart + calmPart, gasPlan[0].extraEmergencyRequirement, 0.001)
    }

    @Test
    fun calculateGasPlan_stressWindowExpiringExactlyOnSegmentBoundary() {
        val configuration = Configuration(sacRate = defaultSacRate, sacRateStress = defaultSacRateStress, stressDurationMinutes = 5)
        val firstAscentSegment = segment(start = defaultBottomSegment.end, duration = 5, type = DiveSegment.Type.ASCENT, startPressure = 4.0, endPressure = 3.0)
        val secondAscentSegment = segment(start = firstAscentSegment.end, duration = 5, type = DiveSegment.Type.ASCENT, startPressure = 3.0, endPressure = 1.0)
        val divePlan = ocDivePlan(
            configuration = configuration,
            ascent = listOf(firstAscentSegment, secondAscentSegment),
        )

        val gasPlan = GasPlanner().calculateGasPlan(divePlan)

        val stressRate = 2 * defaultSacRateStress - defaultSacRate
        val firstSegmentStressed = 5 * stressRate * (4.0 + 3.0) / 2
        val secondSegmentCalm = 5 * defaultSacRate * (3.0 + 1.0) / 2
        assertEquals(firstSegmentStressed + secondSegmentCalm, gasPlan[0].extraEmergencyRequirement, 0.001)
    }

    @Test
    fun calculateGasPlan_stressWindowLongerThanWholeAscent() {
        val configuration = Configuration(sacRate = defaultSacRate, sacRateStress = defaultSacRateStress, stressDurationMinutes = 60)
        val divePlan = ocDivePlan(
            configuration = configuration,
            ascent = listOf(segment(start = defaultBottomSegment.end, duration = 5, type = DiveSegment.Type.ASCENT, startPressure = 4.0, endPressure = 1.0)),
        )

        val gasPlan = GasPlanner().calculateGasPlan(divePlan)

        val stressRate = 2 * defaultSacRateStress - defaultSacRate
        assertEquals(5 * stressRate * (4.0 + 1.0) / 2, gasPlan[0].extraEmergencyRequirement, 0.001)
    }

    @Test
    fun calculateGasPlan_stressWindowOfZeroAppliesNormalAndDecoRatesThroughout() {
        val configuration = Configuration(
            sacRate = defaultSacRate,
            sacRateDeco = defaultSacRateDeco,
            sacRateStress = defaultSacRateStress,
            stressDurationMinutes = 0,
        )
        val ascentSegment = segment(start = defaultBottomSegment.end, duration = 5, type = DiveSegment.Type.ASCENT, startPressure = 4.0, endPressure = 3.0)
        val decoStopSegment = segment(start = ascentSegment.end, duration = 5, type = DiveSegment.Type.DECO_STOP, startPressure = 3.0)
        val divePlan = ocDivePlan(
            configuration = configuration,
            ascent = listOf(ascentSegment, decoStopSegment),
        )

        val gasPlan = GasPlanner().calculateGasPlan(divePlan)

        val ascentUsage = 5 * defaultSacRate * (4.0 + 3.0) / 2
        val decoStopUsage = 5 * defaultSacRateDeco * 3.0
        assertEquals(ascentUsage + decoStopUsage, gasPlan[0].extraEmergencyRequirement, 0.001)
    }

    @Test
    fun calculateGasPlan_ccrBailoutStressOriginIsFirstOpenCircuitSegmentNotAscentStart() {
        val configuration = Configuration(sacRate = defaultSacRate, sacRateStress = defaultSacRateStress, stressDurationMinutes = 3)
        // Problem-solving time on the loop: still closed-circuit, so it does not consume the
        // stress window even though it is part of the ascent.
        val problemSolvingSegment = segment(start = defaultCcrBottomSegment.end, duration = 3, type = DiveSegment.Type.FLAT, startPressure = 3.0, cylinder = diluentCylinder)
            .copy(breathingMode = BreathingMode.ccr(1.2))
        val bailoutSegment = segment(start = problemSolvingSegment.end, duration = 5, type = DiveSegment.Type.ASCENT, startPressure = 3.0, endPressure = 1.0, cylinder = bailoutCylinder)
        val divePlan = ccrDivePlan(configuration = configuration, ascent = listOf(problemSolvingSegment, bailoutSegment))

        val gasPlan = GasPlanner().calculateGasPlan(divePlan)
        val bailoutEntry = gasPlan.first { it.cylinder == bailoutCylinder }

        // CCR uses sacRateStress directly, not the two-diver OC formula. The timeout starts when
        // the bailout segment starts (T=13), not when the ascent starts (T=10).
        val boundaryPressure = 3.0 - (3.0 - 1.0) / 5 * 3
        val stressedPart = 3 * defaultSacRateStress * (3.0 + boundaryPressure) / 2
        val calmPart = 2 * defaultSacRate * (boundaryPressure + 1.0) / 2
        assertEquals(stressedPart + calmPart, bailoutEntry.extraEmergencyRequirement, 0.001)
    }

    @Test
    fun calculateGasPlan_ccrBailoutReserveIncreasesRelativeToSacRateOnlyBaseline() {
        val configuration = Configuration(sacRate = defaultSacRate, sacRateStress = defaultSacRateStress, stressDurationMinutes = 60)
        val divePlan = ccrDivePlan(
            configuration = configuration,
            ascent = listOf(segment(start = defaultCcrBottomSegment.end, duration = 5, type = DiveSegment.Type.ASCENT, startPressure = 4.0, endPressure = 1.0, cylinder = bailoutCylinder)),
        )

        val gasPlan = GasPlanner().calculateGasPlan(divePlan)
        val bailoutEntry = gasPlan.first { it.cylinder == bailoutCylinder }

        // Bailout is charged at sacRateStress directly, not the two-diver OC formula.
        val actualReserve = 5 * defaultSacRateStress * (4.0 + 1.0) / 2
        assertEquals(actualReserve, bailoutEntry.extraEmergencyRequirement, 0.001)

        val plainSacRateBaseline = 5 * defaultSacRate * (4.0 + 1.0) / 2
        assertTrue(actualReserve > plainSacRateBaseline)
    }

    private fun ocDivePlan(
        configuration: Configuration,
        ascent: List<DiveSegment>,
        bottomSegment: DiveSegment = defaultBottomSegment,
    ) = ocDivePlan(configuration, listOf(bottomSegment), mapOf(bottomSegment.end to ascent))

    private fun ocDivePlan(
        configuration: Configuration,
        segments: List<DiveSegment>,
        // Add empty alternative ascents for segments that have a TTS but no alternative ascent
        alternativeAscents: Map<Int, List<DiveSegment>> = segments
            .filter { it.ttsAfter != null }
            .associate { it.end to emptyList() },
    ): DivePlan {
        val cylinders = (segments + alternativeAscents.values.flatten())
            .map { it.cylinder }
            .distinct()
            .map { it.assign() }
        val alternativeAccents: ImmutableMap<Int, ImmutableList<DiveSegment>> = persistentMapOf<Int, ImmutableList<DiveSegment>>()
            .putAll(alternativeAscents.mapValues { it.value.toImmutableList() })
        return DivePlan(
            segments = segments.toImmutableList(),
            alternativeAccents = alternativeAccents,
            cylinders = cylinders.toImmutableList(),
            configuration = configuration,
            totalCns = 0.0,
            totalOtu = 0.0,
        )
    }

    private fun ccrDivePlan(
        configuration: Configuration,
        ascent: List<DiveSegment>,
    ) = DivePlan(
        segments = listOf(defaultCcrBottomSegment).toImmutableList(),
        alternativeAccents = persistentMapOf(defaultCcrBottomSegment.end to ascent.toImmutableList()),
        cylinders = listOf(
            diluentCylinder.assign(CylinderRole.CCR_DILUENT),
            bailoutCylinder.assign(),
        ).toImmutableList(),
        configuration = configuration,
        totalCns = 0.0,
        totalOtu = 0.0,
    )

    private fun segment(
        start: Int,
        duration: Int,
        type: DiveSegment.Type,
        startPressure: Double,
        endPressure: Double = startPressure,
        cylinder: Cylinder = bottomGas,
        ttsAfter: Int? = null,
    ) = DiveSegment(
        start = start,
        duration = duration,
        startPressure = startPressure,
        endPressure = endPressure,
        startDepth = startPressure,
        endDepth = endPressure,
        cylinder = cylinder,
        gfCeilingAtEnd = 0.0,
        type = type,
        ttsAfter = ttsAfter,
    )
}
