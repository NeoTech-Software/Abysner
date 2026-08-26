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

/**
 * Figure space (U+2007): renders at the same width as a tabular digit in fonts that support
 * [tabular figures][org.neotech.app.abysner.presentation.theme.withTabularFigures], unlike a
 * regular space. Used to pad the oxygen and helium percentages so mixes like "21/0" and "21/35"
 * line up instead of one being a character shorter.
 */
private const val FIGURE_SPACE = ' '

/**
 * Formats this gas as "oxygen/helium" padded with [FIGURE_SPACE] so the combined string is always
 * the same width, regardless of how many digits the oxygen and helium percentages have. Intended
 * to be displayed with
 * [tabular figures][org.neotech.app.abysner.presentation.theme.withTabularFigures] enabled, so
 * mixes line up across rows in a table or diagram.
 */
fun Gas.toPaddedMixString(): String {
    return if (oxygenPercentage >= 100) {
        "100/0"
    } else if (oxygenPercentage >= 10) {
        "$oxygenPercentage/${heliumPercentage.toString().padEnd(2, FIGURE_SPACE)}"
    } else {
        "$oxygenPercentage/${heliumPercentage.toString().padEnd(3, FIGURE_SPACE)}"
    }
}
