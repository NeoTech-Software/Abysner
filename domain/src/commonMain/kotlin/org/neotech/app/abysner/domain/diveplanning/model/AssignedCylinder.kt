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

package org.neotech.app.abysner.domain.diveplanning.model

import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas

data class AssignedCylinder(
    val cylinder: Cylinder,
    val role: CylinderRole? = null,
) {
    val gas: Gas
        get() = cylinder.gas

    val isCcrOxygen: Boolean get() = role.isCcrOxygen
    val isCcrDiluent: Boolean get() = role.isCcrDiluent
    val isAvailableForBailout: Boolean get() = role.isAvailableForBailout
}

fun List<AssignedCylinder>.ccrOxygenCylinder(): Cylinder? =
    firstOrNull { it.isCcrOxygen }?.cylinder

fun Cylinder.assign(
    role: CylinderRole? = null,
) = AssignedCylinder(
    cylinder = this,
    role = role,
)

fun List<Cylinder>.assign() = map { it.assign() }
