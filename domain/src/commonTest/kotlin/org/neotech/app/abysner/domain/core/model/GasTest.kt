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

import org.neotech.app.abysner.domain.core.physics.ambientPressureToMeters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.neotech.app.abysner.domain.core.physics.metersToAmbientPressure
import kotlin.math.floor

class GasTest {

    @Test
    fun init_rejectsGasWhenCombinedOxygenAndHeliumFractionsExceedOneHundredPercent() {
        assertFailsWith<IllegalStateException> {
            Gas(oxygenFraction = 0.8, heliumFraction = 0.3)
        }
    }

    @Test
    fun oxygenMod_returnsCorrectModForCommonGases() {
        assertEquals(metersToAmbientPressure(40.653, Environment.SeaLevelFresh).value, Gas.Nitrox28.oxygenModAmbientPressure(1.4).value, DOUBLE_TOLERANCE)
        assertEquals(metersToAmbientPressure(39.469, Environment.SeaLevelSalt).value, Gas.Nitrox28.oxygenModAmbientPressure(1.4).value, DOUBLE_TOLERANCE)

        assertEquals(metersToAmbientPressure(34.280, Environment.SeaLevelFresh).value, Gas.Nitrox32.oxygenModAmbientPressure(1.4).value, DOUBLE_TOLERANCE)
        assertEquals(metersToAmbientPressure(33.281, Environment.SeaLevelSalt).value, Gas.Nitrox32.oxygenModAmbientPressure(1.4).value, DOUBLE_TOLERANCE)

        assertEquals(metersToAmbientPressure(22.299, Environment.SeaLevelFresh).value, Gas.Nitrox50.oxygenModAmbientPressure(1.6).value, DOUBLE_TOLERANCE)
        assertEquals(metersToAmbientPressure(21.649, Environment.SeaLevelSalt).value, Gas.Nitrox50.oxygenModAmbientPressure(1.6).value, DOUBLE_TOLERANCE)

        assertEquals(metersToAmbientPressure(10.062, Environment.SeaLevelFresh).value, Gas.Nitrox80.oxygenModAmbientPressure(1.6).value, DOUBLE_TOLERANCE)
        assertEquals(metersToAmbientPressure(9.769, Environment.SeaLevelSalt).value, Gas.Nitrox80.oxygenModAmbientPressure(1.6).value, DOUBLE_TOLERANCE)

        assertEquals(metersToAmbientPressure(5.983, Environment.SeaLevelFresh).value, Gas.Oxygen.oxygenModAmbientPressure(1.6).value, DOUBLE_TOLERANCE)
        assertEquals(metersToAmbientPressure(5.809, Environment.SeaLevelSalt).value, Gas.Oxygen.oxygenModAmbientPressure(1.6).value, DOUBLE_TOLERANCE)
    }

    @Test
    fun oxygenModAmbientPressureWithTolerance_matchesCommonlyAcceptedDepths() {
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
        assertEquals(41.0, floor(ambientPressureToMeters(Gas.Nitrox28.oxygenModAmbientPressureWithTolerance(1.4).value, Environment.SeaLevelFresh)))
        assertEquals(39.0, floor(ambientPressureToMeters(Gas.Nitrox28.oxygenModAmbientPressureWithTolerance(1.4).value, Environment.SeaLevelSalt)))

        assertEquals(34.0,  floor(ambientPressureToMeters(Gas.Nitrox32.oxygenModAmbientPressureWithTolerance(1.4).value, Environment.SeaLevelFresh)))
        assertEquals(33.0,  floor(ambientPressureToMeters(Gas.Nitrox32.oxygenModAmbientPressureWithTolerance(1.4).value, Environment.SeaLevelSalt)))

        assertEquals(22.0,  floor(ambientPressureToMeters(Gas.Nitrox50.oxygenModAmbientPressureWithTolerance(1.6).value, Environment.SeaLevelFresh)))
        assertEquals(22.0,  floor(ambientPressureToMeters(Gas.Nitrox50.oxygenModAmbientPressureWithTolerance(1.6).value, Environment.SeaLevelSalt)))

        assertEquals(10.0,  floor(ambientPressureToMeters(Gas.Nitrox80.oxygenModAmbientPressureWithTolerance(1.6).value, Environment.SeaLevelFresh)))
        assertEquals(10.0,  floor(ambientPressureToMeters(Gas.Nitrox80.oxygenModAmbientPressureWithTolerance(1.6).value, Environment.SeaLevelSalt)))

        assertEquals(6.0,  floor(ambientPressureToMeters(Gas.Oxygen.oxygenModAmbientPressureWithTolerance(1.6).value, Environment.SeaLevelFresh)))
        assertEquals(6.0,  floor(ambientPressureToMeters(Gas.Oxygen.oxygenModAmbientPressureWithTolerance(1.6).value, Environment.SeaLevelSalt)))
    }

    @Test
    fun nitrogenFraction_isRemainderOfOxygenAndHelium() {
        assertEquals(0.79, Gas.Air.nitrogenFraction, DOUBLE_TOLERANCE)
        assertEquals(0.44, Gas.Trimix2135.nitrogenFraction, DOUBLE_TOLERANCE)
        assertEquals(0.0, Gas.Oxygen.nitrogenFraction, DOUBLE_TOLERANCE)
    }

    @Test
    fun densityAtAmbientPressure_outputs_correct_value_for_given_depth_and_salinity() {
        assertEquals(
            2.568,
            Gas.Air.densityAtAmbientPressure(metersToAmbientPressure(10.0, Environment.SeaLevelFresh)),
            DOUBLE_TOLERANCE
        )
        assertEquals(
            2.606,
            Gas.Air.densityAtAmbientPressure(metersToAmbientPressure(10.0, Environment.SeaLevelSalt)),
            DOUBLE_TOLERANCE
        )

        assertEquals(
            1.819,
            Gas.Trimix2135.densityAtAmbientPressure(metersToAmbientPressure(10.0, Environment.SeaLevelFresh)),
            DOUBLE_TOLERANCE
        )
        assertEquals(
            1.846,
            Gas.Trimix2135.densityAtAmbientPressure(metersToAmbientPressure(10.0, Environment.SeaLevelSalt)),
            DOUBLE_TOLERANCE
        )
    }

    @Test
    fun densityModAmbientPressure_outputs_correct_value_for_given_depth_and_salinity() {
        assertEquals(
            metersToAmbientPressure(38.746, Environment.SeaLevelFresh).value,
            Gas.Air.densityModAmbientPressure(Gas.MAX_GAS_DENSITY).value,
            DOUBLE_TOLERANCE
        )
        assertEquals(
            metersToAmbientPressure(37.618, Environment.SeaLevelSalt).value,
            Gas.Air.densityModAmbientPressure(Gas.MAX_GAS_DENSITY).value,
            DOUBLE_TOLERANCE
        )

        assertEquals(
            metersToAmbientPressure(58.943, Environment.SeaLevelFresh).value,
            Gas.Trimix2135.densityModAmbientPressure(Gas.MAX_GAS_DENSITY).value,
            DOUBLE_TOLERANCE
        )
        assertEquals(
            metersToAmbientPressure(57.226, Environment.SeaLevelSalt).value,
            Gas.Trimix2135.densityModAmbientPressure(Gas.MAX_GAS_DENSITY).value,
            DOUBLE_TOLERANCE
        )

        assertEquals(
            metersToAmbientPressure(30.830, Environment.SeaLevelFresh).value,
            Gas.Air.densityModAmbientPressure(Gas.MAX_RECOMMENDED_GAS_DENSITY).value,
            DOUBLE_TOLERANCE
        )
        assertEquals(
            metersToAmbientPressure(29.932, Environment.SeaLevelSalt).value,
            Gas.Air.densityModAmbientPressure(Gas.MAX_RECOMMENDED_GAS_DENSITY).value,
            DOUBLE_TOLERANCE
        )

        assertEquals(
            metersToAmbientPressure(47.769, Environment.SeaLevelFresh).value,
            Gas.Trimix2135.densityModAmbientPressure(Gas.MAX_RECOMMENDED_GAS_DENSITY).value,
            DOUBLE_TOLERANCE
        )
        assertEquals(
            metersToAmbientPressure(46.378, Environment.SeaLevelSalt).value,
            Gas.Trimix2135.densityModAmbientPressure(Gas.MAX_RECOMMENDED_GAS_DENSITY).value,
            DOUBLE_TOLERANCE
        )
    }

    @Test
    fun inspiredGas_normalDepthProducesExpectedMix() {
        val inspired = Gas.Air.inspiredGas(metersToAmbientPressure(30.0, Environment.SeaLevelFresh), 1.3)
        assertEquals(0.328, inspired.oxygenFraction, DOUBLE_TOLERANCE)
        assertEquals(0.0, inspired.heliumFraction, DOUBLE_TOLERANCE)
    }

    /**
     * At ambient pressure of about 1.2093 bar (2 meters) the loop would require just under 108%
     * oxygen fraction to reach the setpoint, which is physically impossible so it clamps to 100%.
     */
    @Test
    fun inspiredGas_shallowDepthClampsToMaximumOxygenFraction() {
        val inspired = Gas.Air.inspiredGas(metersToAmbientPressure(2.0, Environment.SeaLevelFresh), 1.3)
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
        val inspired = Gas.Air.inspiredGas(metersToAmbientPressure(200.0, Environment.SeaLevelFresh), 0.5)
        assertEquals(Gas.Air.oxygenFraction, inspired.oxygenFraction, DOUBLE_TOLERANCE)
        assertEquals(Gas.Air.heliumFraction, inspired.heliumFraction, DOUBLE_TOLERANCE)
    }

    @Test
    fun inspiredGas_trimixDiluentScalesHeliumCorrectly() {
        val inspired = Gas.Trimix2135.inspiredGas(metersToAmbientPressure(30.0, Environment.SeaLevelFresh), 1.3)
        assertEquals(0.328, inspired.oxygenFraction, DOUBLE_TOLERANCE)
        assertEquals(0.298, inspired.heliumFraction, DOUBLE_TOLERANCE)
    }

    @Test
    fun inspiredGas_pureOxygenDiluentReturnsOxygenOnly() {
        val inspired = Gas.Oxygen.inspiredGas(metersToAmbientPressure(6.0, Environment.SeaLevelFresh), 1.3)
        assertEquals(1.0, inspired.oxygenFraction, DOUBLE_TOLERANCE)
        assertEquals(0.0, inspired.heliumFraction, DOUBLE_TOLERANCE)
    }

    @Test
    fun endAmbientPressure_airEqualsAmbientPressure() {
        // Air has no helium, so END equals actual depth
        val ambient = metersToAmbientPressure(30.0, Environment.SeaLevelFresh).value
        assertEquals(ambient, Gas.Air.endAmbientPressure(ambient), DOUBLE_TOLERANCE)
    }

    @Test
    fun endAmbientPressure_trimixReducesNarcoticDepth() {
        val ambient = metersToAmbientPressure(60.0, Environment.SeaLevelFresh).value
        // Trimix 21/35 has 35% helium (non-narcotic), so at 60m (6.897 bar ambient) only the
        // remaining 65% (21% O2 + 44% N2) counts towards narcotic depth, an END of about 4.483 bar.
        assertEquals(4.483, Gas.Trimix2135.endAmbientPressure(ambient), DOUBLE_TOLERANCE)
    }

    @Test
    fun diveIndustryName_returnsOxygenForPureO2() {
        assertEquals("Oxygen", Gas.Oxygen.diveIndustryName())
    }

    @Test
    fun diveIndustryName_returnsAirForStandardAir() {
        assertEquals("Air", Gas.Air.diveIndustryName())
    }

    @Test
    fun diveIndustryName_returnsNitroxForAnyEnrichedAirAbove21Percent() {
        for (percentage in 22..99) {
            val gas = Gas(oxygenFraction = percentage / 100.0, heliumFraction = 0.0)
            assertEquals("Nitrox", gas.diveIndustryName())
        }
    }

    @Test
    fun diveIndustryName_returnsHelioxForAnyZeroNitrogenHeliumBlend() {
        // Any gas with He > 0 and N2 = 0 (and not pure O2) is Heliox
        for (oxygenPercentage in 1..99) {
            val heliumPercentage = 100 - oxygenPercentage
            val gas = Gas(oxygenFraction = oxygenPercentage / 100.0, heliumFraction = heliumPercentage / 100.0)
            assertEquals("Heliox", gas.diveIndustryName())
        }
    }

    @Test
    fun diveIndustryName_returnsTrimixForHypoxicHeliumBlendWithNitrogen() {
        // Any gas with O2 <= 21%, He > 0, and N2 > 0 is Trimix
        for (oxygenPercentage in 1..21) {
            for (heliumPercentage in 1 until (100 - oxygenPercentage)) {
                val gas = Gas(oxygenFraction = oxygenPercentage / 100.0, heliumFraction = heliumPercentage / 100.0)
                assertEquals("Trimix", gas.diveIndustryName())
            }
        }
    }

    @Test
    fun diveIndustryName_returnsHelitroxForHyperoxicHeliumBlendWithNitrogen() {
        // Any gas with O2 > 21%, He > 0, and N2 > 0 is Helitrox
        for (oxygenPercentage in 22..98) {
            for (heliumPercentage in 1 until (100 - oxygenPercentage)) {
                val gas = Gas(oxygenFraction = oxygenPercentage / 100.0, heliumFraction = heliumPercentage / 100.0)
                assertEquals("Helitrox", gas.diveIndustryName(), "Expected Helitrox for $oxygenPercentage/$heliumPercentage")
            }
        }
    }

    @Test
    fun diveIndustryName_returnsHypoxicForAnySubAirO2WithoutHelium() {
        for (percentage in 1..20) {
            val gas = Gas(oxygenFraction = percentage / 100.0, heliumFraction = 0.0)
            assertEquals("Hypoxic", gas.diveIndustryName(), "Expected Hypoxic for $percentage% O2")
        }
    }
}

private const val DOUBLE_TOLERANCE = 1e-3
