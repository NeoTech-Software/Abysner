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

package org.neotech.app.abysner.domain.core.physics

import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PolynomialRealGasModelTest {

    @Test
    fun getGasVolume_andGetGasPressure_roundTripCorrectly() {
        val model = PolynomialRealGasModel()

        fun test(pressure: Double) {
            val cylinder = Cylinder(Gas.Air, pressure, 10.0)
            assertEquals(model.getGasPressure(cylinder, model.getGasVolume(cylinder)), pressure, DOUBLE_TOLERANCE)
        }

        // Test 0 to 300 bar in increments of 30 bar
        repeat(11) {
            test(it * 30.0)
        }
    }

    @Test
    fun getGasPressure_constantTimeCalculation_producesReasonableResult() {
        val model = PolynomialRealGasModel(constantTimeCalculation = true)
        val cylinder = Cylinder(Gas.Air, 200.0, 10.0)
        val volume = model.getGasVolume(cylinder)
        val recoveredPressure = model.getGasPressure(cylinder, volume)
        // constantTimeCalculation is an approximation, allow wider tolerance
        assertEquals(200.0, recoveredPressure, 5.0)
    }

    @Test
    fun getGasVolume_heliumRichGas_differFromIdealGas() {
        val realModel = PolynomialRealGasModel()
        val heliox = Gas(oxygenFraction = 0.21, heliumFraction = 0.79)
        val volume = realModel.getGasVolume(heliox, 12.0, 200.0)
        val idealVolume = 200.0 * 12.0  // 2400L
        // Real gas model should produce a different result than ideal gas
        assertTrue(volume != idealVolume, "Real gas volume should differ from ideal")
        // But should still be in a reasonable range (within 10% of ideal)
        assertTrue(volume > idealVolume * 0.9, "Volume $volume too low")
        assertTrue(volume < idealVolume * 1.1, "Volume $volume too high")
    }

    @Test
    fun getGasPressure_heliumRichGasHighPressure_convergesWithinTolerance() {
        val model = PolynomialRealGasModel()
        val heliox = Gas(oxygenFraction = 0.10, heliumFraction = 0.90)
        val cylinder = Cylinder(heliox, 300.0, 10.0)
        val volume = model.getGasVolume(cylinder)
        val recoveredPressure = model.getGasPressure(cylinder, volume)
        assertEquals(300.0, recoveredPressure, 1.0)
    }

    @Test
    fun getGasVolume_nitroxAtVariousPressures_increasesMonotonically() {
        val model = PolynomialRealGasModel()
        var previousVolume = 0.0
        for (pressure in 1..300 step 50) {
            val cylinder = Cylinder(Gas.Nitrox50, pressure.toDouble(), 10.0)
            val volume = model.getGasVolume(cylinder)
            assertTrue(volume > previousVolume, "Volume should increase with pressure")
            previousVolume = volume
        }
    }
}

private const val DOUBLE_TOLERANCE = 1e-6
