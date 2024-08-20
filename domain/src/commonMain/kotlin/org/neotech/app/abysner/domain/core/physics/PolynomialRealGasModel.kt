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

package org.neotech.app.abysner.domain.core.physics

import org.neotech.app.abysner.domain.core.model.Gas
import kotlin.math.abs

/**
 * Real gas model that is built on a combination of collective research done by many people, the raw
 * data is based on O2, N2 and He compressibility factors found in:
 *  - PERRY’S CHEMICAL ENGINEERS’ HANDBOOK SEVENTH EDITION (page 2-165)
 *  - VOLUMETRIC BEHAVIOR OF HELIUM-ARGON MIXTURES AT HIGH PRESSURE AND MODERATE TEMPERATURE (page 108)
 *
 * This data has been normalized to 300K and fitted to a curve by Lubomir I. Ivanov, as can be found here:
 *
 * https://web.archive.org/web/20240818221653/https://mailman.subsurface-divelog.org/hyperkitty/list/subsurface@subsurface-divelog.org/thread/6R4EOT5WP42TAP2YK2LB23JU6U2XGUW2/
 *
 * Lubomir originally did this work for Subsurface, I'm using the same coefficients here with his
 * written permission.
 */
class PolynomialRealGasModel(
    private val constantTimeCalculation: Boolean = false
): GasEquationOfStateModel {

    private inline fun virialO2(p: Double) = virial(p,-7.18092073703e-04, 2.81852572808e-06, -1.50290620492e-09)
    private inline fun virialHe(p: Double) = virial(p,4.87320026468e-04, -8.83632921053e-08, 5.33304543646e-11)
    private inline fun virialN2(p: Double) = virial(p, -2.19260353292e-04, 2.92844845532e-06, -2.07613482075e-09)

    private inline fun virial(p: Double, coefficientOne: Double, coefficientTwo: Double, coefficientThree: Double) =
        coefficientOne * p + coefficientTwo * p * p + coefficientThree * p * p * p

    private fun compressibilityFactor(pressure: Double, gas: Gas) =
        1.0 + gas.oxygenFraction * virialO2(pressure) + gas.heliumFraction * virialHe(pressure) + gas.nitrogenFraction * virialN2(pressure)

    private fun volumeCorrectionFactor(pressure: Double, gas: Gas) = (pressure * compressibilityFactor(1.0, gas)) / compressibilityFactor(pressure, gas)

    /**
     * Find the pressure when compressing the given volume of gas (at 1 atm) into a 1 liter
     * cylinder/space.
     */
    private fun approximatePressure(gas: Gas, volume: Double): Double {
        // TODO some clamping needs to be done here, this can get stuck on high pressures.

        // Note: volume serves a double purpose here, it is both the initial estimate (Boyle's Law:
        // `pressure = volume / 1.0` where 1.0 is the size of the cylinder). But it also still
        // serves as the actual volume of gas that is being compressed.
        var adjustedEstimatedPressure = volume
        val zFactorAtOneAtm = compressibilityFactor(1.0, gas)

        do {
            val currentVolume = zFactorAtOneAtm * adjustedEstimatedPressure
            val zFactorAtEstimatedPressure = compressibilityFactor(adjustedEstimatedPressure, gas)

            val estimatedVolume = zFactorAtEstimatedPressure * volume

            // Update the pressure estimate based on the target volume
            adjustedEstimatedPressure = volume * zFactorAtEstimatedPressure / zFactorAtOneAtm
        } while(abs(currentVolume - estimatedVolume) > VOLUME_PRECISION_DELTA)

        return adjustedEstimatedPressure
    }

    /**
     * Same as [approximatePressure] but with some extra mechanics to detect a stable oscillation.
     */
    private fun approximatePressureWithOscillationDetection(gas: Gas, volume: Double): Double {
        var estimatedPressure = volume
        val zFactorAtOneAtm = compressibilityFactor(1.0, gas)

        var previousDifferencePositive: Double = Double.MAX_VALUE
        var previousDifferenceNegative: Double = Double.MIN_VALUE
        do {
            val currentVolume = zFactorAtOneAtm * estimatedPressure
            val zFactorAtEstimatedPressure = compressibilityFactor(estimatedPressure, gas)

            val estimatedVolume = zFactorAtEstimatedPressure * volume

            // Update the pressure estimate based on the target volume
            estimatedPressure = volume * zFactorAtEstimatedPressure / zFactorAtOneAtm

           val difference = currentVolume - estimatedVolume

            if(difference > 0) {
                if(abs(previousDifferencePositive - difference) < 0.001) {
                    // Delta has not changed enough since previous either we have stabilized precise enough, or a stabilized oscillation may be going on.
                    return estimatedPressure
                }
                previousDifferencePositive = difference
            } else {
                if(abs(previousDifferenceNegative - difference) < 0.001) {
                    // Delta has not changed enough since previous either we have stabilized precise enough, or a stabilized oscillation may be going on.
                    return estimatedPressure
                }
                previousDifferenceNegative = difference
            }
        } while(abs(difference) > VOLUME_PRECISION_DELTA)
        return estimatedPressure
    }

    override fun getGasVolume(gas: Gas, cylinderSize: Double, pressure: Double): Double {
        return volumeCorrectionFactor(pressure, gas) * cylinderSize
    }

    override fun getGasPressure(gas: Gas, cylinderSize: Double, gasVolume: Double): Double {
        return if(constantTimeCalculation) {
            approximatePressureConstantTime(gas, cylinderSize, gasVolume)
        } else {
            approximatePressure(gas, gasVolume / cylinderSize)
        }
    }

    private fun approximatePressureConstantTime(gas: Gas, cylinderSize: Double, gasVolume: Double): Double {
        val p1 = 1.0  // Assuming initial pressure is 1 atm; adjust as needed
        val initialPressure = p1 * gasVolume / cylinderSize
        val idealPressure = initialPressure / compressibilityFactor(p1, gas)
        return idealPressure * compressibilityFactor(idealPressure, gas)
    }
}

/**
 * Delta at which to stop searching for a better pressure estimate
 */
private const val VOLUME_PRECISION_DELTA = 0.000001
