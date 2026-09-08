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
 * Figure space (U+2007) renders at the same width as a tabular digit in fonts that support
 * [tabular figures][org.neotech.app.abysner.presentation.theme.withTabularFigures], unlike a
 * regular space. Used for example to pad the oxygen and helium percentages so mixes like "21/0" and
 * "21/35" to line up instead of one being a character shorter.
 */
internal const val FIGURE_SPACE = ' '

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

private const val PADDED_MIX_WIDTH = 5
