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

package org.neotech.app.abysner.domain.gasblending

import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.settings.model.GasBlendingSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BlendCostTest {

    /** Prices per liter at 1 bar, equivalent to 8.00, 45.00 and 1.00 per cubic meter. */
    private val prices = mapOf(Gas.Oxygen to 0.008, Gas.Helium to 0.045, Gas.Air to 0.001)

    private fun settings(
        trimixMode: Boolean = false,
        cylinderSize: Cylinder.Size = Cylinder.STEEL_12L,
        startGas: Gas = Gas.Air,
        startPressure: Double = 50.0,
        targetGas: Gas = Gas.Nitrox32,
        firstTopUpGas: Gas = Gas.Oxygen,
        finalTopUpGas: Gas = Gas.Air,
        gasPrices: Map<Gas, Double> = prices,
    ) = GasBlendingSettings(
        trimixMode = trimixMode,
        cylinderSize = cylinderSize,
        startGas = startGas,
        startPressure = startPressure,
        targetGas = targetGas,
        targetPressure = 200.0,
        firstTopUpGas = firstTopUpGas,
        finalTopUpGas = finalTopUpGas,
        gasPrices = gasPrices,
    )

    @Test
    fun total_isEachGasVolumeTimesItsPrice() {
        // 12 L cylinder, 50 bar air to 200 bar EAN32: 27.1636 L/L of oxygen and 117.4736 L/L of air.
        val expected = (27.1636457772 * 0.008 + 117.4735789411 * 0.001) * 12.0

        val cost = settings().blendCost()

        assertIs<BlendCost.Known>(cost)
        assertEquals(expected, cost.total, 1e-6)
    }

    @Test
    fun total_scalesWithCylinderSize() {
        val small = settings(cylinderSize = Cylinder.STEEL_10L).blendCost()
        val large = settings(cylinderSize = Cylinder.D12).blendCost()

        assertIs<BlendCost.Known>(small)
        assertIs<BlendCost.Known>(large)
        // D12 holds 24 L of water against the 10 L cylinder, so it costs 2.4 times as much.
        assertEquals(small.total * 2.4, large.total, 1e-6)
    }

    @Test
    fun missingPriceForAnyGasInvolved_reportsPriceMissing() {
        val cost = settings(gasPrices = mapOf(Gas.Air to 0.001)).blendCost()

        assertEquals(BlendCost.PriceMissing, cost)
    }

    @Test
    fun priceForAGasNotUsedInTheBlend_doesNotCount() {
        // Helium is priced but never used when blending nitrox, so it must not affect the total.
        val withHeliumPriced = settings().blendCost()
        val withoutHeliumPriced = settings(
            gasPrices = mapOf(Gas.Oxygen to 0.008, Gas.Air to 0.001)
        ).blendCost()

        assertEquals(withHeliumPriced, withoutHeliumPriced)
    }

    @Test
    fun unblendableCombination_reportsNotBlendable() {
        // EAN32 needs nitrogen, and neither top-up gas supplies any.
        val cost = settings(
            startPressure = 0.0,
            firstTopUpGas = Gas.Nitrox50,
            finalTopUpGas = Gas.Oxygen,
        ).blendCost()

        assertEquals(BlendCost.NotBlendable, cost)
    }

    @Test
    fun identicalTopUpGases_reportNotBlendable() {
        val cost = settings(firstTopUpGas = Gas.Air, finalTopUpGas = Gas.Air).blendCost()

        assertEquals(BlendCost.NotBlendable, cost)
    }

    @Test
    fun nothingToBlend_costsNothing() {
        val cost = settings(startGas = Gas.Nitrox32, startPressure = 200.0).blendCost()

        assertIs<BlendCost.Known>(cost)
        assertEquals(0.0, cost.total, 1e-9)
    }

    @Test
    fun candidateWithoutAPrice_reportsPriceMissing() {
        // Exactly what the gas picker shows for an option whose price has not been filled in.
        val cost = settings(firstTopUpGas = Gas.Nitrox80).blendCost()

        assertEquals(BlendCost.PriceMissing, cost)
    }

    @Test
    fun comparingTopUpGasChoices_ranksThemOnTotalCost() {
        // This is what the gas picker annotation does: substitute each candidate into a slot and see
        // what the whole blend would cost.
        //
        // Here each pre-mix is priced at what its own oxygen content would cost if blended from
        // oxygen and air. That particular assumption is what makes pure oxygen come out ahead, and
        // it does not hold for gas that is not made that way: see
        // [cheaplyProducedPreMix_beatsPureOxygen].
        val pricesWithPreMixes = prices + mapOf(Gas.Nitrox50 to 0.0045, Gas.Nitrox80 to 0.0066)

        fun costOf(candidate: Gas) =
            settings(firstTopUpGas = candidate, gasPrices = pricesWithPreMixes).blendCost()

        // Pure helium and air cannot make EAN32 together with air.
        assertEquals(BlendCost.NotBlendable, costOf(Gas.Helium))
        assertEquals(BlendCost.NotBlendable, costOf(Gas.Air))

        val oxygen = costOf(Gas.Oxygen)
        val nitrox80 = costOf(Gas.Nitrox80)
        val nitrox50 = costOf(Gas.Nitrox50)
        assertIs<BlendCost.Known>(oxygen)
        assertIs<BlendCost.Known>(nitrox80)
        assertIs<BlendCost.Known>(nitrox50)

        assertEquals(4.01739, oxygen.total, 1e-4)
        assertEquals(4.17982, nitrox80.total, 1e-4)
        assertEquals(4.84354, nitrox50.total, 1e-4)

        // With pre-mixes priced this way oxygen wins, because its oxygen content costs the same
        // either way while a pre-mix additionally displaces air that is nearly free. The closer a
        // pre-mix is to the target, the more air it displaces, hence the ordering.
        assertTrue(
            oxygen.total < nitrox80.total && nitrox80.total < nitrox50.total,
            "Unexpected ranking: $oxygen, $nitrox80, $nitrox50",
        )
    }

    @Test
    fun cheaplyProducedPreMix_beatsPureOxygen() {
        // Nitrox straight out of a membrane compressor costs little more than air, nothing like what
        // the same mix would cost blended from oxygen. Then the ranking flips, which is the whole
        // reason for showing a price per option rather than assuming oxygen is always the answer.
        val membranePrice = 0.0015
        val fairBlendedPrice = 0.0038

        fun costOf(price: Double) = settings(
            firstTopUpGas = Gas.Nitrox40,
            gasPrices = prices + mapOf(Gas.Nitrox40 to price),
        ).blendCost()

        val fromMembrane = costOf(membranePrice)
        val blendedFromOxygen = costOf(fairBlendedPrice)
        val pureOxygen = settings().blendCost()
        assertIs<BlendCost.Known>(fromMembrane)
        assertIs<BlendCost.Known>(blendedFromOxygen)
        assertIs<BlendCost.Known>(pureOxygen)

        assertEquals(2.4133, fromMembrane.total, 1e-4)
        assertEquals(5.5306, blendedFromOxygen.total, 1e-4)
        assertEquals(4.0174, pureOxygen.total, 1e-4)

        assertTrue(
            fromMembrane.total < pureOxygen.total,
            "Membrane EAN40 (${fromMembrane.total}) should beat oxygen (${pureOxygen.total})",
        )
        assertTrue(
            blendedFromOxygen.total > pureOxygen.total,
            "The same mix priced as if blended from oxygen should lose to oxygen",
        )
    }
}
