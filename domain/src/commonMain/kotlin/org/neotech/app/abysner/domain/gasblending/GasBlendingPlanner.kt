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

import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.physics.GasEquationOfStateModel
import org.neotech.app.abysner.domain.gasblending.model.BlendStep
import org.neotech.app.abysner.domain.gasblending.model.GasBlendingInput
import org.neotech.app.abysner.domain.gasblending.model.GasBlendingPlan
import kotlin.math.abs
import kotlin.math.min

/**
 * Computes how to blend a target gas mix from what is currently in a cylinder plus a set of top-up
 * gases, taking real-gas compressibility into account via [GasEquationOfStateModel].
 *
 * The whole calculation happens in "normal volume": liters of gas measured at 1 bar. That makes gas
 * mixing linear (volumes and their oxygen/helium content simply add up), so the required amount of
 * each top-up gas follows from a small system of balance equations. Only the conversion between
 * normal volume and cylinder pressure is non-linear, and that is exactly what the equation of state
 * model handles.
 *
 * Both the balance equations and the real-gas correction are standard. The correction is the same
 * polynomial fit Abysner already applies elsewhere, credited to Lubomir I. Ivanov in
 * [org.neotech.app.abysner.domain.core.physics.PolynomialRealGasModel].
 *
 * Results were validated against realblender (https://github.com/atdotde/realblender) by
 * Robert C. Helling, which solves the same problem on the same fit, and the two agree to every
 * digit realblender reports. Two things here go further than it does: a blend that cannot be
 * reached by topping up alone reports how far to drain first rather than simply failing, and a
 * helium imbalance is detected instead of being silently ignored.
 */
class GasBlendingPlanner(
    private val gasModel: GasEquationOfStateModel = GasEquationOfStateModel.Default,
) {

    fun plan(input: GasBlendingInput): GasBlendingPlan {
        val waterVolume = input.cylinderSize.waterVolume
        val targetVolume = gasModel.getGasVolume(input.targetGas, waterVolume, input.targetPressure)
        val startVolume = gasModel.getGasVolume(input.startGas, waterVolume, input.startPressure)

        if (isDegenerate(input.topUpGases)) {
            return GasBlendingPlan.DegenerateTopUpGases
        }

        val volumes = solve(input, startVolume, targetVolume)
        if (!isHeliumBalanced(input, startVolume, targetVolume, volumes)) {
            return GasBlendingPlan.HeliumNotBalanced
        }

        if (volumes.all { it >= -VOLUME_EPSILON }) {
            return buildPlan(input, waterVolume, startVolume, targetVolume, null, volumes)
        }

        // A negative top-up volume means the cylinder already holds more of something than the
        // target allows, so no amount of topping up can get there. Draining lowers every component
        // proportionally and may bring the blend back into reach.
        if (startVolume <= VOLUME_EPSILON) {
            // Nothing left to drain, so the target is simply out of reach with these gases.
            return GasBlendingPlan.ImpossibleWithTheseGases
        }

        val volumesWhenEmpty = solve(input, 0.0, targetVolume)

        // Each top-up volume is affine in the start volume: `v(x) = v(0) - slope * x`. A positive
        // slope means that gas runs out as the cylinder fills, which puts an upper bound on how much
        // may be left in the cylinder. Negative slopes instead impose a lower bound, which draining
        // can never satisfy, so the candidate is re-validated below rather than trusted.
        var upperBound = Double.POSITIVE_INFINITY
        volumesWhenEmpty.indices.forEach { index ->
            val slope = (volumesWhenEmpty[index] - volumes[index]) / startVolume
            if (slope > SLOPE_EPSILON) {
                upperBound = min(upperBound, volumesWhenEmpty[index] / slope)
            }
        }
        // An upper bound of exactly zero is still workable: it means draining the cylinder
        // completely. Only a negative bound is impossible, and anything else that does not work out
        // is caught by re-validating the solution below.
        if (upperBound < -VOLUME_EPSILON) {
            return GasBlendingPlan.ImpossibleWithTheseGases
        }

        val drainedVolume = min(upperBound, startVolume)
        val volumesAfterDrain = solve(input, drainedVolume, targetVolume)
        if (volumesAfterDrain.any { it < -VOLUME_EPSILON }) {
            return GasBlendingPlan.ImpossibleWithTheseGases
        }
        if (!isHeliumBalanced(input, drainedVolume, targetVolume, volumesAfterDrain)) {
            return GasBlendingPlan.HeliumNotBalanced
        }

        val drainToPressure = gasModel.getGasPressure(input.startGas, waterVolume, drainedVolume)
        return buildPlan(input, waterVolume, drainedVolume, targetVolume, drainToPressure, volumesAfterDrain)
    }

    private fun buildPlan(
        input: GasBlendingInput,
        waterVolume: Double,
        startVolume: Double,
        targetVolume: Double,
        drainToPressure: Double?,
        volumes: List<Double>,
    ): GasBlendingPlan.Success {

        var cumulativeVolume = startVolume
        var oxygenVolume = input.startGas.oxygenFraction * startVolume
        var heliumVolume = input.startGas.heliumFraction * startVolume

        val steps = buildList {
            input.topUpGases.forEachIndexed { index, gas ->
                val volume = volumes[index]

                // Boundary solutions produce exactly zero (or even -0.0) for a gas that turns out
                // not to be needed. Rendering those would tell the blender to top up to a pressure
                // the cylinder is already at.
                if (abs(volume) < VOLUME_EPSILON) {
                    return@forEachIndexed
                }

                cumulativeVolume += volume
                oxygenVolume += gas.oxygenFraction * volume
                heliumVolume += gas.heliumFraction * volume

                val mixAfter = gasOf(
                    oxygenFraction = oxygenVolume / cumulativeVolume,
                    heliumFraction = heliumVolume / cumulativeVolume,
                )
                add(
                    BlendStep(
                        gas = gas,
                        volume = volume,
                        mixAfter = mixAfter,
                        pressureAfter = gasModel.getGasPressure(mixAfter, waterVolume, cumulativeVolume),
                    )
                )
            }
        }

        return GasBlendingPlan.Success(
            drainToPressure = drainToPressure,
            startVolume = startVolume,
            targetVolume = targetVolume,
            steps = steps,
        )
    }

    /**
     * Returns true when the top-up gases cannot be distinguished by the balance equations, in which
     * case the system has no unique solution.
     */
    private fun isDegenerate(topUpGases: List<Gas>): Boolean = if (topUpGases.size == 2) {
        abs(topUpGases[0].oxygenFraction - topUpGases[1].oxygenFraction) < FRACTION_EPSILON
    } else {
        abs(determinant(topUpGases)) < DETERMINANT_EPSILON
    }

    /**
     * Solves for the normal volume of each top-up gas required to go from [startVolume] of the start
     * gas to [targetVolume] of the target gas. Results may be negative, which means the blend is not
     * reachable by topping up alone. Assumes the system is not degenerate.
     */
    private fun solve(input: GasBlendingInput, startVolume: Double, targetVolume: Double): List<Double> {
        val gases = input.topUpGases

        // Right hand side: how much of each component has to be added.
        val total = targetVolume - startVolume
        val oxygen = input.targetGas.oxygenFraction * targetVolume - input.startGas.oxygenFraction * startVolume

        if (gases.size == 2) {
            // Two equations: total volume and oxygen content.
            val first = (oxygen - gases[1].oxygenFraction * total) /
                    (gases[0].oxygenFraction - gases[1].oxygenFraction)
            return listOf(first, total - first)
        }

        // Three equations: helium, nitrogen and oxygen content. Nitrogen is used rather than total
        // volume so all three rows are component balances; the total follows from them anyway.
        val helium = input.targetGas.heliumFraction * targetVolume - input.startGas.heliumFraction * startVolume
        val nitrogen = input.targetGas.nitrogenFraction * targetVolume - input.startGas.nitrogenFraction * startVolume

        val denominator = determinant(gases)
        return List(3) { column ->
            determinantWithColumnReplaced(gases, column, helium, nitrogen, oxygen) / denominator
        }
    }

    /**
     * Two top-up gases balance total volume and oxygen only, so a helium imbalance would silently
     * produce a plan that does not actually reach the target mix. This detects that case.
     */
    private fun isHeliumBalanced(
        input: GasBlendingInput,
        startVolume: Double,
        targetVolume: Double,
        volumes: List<Double>,
    ): Boolean {
        if (input.topUpGases.size != 2) {
            return true
        }
        val required = input.targetGas.heliumFraction * targetVolume - input.startGas.heliumFraction * startVolume
        val supplied = input.topUpGases.indices.sumOf { input.topUpGases[it].heliumFraction * volumes[it] }
        return abs(required - supplied) < VOLUME_EPSILON
    }

    /**
     * Builds a [Gas] from fractions that may be a hair outside the valid range because of floating
     * point error, which [Gas] itself rejects. Oxygen is clamped first, since it is the
     * safety-relevant number and must never be silently reduced to make room for helium.
     */
    private fun gasOf(oxygenFraction: Double, heliumFraction: Double): Gas {
        val oxygen = oxygenFraction.coerceIn(0.0, 1.0)
        return Gas(
            oxygenFraction = oxygen,
            heliumFraction = heliumFraction.coerceIn(0.0, 1.0 - oxygen),
        )
    }
}

/**
 * Determinant of the component matrix of the three top-up gases, with helium, nitrogen and oxygen
 * as rows and one column per gas.
 */
private fun determinant(gases: List<Gas>): Double = determinant(
    gases[0].heliumFraction, gases[1].heliumFraction, gases[2].heliumFraction,
    gases[0].nitrogenFraction, gases[1].nitrogenFraction, gases[2].nitrogenFraction,
    gases[0].oxygenFraction, gases[1].oxygenFraction, gases[2].oxygenFraction,
)

/**
 * Determinant of the component matrix with [column] replaced by the right hand side, as needed by
 * Cramer's rule.
 */
private fun determinantWithColumnReplaced(
    gases: List<Gas>,
    column: Int,
    helium: Double,
    nitrogen: Double,
    oxygen: Double,
): Double {
    val he = DoubleArray(3) { gases[it].heliumFraction }
    val n2 = DoubleArray(3) { gases[it].nitrogenFraction }
    val o2 = DoubleArray(3) { gases[it].oxygenFraction }
    he[column] = helium
    n2[column] = nitrogen
    o2[column] = oxygen
    return determinant(
        he[0], he[1], he[2],
        n2[0], n2[1], n2[2],
        o2[0], o2[1], o2[2],
    )
}

private fun determinant(
    a: Double, b: Double, c: Double,
    d: Double, e: Double, f: Double,
    g: Double, h: Double, i: Double,
): Double = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g)

/**
 * Volumes (in liters at 1 bar) closer together than this are considered equal. Matches the precision
 * the equation of state model itself converges to.
 */
private const val VOLUME_EPSILON = 1e-6

/** Gas fractions closer together than this are considered equal. */
private const val FRACTION_EPSILON = 1e-9

private const val DETERMINANT_EPSILON = 1e-9

private const val SLOPE_EPSILON = 1e-12
