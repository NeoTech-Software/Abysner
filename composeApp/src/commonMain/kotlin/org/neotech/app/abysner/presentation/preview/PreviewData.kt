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
import org.neotech.app.abysner.domain.core.model.DiveMode
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.model.UnitSystem
import org.neotech.app.abysner.domain.core.physics.METERS_PER_FOOT
import org.neotech.app.abysner.domain.diveplanning.DivePlanner
import org.neotech.app.abysner.domain.diveplanning.model.CylinderRole
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanSet
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.diveplanning.model.PlannedCylinderModel
import org.neotech.app.abysner.domain.diveplanning.model.toAssignedCylinders
import org.neotech.app.abysner.domain.gasplanning.GasPlanner

object PreviewData {

    private val airCylinder = Cylinder.steel12Liter(gas = Gas.Air)
    private val airCylinderImperial = Cylinder.AL100.fill(gas = Gas.Air)
    private val nitrox50Cylinder = Cylinder.aluminium80Cuft(gas = Gas.Nitrox50)
    private val nitrox80Cylinder = Cylinder.aluminium63Cuft(gas = Gas.Nitrox80)
    private val oxygenCylinder = Cylinder.steel3LiterOxygen()
    private val diluentCylinder = Cylinder.aluminium80Cuft(gas = Gas.Trimix1845)

    val diveProfile30Meters by lazy {
        persistentListOf(DiveProfileSection(25, 30.0, airCylinder))
    }

    val diveProfile60Meters by lazy {
        persistentListOf(DiveProfileSection(15, 60.0, airCylinder))
    }

    val divePlan1Cylinders by lazy {
        listOf(
            PlannedCylinderModel(cylinder = airCylinder, isLocked = true, isChecked = true),
            PlannedCylinderModel(cylinder = nitrox50Cylinder, isLocked = false, isChecked = true),
            PlannedCylinderModel(cylinder = nitrox80Cylinder, isLocked = false, isChecked = false),
        )
    }

    val divePlan1: DivePlanSet by lazy {
        val divePlan = DivePlanner().addDive(
            plan = diveProfile30Meters,
            cylinders = divePlan1Cylinders.filter { it.isChecked }.toAssignedCylinders(),
        )
        val gasPlan = GasPlanner().calculateGasPlan(divePlan)
        DivePlanSet(
            base = divePlan,
            deeper = null,
            longer = null,
            bailout = false,
            diveMode = DiveMode.OPEN_CIRCUIT,
            gasPlan = gasPlan
        )
    }

    val divePlan2Segments by lazy {
        persistentListOf(DiveProfileSection(35, 68.0, airCylinder))
    }

    val divePlan2Cylinders by lazy {
        listOf(
            PlannedCylinderModel(cylinder = airCylinder, isLocked = true, isChecked = true),
            PlannedCylinderModel(cylinder = nitrox50Cylinder, isLocked = false, isChecked = true),
            PlannedCylinderModel(cylinder = nitrox80Cylinder, isLocked = false, isChecked = true),
        )
    }

    /**
     * A deliberately extreme open-circuit dive plan designed to trigger the maximum number of
     * warnings in the UI for preview/testing purposes.
     */
    val divePlan2: DivePlanSet by lazy {
        val divePlan = DivePlanner(Configuration(
            // Aggressive gradient factors to trigger warnings and errors without making the profile
            // too long
            gfLow = 0.85,
            gfHigh = 0.90
        ))
            .addDive(
                plan = divePlan2Segments,
                cylinders = divePlan2Cylinders.filter { it.isChecked }.toAssignedCylinders(),
        )
        val gasPlan = GasPlanner().calculateGasPlan(divePlan)
        DivePlanSet(
            base = divePlan,
            deeper = null,
            longer = null,
            bailout = false,
            diveMode = DiveMode.OPEN_CIRCUIT,
            gasPlan = gasPlan
        )
    }

    val divePlanCcrCylinders by lazy {
        listOf(
            PlannedCylinderModel(cylinder = oxygenCylinder, isLocked = true, isChecked = true, role = CylinderRole.CCR_OXYGEN),
            PlannedCylinderModel(cylinder = diluentCylinder, isLocked = true, isChecked = true, role = CylinderRole.CCR_DILUENT_AND_BAILOUT),
            PlannedCylinderModel(cylinder = nitrox50Cylinder, isLocked = false, isChecked = true),
        )
    }

    val divePlanCcr: DivePlanSet by lazy {
        val divePlan = DivePlanner().addDive(
            plan = diveProfile60Meters,
            cylinders = divePlanCcrCylinders.filter { it.isChecked }.toAssignedCylinders(),
            diveMode = DiveMode.CLOSED_CIRCUIT,
        )
        val gasPlan = GasPlanner().calculateGasPlan(divePlan)
        DivePlanSet(
            base = divePlan,
            deeper = null,
            longer = null,
            bailout = false,
            diveMode = DiveMode.CLOSED_CIRCUIT,
            gasPlan = gasPlan
        )
    }

    val divePlanCcrBailout: DivePlanSet by lazy {
        val divePlan = DivePlanner().addDive(
            plan = diveProfile30Meters,
            cylinders = divePlanCcrCylinders.filter { it.isChecked }.toAssignedCylinders(),
            diveMode = DiveMode.CLOSED_CIRCUIT,
            bailout = true,
        )
        val gasPlan = GasPlanner().calculateGasPlan(divePlan)
        DivePlanSet(
            base = divePlan,
            deeper = null,
            longer = null,
            bailout = true,
            diveMode = DiveMode.CLOSED_CIRCUIT,
            gasPlan = gasPlan
        )
    }

    val divePlan1ImperialProfile by lazy {
        persistentListOf(DiveProfileSection(25, 100.0 * METERS_PER_FOOT, airCylinderImperial))
    }

    val divePlan1ImperialCylinders by lazy {
        listOf(
            PlannedCylinderModel(cylinder = airCylinderImperial, isLocked = true, isChecked = true),
            PlannedCylinderModel(cylinder = nitrox50Cylinder, isLocked = false, isChecked = true),
        )
    }

    /**
     * Similar to [divePlan1] but with depth and cylinders changed to the closest sensible imperial
     * numbers.
     */
    val divePlan1Imperial: DivePlanSet by lazy {
        val divePlan = DivePlanner(unitSystem = UnitSystem.IMPERIAL).addDive(
            plan = divePlan1ImperialProfile,
            cylinders = divePlan1ImperialCylinders.filter { it.isChecked }.toAssignedCylinders(),
        )
        val gasPlan = GasPlanner().calculateGasPlan(divePlan)
        DivePlanSet(
            base = divePlan,
            deeper = null,
            longer = null,
            bailout = false,
            diveMode = DiveMode.OPEN_CIRCUIT,
            gasPlan = gasPlan
        )
    }
}
