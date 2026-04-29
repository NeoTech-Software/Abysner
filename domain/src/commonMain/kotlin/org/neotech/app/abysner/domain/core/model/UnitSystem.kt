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

package org.neotech.app.abysner.domain.core.model

import org.neotech.app.abysner.domain.core.physics.METERS_PER_FOOT
import org.neotech.app.abysner.domain.core.physics.Pressure
import org.neotech.app.abysner.domain.core.physics.ambientPressureToFeet
import org.neotech.app.abysner.domain.core.physics.ambientPressureToMeters
import org.neotech.app.abysner.domain.core.physics.feetToAmbientPressure
import org.neotech.app.abysner.domain.core.physics.metersToAmbientPressure
import kotlin.math.round
import kotlin.math.roundToInt

enum class UnitSystem {
    /**
     * Metric system for divers: meters, bar and liters.
     */
    METRIC,

    /**
     * Imperial system for divers: feet, psi and cubic feet.
     */
    IMPERIAL;

    fun depthToAmbientPressure(depth: Double, environment: Environment): Pressure {
        return when(this) {
            METRIC -> metersToAmbientPressure(depth, environment)
            IMPERIAL -> feetToAmbientPressure(depth, environment)
        }
    }

    fun ambientPressureToDepth(ambientPressure: Pressure, environment: Environment): Double {
        return when(this) {
            METRIC -> ambientPressureToMeters(ambientPressure.value, environment)
            IMPERIAL -> ambientPressureToFeet(ambientPressure.value, environment)
        }
    }

    /**
     * Rounds a metric-meter value to the nearest whole display unit and converts back to meters.
     * In metric mode this rounds to the nearest whole meter. In imperial mode it rounds to the
     * nearest whole foot and converts back to meters.
     */
    fun snapMetersToDisplayUnit(value: Double): Double = when (this) {
        METRIC -> value.roundToInt().toDouble()
        IMPERIAL -> (value / METERS_PER_FOOT).roundToInt().toDouble() * METERS_PER_FOOT
    }

    /**
     * Converts a depth value in display units (meters or feet) to meters.
     */
    fun displayDepthToMeters(displayDepth: Double): Double = when (this) {
        METRIC -> displayDepth
        IMPERIAL -> displayDepth * METERS_PER_FOOT
    }

    /**
     * Converts a depth value in meters to display units (meters or feet), rounded to the nearest
     * whole display unit.
     */
    fun metersToDisplayDepth(meters: Double): Double = when (this) {
        METRIC -> round(meters)
        IMPERIAL -> round(meters / METERS_PER_FOOT)
    }
}
