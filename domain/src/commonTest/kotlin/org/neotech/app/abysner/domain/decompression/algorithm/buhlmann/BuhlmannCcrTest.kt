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

package org.neotech.app.abysner.domain.decompression.algorithm.buhlmann

import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.physics.metersToAmbientPressure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuhlmannCcrTest {

    private val environment = Environment.SeaLevelSalt

    private fun createModel(gfLow: Double = 0.6, gfHigh: Double = 0.7) = Buhlmann(
        version = Buhlmann.Version.ZH16C,
        environment = environment,
        gfLow = gfLow,
        gfHigh = gfHigh,
    )

    @Test
    fun ccrCeiling_isLowerThanOcCeilingAtSameDepthAndTime() {
        val ocModel = createModel()
        val ccrModel = createModel()

        val depth = metersToAmbientPressure(30.0, environment)

        ocModel.addPressureChange(depth, depth, Gas.Air, timeInMinutes = 30)
        ccrModel.addPressureChange(depth, depth, Gas.Air, timeInMinutes = 30, ccrSetpoint = 1.3)

        val ocCeiling = ocModel.getCeiling()
        val ccrCeiling = ccrModel.getCeiling()

        // Assert CCR ceiling is lower, since CCR uses a constant ppO2 that is higher than the ppO2
        // of air at 30 meters it reduces inert gas loading.
        assertTrue(
            ccrCeiling.value < ocCeiling.value,
            "CCR ceiling (${ccrCeiling.value}) should be lower than OC ceiling (${ocCeiling.value})"
        )
    }

    @Test
    fun ccrPerMinuteConsistency_singleCallMatchesMultipleCalls() {
        val singleCallModel = createModel()
        val multiCallModel = createModel()

        val depth = metersToAmbientPressure(30.0, environment)
        val gas = Gas.Air
        val setpoint = 1.3

        // 1 x 30-minute
        singleCallModel.addPressureChange(depth, depth, gas, timeInMinutes = 30, ccrSetpoint = setpoint)

        // 30 x 1-minute
        repeat(30) {
            multiCallModel.addPressureChange(depth, depth, gas, timeInMinutes = 1, ccrSetpoint = setpoint)
        }

        val singleCeiling = singleCallModel.getCeiling()
        val multiCeiling = multiCallModel.getCeiling()

        // Verify that the ceiling is above atmospheric
        assertTrue(singleCeiling.value > environment.atmosphericPressure)

        assertEquals(
            singleCeiling.value,
            multiCeiling.value,
            1e-10,
            "A single 30-minute CCR call must produce the same ceiling as 30 x 1-minute calls."
        )
    }

    /**
     * Test all three CCR tissue loading cases where ambient pressure crosses the setpoint in a
     * single dive profile:
     * 1. Descent through the setpoint (ambient pressure crosses from below to above the setpoint)
     * 2. Flat at depth (ambient pressure stays above setpoint, no crossing)
     * 3. Ascent through the setpoint (ambient pressure crosses from above to below setpoint)
     *
     * No assertions: this is a smoke test that verifies none of the code paths crash.
     */
    @Test
    fun ccrPressureChange_descentFlatAndAscentDoNotCrash() {
        val model = createModel()

        val surface = metersToAmbientPressure(0.0, environment)
        val bottom = metersToAmbientPressure(20.0, environment)
        val gas = Gas.Air
        val setpoint = 1.3

        // 1. Descent: surface to 20m over 2 minutes (crosses setpoint boundary)
        model.addPressureChange(surface, bottom, gas, timeInMinutes = 2, ccrSetpoint = setpoint)

        // 2. Flat: 5 minutes at 20m
        model.addPressureChange(bottom, bottom, gas, timeInMinutes = 5, ccrSetpoint = setpoint)

        // 3. Ascent: 20m to surface over 2 minutes (crosses setpoint boundary)
        model.addPressureChange(bottom, surface, gas, timeInMinutes = 2, ccrSetpoint = setpoint)
    }

    @Test
    fun referencePlan6_producesExpectedNoDecompressionLimit() {
        // TODO once planner is CCR aware, this plan/test should move to DivePlannerTest
        val model = createModel(gfLow = 0.3, gfHigh = 0.7)
        val surface = metersToAmbientPressure(0.0, environment)
        val bottom = metersToAmbientPressure(30.0, environment)
        val gas = Gas.Air
        val setpoint = 1.3

        // Descend from surface to 30m at 5 m/min (6 minutes)
        model.addPressureChange(surface, bottom, gas, timeInMinutes = 6, ccrSetpoint = setpoint)

        // Load minute by minute at bottom until ceiling appears
        var minutesAtBottom = 0
        while (model.getCeiling().value <= environment.atmosphericPressure) {
            minutesAtBottom++
            model.addPressureChange(bottom, bottom, gas, timeInMinutes = 1, ccrSetpoint = setpoint)
            if (minutesAtBottom > 200) break
        }

        // According to other planning software this should lead to about 13 minutes of bottom time
        // without hitting deco
        assertEquals(13, minutesAtBottom)
    }

    @Test
    fun getNoDecompressionLimit_ccrIsLongerThanOc() {
        val ocModel = createModel()
        val ccrModel = createModel()

        val depth = metersToAmbientPressure(30.0, environment)

        val ocNdl = ocModel.getNoDecompressionLimit(depth, Gas.Air)
        val ccrNdl = ccrModel.getNoDecompressionLimit(depth, Gas.Air, ccrSetpoint = 1.3)

        // At 30m with air the OC ppO2 is about 0.84 bar, so a 1.3 bar setpoint displaces
        // significantly more inert gas with O2, resulting in slower tissue loading and a longer
        // NDL. This would not hold if the setpoint were below the OC ppO2.
        assertTrue(
            ccrNdl > ocNdl,
            "CCR NDL ($ccrNdl min) should be longer than OC NDL ($ocNdl min) at the same depth"
        )
    }

    @Test
    fun getNoDecompressionLimit_ccrDoesNotAlterTissueState() {
        val model = createModel()
        val depth = metersToAmbientPressure(30.0, environment)

        val ceilingBefore = model.getCeiling()
        model.getNoDecompressionLimit(depth, Gas.Air, ccrSetpoint = 1.3)
        val ceilingAfter = model.getCeiling()

        assertEquals(ceilingBefore.value, ceilingAfter.value)
    }
}
