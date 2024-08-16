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

package org.neotech.app.abysner.domain.gasplanning.model

import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.utilities.merge

data class GasPlan(
    val base: Map<Cylinder, Double>,
    val extraRequiredForWorstCaseOutOfAir: Map<Cylinder, Double>
) {

    val sortedBase = base.toList().sortedByDescending { it.second }
    val extraRequiredForWorstCaseOutOfAirSorted = extraRequiredForWorstCaseOutOfAir.toList().sortedByDescending { it.second }

    val total = base.merge(extraRequiredForWorstCaseOutOfAir, Double::plus).toList().sortedByDescending { it.second }
}
