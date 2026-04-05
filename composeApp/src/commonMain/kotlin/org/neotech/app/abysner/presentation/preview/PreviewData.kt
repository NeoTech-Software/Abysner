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

package org.neotech.app.abysner.presentation.preview

import kotlinx.collections.immutable.persistentListOf
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.DivePlanner
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanSet
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.diveplanning.model.PlannedCylinderModel
import org.neotech.app.abysner.domain.gasplanning.GasPlanner

object PreviewData {

    val divePlan1Segments by lazy {
        persistentListOf(
            DiveProfileSection(16, 45, Cylinder(gas = Gas.Air, pressure = 232.0, waterVolume = 12.0)),
        )
    }

    val divePlan1Cylinders by lazy {
        listOf(
            PlannedCylinderModel(
                cylinder = Cylinder.steel12Liter(gas = Gas.Air, pressure = 232.0),
                isLocked = true,
                isChecked = true
            ),
            PlannedCylinderModel(
                cylinder = Cylinder.aluminium80Cuft(gas = Gas.Nitrox50, pressure = 207.0),
                isLocked = false,
                isChecked = true
            ),
            PlannedCylinderModel(
                cylinder = Cylinder.aluminium63Cuft(gas = Gas.Nitrox80, pressure = 207.0),
                isLocked = false,
                isChecked = false
            ),
        )
    }

    val divePlan1: DivePlanSet by lazy {
        val divePlan = DivePlanner().apply {
            configuration = Configuration()
        }.addDive(
            plan = divePlan1Segments,
            cylinders = divePlan1Cylinders.filter { it.isChecked }.map { it.cylinder },
        )
        val gasPlan = GasPlanner().calculateGasPlan(divePlan)
        DivePlanSet(
            base = divePlan,
            deeper = null,
            longer = null,
            gasPlan = gasPlan
        )
    }
}
