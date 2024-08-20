/*
 * Abysner - Dive planner
 * Copyright (C) 2024 Neotech
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
    fun referencePlan1() {
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

        val divePlan = divePlanner.getDecoPlan(plannedSections, emptyList())
        val plan = divePlan.segmentsCollapsed

        // println(divePlan.toString(compact = false))

        assertEquals(2.731, divePlan.totalCns, tenthAtDecimalPoint(3))
        assertEquals(5.443, divePlan.totalOtu, tenthAtDecimalPoint(3))

        plan.assertSegment(0, DiveSegment.Type.DECENT, isDecompression = false, startDepth = 0.0,  endDepth = 20.0, duration = 4,  gas = bottomGas)
        plan.assertSegment(1, DiveSegment.Type.FLAT,   isDecompression = false, startDepth = 20.0, endDepth = 20.0, duration = 16, gas = bottomGas)
        plan.assertSegment(2, DiveSegment.Type.ASCENT, isDecompression = true,  startDepth = 20.0, endDepth = 0.0,  duration = 4,  gas = bottomGas)
    }

    @Test
    fun referencePlan2() {
        val bottomGas = Cylinder.steel12Liter(Gas.Air)
        val decoGas = Cylinder.aluminium80Cuft(Gas.Oxygen50)
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
            lastDecoStopDepth = 6
        )

        val plannedSections = listOf(
            DiveProfileSection(duration = 30, 30, bottomGas)
        )

        val divePlan = divePlanner.getDecoPlan(plannedSections, listOf(decoGas))
        val plan = divePlan.segmentsCollapsed

        // println(divePlan.toString(compact = false))

        assertEquals(11.656, divePlan.totalCns, tenthAtDecimalPoint(3))
        assertEquals(34.642, divePlan.totalOtu, tenthAtDecimalPoint(3))

        plan.assertSegment(0, DiveSegment.Type.DECENT, isDecompression = false, startDepth = 0.0,  endDepth = 30.0, duration = 6,  gas = bottomGas)
        plan.assertSegment(1, DiveSegment.Type.FLAT,   isDecompression = false, startDepth = 30.0, endDepth = 30.0, duration = 24, gas = bottomGas)
        plan.assertSegment(2, DiveSegment.Type.ASCENT, isDecompression = true,  startDepth = 30.0, endDepth = 21.0, duration = 2,  gas = bottomGas)
        plan.assertSegment(3, DiveSegment.Type.ASCENT, isDecompression = true,  startDepth = 21.0, endDepth = 9.0,  duration = 3,  gas = decoGas)
        plan.assertSegment(4, DiveSegment.Type.FLAT,   isDecompression = true,  startDepth = 9.0,  endDepth = 9.0,  duration = 2,  gas = decoGas)
        plan.assertSegment(5, DiveSegment.Type.ASCENT, isDecompression = true,  startDepth = 9.0,  endDepth = 6.0,  duration = 1,  gas = decoGas)
        plan.assertSegment(6, DiveSegment.Type.FLAT,   isDecompression = true,  startDepth = 6.0,  endDepth = 6.0,  duration = 10, gas = decoGas)
        plan.assertSegment(7, DiveSegment.Type.ASCENT, isDecompression = true,  startDepth = 6.0,  endDepth = 0.0,  duration = 2,  gas = decoGas)
    }

    @Test
    fun referencePlan3() {
        val bottomGas = Cylinder.steel12Liter(Gas.Trimix2135)
        val decoGas = Cylinder.aluminium80Cuft(Gas.Oxygen50)
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
            lastDecoStopDepth = 3
        )

        val plannedSections = listOf(DiveProfileSection(duration = 15, 45, bottomGas))

        val divePlan = divePlanner.getDecoPlan(plannedSections, listOf(decoGas))
        val plan = divePlan.segmentsCollapsed

        // println(divePlan.toString(compact = false))

        assertEquals(8.669, divePlan.totalCns, tenthAtDecimalPoint(3))
        assertEquals(24.399, divePlan.totalOtu, tenthAtDecimalPoint(3))

        plan.assertSegment(0, DiveSegment.Type.DECENT, isDecompression = false, startDepth = 0.0,  endDepth = 45.0, duration = 9, gas = bottomGas)
        plan.assertSegment(1, DiveSegment.Type.FLAT,   isDecompression = false, startDepth = 45.0, endDepth = 45.0, duration = 6, gas = bottomGas)
        plan.assertSegment(2, DiveSegment.Type.ASCENT, isDecompression = true,  startDepth = 45.0, endDepth = 21.0, duration = 5, gas = bottomGas)
        plan.assertSegment(3, DiveSegment.Type.ASCENT, isDecompression = true,  startDepth = 21.0, endDepth = 6.0,  duration = 3, gas = decoGas)
        plan.assertSegment(4, DiveSegment.Type.FLAT,   isDecompression = true,  startDepth = 6.0,  endDepth = 6.0,  duration = 3, gas = decoGas)
        plan.assertSegment(5, DiveSegment.Type.ASCENT, isDecompression = true,  startDepth = 6.0,  endDepth = 3.0,  duration = 1, gas = decoGas)
        plan.assertSegment(6, DiveSegment.Type.FLAT,   isDecompression = true,  startDepth = 3.0,  endDepth = 3.0,  duration = 4, gas = decoGas)
        plan.assertSegment(7, DiveSegment.Type.ASCENT, isDecompression = true,  startDepth = 3.0,  endDepth = 0.0,  duration = 1, gas = decoGas)
    }

    @Test
    fun referencePlan4() {
        val bottomGas = Cylinder.steel12Liter(Gas.Trimix1845)
        val decoGas = Cylinder.aluminium80Cuft(Gas.Oxygen50)
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
            lastDecoStopDepth = 3
        )

        val plannedSections = listOf(DiveProfileSection(duration = 20, 60, bottomGas))

        val divePlan = divePlanner.getDecoPlan(plannedSections, listOf(decoGas))
        val plan = divePlan.segmentsCollapsed

        // println(divePlan.toString(compact = false))

        assertEquals(15.891, divePlan.totalCns, tenthAtDecimalPoint(3))
        assertEquals(42.280, divePlan.totalOtu, tenthAtDecimalPoint(3))

        plan.assertSegment(0,  DiveSegment.Type.DECENT, isDecompression = false, startDepth = 0.0,  endDepth = 60.0, duration = 12, gas = bottomGas)
        plan.assertSegment(1,  DiveSegment.Type.FLAT,   isDecompression = false, startDepth = 60.0, endDepth = 60.0, duration = 8,  gas = bottomGas)
        plan.assertSegment(2,  DiveSegment.Type.ASCENT, isDecompression = true,  startDepth = 60.0, endDepth = 21.0, duration = 8,  gas = bottomGas)
        plan.assertSegment(3,  DiveSegment.Type.FLAT,   isDecompression = true,  startDepth = 21.0, endDepth = 21.0, duration = 1,  gas = decoGas)
        plan.assertSegment(4,  DiveSegment.Type.ASCENT, isDecompression = true,  startDepth = 21.0, endDepth = 18.0, duration = 1,  gas = decoGas)
        plan.assertSegment(5,  DiveSegment.Type.FLAT,   isDecompression = true,  startDepth = 18.0, endDepth = 18.0, duration = 1,  gas = decoGas)
        plan.assertSegment(6,  DiveSegment.Type.ASCENT, isDecompression = true,  startDepth = 18.0, endDepth = 15.0, duration = 1,  gas = decoGas)
        plan.assertSegment(7,  DiveSegment.Type.FLAT,   isDecompression = true,  startDepth = 15.0, endDepth = 15.0, duration = 1,  gas = decoGas)
        plan.assertSegment(8,  DiveSegment.Type.ASCENT, isDecompression = true,  startDepth = 15.0, endDepth = 12.0, duration = 1,  gas = decoGas)
        plan.assertSegment(9,  DiveSegment.Type.FLAT,   isDecompression = true,  startDepth = 12.0, endDepth = 12.0, duration = 1,  gas = decoGas)
        plan.assertSegment(10, DiveSegment.Type.ASCENT, isDecompression = true,  startDepth = 12.0, endDepth = 9.0,  duration = 1,  gas = decoGas)
        plan.assertSegment(11, DiveSegment.Type.FLAT,   isDecompression = true,  startDepth = 9.0,  endDepth = 9.0,  duration = 2,  gas = decoGas)
        plan.assertSegment(12, DiveSegment.Type.ASCENT, isDecompression = true,  startDepth = 9.0,  endDepth = 6.0,  duration = 1,  gas = decoGas)
        plan.assertSegment(13, DiveSegment.Type.FLAT,   isDecompression = true,  startDepth = 6.0,  endDepth = 6.0,  duration = 5,  gas = decoGas)
        plan.assertSegment(14, DiveSegment.Type.ASCENT, isDecompression = true,  startDepth = 6.0,  endDepth = 3.0,  duration = 1,  gas = decoGas)
        plan.assertSegment(15, DiveSegment.Type.FLAT,   isDecompression = true, startDepth = 3.0,   endDepth = 3.0,  duration = 11, gas = decoGas)
        plan.assertSegment(16, DiveSegment.Type.ASCENT, isDecompression = true, startDepth = 3.0,   endDepth = 0.0,  duration = 1,  gas = decoGas)
    }

    @Test
    fun referencePlan5() {
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

        val divePlan = divePlanner.getDecoPlan(plannedSections, emptyList())
        val plan = divePlan.segmentsCollapsed

        // println(divePlan.toString(compact = false))

        assertEquals(8.479, divePlan.totalCns, tenthAtDecimalPoint(3))
        assertEquals(25.542, divePlan.totalOtu, tenthAtDecimalPoint(3))

        plan.assertSegment(0,  DiveSegment.Type.DECENT, isDecompression = false, startDepth = 0.0,  endDepth = 40.0, duration = 8,  gas = bottomGas)
        plan.assertSegment(1,  DiveSegment.Type.FLAT,   isDecompression = false, startDepth = 40.0, endDepth = 40.0, duration = 2,  gas = bottomGas)
        plan.assertSegment(2,  DiveSegment.Type.ASCENT, isDecompression = true,  startDepth = 40.0, endDepth = 30.0, duration = 2,  gas = bottomGas)
        plan.assertSegment(3,  DiveSegment.Type.FLAT,   isDecompression = false, startDepth = 30.0, endDepth = 30.0, duration = 16, gas = bottomGas)
        plan.assertSegment(4,  DiveSegment.Type.DECENT, isDecompression = false, startDepth = 30.0, endDepth = 40.0, duration = 2,  gas = bottomGas)
        plan.assertSegment(5,  DiveSegment.Type.FLAT,   isDecompression = false, startDepth = 40.0, endDepth = 40.0, duration = 2,  gas = bottomGas)
        plan.assertSegment(6,  DiveSegment.Type.ASCENT, isDecompression = true,  startDepth = 40.0, endDepth = 9.0,  duration = 7,  gas = bottomGas)
        plan.assertSegment(7,  DiveSegment.Type.FLAT,   isDecompression = true,  startDepth = 9.0,  endDepth = 9.0,  duration = 3,  gas = bottomGas)
        plan.assertSegment(8,  DiveSegment.Type.ASCENT, isDecompression = true,  startDepth = 9.0,  endDepth = 6.0,  duration = 1,  gas = bottomGas)
        plan.assertSegment(9,  DiveSegment.Type.FLAT,   isDecompression = true,  startDepth = 6.0,  endDepth = 6.0,  duration = 6,  gas = bottomGas)
        plan.assertSegment(10, DiveSegment.Type.ASCENT, isDecompression = true,  startDepth = 6.0,  endDepth = 3.0,  duration = 1,  gas = bottomGas)
        plan.assertSegment(11,  DiveSegment.Type.FLAT,  isDecompression = true,  startDepth = 3.0,  endDepth = 3.0,  duration = 15, gas = bottomGas)
        plan.assertSegment(12, DiveSegment.Type.ASCENT, isDecompression = true,  startDepth = 3.0,  endDepth = 0.0,  duration = 1,  gas = bottomGas)
    }
}

fun List<DiveSegment>.assertSegment(index: Int, type: DiveSegment.Type, isDecompression: Boolean, startDepth: Double, endDepth: Double, duration: Int, gas: Cylinder) {
    val actual = this[index]
    assertEquals(type, actual.type)
    assertEquals(isDecompression, actual.isDecompression)
    assertEquals(startDepth, actual.startDepth)
    assertEquals(endDepth, actual.endDepth)
    assertEquals(duration, actual.duration)
    assertEquals(gas, actual.cylinder)
}
