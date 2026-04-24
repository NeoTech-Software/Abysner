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

import org.neotech.app.abysner.domain.core.physics.metersToAmbientPressure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GasSelectionTest {

    @Test
    fun findBestGas_returnsNullWhenAllGasesExceedMaxPPO2() {
        val cylinders = cylinders(Gas.Air, Gas.Nitrox32, Gas.Nitrox50, Gas.Oxygen)
        val bestGas = cylinders.findBestGas(
            ambientPressure = ambient(meters = 70.0),
            maxPpO2 = 1.6,
            maxEquivalentNarcoticAmbientPressure = END_UNSPECIFIED
        )
        assertNull(bestGas)
    }

    @Test
    fun findBestGas_returnsHighestOxygenGasWithinModWhenAllSatisfyEnd() {
        val cylinders = cylinders(Gas.Air, Gas.Nitrox32, Gas.Nitrox50)
        val bestGas = cylinders.findBestGas(
            ambientPressure = ambient(meters = 20.0),
            maxPpO2 = 1.6,
            maxEquivalentNarcoticAmbientPressure = ambient(meters = 40.0)
        )
        assertEquals(Gas.Nitrox50, bestGas?.gas)
    }

    @Test
    fun findBestGas_excludesGasThatExceedsItsMod() {
        val cylinders = cylinders(Gas.Air, Gas.Nitrox32, Gas.Nitrox50)
        val bestGas = cylinders.findBestGas(
            ambientPressure = ambient(meters = 25.0),
            maxPpO2 = 1.6,
            maxEquivalentNarcoticAmbientPressure = ambient(meters = 40.0)
        )
        assertEquals(Gas.Nitrox32, bestGas?.gas)
    }

    @Test
    fun findBestGas_prefersIdealCandidateOverFallback() {
        // Air END is 40 meter and exceeds the maxEND, however Trimix2135 is below the maxEND (about 22 meters)
        val cylinders = cylinders(Gas.Air, Gas.Trimix2135)
        val bestGas = cylinders.findBestGas(
            ambientPressure = ambient(meters = 40.0),
            maxPpO2 = 1.6,
            maxEquivalentNarcoticAmbientPressure = ambient(meters = 30.0)
        )
        assertEquals(Gas.Trimix2135, bestGas?.gas)
    }

    @Test
    fun findBestGas_returnsHighestOxygenWithinModWhenNoGasSatisfiesEnd() {
        val cylinders = cylinders(Gas.Air, Gas.Nitrox32, Gas.Nitrox50)
        val bestGas = cylinders.findBestGas(
            ambientPressure = ambient(meters = 40.0),
            maxPpO2 = 1.6,
            maxEquivalentNarcoticAmbientPressure = ambient(meters = 20.0)
        )
        assertEquals(Gas.Nitrox32, bestGas?.gas)
    }

    @Test
    fun findBestGas_prefersHigherHeliumWhenOxygenFractionsAreEqualAndEndIsUnspecified() {
        val cylinders = cylinders(Gas.Air, Gas.Trimix2135)
        val bestGas = cylinders.findBestGas(
            ambientPressure = ambient(meters = 50.0),
            maxPpO2 = 1.6,
            maxEquivalentNarcoticAmbientPressure = END_UNSPECIFIED
        )
        assertEquals(Gas.Trimix2135, bestGas?.gas)
    }

    @Test
    fun findBestGas_prefersHigherHeliumWhenOxygenFractionsAreEqualAndNoGasSatisfiesEnd() {
        val cylinders = cylinders(Gas.Air, Gas.Trimix2135)
        val bestGas = cylinders.findBestGas(
            ambientPressure = ambient(meters = 50.0),
            maxPpO2 = 1.6,
            maxEquivalentNarcoticAmbientPressure = ambient(meters = 20.0)
        )
        assertEquals(Gas.Trimix2135, bestGas?.gas)
    }

    @Test
    fun findBreathableFallbackGas_returnsLowestOxygenAmongHyperoxicGases() {
        val cylinders = cylinders(Gas.Nitrox50, Gas.Nitrox32)
        assertEquals(Gas.Nitrox32, cylinders.findBreathableFallbackGas(ambientPressure = ambient(meters = 10.0))?.gas)
    }

    @Test
    fun findBreathableFallbackGas_returnsHighestOxygenWhenAllGasesAreHypoxic() {
        val cylinders = cylinders(Gas.Trimix1070, Gas.Trimix1555)
        assertEquals(Gas.Trimix1555, cylinders.findBreathableFallbackGas(ambientPressure = ambient(meters = 0.0))?.gas)
    }

    @Test
    fun findBreathableFallbackGas_ignoresHypoxicGasesWhenNonHypoxicAvailable() {
        val cylinders = cylinders(Gas.Trimix1070, Gas.Trimix1555, Gas.Trimix1845, Gas.Trimix2135)
        assertEquals(Gas.Trimix1845, cylinders.findBreathableFallbackGas(ambientPressure = ambient(meters = 0.0))?.gas)
    }

    @Test
    fun findBreathableFallbackGas_respectsCustomMinPPO2() {
        val cylinders = cylinders(Gas.Air, Gas.Nitrox50)
        assertEquals(Gas.Nitrox50, cylinders.findBreathableFallbackGas(ambientPressure = ambient(meters = 10.0), minPPO2 = 0.50)?.gas)
    }

    @Test
    fun findBetterGasOrFallback_returnsBestGasWhenAvailable() {
        val currentCylinder = Cylinder.steel12Liter(Gas.Air)
        val cylinders = cylinders(Gas.Air, Gas.Nitrox32, Gas.Nitrox50)
        val result = cylinders.findBetterGasOrFallback(
            currentCylinder = currentCylinder,
            ambientPressure = ambient(meters = 20.0),
            maxPPO2 = 1.6,
            maxEquivalentNarcoticAmbientPressure = ambient(meters = 40.0)
        )
        assertEquals(Gas.Nitrox50, result?.gas)
    }

    @Test
    fun findBetterGasOrFallback_fallsBackWhenNoBestGasAvailable() {
        // All gases exceed maxPPO2 at 70 meter, fallback returns Air as the lowest O2 non-hypoxic gas.
        // Current gas is Nitrox32 (higher O2 than Air), so Air is genuinely a better (less toxic) option.
        val currentCylinder = Cylinder.steel12Liter(Gas.Nitrox32)
        val cylinders = cylinders(Gas.Air, Gas.Nitrox32, Gas.Nitrox50, Gas.Oxygen)
        val result = cylinders.findBetterGasOrFallback(
            currentCylinder = currentCylinder,
            ambientPressure = ambient(meters = 70.0),
            maxPPO2 = 1.6,
            maxEquivalentNarcoticAmbientPressure = END_UNSPECIFIED
        )
        assertEquals(Gas.Air, result?.gas)
    }

    @Test
    fun findBetterGasOrFallback_staysOnCurrentGasWhenFallbackIsNotBetter() {
        // At 30m Nitrox50 is outside its MOD, so findBestGas returns null. The fallback would
        // return Nitrox50, but Air already has lower O2, so the current gas (Air) is returned.
        val currentCylinder = Cylinder.steel12Liter(Gas.Air)
        val cylinders = cylinders(Gas.Nitrox50)
        val result = cylinders.findBetterGasOrFallback(
            currentCylinder = currentCylinder,
            ambientPressure = ambient(meters = 30.0),
            maxPPO2 = 1.6,
            maxEquivalentNarcoticAmbientPressure = END_UNSPECIFIED
        )
        assertEquals(Gas.Air, result?.gas)
    }

    @Test
    fun findBestGas_usesExactModWhenToleranceIsZero() {
        // O2 in fresh water has a MOD of about 5.98 meter at ppO2 1.6.
        val cylinders = cylinders(Gas.Air, Gas.Oxygen)
        val result = cylinders.findBestGas(
            ambientPressure = ambient(meters = 6.0),
            maxPpO2 = 1.6,
            maxEquivalentNarcoticAmbientPressure = END_UNSPECIFIED,
            modTolerance = 0.0
        )
        assertEquals(Gas.Air, result?.gas)
    }

    @Test
    fun findBestGas_usesTolerantModByDefault() {
        // O2 in fresh water has a MOD of about 5.98 meter at ppO2 1.6.
        val cylinders = cylinders(Gas.Air, Gas.Oxygen)
        val result = cylinders.findBestGas(
            ambientPressure = ambient(meters = 6.0),
            maxPpO2 = 1.6,
            maxEquivalentNarcoticAmbientPressure = END_UNSPECIFIED
        )
        assertEquals(Gas.Oxygen, result?.gas)
    }

    private fun cylinders(vararg gas: Gas) = gas.map { Cylinder.steel12Liter(it) }

    private fun ambient(meters: Double, environment: Environment = Environment.Default): Double =
        metersToAmbientPressure(meters, environment).value
}

private const val END_UNSPECIFIED = Double.MAX_VALUE
