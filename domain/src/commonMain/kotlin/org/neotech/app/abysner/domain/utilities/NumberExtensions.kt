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

package org.neotech.app.abysner.domain.utilities

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

/**
 * Returns true if [this] and [other] differ by less than [tolerance]. Use for
 * comparisons where floating-point arithmetic noise might prevent exact equality.
 */
fun Double.equalsTolerant(other: Double, tolerance: Double = FLOATING_POINT_TOLERANCE): Boolean =
    abs(this - other) < tolerance

/**
 * Returns true if [this] is strictly greater than [other], accounting for floating-point noise.
 * Small differences within [tolerance] are treated as equal.
 */
fun Double.greaterThanTolerant(other: Double, tolerance: Double = FLOATING_POINT_TOLERANCE): Boolean =
    this > other + tolerance

/**
 * Returns true if [this] is greater than or equal to [other], accounting for floating-point noise.
 * Values just barely below [other] due to noise are still considered equal.
 */
fun Double.greaterThanOrEqualTolerant(other: Double, tolerance: Double = FLOATING_POINT_TOLERANCE): Boolean =
    this >= other - tolerance

/**
 * Returns true if [this] is strictly less than [other], accounting for floating-point noise.
 * Small differences within [tolerance] are treated as equal.
 */
fun Double.lessThanTolerant(other: Double, tolerance: Double = FLOATING_POINT_TOLERANCE): Boolean =
    this < other - tolerance

/**
 * Returns true if [this] is less than or equal to [other], accounting for floating-point noise.
 * Values just barely above [other] due to noise are still considered equal.
 */
fun Double.lessThanOrEqualTolerant(other: Double, tolerance: Double = FLOATING_POINT_TOLERANCE): Boolean =
    this <= other + tolerance

/**
 * Like [ceil], but subtracts [FLOATING_POINT_TOLERANCE] first so that values just barely above an
 * integer due to floating-point noise round as expected. For example, `ceilTolerant(9.0000000002)`
 * returns 9.0, not 10.0.
 */
fun ceilTolerant(value: Double): Double = ceil(value - FLOATING_POINT_TOLERANCE)

/**
 * Like [floor], but adds [FLOATING_POINT_TOLERANCE] first so that values just barely below an
 * integer due to floating-point noise round as expected. For example, `floorTolerant(2.9999999998)`
 * returns 3.0, not 2.0.
 */
fun floorTolerant(value: Double): Double = floor(value + FLOATING_POINT_TOLERANCE)

/**
 * Strips floating-point arithmetic noise by rounding to [FLOATING_POINT_TOLERANCE] precision (6
 * decimal places). For depth in meters this is micrometer precision, for pressure in bar this is
 * microbar precision, both far beyond what diving calculations need.
 *
 * For example: `removeFloatingPointNoise(6.000000000000001)` returns 6.0.
 */
fun removeFloatingPointNoise(value: Double): Double {
    val factor = 1.0 / FLOATING_POINT_TOLERANCE
    return round(value * factor) / factor
}

/**
 * Small absolute tolerance used to absorb floating-point arithmetic noise.
 */
private const val FLOATING_POINT_TOLERANCE = 1e-6
