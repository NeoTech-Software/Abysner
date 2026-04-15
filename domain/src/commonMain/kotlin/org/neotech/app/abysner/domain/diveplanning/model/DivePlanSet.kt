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
import org.neotech.app.abysner.domain.core.model.DiveMode
import org.neotech.app.abysner.domain.gasplanning.model.GasPlan

// TODO: Find a better name, now it implies a collection of plans. Maybe DivePlanResult or ComputedDivePlan?
data class DivePlanSet(
    val base: DivePlan,
    val deeper: Int?,
    val longer: Int?,
    val bailout: Boolean,
    val diveMode: DiveMode,
    val gasPlan: GasPlan,
) {

    val isDeeper = deeper != null
    val isLonger = longer != null
    val isCcr = diveMode.isCcr

    val configuration: Configuration = base.configuration
    val isEmpty: Boolean = base.isEmpty
}
