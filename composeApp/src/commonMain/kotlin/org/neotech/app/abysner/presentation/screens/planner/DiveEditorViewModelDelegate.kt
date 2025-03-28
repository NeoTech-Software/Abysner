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
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanInputModel
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.diveplanning.model.PlannedCylinderModel

/**
 * ViewModel delegate that handles single-dive mutations. Every method accepts a
 * [DivePlanInputModel], applies one logical change, and returns the updated dive. The caller
 * (ViewModel) is responsible for selecting the right dive out of the multi-dive model and writing
 * the result back.
 */
object DiveEditorViewModelDelegate {

    fun addSegment(dive: DivePlanInputModel, section: DiveProfileSection): DivePlanInputModel {
        val updatedProfile = dive.plannedProfile + section
        return dive.copy(plannedProfile = updatedProfile, cylinders = recomputeCylinderState(updatedProfile, dive.cylinders))
    }

    fun updateSegment(dive: DivePlanInputModel, index: Int, section: DiveProfileSection): DivePlanInputModel {
        val updatedProfile = dive.plannedProfile.toMutableList().apply { set(index, section) }
        return dive.copy(plannedProfile = updatedProfile, cylinders = recomputeCylinderState(updatedProfile, dive.cylinders))
    }

    fun removeSegment(dive: DivePlanInputModel, index: Int): DivePlanInputModel {
        val updatedProfile = dive.plannedProfile.toMutableList().apply { removeAt(index) }
        return dive.copy(plannedProfile = updatedProfile, cylinders = recomputeCylinderState(updatedProfile, dive.cylinders))
    }

    fun addCylinder(dive: DivePlanInputModel, cylinder: Cylinder): DivePlanInputModel {
        val updatedCylinders = (dive.cylinders + PlannedCylinderModel(cylinder, isChecked = false, isLocked = false))
            .sortedBy { it.cylinder.gas.oxygenFraction }
        return dive.copy(cylinders = recomputeCylinderState(dive.plannedProfile, updatedCylinders))
    }

    fun updateCylinder(dive: DivePlanInputModel, cylinder: Cylinder): DivePlanInputModel {
        val indexToUpdate = dive.cylinders.indexOfFirst { it.cylinder.uniqueIdentifier == cylinder.uniqueIdentifier }
        val newCylinders = dive.cylinders.toMutableList().apply { set(indexToUpdate, get(indexToUpdate).copy(cylinder = cylinder)) }
        val newProfile = dive.plannedProfile.map {
            // Cylinder objects are immutable, if updated, also update the profile sections that use it.
            if (it.cylinder.uniqueIdentifier == cylinder.uniqueIdentifier) { it.copy(cylinder = cylinder) } else { it }
        }
        return dive.copy(plannedProfile = newProfile, cylinders = recomputeCylinderState(newProfile, newCylinders))
    }

    fun removeCylinder(dive: DivePlanInputModel, cylinder: Cylinder): DivePlanInputModel {
        check(dive.cylinders.none { it.cylinder == cylinder && it.isLocked }) {
            "A locked cylinder cannot be removed, the caller must not allow this action."
        }
        val updatedCylinders = dive.cylinders.filterNot { it.cylinder == cylinder }
        return dive.copy(cylinders = recomputeCylinderState(dive.plannedProfile, updatedCylinders))
    }

    fun toggleCylinder(dive: DivePlanInputModel, cylinder: Cylinder, enabled: Boolean): DivePlanInputModel {
        check(dive.cylinders.none { it.cylinder == cylinder && it.isLocked } || enabled) {
            "A locked cylinder cannot be disabled, the caller must not allow this action."
        }
        val updatedCylinders = dive.cylinders.map { if (it.cylinder == cylinder) { it.copy(isChecked = enabled) } else { it } }
        return dive.copy(cylinders = recomputeCylinderState(dive.plannedProfile, updatedCylinders))
    }

    fun setContingency(dive: DivePlanInputModel, deeper: Boolean, longer: Boolean): DivePlanInputModel =
        dive.copy(deeper = deeper, longer = longer)

    /**
     * Recomputes [PlannedCylinderModel.isLocked] for a single dive. Call this for every dive
     * after loading persisted data (e.g. via `model.dives.map { recomputeCylinderState(it) }`).
     */
    fun recomputeCylinderState(dive: DivePlanInputModel): DivePlanInputModel =
        dive.copy(cylinders = recomputeCylinderState(dive.plannedProfile, dive.cylinders))

    private fun recomputeCylinderState(segments: List<DiveProfileSection>, cylinders: List<PlannedCylinderModel>): List<PlannedCylinderModel> {
        val gasesInUse = segments.mapTo(mutableSetOf()) { it.cylinder.gas }
        val autoChecked = mutableSetOf<Gas>()

        val updated = cylinders.map { planned ->
            val gas = planned.cylinder.gas
            val shouldAutoCheck = gas in gasesInUse && gas !in autoChecked && cylinders.count { it.cylinder.gas == gas && it.isChecked } == 0
            if (shouldAutoCheck) {
                autoChecked += gas
                planned.copy(isChecked = true)
            } else {
                planned
            }
        }
        return updated.map { planned ->
            val isUniqueInUse = { updated.count { it.cylinder.gas == planned.cylinder.gas && it.isChecked } == 1 }
            val isInUse = { planned.cylinder.gas in gasesInUse }
            planned.copy(isLocked = planned.isChecked && isInUse() && isUniqueInUse())
        }
    }
}
