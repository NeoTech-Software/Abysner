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
import org.neotech.app.abysner.domain.gasblending.model.GasBlendingInput
import org.neotech.app.abysner.domain.gasblending.model.GasBlendingPlan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Reference values come from realblender (https://github.com/atdotde/realblender), which reports its
 * results per liter of cylinder volume. Using a 1 liter cylinder here makes the numbers directly
 * comparable.
 *
 * Volumes and gas fractions are closed-form and reproduce to machine precision. Pressures go through
 * the equation of state model's iterative solver, which converges on a volume residual of 1e-6, so
 * they are only asserted to [PRESSURE_TOLERANCE].
 */
class GasBlendingPlannerTest {

    private val planner = GasBlendingPlanner()

    private fun input(
        startGas: Gas = Gas.Air,
        startPressure: Double = 0.0,
        targetGas: Gas = Gas.Nitrox32,
        targetPressure: Double = 200.0,
        topUpGases: List<Gas> = listOf(Gas.Oxygen, Gas.Air),
        waterVolume: Double = 1.0,
    ) = GasBlendingInput(
        cylinderSize = Cylinder.Size(waterVolume = waterVolume, workingPressure = 300.0),
        startGas = startGas,
        startPressure = startPressure,
        targetGas = targetGas,
        targetPressure = targetPressure,
        topUpGases = topUpGases,
    )

    private fun success(input: GasBlendingInput): GasBlendingPlan.Success {
        val plan = planner.plan(input)
        assertIs<GasBlendingPlan.Success>(plan, "Expected a workable plan but got $plan")
        return plan
    }

    @Test
    fun nitrox_matchesRealblenderReference() {
        // 50 bar of air topped up with oxygen and then air, to 200 bar of EAN32.
        val plan = success(input(startGas = Gas.Air, startPressure = 50.0))

        assertEquals(50.4471404086, plan.startVolume, VOLUME_TOLERANCE)
        assertEquals(195.0843651268, plan.targetVolume, VOLUME_TOLERANCE)

        assertEquals(2, plan.steps.size)
        assertEquals(Gas.Oxygen, plan.steps[0].gas)
        assertEquals(27.1636457772, plan.steps[0].volume, VOLUME_TOLERANCE)
        assertEquals(0.486498682961, plan.steps[0].mixAfter.oxygenFraction, FRACTION_TOLERANCE)
        assertEquals(76.14794, plan.steps[0].pressureAfter, PRESSURE_TOLERANCE)

        assertEquals(Gas.Air, plan.steps[1].gas)
        assertEquals(117.4735789411, plan.steps[1].volume, VOLUME_TOLERANCE)
    }

    @Test
    fun trimix_matchesRealblenderReference() {
        // 20 bar of air topped up with oxygen, helium and then air, to 200 bar of trimix 18/45.
        val plan = success(
            input(
                startGas = Gas.Air,
                startPressure = 20.0,
                targetGas = Gas.Trimix1845,
                topUpGases = listOf(Gas.Oxygen, Gas.Helium, Gas.Air),
            )
        )

        assertEquals(20.1007913784, plan.startVolume, VOLUME_TOLERANCE)
        assertEquals(189.4482072479, plan.targetVolume, VOLUME_TOLERANCE)

        assertEquals(3, plan.steps.size)
        assertEquals(15.4676067943, plan.steps[0].volume, VOLUME_TOLERANCE)
        assertEquals(85.2516932615, plan.steps[1].volume, VOLUME_TOLERANCE)
        assertEquals(68.6281158136, plan.steps[2].volume, VOLUME_TOLERANCE)

        assertEquals(0.55354680, plan.steps[0].mixAfter.oxygenFraction, FRACTION_TOLERANCE)
        assertEquals(0.0, plan.steps[0].mixAfter.heliumFraction, FRACTION_TOLERANCE)
        assertEquals(35.090273, plan.steps[0].pressureAfter, PRESSURE_TOLERANCE)

        assertEquals(0.16295943, plan.steps[1].mixAfter.oxygenFraction, FRACTION_TOLERANCE)
        assertEquals(0.70560858, plan.steps[1].mixAfter.heliumFraction, FRACTION_TOLERANCE)
        assertEquals(125.155448, plan.steps[1].pressureAfter, PRESSURE_TOLERANCE)
    }

    @Test
    fun scalesWithCylinderVolume() {
        val perLiter = success(input(startPressure = 50.0, waterVolume = 1.0))
        val twelveLiter = success(input(startPressure = 50.0, waterVolume = 12.0))

        assertEquals(perLiter.steps.size, twelveLiter.steps.size)
        perLiter.steps.indices.forEach { index ->
            assertEquals(
                perLiter.steps[index].volume * 12.0,
                twelveLiter.steps[index].volume,
                VOLUME_TOLERANCE * 12.0,
            )
            // Pressures are independent of cylinder size.
            assertEquals(
                perLiter.steps[index].pressureAfter,
                twelveLiter.steps[index].pressureAfter,
                PRESSURE_TOLERANCE,
            )
        }
    }

    @Test
    fun sumOfTopUpVolumes_equalsTargetMinusStart() {
        blendingScenarios().forEach { input ->
            val plan = success(input)
            assertEquals(
                plan.targetVolume - plan.startVolume,
                plan.totalAdded,
                VOLUME_TOLERANCE,
                "Volume is not conserved for $input",
            )
        }
    }

    @Test
    fun finalStep_reachesTargetGasAtTargetPressure() {
        blendingScenarios().forEach { input ->
            val plan = success(input)
            val last = plan.steps.lastOrNull() ?: return@forEach
            assertEquals(input.targetPressure, last.pressureAfter, PRESSURE_TOLERANCE, "for $input")
            assertEquals(
                input.targetGas.oxygenFraction,
                last.mixAfter.oxygenFraction,
                FRACTION_TOLERANCE,
                "for $input",
            )
            assertEquals(
                input.targetGas.heliumFraction,
                last.mixAfter.heliumFraction,
                FRACTION_TOLERANCE,
                "for $input",
            )
        }
    }

    @Test
    fun allStepsAddGas_andPressureOnlyIncreases() {
        blendingScenarios().forEach { input ->
            val plan = success(input)
            var previousPressure = 0.0
            plan.steps.forEach { step ->
                assertTrue(step.volume > 0.0, "Step adds no gas in $input")
                assertTrue(
                    step.pressureAfter > previousPressure,
                    "Pressure does not increase monotonically in $input",
                )
                previousPressure = step.pressureAfter
            }
        }
    }

    @Test
    fun cylinderHoldingTooMuchOxygen_isDrainedFirst() {
        // 100 bar of EAN50 cannot be topped up to 200 bar of EAN32: it already holds more oxygen
        // than the target allows, so it has to come down to 72.58 bar first.
        val plan = success(input(startGas = Gas.Nitrox50, startPressure = 100.0))

        assertEquals(72.5836, plan.drainToPressure!!, PRESSURE_TOLERANCE)

        // Oxygen is not needed at all after draining, so it must not show up as a no-op step.
        assertEquals(1, plan.steps.size)
        assertEquals(Gas.Air, plan.steps[0].gas)
        assertEquals(121.0868473201, plan.steps[0].volume, VOLUME_TOLERANCE)
        assertEquals(200.0, plan.steps[0].pressureAfter, PRESSURE_TOLERANCE)
        assertEquals(0.32, plan.steps[0].mixAfter.oxygenFraction, FRACTION_TOLERANCE)
    }

    @Test
    fun targetPressureBelowStart_isAPlainDrain() {
        val plan = success(input(startGas = Gas.Air, startPressure = 200.0, targetGas = Gas.Air, targetPressure = 100.0))

        assertEquals(100.0, plan.drainToPressure!!, PRESSURE_TOLERANCE)
        assertTrue(plan.steps.isEmpty(), "Expected no top-up steps but got ${plan.steps}")
    }

    @Test
    fun zeroTargetPressure_drainsCompletely() {
        // Reachable from the UI, since the pressure fields allow 0. Emptying the cylinder is a
        // perfectly good answer here, so it must not be reported as impossible.
        val plan = success(input(startGas = Gas.Air, startPressure = 200.0, targetPressure = 0.0))

        assertEquals(0.0, plan.drainToPressure!!, PRESSURE_TOLERANCE)
        assertTrue(plan.steps.isEmpty(), "Expected no top-up steps but got ${plan.steps}")
    }

    @Test
    fun zeroTargetAndZeroStartPressure_needsNothing() {
        val plan = success(input(startPressure = 0.0, targetPressure = 0.0))

        assertEquals(null, plan.drainToPressure)
        assertTrue(plan.steps.isEmpty(), "Expected no top-up steps but got ${plan.steps}")
    }

    @Test
    fun cylinderAlreadyAtTarget_needsNothing() {
        val plan = success(input(startGas = Gas.Nitrox32, startPressure = 200.0))

        assertEquals(null, plan.drainToPressure)
        assertTrue(plan.steps.isEmpty(), "Expected no top-up steps but got ${plan.steps}")
    }

    @Test
    fun emptyCylinderAndNoNitrogenSource_isImpossible() {
        // EAN32 needs nitrogen, but neither top-up gas supplies any.
        val plan = planner.plan(
            input(startPressure = 0.0, topUpGases = listOf(Gas.Nitrox50, Gas.Oxygen))
        )
        assertEquals(GasBlendingPlan.ImpossibleWithTheseGases, plan)
    }

    @Test
    fun identicalTopUpGases_areDegenerate() {
        val plan = planner.plan(input(topUpGases = listOf(Gas.Air, Gas.Air)))
        assertEquals(GasBlendingPlan.DegenerateTopUpGases, plan)
    }

    @Test
    fun threeHeliumFreeTopUpGases_areDegenerate() {
        // Without a helium source the helium row of the matrix is all zeros.
        val plan = planner.plan(
            input(
                targetGas = Gas.Trimix1845,
                topUpGases = listOf(Gas.Oxygen, Gas.Nitrox32, Gas.Air),
            )
        )
        assertEquals(GasBlendingPlan.DegenerateTopUpGases, plan)
    }

    @Test
    fun heliumWithOnlyTwoTopUpGases_isReportedRatherThanIgnored() {
        // Two gases can only balance total volume and oxygen, so the helium already in the cylinder
        // would silently go unaccounted for.
        val plan = planner.plan(
            input(
                startGas = Gas.Trimix1845,
                startPressure = 50.0,
                targetGas = Gas.Nitrox32,
                topUpGases = listOf(Gas.Oxygen, Gas.Air),
            )
        )
        assertEquals(GasBlendingPlan.HeliumNotBalanced, plan)
    }

    @Test
    fun anyMixCombination_neverThrows() {
        // Guards the clamping of intermediate mixes: Gas rejects fractions outside 0..1, which
        // floating point error can produce.
        val topUpGases = listOf(Gas.Oxygen, Gas.Helium, Gas.Air)
        var planned = 0
        (0..100 step 5).forEach { oxygen ->
            (0..(100 - oxygen) step 5).forEach { helium ->
                listOf(0.0, 50.0, 150.0).forEach { startPressure ->
                    val plan = planner.plan(
                        input(
                            startPressure = startPressure,
                            targetGas = Gas(oxygen / 100.0, helium / 100.0),
                            topUpGases = topUpGases,
                        )
                    )
                    if (plan is GasBlendingPlan.Success) {
                        planned++
                        plan.steps.forEach { step ->
                            assertTrue(
                                step.mixAfter.oxygenFraction in 0.0..1.0 &&
                                        step.mixAfter.heliumFraction in 0.0..1.0 &&
                                        !step.pressureAfter.isNaN(),
                                "Nonsensical step $step for target $oxygen/$helium",
                            )
                        }
                    }
                }
            }
        }
        assertTrue(planned > 100, "Expected most combinations to be blendable, got $planned")
    }

    @Test
    fun volumePerGas_sumsRepeatedGases() {
        // Air is both the second and the final top-up gas here.
        val plan = success(
            input(
                startGas = Gas.Air,
                startPressure = 20.0,
                targetGas = Gas.Trimix3030,
                topUpGases = listOf(Gas.Oxygen, Gas.Helium, Gas.Air),
            )
        )
        val perGas = plan.volumePerGas
        assertEquals(plan.totalAdded, perGas.values.sum(), VOLUME_TOLERANCE)
        perGas.forEach { (gas, volume) ->
            assertEquals(
                plan.steps.filter { it.gas == gas }.sumOf { it.volume },
                volume,
                VOLUME_TOLERANCE,
            )
        }
    }

    /**
     * A spread of blends that are all reachable by topping up, for the invariant tests.
     */
    private fun blendingScenarios(): List<GasBlendingInput> = listOf(
        input(startPressure = 0.0),
        input(startGas = Gas.Air, startPressure = 50.0),
        input(startGas = Gas.Air, startPressure = 30.0, targetGas = Gas.Nitrox36, targetPressure = 232.0),
        input(startGas = Gas.Nitrox28, startPressure = 40.0, targetGas = Gas.Nitrox50, targetPressure = 200.0),
        input(
            startGas = Gas.Air,
            startPressure = 20.0,
            targetGas = Gas.Trimix1845,
            topUpGases = listOf(Gas.Oxygen, Gas.Helium, Gas.Air),
        ),
        input(
            startGas = Gas.Air,
            startPressure = 0.0,
            targetGas = Gas.Trimix2135,
            targetPressure = 232.0,
            topUpGases = listOf(Gas.Oxygen, Gas.Helium, Gas.Air),
        ),
        input(startPressure = 50.0, waterVolume = 12.0),
    )

    private companion object {
        const val VOLUME_TOLERANCE = 1e-7
        const val FRACTION_TOLERANCE = 1e-7
        const val PRESSURE_TOLERANCE = 1e-4
    }
}
