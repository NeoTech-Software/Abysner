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
import org.neotech.app.abysner.domain.diveplanning.model.CylinderRole
import org.neotech.app.abysner.domain.diveplanning.model.toggleAvailableForBailout
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanInputModel
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.diveplanning.model.PlannedCylinderModel
import org.neotech.app.abysner.domain.diveplanning.model.ccrDiluentCylinder
import org.neotech.app.abysner.domain.diveplanning.model.countCheckedGas

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
            // Cylinder objects are immutable, if updated, also update the profile sections that use
            // it, since the object references will be different.
            if (it.cylinder.uniqueIdentifier == cylinder.uniqueIdentifier) {
                it.copy(cylinder = cylinder)
            } else {
                it
            }
        }
        return dive.update(profile = newProfile, cylinders = newCylinders)
    }

    fun removeCylinder(dive: DivePlanInputModel, cylinder: Cylinder): DivePlanInputModel {
        check(dive.cylinders.none { it.cylinder == cylinder && it.isLocked }) {
            "A locked cylinder cannot be removed, the caller must not allow this action."
        }
        val remainingCylinders = dive.cylinders.filterNot { it.cylinder == cylinder }

        // Reassign any segments that referenced the removed cylinder. In CCR mode this can happen
        // because bailout cylinders are not locked. A diluent always exists as fallback because
        // it is locked and cannot be removed.
        val fallback = remainingCylinders.ccrDiluentCylinder()?.cylinder
            ?: remainingCylinders.first().cylinder
        // TODO in theory this will probably impossible given that the interface forbids it, the
        //      user might be able to orphan a section from a true cylinder, then this could cause
        //      data restore failures in  DivePlanInputResourceV1.toModel()?
        //      Perhaps sections should be allowed to have a nullable cylinder? This allows for auto
        //      selection mode during planning?
        val updatedProfile = dive.plannedProfile.map { section ->
            if (section.cylinder == cylinder) {
                section.copy(cylinder = fallback)
            } else {
                section
            }
        }

        return dive.update(profile = updatedProfile, cylinders = remainingCylinders)
    }

    fun toggleCylinder(dive: DivePlanInputModel, cylinder: Cylinder, enabled: Boolean): DivePlanInputModel {
        check(dive.cylinders.none { it.cylinder == cylinder && it.isLocked } || enabled) {
            "A locked cylinder cannot be disabled, the caller must not allow this action."
        }
        return dive.update(
            cylinders = dive.cylinders.map {
                if (it.cylinder == cylinder) {
                    it.copy(isChecked = enabled)
                } else {
                    it
                }
            }
        )
    }

    fun toggleAvailableForBailout(dive: DivePlanInputModel, cylinder: Cylinder, availableForBailout: Boolean): DivePlanInputModel {
        return dive.update(
            cylinders = dive.cylinders.map {
                if (it.cylinder == cylinder) {
                    it.copy(role = it.role.toggleAvailableForBailout(availableForBailout))
                } else {
                    it
                }
            }
        )
    }

    fun setContingency(dive: DivePlanInputModel, deeper: Boolean, longer: Boolean, bailout: Boolean): DivePlanInputModel =
        dive.copy(deeper = deeper, longer = longer, bailout = bailout)

    /**
     * Switches the dive mode between OC and CCR.
     *
     * Switch to CCR:
     * - Oxygen cylinder is automatically added if no [CylinderRole.CCR_OXYGEN] is found.
     * - Diluent cylinder is taken from the deepest dive segment if no
     *   [CylinderRole.CCR_DILUENT_AND_BAILOUT] or [CylinderRole.CCR_DILUENT] role is found.
     *
     * Switch to OC:
     *  - Oxygen cylinder is unchecked, role is kept (for round-trip to CCR)
     *  - Diluent cylinder is unchecked, role is kept (for round-trip to CCR)
     *  - Note: Both cylinders may be checked and even locked again if they are in use in the profile
     */
    fun setDiveMode(dive: DivePlanInputModel, mode: DiveMode): DivePlanInputModel {
        if (dive.diveMode == mode) {
            return dive
        }

        return when (mode) {
            DiveMode.OPEN_CIRCUIT -> {
                // Uncheck CCR specific cylinders, but preserve roles for round-trip, they may be
                // checked and locked again if required by the OC plan (recomputeCylinderState)
                val updatedCylinders = dive.cylinders.map {
                    if (it.isCcrOxygen || it.isCcrDiluent) {
                        it.copy(isChecked = false, isLocked = false)
                    } else {
                        it
                    }
                }
                dive.copy(
                    diveMode = DiveMode.OPEN_CIRCUIT,
                    bailout = false,
                    cylinders = recomputeCylinderState(dive.plannedProfile, updatedCylinders, DiveMode.OPEN_CIRCUIT)
                )
            }
            DiveMode.CLOSED_CIRCUIT -> {
                // Re-enable any existing CCR cylinders
                var updatedCylinders = dive.cylinders.map {
                    if (it.isCcrOxygen || it.isCcrDiluent) {
                        it.copy(isChecked = true)
                    } else {
                        it
                    }
                }

                // If no oxygen cylinder exists: add one
                if (updatedCylinders.none { it.isCcrOxygen }) {
                    updatedCylinders = updatedCylinders + PlannedCylinderModel(
                        cylinder = Cylinder(Gas.Oxygen, pressure = 200.0, waterVolume = 3.0),
                        isChecked = true,
                        isLocked = true,
                        role = CylinderRole.CCR_OXYGEN,
                    )
                }

                // If no diluent exists: select one from the dive profile or add a default.
                if (updatedCylinders.none { it.isCcrDiluent }) {
                    updatedCylinders = ensureDiluent(updatedCylinders, dive.plannedProfile)
                }

                dive.copy(
                    diveMode = DiveMode.CLOSED_CIRCUIT,
                    cylinders = recomputeCylinderState(dive.plannedProfile, updatedCylinders, DiveMode.CLOSED_CIRCUIT)
                )
            }
        }
    }

    /**
     * Finds the best diluent candidate from the planned profile and marks it as diluent, or creates
     * a default Air diluent if no suitable candidate exists.
     */
    private fun ensureDiluent(
        cylinders: List<PlannedCylinderModel>,
        profile: List<DiveProfileSection>
    ): List<PlannedCylinderModel> {
        val diluentGas = profile.maxByOrNull { it.depth }?.cylinder?.gas

        val mostLikelyDiluentCylinder = if (diluentGas != null) {
            cylinders.filter { it.cylinder.gas == diluentGas && !it.isCcrOxygen }
                .minByOrNull { it.cylinder.waterVolume }
        } else {
            null
        }

        return if (mostLikelyDiluentCylinder != null) {
            cylinders.map {
                if (it == mostLikelyDiluentCylinder) {
                    it.copy(role = CylinderRole.CCR_DILUENT_AND_BAILOUT, isChecked = true)
                } else {
                    it
                }
            }
        } else {
            cylinders + PlannedCylinderModel(
                cylinder = Cylinder(Gas.Air, pressure = 200.0, waterVolume = 12.0),
                isChecked = true,
                isLocked = true,
                role = CylinderRole.CCR_DILUENT_AND_BAILOUT,
            )
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

        val autoChecked = mutableSetOf<Gas>()
        val updated = cylinders.map { planned ->
            if (diveMode.isCcr && (planned.isCcrOxygen || planned.isCcrDiluent)) {
                planned.copy(isChecked = true)
            } else if (diveMode.isCcr) {
                // In CCR mode, bailout cylinders are fully managed by the user, no auto-checking.
                planned
            } else {
                // In OC mode, we check cylinders that are in use by a segment
                val gas = planned.cylinder.gas
                val shouldAutoCheck = gas in gasesInUse && cylinders.countCheckedGas(gas) == 0
                if (shouldAutoCheck && gas !in autoChecked) {
                    autoChecked += gas
                    planned.copy(isChecked = true)
                } else {
                    planned
                }
            }
        }

        return updated.map { planned ->
            if (diveMode.isCcr && (planned.isCcrOxygen || planned.isCcrDiluent)) {
                planned.copy(isLocked = true)
            } else if (diveMode.isCcr) {
                // In CCR mode, bailout cylinders are never locked, always removable.
                planned.copy(isLocked = false)
            } else {
                val isUniqueInUse = { updated.countCheckedGas(planned.cylinder.gas) == 1 }
                val isInUse = { planned.cylinder.gas in gasesInUse }
                planned.copy(isLocked = planned.isChecked && isInUse() && isUniqueInUse())
            }
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
