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

import org.neotech.app.abysner.domain.core.model.Configuration

data class MultiDivePlanSet(
    val divePlanSets: List<DivePlanSet>,
) {
    val isEmpty: Boolean = divePlanSets.all { it.isEmpty }

    // Configuration should be the same for all dives, from the UI not possible to change this.
    // In the future we probably want to support different configurations per dive
    val configuration: Configuration = divePlanSets.first().configuration
}

