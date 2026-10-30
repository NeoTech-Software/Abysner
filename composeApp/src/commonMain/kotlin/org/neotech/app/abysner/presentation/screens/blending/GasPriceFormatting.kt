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

package org.neotech.app.abysner.presentation.screens.blending

import org.neotech.app.abysner.domain.core.model.UnitSystem
import org.neotech.app.abysner.domain.core.physics.LITERS_PER_CUBIC_FOOT

/**
 * The denominator prices are entered and shown in. Gas is sold by the cubic meter in metric
 * countries and by the cubic foot in imperial ones, and both give numbers of a sensible size, unlike
 * a price per liter which would come out as something like 0.045.
 */
internal val UnitSystem.priceVolumeUnitLabel: String
    get() = when (this) {
        UnitSystem.METRIC -> "m³"
        UnitSystem.IMPERIAL -> "ft³"
    }

/** Cubic meter prices are whole currency units, cubic foot prices are an order of magnitude smaller. */
internal val UnitSystem.priceFractionDigits: Int
    get() = when (this) {
        UnitSystem.METRIC -> 2
        UnitSystem.IMPERIAL -> 3
    }

/** Liters of gas at 1 bar in the volume prices are quoted per. */
internal val UnitSystem.litersPerPriceUnit: Double
    get() = when (this) {
        UnitSystem.METRIC -> LITERS_PER_CUBIC_METER
        UnitSystem.IMPERIAL -> LITERS_PER_CUBIC_FOOT
    }

/**
 * Prices are stored per liter, the same way pressures are stored in bar, and converted to the
 * denominator the user actually enters them in.
 */
internal fun Double.toDisplayPrice(unitSystem: UnitSystem): Double = this * unitSystem.litersPerPriceUnit

internal fun Double.toPricePerLiter(unitSystem: UnitSystem): Double = this / unitSystem.litersPerPriceUnit

internal const val LITERS_PER_CUBIC_METER = 1000.0
