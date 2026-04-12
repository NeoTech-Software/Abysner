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

import org.neotech.app.abysner.domain.core.physics.asDegreesCelsiusToDegreesKelvin
import org.neotech.app.abysner.domain.core.physics.asDegreesKelvinToDegreesCelsius
import org.neotech.app.abysner.domain.core.physics.pascalToBar
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

/**
 * Returns the Water Vapor pressure in pascals based on the given temperature in degrees Kelvin.
 * Valid temperature range is 1.0-374.0 degrees Celsius (inclusive).
 *
 * Initial implementation is based on:
 * https://github.com/nyxtom/dive/blob/08b182cf7b00ab1878344a19f14d9d1ae9b219d0/lib/core.js#L378
 *
 * However, the implementation and constants have been modified to work with SI units instead.
 */
fun waterVapourPressure(degreesKelvin: Double): Double {
    // Below is an implementation of the Antoine Equation:
    // - https://en.wikipedia.org/wiki/Antoine_equation
    // - http://en.wikipedia.org/wiki/Vapour_pressure_of_water */

    val degreesCelsius = degreesKelvin.asDegreesKelvinToDegreesCelsius()
    require(degreesCelsius in 1.0..374.0) {
        "Temperature provided is outside supported range, use a temperature from 1.0 to 374.0 degrees Celsius (inclusive)."
    }

    // According to Wikipedia the valid range for water goes from 1 to 100 and from 99 to 374.
    // The two ranges overlap at 99-100; prefer the low-range constants there.
    val temperatureConstants = if (degreesCelsius <= 100.0) {
        ANTOINE_SI_CONSTANTS_FOR_WATER_1_TO_100
    } else {
        ANTOINE_SI_CONSTANTS_FOR_WATER_99_TO_374
    }
    val log10P = temperatureConstants[0] - (temperatureConstants[1] / (degreesKelvin + temperatureConstants[2]))
    return 10.0.pow(log10P)
}

// Below constants have been calculated with the following A, B and C constants in mmHg and Celsius:
//
// 1-100  = arrayOf(8.07131, 1730.63, 233.426) (A, B and C)
// 99-374 = arrayOf(8.14019, 1810.94, 244.485) (A, B and C)
//
// And the following formula (see: https://en.wikipedia.org/wiki/Antoine_equation#Units)
//
// 101325 Pa is equivalent to an absolute pressure of 760 mmHg
// val aPa = a + log10(101325.0 / 760.0)
// val cK = c - 273.15
private val ANTOINE_SI_CONSTANTS_FOR_WATER_1_TO_100 = arrayOf(10.196213020132939, 1730.63, -39.72399999999999)
private val ANTOINE_SI_CONSTANTS_FOR_WATER_99_TO_374 = arrayOf(10.26509302013294, 1810.94, -28.664999999999964)

internal fun waterVapourPressureInBars(degreesCelsius: Double): Double {
    return pascalToBar(waterVapourPressure(degreesCelsius.asDegreesCelsiusToDegreesKelvin()))
}

/**
 * Calculates the change in pressure in bars per minute.
 *
 * @return the pressure changes in bars per minute, positive if descending, negative if ascending.
 */
internal fun pressureChangeInBarsPerMinute(beginPressure: Double, endPressure: Double, timeInMinutes: Int): Double {
    require(timeInMinutes > 0) { "timeInMinutes must be a positive integer, instead got: $timeInMinutes." }
    return (endPressure - beginPressure) / timeInMinutes
}

/**
 * Performs the Schreiner equation for one inert gas type and calculates the inert gas pressure in a
 * compartment.
 *
 * **Water vapor pressure**:
 * This function is unaware of water vapor pressure. The caller must subtract water vapor pressure
 * from ambient pressure before computing [inspiredGasPressure] and [inspiredGasRate] (see
 * [TissueCompartment.addPressureChange]).
 *
 * @param initialTissuePressure the initial partial inert gas pressure for this compartment.
 * @param inspiredGasPressure the partial pressure of the inspired inert gas at the current depth.
 * @param halfTime the half-time for this compartment in minutes.
 * @param inspiredGasRate the rate at which the inspired inert gas partial pressure changes per
 * minute (due to depth change).
 *
 * @return the new partial inert gas pressure in this compartment.
 */
internal fun schreinerEquation(initialTissuePressure: Double, inspiredGasPressure: Double, time: Double, halfTime: Double, inspiredGasRate: Double): Double {
    val timeConstant = ln(2.0) / halfTime
    return (inspiredGasPressure + (inspiredGasRate * (time - (1.0 / timeConstant))) - ((inspiredGasPressure - initialTissuePressure - (inspiredGasRate / timeConstant)) * exp(-timeConstant * time)))
}

/**
 * Computes the effective inspired inert gas pressure and its rate of change for a CCR segment,
 * for use as drop-in replacements for the OC values passed to [schreinerEquation].
 *
 * **Water vapor pressure**:
 * This function is unaware of water vapor pressure (likewise [schreinerEquation]). The caller must
 * add water vapor pressure to the setpoint before passing it in.
 *
 * **Ambient-setpoint transitions**:
 * This function assumes the entire segment stays on one side of the setpoint pressure (ambient
 * stays above or below the setpoint for the full segment). If a segment crosses the setpoint
 * during ascent or descent, the caller must split it into two sub-segments at the point where
 * ambient equals the setpoint, and call this function separately for each.
 *
 * **Why this works**:
 * On OC the inspired inert gas fraction is fixed, so the inspired inert gas partial pressure
 * changes linearly with ambient pressure. On CCR the O₂ partial pressure is held constant at the
 * setpoint, so the inert gas partial pressure is:
 *
 * ```
 * (ambient - setpoint) * inertFraction / (1 - oxygenFractionDiluent)
 * ```
 *
 * Where `(ambient - setpoint)` is the pressure left for non-O₂ gases. Since `inertFraction` is
 * defined relative to the whole diluent (including its O₂), dividing by `(1 - oxygenFractionDiluent)`
 * rescales it to exclude the diluent's O₂, which is already accounted for in the setpoint.
 *
 * Since ambient pressure changes linearly during a segment, the inspired inert gas pressure is also
 * linear in time, with a constant rate of change. The Schreiner equation solves for any linear
 * input, so the same equation handles both OC and CCR: only the starting value and slope differ.
 *
 * Verified by tests against the Helling CCR Schreiner equation and a brute-force iterative Haldane
 * simulation (see [BuhlmannUtilitiesTest]).
 *
 * @param startPressure absolute ambient pressure at segment start
 * @param pressureRate change in ambient pressure per minute
 * @param inertFraction fraction of the inert gas (He or N₂) in the diluent
 * @param oxygenFractionDiluent fraction of O₂ in the diluent
 * @param setpoint water-vapor-corrected O₂ setpoint
 *
 * @return Pair(inspiredGasPressure, inspiredGasRate) for [schreinerEquation], or (0.0, 0.0) when
 * startPressure < setpoint (ambient is below the setpoint, the loop cannot reach the setpoint and
 * maxes out at pure O₂, so no inert gas is inspired). When startPressure == setpoint,
 * inspiredGasPressure is naturally 0 (no diluent in loop yet at this depth) but inspiredGasRate is
 * non-zero because as the diver descends, ambient pressure will exceed the setpoint and inert gas
 * begins to appear in the loop.
 */
internal fun ccrSchreinerInputs(
    startPressure: Double,
    pressureRate: Double,
    inertFraction: Double,
    oxygenFractionDiluent: Double,
    setpoint: Double,
): Pair<Double, Double> {
    require(oxygenFractionDiluent < 1.0) {
        "Diluent should contain at least some inert gas, 100% O₂ is unrealistic for CCR diving"
    }
    require(inertFraction in 0.0..1.0) {
        "Inert fraction must be between 0.0 and 1.0, got: $inertFraction"
    }
    if (startPressure < setpoint) {
        return Pair(0.0, 0.0)
    }
    val denominator = 1.0 - oxygenFractionDiluent
    return Pair(
        inertFraction * (startPressure - setpoint) / denominator,
        inertFraction * pressureRate / denominator
    )
}
