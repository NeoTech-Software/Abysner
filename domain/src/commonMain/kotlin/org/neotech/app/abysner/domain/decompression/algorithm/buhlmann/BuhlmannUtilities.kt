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
 * @return pressure change per minute, positive if descending, negative if ascending.
 */
internal fun pressureChangeInBarsPerMinute(beginPressure: Double, endPressure: Double, timeInMinutes: Double): Double {
    require(timeInMinutes > 0.0) { "timeInMinutes must be positive, instead got: $timeInMinutes." }
    return (endPressure - beginPressure) / timeInMinutes
}

/**
 * Performs the Schreiner equation for one inert gas type and calculates the inert gas pressure in a
 * compartment. This function is unaware of water vapor pressure, the caller must subtract it from
 * ambient before computing [inspiredGasPressure] and [inspiredGasRate] (see
 * [TissueCompartment.addPressureChange]).
 *
 * @param initialTissuePressure the initial partial inert gas pressure for this compartment.
 * @param inspiredGasPressure the partial pressure of the inspired inert gas at the current depth.
 * @param halfTime the half-time for this compartment in minutes.
 * @param inspiredGasRate the rate at which the inspired inert gas partial pressure changes per
 * minute (due to depth change).
 */
fun schreinerEquation(initialTissuePressure: Double, inspiredGasPressure: Double, time: Double, halfTime: Double, inspiredGasRate: Double): Double {
    val timeConstant = ln(2.0) / halfTime
    return (inspiredGasPressure + (inspiredGasRate * (time - (1.0 / timeConstant))) - ((inspiredGasPressure - initialTissuePressure - (inspiredGasRate / timeConstant)) * exp(-timeConstant * time)))
}

/**
 * Computes the effective inspired inert gas pressure and its rate of change for a CCR segment,
 * for use as drop-in replacements for the OC values passed to [schreinerEquation].
 *
 * On OC the inspired inert gas fraction is fixed, so the inspired partial pressure changes linearly
 * with ambient pressure. On CCR the O2 partial pressure is held constant at the setpoint, so the
 * inert gas partial pressure becomes:
 *
 * ```
 * (ambient - setpoint) * inertFraction / (1 - oxygenFractionDiluent)
 * ```
 *
 * `(ambient - setpoint)` is the pressure left over for non-O2 gases. Since `inertFraction` is
 * relative to the whole diluent (including its O2), dividing by `(1 - oxygenFractionDiluent)`
 * rescales it to exclude the diluent O2 that the setpoint already accounts for. The result is
 * still linear in ambient pressure, so the same Schreiner equation applies to both OC and CCR,
 * just with different starting values and slope.
 *
 * Like [schreinerEquation] this function is unaware of water vapor pressure, the caller must add it
 * to the setpoint before passing it in. Also assumes the segment stays on one side of the setpoint
 * (no crossing during ascent/descent), the caller is responsible for splitting if needed (see
 * [TissueCompartment.addPressureChangeCcr]).
 *
 * Verified against the Helling CCR Schreiner equation and a brute-force Haldane simulation (see
 * [BuhlmannUtilitiesTest]).
 *
 * @param startPressure absolute ambient pressure at segment start
 * @param pressureRate change in ambient pressure per minute
 * @param inertFraction fraction of the inert gas (He or N2) in the diluent
 * @param oxygenFractionDiluent fraction of O2 in the diluent (must be < 1.0)
 * @param setpoint water-vapor-corrected O2 setpoint
 *
 * @return (inspiredGasPressure, inspiredGasRate) for [schreinerEquation], or (0.0, 0.0) when
 * ambient is below the setpoint (loop maxes out on pure O2, no inert gas inspired).
 */
fun ccrSchreinerInputs(
    startPressure: Double,
    pressureRate: Double,
    inertFraction: Double,
    oxygenFractionDiluent: Double,
    setpoint: Double,
): Pair<Double, Double> {
    require(oxygenFractionDiluent < 1.0) {
        "Diluent should contain at least some inert gas, 100% O2 is unrealistic for CCR diving"
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
