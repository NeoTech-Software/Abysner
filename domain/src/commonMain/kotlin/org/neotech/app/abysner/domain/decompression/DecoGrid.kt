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

package org.neotech.app.abysner.domain.decompression

import org.neotech.app.abysner.domain.utilities.ceilTolerant
import org.neotech.app.abysner.domain.utilities.equalsTolerant
import org.neotech.app.abysner.domain.utilities.greaterThanTolerant
import org.neotech.app.abysner.domain.utilities.lessThanOrEqualTolerant
import org.neotech.app.abysner.domain.utilities.lessThanTolerant
import kotlin.math.floor
import kotlin.math.round

/**
 * While the decompression algorithm calculates the required deco stops and gas switches at any
 * arbitrary pressure, divers plan and work in whole meters or feet, and for gas switches and
 * decompression stops in steps of 3 meter or 10 feet.
 *
 * The DecoGrid encapsulates this grid-thinking logic, and allows configuring a grid for different
 * display units (meters, feet or anything custom).
 */
class DecoGrid(
    val surfacePressure: Double,
    /**
     * The deco step size in pressure (for example about 0.3 bar for 3m steps) used to determine at
     * which pressures deco stops are required. Must be an exact multiple of
     * [displayUnitPressureDelta].
     */
    val decoStepSizePressureDelta: Double,
    /**
     * The ambient pressure of the last allowed deco stop, must fall on a
     * [displayUnitPressureDelta] grid point relative to [surfacePressure].
     */
    val lastDecoStopAmbientPressure: Double,
    /**
     * The pressure delta that corresponds to the smallest display unit step (for example about
     * 0.1 bar for steps of 1 meter).
     */
    val displayUnitPressureDelta: Double,
) {

    init {
        // Both decoStepSizePressureDelta and lastDecoStopAmbientPressure must align to
        // displayUnitPressureDelta so that deco stops always land on whole display units (e.g.
        // 3, 6 or 9 meter instead of 2.9, 5.8 or 8.7 meter).
        val decoStepInDisplayUnits = decoStepSizePressureDelta / displayUnitPressureDelta
        require(decoStepInDisplayUnits.equalsTolerant(round(decoStepInDisplayUnits))) {
            "decoStepSizePressureDelta ($decoStepSizePressureDelta) must be an exact multiple of displayUnitPressureDelta ($displayUnitPressureDelta)."
        }
        val lastDecoStopDepthPressure = lastDecoStopAmbientPressure - surfacePressure
        val lastDecoStopInDisplayUnits = lastDecoStopDepthPressure / displayUnitPressureDelta
        require(lastDecoStopInDisplayUnits.equalsTolerant(round(lastDecoStopInDisplayUnits))) {
            "lastDecoStopAmbientPressure ($lastDecoStopAmbientPressure) must fall on a display unit grid point relative to surfacePressure ($surfacePressure)."
        }
    }

    /**
     * Snaps a raw ceiling pressure to the nearest deeper deco stop pressure on the grid, clamping
     * to [lastDecoStopAmbientPressure] if the result falls between [surfacePressure] and the last
     * allowed stop. Returns [surfacePressure] when the ceiling is at or above the surface.
     */
    fun snapCeilingToDecoGrid(rawCeilingPressure: Double): Double {
        if (rawCeilingPressure.lessThanOrEqualTolerant(surfacePressure)) {
            return surfacePressure
        }

        val depthPressure = rawCeilingPressure - surfacePressure

        // Snap (ceil) to the deco grid (e.g. 3 meter or 10 feet increments). Because
        // decoStepSizePressureDelta is guaranteed to be an exact multiple of
        // displayUnitPressureDelta, this always lands on a display-unit boundary too.
        val decoGridSteps = ceilTolerant(depthPressure / decoStepSizePressureDelta).toInt()
        if (decoGridSteps <= 0) {
            return surfacePressure
        }
        val snappedPressure = surfacePressure + decoGridSteps * decoStepSizePressureDelta

        // If the deco stop falls between surface and the last allowed deco stop depth, clamp to
        // the last allowed deco stop.
        return if (snappedPressure.lessThanTolerant(lastDecoStopAmbientPressure) && snappedPressure.greaterThanTolerant(surfacePressure)) {
            lastDecoStopAmbientPressure
        } else {
            snappedPressure
        }
    }

    /**
     * Returns the next shallower deco stop pressure that is shallower than [fromAmbientPressure].
     * If the given ambient pressure is already on a deco stop pressure, returns one step shallower.
     * Returns [surfacePressure] when the next shallower deco stop would fall between
     * [surfacePressure] and [lastDecoStopAmbientPressure].
     */
    fun findNextDecoStopPressure(fromAmbientPressure: Double): Double {
        val depthPressure = fromAmbientPressure - surfacePressure
        if (depthPressure.lessThanOrEqualTolerant(0.0)) {
            return surfacePressure
        }

        val steps = depthPressure / decoStepSizePressureDelta
        val nearestWholeStep = round(steps)
        val isOnGrid = steps.equalsTolerant(nearestWholeStep)

        // If exactly on a deco-grid point, go one step shallower, so we select the next stop. If
        // not, we are in between deco-grid points. In that case the next stop is the nearest
        // shallower one, so use floor to find it.
        val nextStep = if (isOnGrid) {
            nearestWholeStep.toInt() - 1
        } else {
            // No need for tolerance, if there was any floating-point noise the isOnGrid check would have caught it
            floor(steps).toInt()
        }
        if (nextStep <= 0) {
            // No more deco stops, next step on the grid is surface, return early and exactly
            return surfacePressure
        }

        val nextPressure = surfacePressure + nextStep * decoStepSizePressureDelta

        // If the next stop falls between surface and the configured last deco stop depth, skip to
        // surface (the dive was configured to not stop in between the surface and
        // lastDecoStopAmbientPressure).
        return if (nextPressure.lessThanTolerant(lastDecoStopAmbientPressure)) {
            surfacePressure
        } else {
            nextPressure
        }
    }

    /**
     * Returns true if [ambientPressure] falls on a deco stop level.
     */
    fun isAtDecoStop(ambientPressure: Double): Boolean {
        val steps = (ambientPressure - surfacePressure) / decoStepSizePressureDelta
        val nearestWholeStep = round(steps)
        // If nearest whole step is within tolerance to the actual step we are at, the diver is on a
        // deco grid point.
        return steps.equalsTolerant(nearestWholeStep)
    }
}
