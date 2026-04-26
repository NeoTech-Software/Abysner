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

package org.neotech.app.abysner.domain.decompression.algorithm.buhlmann

import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.physics.metersToAmbientPressure
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BuhlmannUtilitiesTest {

    @Test
    fun waterVapourPressureInBars_returnsExpectedValueAt37Celsius() {
        assertEquals(0.0625993025768047, waterVapourPressureInBars(37.0))
    }

    /**
     * A diver ascending from a deco stop with a 1.3 bar setpoint. Once ambient drops below
     * 1.3 bar (around 3 meters), the setpoint can no longer be maintained and the loop
     * transitions to pure O2, so no inert gas is inspired.
     */
    @Test
    fun ccrSchreinerInputs_returnsZeroWhenAmbientBelowSetpoint() {
        val (inspiredGasPressure, inspiredGasRate) = ccrSchreinerInputs(
            startPressure = Environment.SeaLevelFresh.atmosphericPressure,
            pressureRate = 0.3,
            inertFraction = 0.79,
            oxygenFractionDiluent = 0.21,
            setpoint = 1.3,
        )
        assertEquals(0.0, inspiredGasPressure)
        assertEquals(0.0, inspiredGasRate)
    }

    /**
     * A diver beginning descent at the surface (assume 1.0 bar) with a 1.0 bar setpoint. Ambient
     * pressure is exactly equal the setpoint so no diluent is needed yet, giving an
     * inspiredGasPressure of 0. However, inspiredGasRate should remain non-zero because as the
     * diver descends further, ambient will exceed the setpoint and inert gas will begin to appear
     * in the loop.
     */
    @Test
    fun ccrSchreinerInputs_ambientAtSetpointYieldsZeroInspiredPressureButNonZeroRate() {
        val (inspiredGasPressure, inspiredGasRate) = ccrSchreinerInputs(
            startPressure = 1.0,
            pressureRate = 1.8,
            inertFraction = 0.20,
            oxygenFractionDiluent = 0.10,
            setpoint = 1.0,
        )
        assertEquals(0.0, inspiredGasPressure)
        assertEquals(0.20 * 1.8 / (1.0 - 0.10), inspiredGasRate, absoluteTolerance = 1e-15)
    }

    @Test
    fun ccrSchreinerInputs_throwsWhenDiluentIsPureOxygen() {
        assertFailsWith<IllegalArgumentException> {
            ccrSchreinerInputs(
                startPressure = 5.0,
                pressureRate = 1.8,
                inertFraction = 0.0,
                oxygenFractionDiluent = 1.0,
                setpoint = 1.3,
            )
        }
    }

    @Test
    fun ccrSchreinerInputs_throwsWhenInertFractionOutOfRange() {
        assertFailsWith<IllegalArgumentException> {
            ccrSchreinerInputs(
                startPressure = 5.0,
                pressureRate = 1.8,
                inertFraction = 1.1,
                oxygenFractionDiluent = 0.21,
                setpoint = 1.3,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            ccrSchreinerInputs(
                startPressure = 5.0,
                pressureRate = 1.8,
                inertFraction = -0.1,
                oxygenFractionDiluent = 0.21,
                setpoint = 1.3,
            )
        }
    }

    /**
     * Verifies that [ccrSchreinerInputs] fed into [schreinerEquation] produces the same result as
     * the independent CCR Schreiner equation, to a high degree of precision.
     *
     * The scenario used here is based on the one found in this forum post:
     * https://scubaboard.com/community/threads/schreiner-equations-for-ccr.554316/post-8154670
     *
     * Scenario:
     * Descent to 90 meter at 18 meter/min using a 10/70 diluent with a 1.0 setpoint
     *
     * Note: No water vapor correction, since that responsibility lies elsewhere in the code base.
     *
     * Tests the same four ZH-16C N2 compartments (half-times 5.0, 18.5, 54.3, 635.0 min) as found
     * in the forum post.
     */
    @Test
    fun ccrSchreinerInputs_matchesHellingEquation() {
        val oxygenFractionDiluent = 0.10
        val nitrogenFractionDiluent = 0.20
        val setpoint = 1.0
        val surfacePressure = Environment.SeaLevelFresh.atmosphericPressure
        val endPressure = metersToAmbientPressure(90.0, Environment.SeaLevelFresh).value
        val time = 5.0
        val pressureRate = (endPressure - surfacePressure) / time
        val initialNitrogenPressure = 0.79 * surfacePressure
        val fractionOfInert = nitrogenFractionDiluent / (1.0 - oxygenFractionDiluent)

        val halfTimes = listOf(5.0, 18.5, 54.3, 635.0)

        val (inspiredGasPressure, inspiredGasRate) = ccrSchreinerInputs(
            startPressure = surfacePressure,
            pressureRate = pressureRate,
            inertFraction = nitrogenFractionDiluent,
            oxygenFractionDiluent = oxygenFractionDiluent,
            setpoint = setpoint,
        )

        for (halfTime in halfTimes) {
            val transformedSchreiner = schreinerEquation(
                initialTissuePressure = initialNitrogenPressure,
                inspiredGasPressure = inspiredGasPressure,
                time = time,
                halfTime = halfTime,
                inspiredGasRate = inspiredGasRate,
            )
            val helling = hellingCcrEquation(
                initialTissuePressure = initialNitrogenPressure,
                fractionOfInert = fractionOfInert,
                startPressure = surfacePressure,
                pressureRate = pressureRate,
                setpoint = setpoint,
                halfTime = halfTime,
                time = time,
            )

            assertEquals(transformedSchreiner, helling, absoluteTolerance = 1e-12)
        }
    }

    /**
     * Verifies that [ccrSchreinerInputs] fed into [schreinerEquation] converges to the
     * same result as a fine-grained iterative Haldane simulation (60,000 steps/min).
     *
     * Same scenario as [ccrSchreinerInputs_matchesHellingEquation].
     * The iterative approach is an independent numerical method with no shared code.
     * Agreement to ~1e-5 confirms the analytic CCR transform is correct.
     */
    @Test
    fun ccrSchreinerInputs_matchesIterativeHaldaneGroundTruth() {
        val oxygenFractionDiluent = 0.10
        val nitrogenFractionDiluent = 0.20
        val setpoint = 1.0
        val surfacePressure = Environment.SeaLevelFresh.atmosphericPressure
        val endPressure = metersToAmbientPressure(90.0, Environment.SeaLevelFresh).value
        val time = 5.0
        val pressureRate = (endPressure - surfacePressure) / time
        val initialNitrogenPressure = 0.79 * surfacePressure
        val fractionOfInert = nitrogenFractionDiluent / (1.0 - oxygenFractionDiluent)

        val halfTimes = listOf(5.0, 18.5, 54.3, 635.0)

        val (inspiredGasPressure, inspiredGasRate) = ccrSchreinerInputs(
            startPressure = surfacePressure,
            pressureRate = pressureRate,
            inertFraction = nitrogenFractionDiluent,
            oxygenFractionDiluent = oxygenFractionDiluent,
            setpoint = setpoint,
        )

        for (halfTime in halfTimes) {
            val transformedSchreiner = schreinerEquation(
                initialTissuePressure = initialNitrogenPressure,
                inspiredGasPressure = inspiredGasPressure,
                time = time,
                halfTime = halfTime,
                inspiredGasRate = inspiredGasRate,
            )
            val iterative = iterativeHaldaneCcr(
                initialTissuePressure = initialNitrogenPressure,
                fractionOfInert = fractionOfInert,
                startPressure = surfacePressure,
                pressureRate = pressureRate,
                setpoint = setpoint,
                halfTime = halfTime,
                time = time,
            )

            assertEquals(transformedSchreiner, iterative, absoluteTolerance = 1e-5)
        }
    }

    /**
     * Direct implementation of the Helling blog CCR Schreiner equation. Used to verify
     * the more indirect form used in code.
     *
     * Used as an independent ground truth reference.
     *
     * Original blog equation (The Theoretical Diver, Helling, 2017):
     *
     * ```
     * p_t = (f_i / (1 - f_O2)) * (e^(-gamma*t) - 1) * (p_s + (v * g * rho) / gamma)
     *     + e^(-gamma*t) * (p_0 - (f_i / (1 - f_O2)) * (p_surf + d_0 * g * rho))
     *     + (f_i / (1 - f_O2)) * (p_surf + d * g * rho)
     * ```
     *
     * Where:
     *  - p_t = Tissue inert gas pressure after time `t`
     *  - p_0 = Initial tissue inert gas pressure
     *  - p_s = Setpoint
     *  - gamma = Tissue compartment time constant (`ln(2) / halfTime`)
     *
     * For my own sanity I'm substituting some of the equation for a more "engineering" naming scheme:
     *  - [initialTissuePressure] = `p_0`
     *  - [setpoint] = `p_s`
     *  - [pressureRate] = `v * g * rho` (bar/min)
     *  - [startPressure] = `p_surf + d_0 * g * rho` (absolute ambient at start)
     *  - [fractionOfInert] = `f_i / (1 - f_O2)`
     *  - [time] = `t`
     *  - timeConstant = `gamma`
     *  - exponentialDecay = `e^(-timeConstant * t)`
     *
     * This gives the following rewritten form:
     *
     * ```
     * resultTissuePressure = fractionOfInert * (exponentialDecay - 1) * (setpoint + pressureRate / timeConstant)
     *     + exponentialDecay * (initialTissuePressure - fractionOfInert * startPressure)
     *     + fractionOfInert * (startPressure + pressureRate * time)
     * ```
     *
     * Water vapor is handled by replacing the setpoint (`p_s`) with `setpoint + waterVaporPressure`,
     * as stated in the blog update. This function takes the setpoint as-is, it is the callers
     * responsibility to adjust for waterVaporPressure this is inline with the code base.
     *
     * Source: https://thetheoreticaldiver.org/wordpress/index.php/2017/11/30/ccr-schreiner-equation/
     */
    private fun hellingCcrEquation(
        initialTissuePressure: Double,
        fractionOfInert: Double,
        startPressure: Double,
        pressureRate: Double,
        setpoint: Double,
        halfTime: Double,
        time: Double,
    ): Double {
        val timeConstant = ln(2.0) / halfTime
        val exponentialDecay = exp(-timeConstant * time)
        return (fractionOfInert * (exponentialDecay - 1.0) * (setpoint + pressureRate / timeConstant)
                + exponentialDecay * (initialTissuePressure - fractionOfInert * startPressure)
                + fractionOfInert * (startPressure + pressureRate * time))
    }

    /**
     * Iterative approach to CCR tissue loading (Haldane approximation). With a sufficiently fine
     * step size this approximates the analytic solution (the one provided by Helling) closely, but
     * floating-point accumulation errors prevent true convergence at as steps become too small.
     *
     * Used as an independent ground truth reference.
     */
    private fun iterativeHaldaneCcr(
        initialTissuePressure: Double,
        fractionOfInert: Double,
        startPressure: Double,
        pressureRate: Double,
        setpoint: Double,
        halfTime: Double,
        time: Double,
        stepsPerMinute: Int = 60_000,
    ): Double {
        val timeStep = 1.0 / stepsPerMinute
        val totalSteps = (time * stepsPerMinute).toInt()
        var tissuePressure = initialTissuePressure
        val timeConstant = ln(2.0) / halfTime

        for (step in 0 until totalSteps) {
            val currentPressure = startPressure + pressureRate * step * timeStep
            val inertGasPressure = max(0.0, currentPressure - setpoint) * fractionOfInert
            val decayFactor = 1.0 - exp(-timeConstant * timeStep)
            tissuePressure += (inertGasPressure - tissuePressure) * decayFactor
        }
        return tissuePressure
    }
}
