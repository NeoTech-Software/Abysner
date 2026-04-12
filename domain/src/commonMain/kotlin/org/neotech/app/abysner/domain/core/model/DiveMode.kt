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

package org.neotech.app.abysner.domain.core.model

import org.neotech.app.abysner.domain.persistence.EnumPreference

enum class DiveMode(val humanReadableName: String) : EnumPreference {
    OPEN_CIRCUIT("Open circuit") {
        override val preferenceValue: String = "open-circuit"
    },
    CLOSED_CIRCUIT("CCR") {
        override val preferenceValue: String = "closed-circuit"
    },
}
