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

package org.neotech.app.abysner.presentation.formatting

import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.utilities.greaterThanTolerant
import org.neotech.app.abysner.domain.utilities.lessThanTolerant
import org.neotech.app.abysner.presentation.component.AlertSeverity

/**
 * Formats this gas as "oxygen/helium" padded with [FIGURE_SPACE] at the end or start so the
 * combined string is always the same width, regardless of how many digits the oxygen and helium
 * percentages have. Intended to be displayed with
 * [tabular figures][org.neotech.app.abysner.presentation.theme.withTabularFigures] enabled, so
 * the strings actually take up the same width.
 */
fun Gas.toPaddedMixString(padEnd: Boolean = true): String {
    val formatted = toString()
    return if (padEnd) {
        formatted.padEnd(PADDED_MIX_WIDTH, FIGURE_SPACE)
    } else {
        formatted.padStart(PADDED_MIX_WIDTH, FIGURE_SPACE)
    }
}

/**
 * Figure space (U+2007) renders at the same width as a tabular digit in fonts that support
 * [tabular figures][org.neotech.app.abysner.presentation.theme.withTabularFigures], unlike a
 * regular space. Used for example to pad the oxygen and helium percentages so mixes like "21/0" and
 * "21/35" to line up instead of one being a character shorter.
 */
internal const val FIGURE_SPACE = ' '

private const val PADDED_MIX_WIDTH = 5

/**
 * Returns [AlertSeverity.ERROR] when [ppo2] falls outside the range considered safe to breathe,
 * above [Gas.MAX_PPO2] or below [Gas.MIN_PPO2], otherwise [AlertSeverity.NONE]. Values within
 * [tolerance] of either threshold are considered within range.
 */
fun ppo2AlertSeverity(ppo2: Double, tolerance: Double): AlertSeverity = when {
    ppo2.greaterThanTolerant(Gas.MAX_PPO2, tolerance) -> AlertSeverity.ERROR
    ppo2.lessThanTolerant(Gas.MIN_PPO2, tolerance) -> AlertSeverity.ERROR
    else -> AlertSeverity.NONE
}

/**
 * Returns [AlertSeverity.ERROR] when [density] exceeds [Gas.MAX_GAS_DENSITY],
 * [AlertSeverity.WARNING] when it exceeds [Gas.MAX_RECOMMENDED_GAS_DENSITY], otherwise
 * [AlertSeverity.NONE].
 */
fun densityAlertSeverity(density: Double): AlertSeverity = when {
    density.greaterThanTolerant(Gas.MAX_GAS_DENSITY) -> AlertSeverity.ERROR
    density.greaterThanTolerant(Gas.MAX_RECOMMENDED_GAS_DENSITY) -> AlertSeverity.WARNING
    else -> AlertSeverity.NONE
}

/**
 * Half-unit at 2 decimal places: prevents alerts when the displayed value rounds to exactly the 2
 * decimal threshold.
 */
internal const val ALERT_DISPLAY_TOLERANCE_TWO_DECIMAL_PLACES = 0.005

/**
 * Half-unit at 1 decimal place: prevents alerts when the displayed value rounds to exactly the 1
 * decimal threshold.
 */
internal const val ALERT_DISPLAY_TOLERANCE_ONE_DECIMAL_PLACE = 0.05
