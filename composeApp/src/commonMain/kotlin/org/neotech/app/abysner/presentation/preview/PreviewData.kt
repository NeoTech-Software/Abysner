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

package org.neotech.app.abysner.presentation.preview

import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.DivePlanner
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanSet
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.gasplanning.GasPlanner

object PreviewData {
    val divePlan: DivePlanSet
        get() {
            val divePlan = DivePlanner().apply {
                configuration = Configuration()
            }.addDive(
                plan = listOf(
                    DiveProfileSection(16, 45, Cylinder(gas = Gas.Air, pressure = 232.0, waterVolume = 12.0)),
                ),
                decoGases = listOf(Cylinder.aluminium80Cuft(Gas.Nitrox50)),
            )

            val gasPlan = GasPlanner().calculateGasPlan(
                divePlan
            )
            return DivePlanSet(
                base = divePlan,
                deeper = null,
                longer = null,
                gasPlan = gasPlan
            )
        }
}
