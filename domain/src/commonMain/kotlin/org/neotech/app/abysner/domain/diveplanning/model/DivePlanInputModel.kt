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
) {

    fun addSegment(section: DiveProfileSection): DivePlanInputModel =
        update(profile = plannedProfile + section)

    fun updateSegment(index: Int, section: DiveProfileSection): DivePlanInputModel {
        val updatedProfile = plannedProfile.toMutableList().apply { set(index, section) }
        return update(profile = updatedProfile)
    }

    fun removeSegment(index: Int): DivePlanInputModel {
        val updatedProfile = plannedProfile.toMutableList().apply { removeAt(index) }
        return update(profile = updatedProfile)
    }

    fun addCylinder(cylinder: Cylinder): DivePlanInputModel {
        val newCylinder = PlannedCylinderModel(cylinder, isChecked = false, isLocked = false)
        val updatedCylinders = (cylinders + newCylinder)
            .sortedBy { it.cylinder.gas.oxygenFraction }
        return update(cylinders = updatedCylinders)
    }

    fun updateCylinder(cylinder: Cylinder): DivePlanInputModel {
        val newCylinders = cylinders.toMutableList().apply {
            val index = indexOfFirst { it.cylinder.uniqueIdentifier == cylinder.uniqueIdentifier }
            set(index, get(index).copy(cylinder = cylinder))
        }
        val newProfile = plannedProfile.map {
            // Cylinder objects are immutable, if updated, also update the profile sections that use
            // it, since the object references will be different.
            if (it.cylinder.uniqueIdentifier == cylinder.uniqueIdentifier) {
                it.copy(cylinder = cylinder)
            } else {
                it
            }
        }
        return update(profile = newProfile, cylinders = newCylinders)
    }

    fun removeCylinder(cylinder: Cylinder): DivePlanInputModel {
        check(cylinders.none { it.cylinder == cylinder && it.isLocked }) {
            "A locked cylinder cannot be removed, the caller must not allow this action."
        }
        val remainingCylinders = cylinders.filterNot { it.cylinder == cylinder }

        val updatedProfile = if (diveMode.isCcr) {
            // In CCR mode, bailout cylinders are not locked and may be referenced by segments. But
            // segments should not reference removed cylinders so we need to update those. The
            // diluent is always locked and available as a fallback for reassignment.
            val fallback = remainingCylinders.ccrDiluentCylinder()!!.cylinder
            plannedProfile.map { section ->
                if (section.cylinder == cylinder) {
                    section.copy(cylinder = fallback)
                } else {
                    section
                }
            }
        } else {
            // In OC mode, a cylinder referenced by a segment is locked and should not be removable,
            // the profile does not change when removing an unused cylinder.
            plannedProfile
        }

        return update(profile = updatedProfile, cylinders = remainingCylinders)
    }

    fun toggleCylinder(cylinder: Cylinder, enabled: Boolean): DivePlanInputModel {
        check(cylinders.none { it.cylinder == cylinder && it.isLocked } || enabled) {
            "A locked cylinder cannot be disabled, the caller must not allow this action."
        }
        return update(
            cylinders = cylinders.map {
                if (it.cylinder == cylinder) {
                    it.copy(isChecked = enabled)
                } else {
                    it
                }
            }
        )
    }

    fun toggleAvailableForBailout(cylinder: Cylinder, availableForBailout: Boolean): DivePlanInputModel {
        return update(
            cylinders = cylinders.map {
                if (it.cylinder == cylinder) {
                    it.copy(role = it.role.toggleAvailableForBailout(availableForBailout))
                } else {
                    it
                }
            }
        )
    }

    fun setContingency(deeper: Boolean, longer: Boolean, bailout: Boolean): DivePlanInputModel =
        copy(deeper = deeper, longer = longer, bailout = bailout)

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
    fun setDiveMode(mode: DiveMode): DivePlanInputModel {
        if (diveMode == mode) {
            return this
        }

        return when (mode) {
            DiveMode.OPEN_CIRCUIT -> {
                // Uncheck CCR specific cylinders, but preserve roles for round-trip, they may be
                // checked and locked again if required by the OC plan (recomputeCylinderState)
                val updatedCylinders = cylinders.map {
                    if (it.isCcrOxygen || it.isCcrDiluent) {
                        it.copy(isChecked = false, isLocked = false)
                    } else {
                        it
                    }
                }
                copy(
                    diveMode = DiveMode.OPEN_CIRCUIT,
                    bailout = false,
                    cylinders = recomputeCylinderState(plannedProfile, updatedCylinders, DiveMode.OPEN_CIRCUIT)
                )
            }
            DiveMode.CLOSED_CIRCUIT -> {
                // Re-enable any existing CCR cylinders
                var updatedCylinders = cylinders.map {
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
                    updatedCylinders = ensureDiluent(updatedCylinders, plannedProfile)
                }

                copy(
                    diveMode = DiveMode.CLOSED_CIRCUIT,
                    cylinders = recomputeCylinderState(plannedProfile, updatedCylinders, DiveMode.CLOSED_CIRCUIT)
                )
            }
        }
    }

    /**
     * Recomputes [PlannedCylinderModel.isLocked] for this dive. Call this for every dive after
     * loading persisted data (e.g. via `model.dives.map { it.recomputeCylinderState() }`).
     */
    fun recomputeCylinderState(): DivePlanInputModel =
        copy(cylinders = recomputeCylinderState(plannedProfile, cylinders, diveMode))

    /**
     * Finds the best diluent candidate from the planned profile and marks it as diluent, or creates
     * a default Air diluent if no suitable candidate exists.
     */
    private fun ensureDiluent(
        cylinders: List<PlannedCylinderModel>,
        profile: List<DiveProfileSection>
    ): List<PlannedCylinderModel> {
        val diluentGas = profile.maxByOrNull { it.depthInMeters }?.cylinder?.gas

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

    private fun recomputeCylinderState(segments: List<DiveProfileSection>, cylinders: List<PlannedCylinderModel>, diveMode: DiveMode = DiveMode.OPEN_CIRCUIT): List<PlannedCylinderModel> {
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

    companion object {

        private val defaultCylinderAir = Cylinder.steel12Liter(gas = Gas.Air, pressure = 232.0)

        val Default: DivePlanInputModel = DivePlanInputModel(
            diveMode = DiveMode.OPEN_CIRCUIT,
            deeper = false,
            longer = false,
            bailout = false,
            plannedProfile = listOf(
                DiveProfileSection(
                    30,
                    25.0,
                    defaultCylinderAir
                )
            ),
            cylinders = listOf(
                PlannedCylinderModel(
                    cylinder = defaultCylinderAir,
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
                )
            ),
            surfaceIntervalBefore = null,
        )
    }
}

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
