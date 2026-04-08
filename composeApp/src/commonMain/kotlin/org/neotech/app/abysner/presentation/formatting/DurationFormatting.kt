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

import kotlin.time.Duration

/**
 * Formats a [Duration] as a short human-readable string in whole hours and minutes.
 *
 * For example: `1h 30m` or `45m`.
 *
 * TODO: For localization this requires a different approach
 */
fun Duration.toHHMM(): String {
    val h = inWholeHours
    val m = inWholeMinutes % 60
    return if (h > 0) {
        "${h}h ${m}m"
    } else {
        "${m}m"
    }
}

