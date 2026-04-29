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

package org.neotech.app.abysner.store

import kotlin.time.Duration.Companion.hours
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.DiveMode
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.physics.METERS_PER_FOOT
import org.neotech.app.abysner.domain.diveplanning.model.CylinderRole
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanInputModel
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.diveplanning.model.MultiDivePlanInputModel
import org.neotech.app.abysner.domain.diveplanning.model.PlannedCylinderModel

object StoreScreenshotFixtures {

    private val airCylinder = Cylinder.D12.fill(gas = Gas.Air)
    private val nitrox50Cylinder = Cylinder.aluminium80Cuft(gas = Gas.Nitrox50, pressure = 207.0)
    private val nitrox80Cylinder = Cylinder.aluminium63Cuft(gas = Gas.Nitrox80, pressure = 232.0)
    private val oxygenCylinder = Cylinder.steel3LiterOxygen()
    val trimixDiluent = Cylinder.aluminium80Cuft(gas = Gas.Trimix2135)

    private val openCircuitDive = DivePlanInputModel(
        diveMode = DiveMode.OPEN_CIRCUIT,
        deeper = true,
        longer = false,
        bailout = false,
        plannedProfile = listOf(DiveProfileSection(30, 25.0, airCylinder)),
        cylinders = listOf(
            PlannedCylinderModel(cylinder = airCylinder, isChecked = true, isLocked = true),
            PlannedCylinderModel(cylinder = nitrox50Cylinder, isChecked = true, isLocked = false),
            PlannedCylinderModel(cylinder = nitrox80Cylinder, isChecked = false, isLocked = false),
        ),
        surfaceIntervalBefore = null,
    )

    private val closedCircuitDive = DivePlanInputModel(
        diveMode = DiveMode.CLOSED_CIRCUIT,
        deeper = false,
        longer = false,
        bailout = true,
        plannedProfile = listOf(DiveProfileSection(30, 40.0, trimixDiluent)),
        cylinders = listOf(
            PlannedCylinderModel(
                cylinder = oxygenCylinder,
                isChecked = true,
                isLocked = true,
                role = CylinderRole.CCR_OXYGEN,
            ),
            PlannedCylinderModel(
                cylinder = trimixDiluent,
                isChecked = true,
                isLocked = true,
                role = CylinderRole.CCR_DILUENT_AND_BAILOUT,
            ),
            PlannedCylinderModel(cylinder = nitrox50Cylinder, isChecked = true, isLocked = false),
        ),
        surfaceIntervalBefore = 1.hours,
    )

    val input = MultiDivePlanInputModel(dives = listOf(openCircuitDive, closedCircuitDive))

    val imperialInput = input.updateDive(0) {
        copy(plannedProfile = listOf(DiveProfileSection(30, 80 * METERS_PER_FOOT, airCylinder)))
    }
}
