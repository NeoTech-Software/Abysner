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

package org.neotech.app.abysner.domain.decompression.algorithm.buhlmann

import org.neotech.app.abysner.domain.core.physics.asDegreesCelsiusToDegreesKelvin
import org.neotech.app.abysner.domain.core.physics.asDegreesKelvinToDegreesCelsius
import org.neotech.app.abysner.domain.core.physics.pascalToBar
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

/**
 * Returns the Water Vapour pressure in pascals based on the given temperature in degrees Kelvin.
 * Valid temperature range is 1-374 degrees.
 *
 * Initial implementation is based on:
 * https://github.com/nyxtom/dive/blob/08b182cf7b00ab1878344a19f14d9d1ae9b219d0/lib/core.js#L378
 *
 * However the implementation and constants have been modified to work with SI units instead.
 */
fun waterVapourPressure(degreesKelvin: Double): Double {
    // Below is an implementation of the Antoine Equation:
    // - https://en.wikipedia.org/wiki/Antoine_equation
    // - http://en.wikipedia.org/wiki/Vapour_pressure_of_water */

    // According to Wikipedia the valid range for water goes from 0 to 100 but on the high range
    // from 99 to 374. So there is some slight overlap between the two ranges. For 99 and 100 the
    // below code prefers the low range.
    val temperatureConstants: Array<Double> = when (degreesKelvin.asDegreesKelvinToDegreesCelsius()) {
        in 1.0..100.0 -> ANTOINE_SI_CONSTANTS_FOR_WATER_1_TO_100
        in 99.0..374.0 -> ANTOINE_SI_CONSTANTS_FOR_WATER_99_TO_374
        else -> throw IllegalArgumentException("Temperature provided is outside supported range, use a temperature between 1.0 and 374.0 degrees Celsius.")
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
 * Performs Schreiners Equation for one inert gas type and calculates the inert gas pressure in a
 * compartment given: an initial compartment partial gas pressure [pBegin], the partial pressure of the
 * inert gas at the current depth [pGas], the half-time of this compartment [halfTime] and the rate
 * at which the inert gas partial pressure changes per minute [gasRate].
 *
 * @param pBegin the initial partial inert gas pressure for this compartment.
 * @param pGas the partial pressure of the inert gas at the current depth
 * @param halfTime the Log2/half-time for this compartment in minutes.
 * @param gasRate the rate at which the partial inert gas pressure changes per minute (depth change)
 *
 * @return the new partial inert gas pressure in this compartment.
 */
internal fun schreinerEquation(pBegin: Double, pGas: Double, timeInMinutes: Int, halfTime: Double, gasRate: Double): Double {
    val timeConstant = ln(2.0) / halfTime
    return (pGas + (gasRate * (timeInMinutes - (1.0 / timeConstant))) - ((pGas - pBegin - (gasRate / timeConstant)) * exp(-timeConstant * timeInMinutes)))
}
