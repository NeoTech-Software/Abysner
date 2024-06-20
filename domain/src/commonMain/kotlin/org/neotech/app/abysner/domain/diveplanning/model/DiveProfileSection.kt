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

package org.neotech.app.abysner.domain.diveplanning.model

import org.neotech.app.abysner.domain.core.model.Gas

data class DiveProfileSection(
    /**
     * Duration is minutes.
     */
    val duration: Int,
    /**
     * Depth of this segment
     */
    val depth: Int,
    /**
     * Selected gas for this segment (usually travel or bottom gas)
     */
    val gas: Gas
)
