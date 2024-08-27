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

package org.neotech.app.abysner.data.plan

import org.neotech.app.abysner.data.plan.resources.DivePlanInputResourceV1
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanInputModel
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.diveplanning.model.PlannedCylinderModel

fun DivePlanInputModel.toResource() = DivePlanInputResourceV1(
    deeper = deeper,
    longer = longer,
    cylinders = cylinders.map { it.toResource() },
    profile = plannedProfile.map { it.toResource() }
)

private fun DiveProfileSection.toResource() = DivePlanInputResourceV1.ProfileSegmentResource(
    duration = duration,
    depth = depth,
    cylinderIdentifier = cylinder.uniqueIdentifier
)

private fun PlannedCylinderModel.toResource() = DivePlanInputResourceV1.CheckableCylinderResource(
    cylinder = cylinder.toResource(),
    checked = isChecked
)

private fun Cylinder.toResource() = DivePlanInputResourceV1.CylinderResource(
    gas = gas.toResource(),
    pressure = pressure,
    volume = waterVolume,
    uniqueIdentifier = uniqueIdentifier
)

private fun Gas.toResource() = DivePlanInputResourceV1.GasResource(
    oxygenFraction = oxygenFraction,
    heliumFraction = heliumFraction,
)

fun DivePlanInputResourceV1.toModel(): DivePlanInputModel {

    val inUse = profile.map { it.cylinderIdentifier }.toSet()
    val cylinders = cylinders.map { it.toModel(inUse.contains(it.cylinder.uniqueIdentifier)) }

    return DivePlanInputModel(
        deeper = deeper,
        longer = longer,
        cylinders = cylinders,
        plannedProfile = profile.map {
            it.toModel(cylinders.find { cylinder -> cylinder.cylinder.uniqueIdentifier == it.cylinderIdentifier  }!!.cylinder)
        }
    )
}

private fun DivePlanInputResourceV1.CheckableCylinderResource.toModel(isInUse: Boolean) = PlannedCylinderModel(
    cylinder = cylinder.toModel(),
    isChecked = checked,
    isInUse = isInUse
)

private fun DivePlanInputResourceV1.ProfileSegmentResource.toModel(cylinder: Cylinder) = DiveProfileSection(
    duration = duration,
    depth = depth,
    cylinder = cylinder
)

private fun DivePlanInputResourceV1.CylinderResource.toModel() = Cylinder(
    gas = gas.toModel(),
    pressure = pressure,
    waterVolume = volume,
    uniqueIdentifier = uniqueIdentifier
)

private fun DivePlanInputResourceV1.GasResource.toModel() = Gas(
    oxygenFraction = oxygenFraction,
    heliumFraction = heliumFraction,
)
