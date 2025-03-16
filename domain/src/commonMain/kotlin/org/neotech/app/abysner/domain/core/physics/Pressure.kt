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

import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.decompression.DecompressionPlanner
import org.neotech.app.abysner.domain.decompression.algorithm.DecompressionModel
import kotlin.jvm.JvmInline
import kotlin.math.exp
import kotlin.math.ln

/**
 * Represents pressure in metric bars, 1 bar is equal to 100.000 Pascal.
 * The idea of using an inline value class for Pressure is not to use it
 * everywhere, but instead to allow for compile-time checks (without run-time
 * performance hits) when crossing abstraction boundaries, such as crossing
 * between the [DecompressionPlanner] (works with meters currently) and
 * [DecompressionModel] (works in pressure).
 */
@JvmInline
value class Pressure(val value: Double)

/**
 * Calculates the partial pressure of an individual gas component from the total pressure of the gas
 * mixture, using a volume fraction.
 *
 * Example:
 *
 * 1 bar of air contains 79% nitrogen
 *
 * The partial pressure of nitrogen in air at 1 bar is 0.79
 *
 * See: https://en.wikipedia.org/wiki/Dalton%27s_law
 *
 * Pressure can be in pascals or bar, the output is in the same unit as the input.
 */
fun partialPressure(totalGasPressure: Double, volumeFraction: Double): Double = totalGasPressure * volumeFraction

/**
 * Convert pressure in Pascal to Bar.
 */
fun pascalToBar(pascals: Double): Double = pascals / 100000.0

/**
 * Convert pressure in Bar to Pascal.
 */
fun barToPascal(bars: Double): Double = bars * 100000.0

fun Double.asPsiToBar(): Double = this / 14.503774

fun Double.asBarToPsi(): Double = this * 14.503774


/**
 * Converts depth in meters given a certain density (salinity) and atmospheric pressure to pressure
 * in bars including atmospheric pressure.
 *
 * @param depth water depth in meters
 * @param environment the density (salinity) of the water and atmospheric pressure.
 * @return pressure at the given depth.
 */
fun depthInMetersToBar(depth: Double, environment: Environment): Pressure {
    val weightDensity = environment.salinity.density * GRAVITY_ON_EARTH
    return Pressure(pascalToBar(depth * weightDensity) + environment.atmosphericPressure)
}

/**
 * Converts pressure in bars including atmospheric pressure to depth in meters given a certain
 * density (salinity) and atmospheric pressure.
 *
 * @param pressure pressure including atmospheric pressure.
 * @param environment the density (salinity) of the water and atmospheric pressure.
 * @return depth in meters for the given pressure.
 */
fun barToDepthInMeters(pressure: Double, environment: Environment): Double {
    val waterPressure = pressure - environment.atmosphericPressure
    val weightDensity = environment.salinity.density * GRAVITY_ON_EARTH
    return barToPascal(waterPressure) / weightDensity
}

@Suppress("NOTHING_TO_INLINE")
inline fun barToDepthInMeters(pressure: Pressure, environment: Environment): Double {
    return barToDepthInMeters(pressure.value, environment)
}

/**
 * Transforms altitude in meters to pressure in bars.
 * Based on: https://en.wikipedia.org/wiki/Barometric_formula
 */
fun altitudeToPressure(altitudeInMeters: Double): Double {
    // Reference temperature is 15 degrees celsius.
    // Raw formula as seen on wikipedia:
    // Altitude = ATMOSPHERIC_PRESSURE_AT_SEA_LEVEL * exp((-GRAVITY_ON_EARTH * MOLAR_MASS_AIR * altitudeInMeters) / (UNIVERSAL_GAS_CONSTANT * ATMOSPHERIC_TEMPERATURE))
    // Move all constants to after the first division, for simplification (let compiler do rest of the optimization)
    return ATMOSPHERIC_PRESSURE_AT_SEA_LEVEL * exp(-altitudeInMeters / ((UNIVERSAL_GAS_CONSTANT * ATMOSPHERIC_TEMPERATURE) / (GRAVITY_ON_EARTH * MOLAR_MASS_AIR)))
}

/**
 * Transforms pressure in bars to altitude in meters.
 * Based on: https://en.wikipedia.org/wiki/Barometric_formula
 */
fun pressureToAltitude(pressureInBar: Double): Double {
    return ln(ATMOSPHERIC_PRESSURE_AT_SEA_LEVEL / pressureInBar) * ((UNIVERSAL_GAS_CONSTANT * ATMOSPHERIC_TEMPERATURE) / (GRAVITY_ON_EARTH * MOLAR_MASS_AIR))
}

private const val UNIVERSAL_GAS_CONSTANT = 8.3144598
private const val MOLAR_MASS_AIR = 0.0289644

/**
 * Pressure in bar at sea level, at [ATMOSPHERIC_TEMPERATURE] degrees Kelvin.
 */
const val ATMOSPHERIC_PRESSURE_AT_SEA_LEVEL = 1.01325

/**
 *  Constant temperature in degrees Kelvin which is used during pressure to altitude calculations.
 *  This is essentially 15 degrees celsius.
 */
private const val ATMOSPHERIC_TEMPERATURE = 273.15 + 15.0

/**
 * Gravity on earth in meters per seconds squared (m/s²)
 */
internal const val GRAVITY_ON_EARTH = 9.80665
