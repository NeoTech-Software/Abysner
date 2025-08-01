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

package org.neotech.app.abysner.domain.gasplanning

import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.model.Salinity
import org.neotech.app.abysner.domain.diveplanning.DivePlanner
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.tenthAtDecimalPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GasPlannerTest {

    @Test
    fun testFindPotentialWorstCaseTtsPointsScenario1() {

        val bottomGas = Cylinder.steel12Liter(Gas.Trimix2135)
        val decoGas = Cylinder.aluminium80Cuft(Gas.Nitrox50)

        val divePlanner = DivePlanner()
        divePlanner.configuration = Configuration(
            sacRate = 15.0,
            maxPPO2 = 1.4,
            maxPPO2Deco = 1.6,
            maxEND = 30.0,
            maxAscentRate = 5.0,
            maxDescentRate = 5.0,
            gfLow = 0.4,
            gfHigh = 0.8,
            forceMinimalDecoStopTime = true,
            decoStepSize = 3,
            lastDecoStopDepth = 3,
            salinity = Salinity.WATER_FRESH,
            algorithm = Configuration.Algorithm.BUHLMANN_ZH16C
        )
        val divePlan = divePlanner.addDive(
            plan = listOf(
                DiveProfileSection(10, 50, bottomGas),
                DiveProfileSection(1, 50, bottomGas),
                DiveProfileSection(10, 20, bottomGas),
                DiveProfileSection(30, 20, bottomGas)
            ),
            decoGases = listOf(bottomGas, decoGas)
        )

        // at T=10 and D=50.0: TTS=11
        // at T=11 and D=50.0: TTS=12
        // at T=21 and D=20.0: TTS=6
        // at T=51 and D=20.0: TTS=14

        // TTS at 51 minutes in the dive (20 meters) is longer then the TTS at 11 minutes in the
        // dive, at depth 50 meters! However the depth of 50 meters could still require more gas due
        // to the depth in general (but figuring out this is done in 'calculateGasPlan' in the
        // GasPlanner)

        val ttsWorstCaseScenarios = GasPlanner().findPotentialWorstCaseTtsPoints(divePlan)
        assertEquals(2, ttsWorstCaseScenarios.size)
        assertTrue { ttsWorstCaseScenarios.any { it.end == 11 && it.endDepth == 50.0 && it.ttsAfter == 12 } }
        assertTrue { ttsWorstCaseScenarios.any { it.end == 51 && it.endDepth == 20.0 && it.ttsAfter == 14 } }
    }

    @Test
    fun testFindPotentialWorstCaseTtsPointsScenario2() {

        val bottomGas = Cylinder.steel12Liter(Gas.Air)

        val divePlanner = DivePlanner()
        divePlanner.configuration = Configuration(
            sacRate = 15.0,
            maxPPO2 = 1.4,
            maxPPO2Deco = 1.6,
            maxEND = 30.0,
            maxAscentRate = 5.0,
            maxDescentRate = 5.0,
            gfLow = 0.3,
            gfHigh = 0.7,
            forceMinimalDecoStopTime = true,
            decoStepSize = 3,
            lastDecoStopDepth = 3,
            salinity = Salinity.WATER_FRESH,
            algorithm = Configuration.Algorithm.BUHLMANN_ZH16C
        )
        val divePlan = divePlanner.addDive(
            plan = listOf(
                DiveProfileSection(15, 10, bottomGas),
                DiveProfileSection(15, 15, bottomGas),
                // +3 + 3 (contingency plan)
                DiveProfileSection(18, 23, bottomGas),
            ),
            decoGases = listOf(bottomGas)
        )

        // at T=15 and D=10.0: TTS=2
        // at T=30 and D=15.0: TTS=3
        // at T=48 and D=23.0: TTS=7

        val ttsWorstCaseScenarios = GasPlanner().findPotentialWorstCaseTtsPoints(divePlan)
        assertEquals(1, ttsWorstCaseScenarios.size)
        assertTrue { ttsWorstCaseScenarios.any { it.end == 48 && it.endDepth == 23.0 && it.ttsAfter == 15 } }
    }

    /**
     * Test for GitHub issue: https://github.com/NeoTech-Software/Abysner/issues/59
     * Making sure the calculated volumes and pressures are as expected.
     */
    @Test
    fun testBarUsage() {

        val bottomGas = Cylinder(Gas.Air, 200, 22)
        val decoGas = Cylinder(Gas.Nitrox50, 207, 7)

        val divePlanner = DivePlanner()
        divePlanner.configuration = Configuration(
            sacRate = 14.0,
            maxPPO2 = 1.4,
            maxPPO2Deco = 1.6,
            maxEND = 30.0,
            maxAscentRate = 5.0,
            maxDescentRate = 10.0,
            gfLow = 0.4,
            gfHigh = 0.8,
            forceMinimalDecoStopTime = true,
            decoStepSize = 3,
            lastDecoStopDepth = 3,
            salinity = Salinity.WATER_FRESH,
            algorithm = Configuration.Algorithm.BUHLMANN_ZH16C
        )
        val divePlan = divePlanner.addDive(
            plan = listOf(
                DiveProfileSection(30, 50, bottomGas),
            ),
            decoGases = listOf(bottomGas, decoGas)
        )

        val gasPlan = GasPlanner().calculateGasPlan(divePlan)

        assertEquals(3769.0, gasPlan[0].totalGasRequirement, tenthAtDecimalPoint(0))
        assertEquals(4131.0, gasPlan[1].totalGasRequirement, tenthAtDecimalPoint(0))
    }
}
