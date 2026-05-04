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

package org.neotech.app.abysner.presentation.utilities

import org.neotech.app.abysner.domain.core.model.UnitSystem
import org.neotech.app.abysner.domain.core.physics.LITERS_PER_CUBIC_FOOT
import org.neotech.app.abysner.domain.core.physics.PSI_PER_BAR
import org.neotech.app.abysner.domain.utilities.DecimalFormat
import kotlin.math.roundToInt

val UnitSystem.depthUnitLabel: String
    get() = when (this) {
        UnitSystem.METRIC -> "m"
        UnitSystem.IMPERIAL -> "ft"
    }

val UnitSystem.rateUnitLabel: String
    get() = when (this) {
        UnitSystem.METRIC -> "m/min"
        UnitSystem.IMPERIAL -> "ft/min"
    }

val UnitSystem.pressureUnitLabel: String
    get() = when (this) {
        UnitSystem.METRIC -> "bar"
        UnitSystem.IMPERIAL -> "psi"
    }

val UnitSystem.volumeUnitLabel: String
    get() = when (this) {
        UnitSystem.METRIC -> "L"
        UnitSystem.IMPERIAL -> "ft³"
    }

val UnitSystem.sacRateUnitLabel: String
    get() = when (this) {
        UnitSystem.METRIC -> "L/min"
        UnitSystem.IMPERIAL -> "ft³/min"
    }

/**
 * Formats a depth value that is already in display-units (meters for metric, feet for imperial).
 */
fun Double.formatDisplayDepth(unitSystem: UnitSystem, decimals: Int = 0, includeUnit: Boolean = true): String {
    val value = DecimalFormat.format(decimals, this)
    return if (includeUnit) { "$value ${unitSystem.depthUnitLabel}" } else { value }
}

/**
 * Converts a depth in meters to a formatted display string with optional unit suffix.
 */
fun Double.formatDepth(unitSystem: UnitSystem, includeUnit: Boolean = true): String {
    val displayValue = unitSystem.metersToDisplayDepth(this).toInt()
    return if (includeUnit) { "$displayValue ${unitSystem.depthUnitLabel}" } else { displayValue.toString() }
}

/**
 * Formats a pressure value in bar to a display string. In imperial mode the value is converted
 * to psi.
 */
fun Double.formatPressure(unitSystem: UnitSystem, includeUnit: Boolean = true): String {
    val value = when (unitSystem) {
        UnitSystem.METRIC -> roundToInt()
        UnitSystem.IMPERIAL -> (this * PSI_PER_BAR).roundToInt()
    }
    return if (includeUnit) { "$value ${unitSystem.pressureUnitLabel}" } else { value.toString() }
}

/**
 * Formats a volume value in liters to a display string. In imperial mode the value is converted
 * to cubic feet.
 */
fun Double.formatVolume(unitSystem: UnitSystem, decimals: Int = 0, unit: String = unitSystem.volumeUnitLabel): String {
    val value = when (unitSystem) {
        UnitSystem.METRIC -> DecimalFormat.format(decimals, this)
        UnitSystem.IMPERIAL -> DecimalFormat.format(decimals, this / LITERS_PER_CUBIC_FOOT)
    }
    return if (unit.isNotEmpty()) { "$value $unit" } else { value }
}
