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

package org.neotech.app.abysner.presentation.screens.planner

import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.DiveMode
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanInputModel
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.diveplanning.model.PlannedCylinderModel
import org.neotech.app.abysner.domain.diveplanning.model.countCheckedGas
import org.neotech.app.abysner.domain.diveplanning.model.hasGas

/**
 * ViewModel delegate that handles single-dive mutations. Every method accepts a
 * [DivePlanInputModel], applies one logical change, and returns the updated dive. The caller
 * (ViewModel) is responsible for selecting the right dive out of the multi-dive model and writing
 * the result back.
 */
object DiveEditorViewModelDelegate {

    fun addSegment(dive: DivePlanInputModel, section: DiveProfileSection): DivePlanInputModel =
        dive.update(profile = dive.plannedProfile + section)

    fun updateSegment(dive: DivePlanInputModel, index: Int, section: DiveProfileSection): DivePlanInputModel {
        val updatedProfile = dive.plannedProfile.toMutableList().apply { set(index, section) }
        return dive.update(profile = updatedProfile)
    }

    fun removeSegment(dive: DivePlanInputModel, index: Int): DivePlanInputModel {
        val updatedProfile = dive.plannedProfile.toMutableList().apply { removeAt(index) }
        return dive.update(profile = updatedProfile)
    }

    fun addCylinder(dive: DivePlanInputModel, cylinder: Cylinder): DivePlanInputModel {
        val newCylinder = PlannedCylinderModel(cylinder, isChecked = false, isLocked = false)
        val updatedCylinders = (dive.cylinders + newCylinder)
            .sortedBy { it.cylinder.gas.oxygenFraction }
        return dive.update(cylinders = updatedCylinders)
    }

    fun updateCylinder(dive: DivePlanInputModel, cylinder: Cylinder): DivePlanInputModel {
        val newCylinders = dive.cylinders.toMutableList().apply {
            val index = indexOfFirst { it.cylinder.uniqueIdentifier == cylinder.uniqueIdentifier }
            set(index, get(index).copy(cylinder = cylinder))
        }
        val newProfile = dive.plannedProfile.map {
            // Cylinder objects are immutable, if updated, also update the profile sections that use it.
            if (it.cylinder.uniqueIdentifier == cylinder.uniqueIdentifier) it.copy(cylinder = cylinder) else it
        }
        return dive.update(profile = newProfile, cylinders = newCylinders)
    }

    fun removeCylinder(dive: DivePlanInputModel, cylinder: Cylinder): DivePlanInputModel {
        check(dive.cylinders.none { it.cylinder == cylinder && it.isLocked }) {
            "A locked cylinder cannot be removed, the caller must not allow this action."
        }
        return dive.update(cylinders = dive.cylinders.filterNot { it.cylinder == cylinder })
    }

    fun toggleCylinder(dive: DivePlanInputModel, cylinder: Cylinder, enabled: Boolean): DivePlanInputModel {
        check(dive.cylinders.none { it.cylinder == cylinder && it.isLocked } || enabled) {
            "A locked cylinder cannot be disabled, the caller must not allow this action."
        }
        return dive.update(cylinders = dive.cylinders.map { if (it.cylinder == cylinder) it.copy(isChecked = enabled) else it })
    }

    fun setContingency(dive: DivePlanInputModel, deeper: Boolean, longer: Boolean, bailout: Boolean): DivePlanInputModel =
        dive.copy(deeper = deeper, longer = longer, bailout = bailout)

    /**
     * Switches the dive mode between OC and CCR, auto-creating the O2 injection cylinder when
     * switching to CCR and removing it when switching back to OC.
     */
    fun setDiveMode(dive: DivePlanInputModel, mode: DiveMode): DivePlanInputModel {
        if (dive.diveMode == mode) {
            return dive
        }

        return when (mode) {
            DiveMode.OPEN_CIRCUIT -> {
                // Uncheck the oxygen CCR cylinder so it is preserved, but do disable it if possible
                val updatedCylinders = dive.cylinders.map {
                    if (it.cylinder.gas == Gas.Oxygen) {
                        it.copy(isChecked = false, isLocked = false)
                    } else {
                        it
                    }
                }
                dive.copy(
                    diveMode = DiveMode.OPEN_CIRCUIT,
                    bailout = false,
                    cylinders = recomputeCylinderState(dive.plannedProfile, updatedCylinders, DiveMode.OPEN_CIRCUIT),
                )
            }
            DiveMode.CLOSED_CIRCUIT -> {
                // Add an oxygen cylinder if non exists
                val updatedCylinders = if (dive.cylinders.hasGas(Gas.Oxygen)) {
                    dive.cylinders
                } else {
                    val newOxygenCylinder = PlannedCylinderModel(
                        cylinder = Cylinder(Gas.Oxygen, pressure = 200.0, waterVolume = 3.0),
                        isChecked = true,
                        isLocked = true
                    )
                    dive.cylinders + newOxygenCylinder
                }
                val recomputed = recomputeCylinderState(dive.plannedProfile, updatedCylinders, DiveMode.CLOSED_CIRCUIT)
                dive.copy(
                    diveMode = DiveMode.CLOSED_CIRCUIT,
                    cylinders = recomputed,
                )
            }
        }
    }

    /**
     * Recomputes [PlannedCylinderModel.isLocked] for a single dive. Call this for every dive
     * after loading persisted data (e.g. via `model.dives.map { recomputeCylinderState(it) }`).
     */
    fun recomputeCylinderState(dive: DivePlanInputModel): DivePlanInputModel =
        dive.copy(cylinders = recomputeCylinderState(dive.plannedProfile, dive.cylinders, dive.diveMode))

    fun recomputeCylinderState(segments: List<DiveProfileSection>, cylinders: List<PlannedCylinderModel>, diveMode: DiveMode = DiveMode.OPEN_CIRCUIT): List<PlannedCylinderModel> {
        val gasesInUse = segments.mapTo(mutableSetOf()) { it.cylinder.gas }

        // In closed-circuit mode, pure oxygen must be treated as in-use
        if (diveMode == DiveMode.CLOSED_CIRCUIT && Gas.Oxygen !in gasesInUse && cylinders.hasGas(Gas.Oxygen)) {
            gasesInUse += Gas.Oxygen
        }

        val autoChecked = mutableSetOf<Gas>()
        val updated = cylinders.map { planned ->
            val gas = planned.cylinder.gas
            val shouldAutoCheck = gas in gasesInUse && cylinders.countCheckedGas(gas) == 0
            if (shouldAutoCheck && gas !in autoChecked) {
                autoChecked += gas
                planned.copy(isChecked = true)
            } else {
                planned
            }
        }
        return updated.map { planned ->
            val isUniqueInUse = { updated.countCheckedGas(planned.cylinder.gas) == 1 }
            val isInUse = { planned.cylinder.gas in gasesInUse }
            planned.copy(isLocked = planned.isChecked && isInUse() && isUniqueInUse())
        }
    }

    private fun DivePlanInputModel.update(
        profile: List<DiveProfileSection> = plannedProfile,
        cylinders: List<PlannedCylinderModel> = this.cylinders,
    ): DivePlanInputModel = copy(
        plannedProfile = profile,
        cylinders = recomputeCylinderState(profile, cylinders, diveMode)
    )
}
