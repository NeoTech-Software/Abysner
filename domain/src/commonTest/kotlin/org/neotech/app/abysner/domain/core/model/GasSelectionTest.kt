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

package org.neotech.app.abysner.domain.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GasSelectionTest {

    private val environment = Environment.Default

    private fun cylinders(vararg gas: Gas) = gas.map { Cylinder.steel12Liter(it) }

    @Test
    fun findBestGas_returnsNullWhenAllGasesExceedMaxPPO2() {
        val cylinders = cylinders(Gas.Air, Gas.Nitrox32, Gas.Nitrox50, Gas.Oxygen)
        assertNull(cylinders.findBestGas(depth = 70.0, environment = environment, maxPpO2 = 1.6, maxEND = END_UNSPECIFIED))
    }

    @Test
    fun findBestGas_returnsHighestOxygenGasWithinModWhenAllSatisfyEnd() {
        val cylinders = cylinders(Gas.Air, Gas.Nitrox32, Gas.Nitrox50)
        assertEquals(Gas.Nitrox50, cylinders.findBestGas(depth = 20.0, environment = environment, maxPpO2 = 1.6, maxEND = 40.0)?.gas)
    }

    @Test
    fun findBestGas_excludesGasThatExceedsItsMod() {
        val cylinders = cylinders(Gas.Air, Gas.Nitrox32, Gas.Nitrox50)
        assertEquals(Gas.Nitrox32, cylinders.findBestGas(depth = 25.0, environment = environment, maxPpO2 = 1.6, maxEND = 40.0)?.gas)
    }

    @Test
    fun findBestGas_prefersIdealCandidateOverFallback() {
        // Air END is 40 meter and exceeds the maxEND, however Trimix2135 is belo the maxEND (about 22 meters)
        val cylinders = cylinders(Gas.Air, Gas.Trimix2135)
        assertEquals(Gas.Trimix2135, cylinders.findBestGas(depth = 40.0, environment = environment, maxPpO2 = 1.6, maxEND = 30.0)?.gas)
    }

    @Test
    fun findBestGas_returnsHighestOxygenWithinModWhenNoGasSatisfiesEnd() {
        val cylinders = cylinders(Gas.Air, Gas.Nitrox32, Gas.Nitrox50)
        assertEquals(Gas.Nitrox32, cylinders.findBestGas(depth = 40.0, environment = environment, maxPpO2 = 1.6, maxEND = 20.0)?.gas)
    }

    @Test
    fun findBestGas_prefersHigherHeliumWhenOxygenFractionsAreEqualAndEndIsUnspecified() {
        val cylinders = cylinders(Gas.Air, Gas.Trimix2135)
        assertEquals(Gas.Trimix2135, cylinders.findBestGas(depth = 50.0, environment = environment, maxPpO2 = 1.6, maxEND = END_UNSPECIFIED)?.gas)
    }

    @Test
    fun findBestGas_prefersHigherHeliumWhenOxygenFractionsAreEqualAndNoGasSatisfiesEnd() {
        val cylinders = cylinders(Gas.Air, Gas.Trimix2135)
        assertEquals(Gas.Trimix2135, cylinders.findBestGas(depth = 50.0, environment = environment, maxPpO2 = 1.6, maxEND = 20.0)?.gas)
    }

    @Test
    fun findBreathableFallbackGas_returnsLowestOxygenAmongHyperoxicGases() {
        val cylinders = cylinders(Gas.Nitrox50, Gas.Nitrox32)
        assertEquals(Gas.Nitrox32, cylinders.findBreathableFallbackGas(depth = 10.0, environment = environment)?.gas)
    }

    @Test
    fun findBreathableFallbackGas_returnsHighestOxygenWhenAllGasesAreHypoxic() {
        val cylinders = cylinders(Gas.Trimix1070, Gas.Trimix1555)
        assertEquals(Gas.Trimix1555, cylinders.findBreathableFallbackGas(depth = 0.0, environment = environment)?.gas)
    }

    @Test
    fun findBreathableFallbackGas_ignoresHypoxicGasesWhenNonHypoxicAvailable() {
        val cylinders = cylinders(Gas.Trimix1070, Gas.Trimix1555, Gas.Trimix1845, Gas.Trimix2135)
        assertEquals(Gas.Trimix1845, cylinders.findBreathableFallbackGas(depth = 0.0, environment = environment)?.gas)
    }

    @Test
    fun findBreathableFallbackGas_respectsCustomMinPPO2() {
        val cylinders = cylinders(Gas.Air, Gas.Nitrox50)
        assertEquals(Gas.Nitrox50, cylinders.findBreathableFallbackGas(depth = 10.0, environment = environment, minPPO2 = 0.50)?.gas)
    }

    @Test
    fun findBetterGasOrFallback_returnsBestGasWhenAvailable() {
        val currentCylinder = Cylinder.steel12Liter(Gas.Air)
        val cylinders = cylinders(Gas.Air, Gas.Nitrox32, Gas.Nitrox50)
        assertEquals(Gas.Nitrox50, cylinders.findBetterGasOrFallback(currentCylinder = currentCylinder, depth = 20.0, environment = environment, maxPPO2 = 1.6, maxEND = 40.0)?.gas)
    }

    @Test
    fun findBetterGasOrFallback_fallsBackWhenNoBestGasAvailable() {
        // All gases exceed maxPPO2 at 70 meter, fallback returns Air as the lowest O2 non-hypoxic gas.
        // Current gas is Nitrox32 (higher O2 than Air), so Air is genuinely a better (less toxic) option.
        val currentCylinder = Cylinder.steel12Liter(Gas.Nitrox32)
        val cylinders = cylinders(Gas.Air, Gas.Nitrox32, Gas.Nitrox50, Gas.Oxygen)
        assertEquals(Gas.Air, cylinders.findBetterGasOrFallback(currentCylinder = currentCylinder, depth = 70.0, environment = environment, maxPPO2 = 1.6, maxEND = END_UNSPECIFIED)?.gas)
    }

    @Test
    fun findBetterGasOrFallback_staysOnCurrentGasWhenFallbackIsNotBetter() {
        // At 30m Nitrox50 is outside its MOD, so findBestGas returns null. The fallback would
        // return Nitrox50, but Air already has lower O2, so the current gas (Air) is returned.
        val currentCylinder = Cylinder.steel12Liter(Gas.Air)
        val cylinders = cylinders(Gas.Nitrox50)
        assertEquals(Gas.Air, cylinders.findBetterGasOrFallback(currentCylinder = currentCylinder, depth = 30.0, environment = environment, maxPPO2 = 1.6, maxEND = END_UNSPECIFIED)?.gas)
    }
}

private const val END_UNSPECIFIED = Double.MAX_VALUE
