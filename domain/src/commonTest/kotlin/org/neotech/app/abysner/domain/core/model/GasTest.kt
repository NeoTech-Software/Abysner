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

package org.neotech.app.abysner.domain.core.model

import org.neotech.app.abysner.domain.tenthAtDecimalPoint
import kotlin.test.Test
import kotlin.test.assertEquals

class GasTest {

    @Test
    fun test() {
        assertEquals(
            5.98,
            Gas.Oxygen.oxygenMod(1.6, Environment.Default),
            DOUBLE_PRECISION_DELTA
        )

    }

    @Test
    fun nitrogenCalculation() {
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
}

private val DOUBLE_PRECISION_DELTA = tenthAtDecimalPoint(2)
