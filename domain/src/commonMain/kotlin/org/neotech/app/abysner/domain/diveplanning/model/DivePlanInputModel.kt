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

import kotlin.time.Duration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.DiveMode
import org.neotech.app.abysner.domain.core.model.Gas

data class DivePlanInputModel(
    val diveMode: DiveMode,
    val deeper: Boolean,
    val longer: Boolean,
    val bailout: Boolean,
    val plannedProfile: List<DiveProfileSection>,
    val cylinders: List<PlannedCylinderModel>,
    /**
     * The surface interval before this dive, null if there was no preceding dive.
     */
    val surfaceIntervalBefore: Duration?,
)

data class PlannedCylinderModel(
    val cylinder: Cylinder,
    val isChecked: Boolean,
    /**
     * If true this cylinder will be locked from disabling or deleting it. Usually a result of
     * being actively referenced in a dive segment while also being the last of its kind (mix).
     */
    val isLocked: Boolean,
    val role: CylinderRole? = null,
) {
    val isCcrOxygen: Boolean get() = role.isCcrOxygen
    val isCcrDiluent: Boolean get() = role.isCcrDiluent
    val isAvailableForBailout: Boolean get() = role.isAvailableForBailout
}

fun List<PlannedCylinderModel>.hasGas(gas: Gas): Boolean = any { it.cylinder.gas == gas }

fun List<PlannedCylinderModel>.countGas(gas: Gas): Int = count { it.cylinder.gas == gas }

fun List<PlannedCylinderModel>.countCheckedGas(gas: Gas): Int = count { it.cylinder.gas == gas && it.isChecked }

fun List<PlannedCylinderModel>.ccrOxygenCylinder(): PlannedCylinderModel? =
    firstOrNull { it.isCcrOxygen }

fun List<PlannedCylinderModel>.ccrDiluentCylinder(): PlannedCylinderModel? =
    firstOrNull { it.isCcrDiluent }

fun List<PlannedCylinderModel>.bailoutCylinders(): List<PlannedCylinderModel> =
    filter { it.isAvailableForBailout && it.isChecked }

fun PlannedCylinderModel.toAssignedCylinder() = AssignedCylinder(
    cylinder = cylinder,
    role = role,
)

fun List<PlannedCylinderModel>.toAssignedCylinders() = map { it.toAssignedCylinder() }
