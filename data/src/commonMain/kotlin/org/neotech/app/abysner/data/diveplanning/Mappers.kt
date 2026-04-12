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

package org.neotech.app.abysner.data.diveplanning

import org.neotech.app.abysner.data.diveplanning.resources.ConfigurationResourceV1
import org.neotech.app.abysner.data.diveplanning.resources.DivePlanInputResourceV1
import org.neotech.app.abysner.data.diveplanning.resources.MultiDivePlanInputResourceV1
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.DiveMode
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.model.Salinity
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanInputModel
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.diveplanning.model.MultiDivePlanInputModel
import org.neotech.app.abysner.domain.diveplanning.model.PlannedCylinderModel
import org.neotech.app.abysner.domain.persistence.fromString
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun Configuration.toResource() = ConfigurationResourceV1(
    sacRate = sacRate,
    sacRateOutOfAir = sacRateOutOfAir,
    maxPPO2Deco = maxPPO2Deco,
    maxPPO2 = maxPPO2,
    maxEND = maxEND,
    maxAscentRate = maxAscentRate,
    maxDescentRate = maxDescentRate,
    gfLow = gfLow,
    gfHigh = gfHigh,
    forceMinimalDecoStopTime = forceMinimalDecoStopTime,
    useDecoGasBetweenSections = useDecoGasBetweenSections,
    decoStepSize = decoStepSize,
    lastDecoStopDepth = lastDecoStopDepth,
    salinity = salinity.preferenceValue,
    altitude = altitude,
    algorithm = algorithm.preferenceValue,
    contingencyDeeper = contingencyDeeper,
    contingencyLonger = contingencyLonger,
    gasSwitchTime = gasSwitchTime
)

fun ConfigurationResourceV1.toModel() = Configuration(
    sacRate = sacRate,
    sacRateOutOfAir = sacRateOutOfAir,
    maxPPO2Deco = maxPPO2Deco,
    maxPPO2 = maxPPO2,
    maxEND = maxEND,
    maxAscentRate = maxAscentRate,
    maxDescentRate = maxDescentRate,
    gfLow = gfLow,
    gfHigh = gfHigh,
    forceMinimalDecoStopTime = forceMinimalDecoStopTime,
    useDecoGasBetweenSections = useDecoGasBetweenSections,
    decoStepSize = decoStepSize,
    lastDecoStopDepth = lastDecoStopDepth,
    salinity = fromString<Salinity>(salinity),
    altitude = altitude,
    algorithm = fromString<Configuration.Algorithm>(algorithm),
    contingencyDeeper = contingencyDeeper,
    contingencyLonger = contingencyLonger,
    gasSwitchTime = gasSwitchTime
)


fun DivePlanInputModel.toResource() = DivePlanInputResourceV1(
    diveMode = diveMode.preferenceValue,
    deeper = deeper,
    longer = longer,
    cylinders = cylinders.map { it.toResource() },
    profile = plannedProfile.map { it.toResource() },
    surfaceIntervalBeforeMinutes = surfaceIntervalBefore?.inWholeMinutes?.toInt(),
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
    val cylinders = cylinders.map { it.toModel() }
    return DivePlanInputModel(
        diveMode = fromString<DiveMode>(diveMode),
        deeper = deeper,
        longer = longer,
        cylinders = cylinders,
        plannedProfile = profile.map {
            it.toModel(cylinders.find { cylinder -> cylinder.cylinder.uniqueIdentifier == it.cylinderIdentifier }!!.cylinder)
        },
        surfaceIntervalBefore = surfaceIntervalBeforeMinutes?.toDuration(DurationUnit.MINUTES),
    )
}

fun MultiDivePlanInputModel.toResource() = MultiDivePlanInputResourceV1(
    dives = dives.map { it.toResource() },
)

fun MultiDivePlanInputResourceV1.toModel() = MultiDivePlanInputModel(
    dives = dives.map { it.toModel() },
)

private fun DivePlanInputResourceV1.CheckableCylinderResource.toModel() = PlannedCylinderModel(
    cylinder = cylinder.toModel(),
    isChecked = checked,

    // Will be recalculated by the ViewModel
    isLocked = false,
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
