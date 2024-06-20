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

package org.neotech.app.abysner.domain.utilities

import kotlin.math.abs
import kotlin.math.max

/**
 * See: https://levelup.gitconnected.com/double-equality-in-kotlin-f99392cba0e4
 */
fun Double.equalsDelta(other: Double, delta: Double) = abs(this - other) < delta * max(abs(this), abs(other))

/**
 * See: https://levelup.gitconnected.com/double-equality-in-kotlin-f99392cba0e4
 */
fun Double.higherThenDelta(other: Double, delta: Double): Boolean {
    return this > other && !this.equalsDelta(other, delta)
}