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

package org.neotech.app.abysner.domain.decompression.algorithm

import org.neotech.app.abysner.domain.core.model.Gas

/**
 * A decompression model is allows adding dive sections to load tissues, and a method that returns
 * the current ceiling.
 */
interface Model: Snapshotable {

    /**
     * Add a flat section of the dive to the tissues, this is the same as calling [addDepthChange]
     * with and equal start and end depth.
     *
     * @see addDepthChange
     */
    fun addFlat(depth: Double, gas: Gas, timeInMinutes: Int) {
        addDepthChange(depth, depth, gas, timeInMinutes)
    }

    /**
     * Add a descending, ascending or flat section of the dive to tissues.
     *
     * @param startDepth start depth in meters from the surface.
     * @param endDepth end depth in meters from the surface.
     * @param gas the gas being breathe by the diver during this section.
     * @param timeInMinutes the timeInMinutes this section takes.
     */
    fun addDepthChange(startDepth: Double, endDepth: Double, gas: Gas, timeInMinutes: Int)

    fun getCeiling(): Double
}
