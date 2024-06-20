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
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.model.Salinity
import org.neotech.app.abysner.domain.diveplanning.DivePlanner
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GasPlannerTest {

    @Test
    fun testFindPotentialWorstCaseTtsPoints() {
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
        val divePlan = divePlanner.getDecoPlan(
            plan = listOf(
                DiveProfileSection(10, 50, Gas.Trimix2135),
                DiveProfileSection(1, 50, Gas.Trimix2135),
                DiveProfileSection(10, 20, Gas.Trimix2135),
                DiveProfileSection(30, 20, Gas.Trimix2135)
            ),
            decoGases = listOf(Gas.Oxygen50)
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
}
