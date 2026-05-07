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

package org.neotech.app.abysner.domain.core.physics

fun Double.asCubicFeetToLiters(): Double = this * LITERS_PER_CUBIC_FOOT

fun Double.asLitersToCubicFeet(): Double = this / LITERS_PER_CUBIC_FOOT

/**
 * Conversion factor from liters to cubic feet.
 */
const val LITERS_PER_CUBIC_FOOT = 28.316846592

