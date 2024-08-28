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

package org.neotech.app.abysner.domain.diveplanning

import kotlinx.coroutines.flow.StateFlow
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanInputModel

interface PlanningRepository {

    val configuration: StateFlow<Configuration>

    fun updateConfiguration(updateBlock: (Configuration) -> Configuration)

    fun setDivePlanInput(divePlanInputModel: DivePlanInputModel)

    suspend fun getDivePlanInput(): DivePlanInputModel?
}
