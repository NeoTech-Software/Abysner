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

package org.neotech.app.abysner.presentation

import org.neotech.app.abysner.domain.diveplanning.DivePlanner

fun Throwable.getUserReadableMessage(): String {
    return when (this) {
        is DivePlanner.NotEnoughTimeToReachDepth ->
            "Your dive cannot be planned as the time to descent from one level (or from the surface) to the next level, is longer then the planned bottom time of that next level. Increase bottom time and/or descent speed."
        is DivePlanner.NotEnoughTimeToDecompress ->
            "Your dive cannot be planned as the time to ascent from a one level to the next (and optionally decompress) is longer then the planned bottom time of that next level. Increase the bottom time and/or ascent speed."
        else -> "Unexpected error:\n${message ?: stackTraceToString()}"
    }
}
