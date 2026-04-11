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

package org.neotech.app.abysner.domain.diveplanning


import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Configuration.Algorithm
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.model.Salinity
import org.neotech.app.abysner.domain.decompression.model.DiveSegment
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.tenthAtDecimalPoint
import kotlin.test.Test
import kotlin.test.assertEquals

class DivePlannerTest {

    @Test
    fun referencePlan1_producesExpectedSegments() {
        val bottomGas = Cylinder.steel12Liter(Gas.Air)
        val divePlanner = DivePlanner()
        divePlanner.configuration = Configuration(
            maxAscentRate = 5.0,
            maxDescentRate = 5.0,
            gfLow = 0.3, gfHigh = 0.7,
            salinity = Salinity.WATER_FRESH,
            algorithm = Algorithm.BUHLMANN_ZH16C,
            altitude = 0.0,
            decoStepSize = 3,
            lastDecoStopDepth = 3
        )

        val plannedSections = listOf(DiveProfileSection(duration = 20, 20, bottomGas))

        val divePlan = divePlanner.addDive(plannedSections, emptyList())
        val plan = divePlan.segmentsCollapsed

        // println(divePlan.toString(compact = false))

        assertEquals(2.731, divePlan.totalCns, tenthAtDecimalPoint(3))
        assertEquals(5.443, divePlan.totalOtu, tenthAtDecimalPoint(3))

        plan.assertSegment(0, DiveSegment.Type.DECENT, startDepth = 0.0,  endDepth = 20.0, duration = 4,  gas = bottomGas)
        plan.assertSegment(1, DiveSegment.Type.FLAT,   startDepth = 20.0, endDepth = 20.0, duration = 16, gas = bottomGas)
        plan.assertSegment(2, DiveSegment.Type.ASCENT, startDepth = 20.0, endDepth = 0.0,  duration = 4,  gas = bottomGas)
    }

    @Test
    fun referencePlan2_producesExpectedSegments() {
        val bottomGas = Cylinder.steel12Liter(Gas.Air)
        val decoGas = Cylinder.aluminium80Cuft(Gas.Nitrox50)
        val divePlanner = DivePlanner()
        divePlanner.configuration = Configuration(
            maxAscentRate = 5.0,
            maxDescentRate = 5.0,
            gfLow = 0.3,
            gfHigh = 0.7,
            salinity = Salinity.WATER_SALT,
            algorithm = Algorithm.BUHLMANN_ZH16C,
            altitude = 0.0,
            decoStepSize = 3,
            lastDecoStopDepth = 6,
            gasSwitchTime = 1
        )

        val plannedSections = listOf(
            DiveProfileSection(duration = 30, 30, bottomGas)
        )

        val divePlan = divePlanner.addDive(plannedSections, listOf(bottomGas, decoGas))
        val plan = divePlan.segmentsCollapsed

        // println(divePlan.toString(compact = false))

        assertEquals(11.526, divePlan.totalCns, tenthAtDecimalPoint(3))
        assertEquals(34.091, divePlan.totalOtu, tenthAtDecimalPoint(3))

        plan.assertSegment(0, DiveSegment.Type.DECENT,     startDepth = 0.0,  endDepth = 30.0, duration = 6,  gas = bottomGas)
        plan.assertSegment(1, DiveSegment.Type.FLAT,       startDepth = 30.0, endDepth = 30.0, duration = 24, gas = bottomGas)
        plan.assertSegment(2, DiveSegment.Type.ASCENT,     startDepth = 30.0, endDepth = 21.0, duration = 2,  gas = bottomGas)
        plan.assertSegment(3, DiveSegment.Type.GAS_SWITCH, startDepth = 21.0, endDepth = 21.0, duration = 1,  gas = bottomGas)
        plan.assertSegment(4, DiveSegment.Type.ASCENT,     startDepth = 21.0, endDepth = 9.0,  duration = 3,  gas = decoGas)
        plan.assertSegment(5, DiveSegment.Type.DECO_STOP,  startDepth = 9.0,  endDepth = 9.0,  duration = 1,  gas = decoGas)
        plan.assertSegment(6, DiveSegment.Type.ASCENT,     startDepth = 9.0,  endDepth = 6.0,  duration = 1,  gas = decoGas)
        plan.assertSegment(7, DiveSegment.Type.DECO_STOP,  startDepth = 6.0,  endDepth = 6.0,  duration = 10, gas = decoGas)
        plan.assertSegment(8, DiveSegment.Type.ASCENT,     startDepth = 6.0,  endDepth = 0.0,  duration = 2,  gas = decoGas)
    }

    @Test
    fun referencePlan3_producesExpectedSegments() {
        val bottomGas = Cylinder.steel12Liter(Gas.Trimix2135)
        val decoGas = Cylinder.aluminium80Cuft(Gas.Nitrox50)
        val divePlanner = DivePlanner()
        divePlanner.configuration = Configuration(
            maxAscentRate = 5.0,
            maxDescentRate = 5.0,
            gfLow = 0.3,
            gfHigh = 0.7,
            salinity = Salinity.WATER_SALT,
            algorithm = Algorithm.BUHLMANN_ZH16C,
            altitude = 0.0,
            decoStepSize = 3,
            lastDecoStopDepth = 3,
            gasSwitchTime = 1
        )

        val plannedSections = listOf(DiveProfileSection(duration = 15, 45, bottomGas))

        val divePlan = divePlanner.addDive(plannedSections, listOf(decoGas))
        val plan = divePlan.segmentsCollapsed

        // println(divePlan.toString(compact = false))

        assertEquals(8.614, divePlan.totalCns, tenthAtDecimalPoint(3))
        assertEquals(24.112, divePlan.totalOtu, tenthAtDecimalPoint(3))

        plan.assertSegment(0, DiveSegment.Type.DECENT,     startDepth = 0.0,  endDepth = 45.0, duration = 9, gas = bottomGas)
        plan.assertSegment(1, DiveSegment.Type.FLAT,       startDepth = 45.0, endDepth = 45.0, duration = 6, gas = bottomGas)
        plan.assertSegment(2, DiveSegment.Type.ASCENT,     startDepth = 45.0, endDepth = 21.0, duration = 5, gas = bottomGas)
        plan.assertSegment(3, DiveSegment.Type.GAS_SWITCH, startDepth = 21.0, endDepth = 21.0, duration = 1, gas = bottomGas)
        plan.assertSegment(4, DiveSegment.Type.ASCENT,     startDepth = 21.0, endDepth = 6.0,  duration = 3, gas = decoGas)
        plan.assertSegment(5, DiveSegment.Type.DECO_STOP,  startDepth = 6.0,  endDepth = 6.0,  duration = 2, gas = decoGas)
        plan.assertSegment(6, DiveSegment.Type.ASCENT,     startDepth = 6.0,  endDepth = 3.0,  duration = 1, gas = decoGas)
        plan.assertSegment(7, DiveSegment.Type.DECO_STOP,  startDepth = 3.0,  endDepth = 3.0,  duration = 4, gas = decoGas)
        plan.assertSegment(8, DiveSegment.Type.ASCENT,     startDepth = 3.0,  endDepth = 0.0,  duration = 1, gas = decoGas)
    }

    @Test
    fun referencePlan4_producesExpectedSegments() {
        val bottomGas = Cylinder.steel12Liter(Gas.Trimix1845)
        val decoGas = Cylinder.aluminium80Cuft(Gas.Nitrox50)
        val divePlanner = DivePlanner()
        divePlanner.configuration = Configuration(
            maxAscentRate = 5.0,
            maxDescentRate = 5.0,
            gfLow = 0.4,
            gfHigh = 0.85,
            salinity = Salinity.WATER_FRESH,
            algorithm = Algorithm.BUHLMANN_ZH16C,
            altitude = 1000.0,
            decoStepSize = 3,
            lastDecoStopDepth = 3,
            gasSwitchTime = 1
        )

        val plannedSections = listOf(DiveProfileSection(duration = 20, 60, bottomGas))

        val divePlan = divePlanner.addDive(plannedSections, listOf(decoGas))
        val plan = divePlan.segmentsCollapsed

        // println(divePlan.toString(compact = false))

        assertEquals(14.867, divePlan.totalCns, tenthAtDecimalPoint(3))
        assertEquals(39.917, divePlan.totalOtu, tenthAtDecimalPoint(3))

        plan.assertSegment(0,  DiveSegment.Type.DECENT,     startDepth = 0.0,  endDepth = 60.0, duration = 12, gas = bottomGas)
        plan.assertSegment(1,  DiveSegment.Type.FLAT,       startDepth = 60.0, endDepth = 60.0, duration = 8,  gas = bottomGas)
        plan.assertSegment(2,  DiveSegment.Type.ASCENT,     startDepth = 60.0, endDepth = 21.0, duration = 8,  gas = bottomGas)
        plan.assertSegment(3,  DiveSegment.Type.GAS_SWITCH, startDepth = 21.0, endDepth = 21.0, duration = 1,  gas = bottomGas)
        plan.assertSegment(4,  DiveSegment.Type.ASCENT,     startDepth = 21.0, endDepth = 15.0, duration = 2,  gas = decoGas)
        plan.assertSegment(5,  DiveSegment.Type.DECO_STOP,  startDepth = 15.0, endDepth = 15.0, duration = 1,  gas = decoGas)
        plan.assertSegment(6,  DiveSegment.Type.ASCENT,     startDepth = 15.0, endDepth = 12.0, duration = 1,  gas = decoGas)
        plan.assertSegment(7,  DiveSegment.Type.DECO_STOP,  startDepth = 12.0, endDepth = 12.0, duration = 1,  gas = decoGas)
        plan.assertSegment(8,  DiveSegment.Type.ASCENT,     startDepth = 12.0, endDepth = 9.0,  duration = 1,  gas = decoGas)
        plan.assertSegment(9,  DiveSegment.Type.DECO_STOP,  startDepth = 9.0,  endDepth = 9.0,  duration = 3,  gas = decoGas)
        plan.assertSegment(10, DiveSegment.Type.ASCENT,     startDepth = 9.0,  endDepth = 6.0,  duration = 1,  gas = decoGas)
        plan.assertSegment(11, DiveSegment.Type.DECO_STOP,  startDepth = 6.0,  endDepth = 6.0,  duration = 5,  gas = decoGas)
        plan.assertSegment(12, DiveSegment.Type.ASCENT,     startDepth = 6.0,  endDepth = 3.0,  duration = 1,  gas = decoGas)
        plan.assertSegment(13, DiveSegment.Type.DECO_STOP,  startDepth = 3.0,  endDepth = 3.0,  duration = 11, gas = decoGas)
        plan.assertSegment(14, DiveSegment.Type.ASCENT,     startDepth = 3.0,  endDepth = 0.0,  duration = 1,  gas = decoGas)
    }

    @Test
    fun referencePlan5_producesExpectedSegments() {
        val bottomGas = Cylinder.steel12Liter(Gas(0.21, 0.20))
        val divePlanner = DivePlanner()
        divePlanner.configuration = Configuration(
            maxAscentRate = 5.0,
            maxDescentRate = 5.0,
            gfLow = 0.50,
            gfHigh = 0.80,
            salinity = Salinity.WATER_FRESH,
            algorithm = Algorithm.BUHLMANN_ZH16C,
            altitude = 0.0,
            decoStepSize = 3,
            lastDecoStopDepth = 3
        )

        val plannedSections = listOf(
            DiveProfileSection(duration = 10, 40, bottomGas),
            DiveProfileSection(duration = 10, 30, bottomGas),
            DiveProfileSection(duration = 8, 30, bottomGas),
            DiveProfileSection(duration = 4, 40, bottomGas)
        )

        val divePlan = divePlanner.addDive(plannedSections, emptyList())
        val plan = divePlan.segmentsCollapsed

        // println(divePlan.toString(compact = false))

        assertEquals(8.480, divePlan.totalCns, tenthAtDecimalPoint(3))
        assertEquals(25.542, divePlan.totalOtu, tenthAtDecimalPoint(3))

        plan.assertSegment(0,  DiveSegment.Type.DECENT,    startDepth = 0.0,  endDepth = 40.0, duration = 8,  gas = bottomGas)
        plan.assertSegment(1,  DiveSegment.Type.FLAT,      startDepth = 40.0, endDepth = 40.0, duration = 2,  gas = bottomGas)
        plan.assertSegment(2,  DiveSegment.Type.ASCENT,    startDepth = 40.0, endDepth = 30.0, duration = 2,  gas = bottomGas)
        plan.assertSegment(3,  DiveSegment.Type.FLAT,      startDepth = 30.0, endDepth = 30.0, duration = 16, gas = bottomGas)
        plan.assertSegment(4,  DiveSegment.Type.DECENT,    startDepth = 30.0, endDepth = 40.0, duration = 2,  gas = bottomGas)
        plan.assertSegment(5,  DiveSegment.Type.FLAT,      startDepth = 40.0, endDepth = 40.0, duration = 2,  gas = bottomGas)
        plan.assertSegment(6,  DiveSegment.Type.ASCENT,    startDepth = 40.0, endDepth = 9.0,  duration = 7,  gas = bottomGas)
        plan.assertSegment(7,  DiveSegment.Type.DECO_STOP, startDepth = 9.0,  endDepth = 9.0,  duration = 3,  gas = bottomGas)
        plan.assertSegment(8,  DiveSegment.Type.ASCENT,    startDepth = 9.0,  endDepth = 6.0,  duration = 1,  gas = bottomGas)
        plan.assertSegment(9,  DiveSegment.Type.DECO_STOP, startDepth = 6.0,  endDepth = 6.0,  duration = 5,  gas = bottomGas)
        plan.assertSegment(10, DiveSegment.Type.ASCENT,    startDepth = 6.0,  endDepth = 3.0,  duration = 1,  gas = bottomGas)
        plan.assertSegment(11, DiveSegment.Type.DECO_STOP, startDepth = 3.0,  endDepth = 3.0,  duration = 14, gas = bottomGas)
        plan.assertSegment(12, DiveSegment.Type.ASCENT,    startDepth = 3.0,  endDepth = 0.0,  duration = 1,  gas = bottomGas)
    }

    @Test
    fun gasSwitchBetweenIdenticalGasMixes_doesNotOccur() {
        val cylinder1 = Cylinder.steel12Liter(Gas.Air)
        val cylinder2 = Cylinder.steel12Liter(Gas.Air)

        val planner = DivePlanner(
            Configuration(
                maxAscentRate = 5.0,
                maxDescentRate = 5.0,
                gfLow = 0.3, gfHigh = 0.7,
                salinity = Salinity.WATER_FRESH,
                algorithm = Algorithm.BUHLMANN_ZH16C,
                decoStepSize = 3,
                lastDecoStopDepth = 3,
                gasSwitchTime = 1,
            )
        )

        // Both cylinders are in the list: the user-planned segment uses cylinder2, the
        // decompression planner will have both cylinders available, but with cylinder1 first in the
        // list. A switch to that first cylinder should not occur, since the gas mixes are the same.
        val divePlan = planner.addDive(
            plan = listOf(DiveProfileSection(duration = 20, depth = 20, cylinder = cylinder2)),
            cylinders = listOf(cylinder1, cylinder2),
        )

        val gasSwitchSegments = divePlan.segmentsCollapsed.filter { it.type == DiveSegment.Type.GAS_SWITCH }
        assertEquals(0, gasSwitchSegments.size, "Expected no GAS_SWITCH between identical gas mixes, found switch(es) at: ${gasSwitchSegments.map { "${it.endDepth}m" }}")
    }
}

fun List<DiveSegment>.assertSegment(
    index: Int,
    type: DiveSegment.Type,
    startDepth: Double,
    endDepth: Double,
    duration: Int,
    gas: Cylinder
) {
    val actual = this[index]
    assertEquals(type, actual.type, "Segment $index: type mismatch")
    assertEquals(startDepth, actual.startDepth, "Segment $index: startDepth mismatch")
    assertEquals(endDepth, actual.endDepth, "Segment $index: endDepth mismatch")
    assertEquals(duration, actual.duration, "Segment $index: duration mismatch")
    assertEquals(gas, actual.cylinder, "Segment $index: gas mismatch")
}
