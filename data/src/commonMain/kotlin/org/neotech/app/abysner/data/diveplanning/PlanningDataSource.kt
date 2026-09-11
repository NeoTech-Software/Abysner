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

package org.neotech.app.abysner.data.diveplanning

import org.neotech.app.abysner.data.diveplanning.resources.ConfigurationResourceV1
import org.neotech.app.abysner.data.diveplanning.resources.MultiDivePlanInputResourceV1

interface PlanningDataSource {

    suspend fun loadConfiguration(): ConfigurationResourceV1?

    suspend fun saveConfiguration(configuration: ConfigurationResourceV1)

    suspend fun loadPlan(): MultiDivePlanInputResourceV1?

    suspend fun savePlan(plan: MultiDivePlanInputResourceV1)
}

