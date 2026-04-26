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
    fun oxygenMod_returnsCorrectModForCommonGases() {
        assertEquals(40.653, Gas.Nitrox28.oxygenMod(1.4, Environment.SeaLevelFresh), DOUBLE_TOLERANCE)
        assertEquals(39.469, Gas.Nitrox28.oxygenMod(1.4, Environment.SeaLevelSalt), DOUBLE_TOLERANCE)

        assertEquals(34.280, Gas.Nitrox32.oxygenMod(1.4, Environment.SeaLevelFresh), DOUBLE_TOLERANCE)
        assertEquals(33.281, Gas.Nitrox32.oxygenMod(1.4, Environment.SeaLevelSalt), DOUBLE_TOLERANCE)

        assertEquals(22.299, Gas.Nitrox50.oxygenMod(1.6, Environment.SeaLevelFresh), DOUBLE_TOLERANCE)
        assertEquals(21.649, Gas.Nitrox50.oxygenMod(1.6, Environment.SeaLevelSalt), DOUBLE_TOLERANCE)

        assertEquals(10.062, Gas.Nitrox80.oxygenMod(1.6, Environment.SeaLevelFresh), DOUBLE_TOLERANCE)
        assertEquals(9.769, Gas.Nitrox80.oxygenMod(1.6, Environment.SeaLevelSalt), DOUBLE_TOLERANCE)

        assertEquals(5.983, Gas.Oxygen.oxygenMod(1.6, Environment.SeaLevelFresh), DOUBLE_TOLERANCE)
        assertEquals(5.809, Gas.Oxygen.oxygenMod(1.6, Environment.SeaLevelSalt), DOUBLE_TOLERANCE)
    }

    @Test
    fun oxygenModRounded_matchesCommonlyAcceptedDepths() {
        // EAN28 is commonly accepted at 40 meter at 1.4 ppO2. However, even with a tolerance of
        // about half a meter it does not qualify for 40 meter dives in salt water at 1013 millibar
        // atmospheric pressure. I guess divers just have to accept that, the current tolerance is
        // big enough to allow for rounding in meters that makes sense, but the idea is not to
        // always match rules of thumb.
        //
        // 1.4 / 0,28 = 5     bar ambient
        // 5 - 1,013  = 3,987 bar hydrostatic
        //
        // Pressure per meter in salt water: 1030 kg/m3 * 9.81 m/s2 / 100000 Pa/bar = 0.101043 bar/m
        //
        // Max depth 28% = 3.987 / 0.101043 = 39.45 meter
        //
        // If we would use Environment.SeaLevelSaltEn13319 instead of Environment.SeaLevelSalt
        // it would just about qualify for 40 meter dives.
        assertEquals(41, Gas.Nitrox28.oxygenModRounded(1.4, Environment.SeaLevelFresh))
        assertEquals(39, Gas.Nitrox28.oxygenModRounded(1.4, Environment.SeaLevelSalt))

        assertEquals(34, Gas.Nitrox32.oxygenModRounded(1.4, Environment.SeaLevelFresh))
        assertEquals(33, Gas.Nitrox32.oxygenModRounded(1.4, Environment.SeaLevelSalt))

        assertEquals(22, Gas.Nitrox50.oxygenModRounded(1.6, Environment.SeaLevelFresh))
        assertEquals(22, Gas.Nitrox50.oxygenModRounded(1.6, Environment.SeaLevelSalt))

        assertEquals(10, Gas.Nitrox80.oxygenModRounded(1.6, Environment.SeaLevelFresh))
        assertEquals(10, Gas.Nitrox80.oxygenModRounded(1.6, Environment.SeaLevelSalt))

        assertEquals(6, Gas.Oxygen.oxygenModRounded(1.6, Environment.SeaLevelFresh))
        assertEquals(6, Gas.Oxygen.oxygenModRounded(1.6, Environment.SeaLevelSalt))
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
            DOUBLE_TOLERANCE
        )
        assertEquals(
            2.606,
            Gas.Air.densityAtDepth(10.0, Environment.SeaLevelSalt),
            DOUBLE_TOLERANCE
        )

        assertEquals(
            1.819,
            Gas.Trimix2135.densityAtDepth(10.0, Environment.SeaLevelFresh),
            DOUBLE_TOLERANCE
        )
        assertEquals(
            1.846,
            Gas.Trimix2135.densityAtDepth(10.0, Environment.SeaLevelSalt),
            DOUBLE_TOLERANCE
        )
    }

    @Test
    fun densityMod_outputs_correct_value_for_given_depth_and_salinity() {
        assertEquals(
            38.746,
            Gas.Air.densityMod(Gas.MAX_GAS_DENSITY, Environment.SeaLevelFresh),
            DOUBLE_TOLERANCE
        )
        assertEquals(
            37.618,
            Gas.Air.densityMod(Gas.MAX_GAS_DENSITY, Environment.SeaLevelSalt),
            DOUBLE_TOLERANCE
        )

        assertEquals(
            58.943,
            Gas.Trimix2135.densityMod(Gas.MAX_GAS_DENSITY, Environment.SeaLevelFresh),
            DOUBLE_TOLERANCE
        )
        assertEquals(
            57.226,
            Gas.Trimix2135.densityMod(Gas.MAX_GAS_DENSITY, Environment.SeaLevelSalt),
            DOUBLE_TOLERANCE
        )

        assertEquals(
            30.830,
            Gas.Air.densityMod(Gas.MAX_RECOMMENDED_GAS_DENSITY, Environment.SeaLevelFresh),
            DOUBLE_TOLERANCE
        )
        assertEquals(
            29.932,
            Gas.Air.densityMod(Gas.MAX_RECOMMENDED_GAS_DENSITY, Environment.SeaLevelSalt),
            DOUBLE_TOLERANCE
        )

        assertEquals(
            47.769,
            Gas.Trimix2135.densityMod(Gas.MAX_RECOMMENDED_GAS_DENSITY, Environment.SeaLevelFresh),
            DOUBLE_TOLERANCE
        )
        assertEquals(
            46.378,
            Gas.Trimix2135.densityMod(Gas.MAX_RECOMMENDED_GAS_DENSITY, Environment.SeaLevelSalt),
            DOUBLE_TOLERANCE
        )
    }

    @Test
    fun inspiredGas_normalDepthProducesExpectedMix() {
        val inspired = Gas.Air.inspiredGas(metersToAmbientPressure(30.0, Environment.SeaLevelFresh).value, 1.3)
        assertEquals(0.328, inspired.oxygenFraction, DOUBLE_TOLERANCE)
        assertEquals(0.0, inspired.heliumFraction, DOUBLE_TOLERANCE)
    }

    /**
     * At ambient pressure of about 1.2093 bar (2 meters) the loop would require just under 108%
     * oxygen fraction to reach the setpoint, which is physically impossible so it clamps to 100%.
     */
    @Test
    fun inspiredGas_shallowDepthClampsToMaximumOxygenFraction() {
        val inspired = Gas.Air.inspiredGas(metersToAmbientPressure(2.0, Environment.SeaLevelFresh).value, 1.3)
        assertEquals(1.0, inspired.oxygenFraction, DOUBLE_TOLERANCE)
        assertEquals(0.0, inspired.heliumFraction, DOUBLE_TOLERANCE)
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
        assertEquals(Gas.Air.oxygenFraction, inspired.oxygenFraction, DOUBLE_TOLERANCE)
        assertEquals(Gas.Air.heliumFraction, inspired.heliumFraction, DOUBLE_TOLERANCE)
    }

    @Test
    fun inspiredGas_trimixDiluentScalesHeliumCorrectly() {
        val inspired = Gas.Trimix2135.inspiredGas(metersToAmbientPressure(30.0, Environment.SeaLevelFresh).value, 1.3)
        assertEquals(0.328, inspired.oxygenFraction, DOUBLE_TOLERANCE)
        assertEquals(0.298, inspired.heliumFraction, DOUBLE_TOLERANCE)
    }
}

private const val DOUBLE_TOLERANCE = 1e-3
