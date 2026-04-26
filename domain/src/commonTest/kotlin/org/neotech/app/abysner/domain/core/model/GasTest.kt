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

package org.neotech.app.abysner.domain.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import org.neotech.app.abysner.domain.core.physics.metersToAmbientPressure

class GasTest {

    @Test
    fun oxygenMod_returnsCorrectModForOxygen() {
        assertEquals(
            5.983,
            Gas.Oxygen.oxygenMod(1.6, Environment.Default),
            DOUBLE_PRECISION_DELTA
        )
    }

    @Test
    fun nitrogenFraction_isComputedFromOxygenAndHelium() {
        val newGas = Gas(oxygenFraction = 21f / 100.0, heliumFraction = 0f / 100.0)
        val nitrogenPercentage = (newGas.nitrogenFraction * 100.0).toInt()
        println(nitrogenPercentage)
    }

    @Test
    fun densityAtDepth_outputs_correct_value_for_given_depth_and_salinity() {
        assertEquals(
            2.568,
            Gas.Air.densityAtDepth(10.0, Environment.SeaLevelFresh),
            DOUBLE_PRECISION_DELTA
        )
        assertEquals(
            2.606,
            Gas.Air.densityAtDepth(10.0, Environment.SeaLevelSalt),
            DOUBLE_PRECISION_DELTA
        )

        assertEquals(
            1.819,
            Gas.Trimix2135.densityAtDepth(10.0, Environment.SeaLevelFresh),
            DOUBLE_PRECISION_DELTA
        )
        assertEquals(
            1.846,
            Gas.Trimix2135.densityAtDepth(10.0, Environment.SeaLevelSalt),
            DOUBLE_PRECISION_DELTA
        )
    }

    @Test
    fun densityMod_outputs_correct_value_for_given_depth_and_salinity() {
        assertEquals(
            38.746,
            Gas.Air.densityMod(Gas.MAX_GAS_DENSITY, Environment.SeaLevelFresh),
            DOUBLE_PRECISION_DELTA
        )
        assertEquals(
            37.618,
            Gas.Air.densityMod(Gas.MAX_GAS_DENSITY, Environment.SeaLevelSalt),
            DOUBLE_PRECISION_DELTA
        )

        assertEquals(
            58.943,
            Gas.Trimix2135.densityMod(Gas.MAX_GAS_DENSITY, Environment.SeaLevelFresh),
            DOUBLE_PRECISION_DELTA
        )
        assertEquals(
            57.226,
            Gas.Trimix2135.densityMod(Gas.MAX_GAS_DENSITY, Environment.SeaLevelSalt),
            DOUBLE_PRECISION_DELTA
        )

        assertEquals(
            30.830,
            Gas.Air.densityMod(Gas.MAX_RECOMMENDED_GAS_DENSITY, Environment.SeaLevelFresh),
            DOUBLE_PRECISION_DELTA
        )
        assertEquals(
            29.932,
            Gas.Air.densityMod(Gas.MAX_RECOMMENDED_GAS_DENSITY, Environment.SeaLevelSalt),
            DOUBLE_PRECISION_DELTA
        )

        assertEquals(
            47.769,
            Gas.Trimix2135.densityMod(Gas.MAX_RECOMMENDED_GAS_DENSITY, Environment.SeaLevelFresh),
            DOUBLE_PRECISION_DELTA
        )
        assertEquals(
            46.378,
            Gas.Trimix2135.densityMod(Gas.MAX_RECOMMENDED_GAS_DENSITY, Environment.SeaLevelSalt),
            DOUBLE_PRECISION_DELTA
        )
    }

    @Test
    fun inspiredGas_normalDepthProducesExpectedMix() {
        val inspired = Gas.Air.inspiredGas(metersToAmbientPressure(30.0, Environment.SeaLevelFresh).value, 1.3)
        assertEquals(0.328, inspired.oxygenFraction, DOUBLE_PRECISION_DELTA)
        assertEquals(0.0, inspired.heliumFraction, DOUBLE_PRECISION_DELTA)
    }

    /**
     * At ambient pressure of about 1.2093 bar (2 meters) the loop would require just under 108%
     * oxygen fraction to reach the setpoint, which is physically impossible so it clamps to 100%.
     */
    @Test
    fun inspiredGas_shallowDepthClampsToMaximumOxygenFraction() {
        val inspired = Gas.Air.inspiredGas(metersToAmbientPressure(2.0, Environment.SeaLevelFresh).value, 1.3)
        assertEquals(1.0, inspired.oxygenFraction, DOUBLE_PRECISION_DELTA)
        assertEquals(0.0, inspired.heliumFraction, DOUBLE_PRECISION_DELTA)
    }

    /**
     * When the diluent oxygen partial pressure itself is higher than the set-point at depth, the
     * loop should clamp to the diluent oxygen partial pressure and thus fraction. This is a
     * simplification/assumption made by the decompression planner as well see:
     * [org.neotech.app.abysner.domain.decompression.algorithm.buhlmann.ccrSchreinerInputs]
     */
    @Test
    fun inspiredGas_deepDepthClampsToMinimumDiluentOxygenFraction() {
        // At very deep depth, setpoint / ambient < diluent O2, should clamp to diluent O2
        val inspired = Gas.Air.inspiredGas(metersToAmbientPressure(200.0, Environment.SeaLevelFresh).value, 0.5)
        assertEquals(Gas.Air.oxygenFraction, inspired.oxygenFraction, DOUBLE_PRECISION_DELTA)
        assertEquals(Gas.Air.heliumFraction, inspired.heliumFraction, DOUBLE_PRECISION_DELTA)
    }

    @Test
    fun inspiredGas_trimixDiluentScalesHeliumCorrectly() {
        val inspired = Gas.Trimix2135.inspiredGas(metersToAmbientPressure(30.0, Environment.SeaLevelFresh).value, 1.3)
        assertEquals(0.328, inspired.oxygenFraction, DOUBLE_PRECISION_DELTA)
        assertEquals(0.298, inspired.heliumFraction, DOUBLE_PRECISION_DELTA)
    }
}

private const val DOUBLE_PRECISION_DELTA = 1e-3
