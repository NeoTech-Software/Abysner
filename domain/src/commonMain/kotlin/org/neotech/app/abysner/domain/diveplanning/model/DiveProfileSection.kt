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

package org.neotech.app.abysner.domain.diveplanning.model

import org.neotech.app.abysner.domain.core.model.Cylinder

data class DiveProfileSection(
    /**
     * Duration in minutes.
     */
    val duration: Int,
    /**
     * Depth of this segment
     */
    val depth: Int,
    /**
     * Selected gas for this segment (usually travel or bottom gas)
     */
    val cylinder: Cylinder
)

/**
 * Truncates the profile sections so that the dive ends at the given [runtime].
 */
fun List<DiveProfileSection>.truncateAtRuntime(runtime: Int): List<DiveProfileSection> {
    val result = mutableListOf<DiveProfileSection>()
    var elapsed = 0
    for (section in this) {
        if (elapsed >= runtime) {
            break
        }
        val remaining = runtime - elapsed
        if (section.duration <= remaining) {
            result.add(section)
            elapsed += section.duration
        } else {
            result.add(section.copy(duration = remaining))
            break
        }
    }
    return result
}

