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
import org.neotech.app.abysner.domain.core.physics.Pressure

/**
 * A decompression model is allows adding dive sections to load tissues, and a method that returns
 * the current ceiling.
 */
interface DecompressionModel: Snapshotable {

    /**
     * Add a flat section of the dive to the tissues, this is the same as calling [addPressureChange]
     * with and equal start and end pressure.
     *
     * @param pressureAtDepth the pressure in bars at the current depth (including atmospheric pressure)
     *
     * @see addPressureChange
     */
    fun addFlat(pressureAtDepth: Pressure, gas: Gas, timeInMinutes: Int) {
        addPressureChange(pressureAtDepth, pressureAtDepth, gas, timeInMinutes)
    }

    /**
     * Add a descending, ascending or flat section of the dive to tissues.
     *
     * @param startPressure start pressure (depth) in bars (including atmospheric pressure)
     * @param endPressure end pressure (depth) in bars (including atmospheric pressure)
     * @param gas the gas being breathe by the diver during this section.
     * @param timeInMinutes the timeInMinutes this section takes.
     */
    fun addPressureChange(startPressure: Pressure, endPressure: Pressure, gas: Gas, timeInMinutes: Int)

    /**
     * Calculates and returns the current tissue ceiling in bars (including atmospheric pressure)
     */
    fun getCeiling(): Pressure

    /**
     * Calculates and returns the NDL (no decompression limit) time in minutes for a given depth and
     * gas, with the current tissue loading as starting point. This method will not alter the
     * current tissue loading, after returning tissues will be reset to their original state.
     */
    fun getNoDecompressionLimit(depth: Pressure, gas: Gas): Int
}
