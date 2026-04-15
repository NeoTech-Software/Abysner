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
import org.neotech.app.abysner.domain.diveplanning.model.DivePlan
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.diveplanning.model.assign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GasSwitchTimeTest {

    private val bottomGas = Cylinder.steel12Liter(Gas.Air)
    private val decoGas = Cylinder.aluminium80Cuft(Gas.Nitrox50)

    /**
     * Based on [DivePlannerTest.referencePlan2] but parameterized by [gasSwitchTime].
     */
    private fun divePlan(gasSwitchTime: Int): DivePlan {
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
            gasSwitchTime = gasSwitchTime
        )
        return divePlanner.addDive(
            listOf(DiveProfileSection(duration = 30, 30, bottomGas)),
            listOf(decoGas).assign()
        )
    }

    @Test
    fun gasSwitchTime_addsSegmentAtSwitchDepth() {
        val plan = divePlan(gasSwitchTime = 1).segmentsCollapsed

        plan.assertSegment(0, DiveSegment.Type.DECENT,    startDepth = 0.0,  endDepth = 30.0, duration = 6,  gas = bottomGas)
        plan.assertSegment(1, DiveSegment.Type.FLAT,      startDepth = 30.0, endDepth = 30.0, duration = 24, gas = bottomGas)
        plan.assertSegment(2, DiveSegment.Type.ASCENT,    startDepth = 30.0, endDepth = 21.0, duration = 2,  gas = bottomGas)
        // Gas switch time: 1 minute flat at 21m still on the old gas (the diver is preparing to switch)
        plan.assertSegment(3, DiveSegment.Type.GAS_SWITCH, startDepth = 21.0, endDepth = 21.0, duration = 1,  gas = bottomGas)
        plan.assertSegment(4, DiveSegment.Type.ASCENT,    startDepth = 21.0, endDepth = 9.0,  duration = 3,  gas = decoGas)
        plan.assertSegment(5, DiveSegment.Type.DECO_STOP, startDepth = 9.0,  endDepth = 9.0,  duration = 1,  gas = decoGas)
        plan.assertSegment(6, DiveSegment.Type.ASCENT,    startDepth = 9.0,  endDepth = 6.0,  duration = 1,  gas = decoGas)
        plan.assertSegment(7, DiveSegment.Type.DECO_STOP, startDepth = 6.0,  endDepth = 6.0,  duration = 10, gas = decoGas)
        plan.assertSegment(8, DiveSegment.Type.ASCENT,    startDepth = 6.0,  endDepth = 0.0,  duration = 2,  gas = decoGas)
    }

    /**
     * Verifies [DiveSegment.isDecompressionStop] returns false for a gas switch segment.
     */
    @Test
    fun gasSwitchSegment_isNotADecoStop() {
        val plan = divePlan(gasSwitchTime = 1).segmentsCollapsed
        val gasSwitchSegment = plan[3]
        assertTrue(gasSwitchSegment.isGasSwitch, "Expected isGasSwitch to be true but was: ${gasSwitchSegment.type}")
        assertFalse(gasSwitchSegment.isDecompressionStop, "Expected isDecompressionStop to be false but was: ${gasSwitchSegment.type}")
    }

    @Test
    fun gasSwitchTimeZero_producesZeroDurationMarkerSegment() {
        val plan = divePlan(gasSwitchTime = 0).segmentsCollapsed
        val gasSwitchSegment = assertNotNull(plan.find { it.isGasSwitch })
        assertEquals(0, gasSwitchSegment.duration)
        assertEquals(21.0, gasSwitchSegment.startDepth)
    }
}
