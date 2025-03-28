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

data class MultiDivePlanInputModel(
    val dives: List<DivePlanInputModel>,
) {
    init {
        require(dives.isNotEmpty()) { "At least one dive is required." }
    }

    fun updateDive(index: Int, block: DivePlanInputModel.() -> DivePlanInputModel) =
        copy(dives = dives.toMutableList().also { it[index] = it[index].block() })
}
