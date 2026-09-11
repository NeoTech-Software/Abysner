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
import org.neotech.app.abysner.domain.core.model.DiveMode
import org.neotech.app.abysner.domain.core.model.Gas
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DivePlanInputModelTest {

    private val airCylinder = Cylinder.steel12Liter(Gas.Air)
    private val nitroxCylinder = Cylinder.aluminium80Cuft(Gas.Nitrox50)
    private val trimixCylinder = Cylinder.steel12Liter(Gas(0.21, 0.35))
    private val airSegment = DiveProfileSection(duration = 30, depthInMeters = 25.0, cylinder = airCylinder)

    @Test
    fun hasGas_trueWhenGasPresent() {
        val list = listOf(
            plannedCylinder(airCylinder),
            plannedCylinder(nitroxCylinder),
        )
        assertTrue(list.hasGas(Gas.Air))
    }

    @Test
    fun hasGas_falseWhenGasAbsent() {
        val list = listOf(plannedCylinder(airCylinder))
        assertFalse(list.hasGas(Gas.Nitrox50))
    }

    @Test
    fun countGas_countsAllMatchingCylinders() {
        val list = listOf(
            plannedCylinder(airCylinder),
            plannedCylinder(airCylinder),
            plannedCylinder(nitroxCylinder),
        )
        assertEquals(2, list.countGas(Gas.Air))
        assertEquals(1, list.countGas(Gas.Nitrox50))
    }

    @Test
    fun countCheckedGas_countsOnlyCheckedCylinders() {
        val list = listOf(
            plannedCylinder(airCylinder, isChecked = true),
            plannedCylinder(airCylinder, isChecked = false),
            plannedCylinder(nitroxCylinder, isChecked = true),
        )
        assertEquals(1, list.countCheckedGas(Gas.Air))
    }

    @Test
    fun ccrOxygenCylinder_returnsFirstOxygenRole() {
        val list = listOf(
            plannedCylinder(airCylinder, role = CylinderRole.CCR_DILUENT),
            plannedCylinder(nitroxCylinder, role = CylinderRole.CCR_OXYGEN),
        )
        val result = list.ccrOxygenCylinder()
        assertNotNull(result)
        assertEquals(nitroxCylinder, result.cylinder)
    }

    @Test
    fun ccrOxygenCylinder_nullWhenNoOxygenRole() {
        val list = listOf(plannedCylinder(airCylinder, role = CylinderRole.CCR_DILUENT))
        assertNull(list.ccrOxygenCylinder())
    }

    @Test
    fun ccrDiluentCylinder_returnsFirstDiluentRole() {
        val list = listOf(
            plannedCylinder(nitroxCylinder, role = CylinderRole.CCR_OXYGEN),
            plannedCylinder(airCylinder, role = CylinderRole.CCR_DILUENT),
        )
        val result = list.ccrDiluentCylinder()
        assertNotNull(result)
        assertEquals(airCylinder, result.cylinder)
    }

    @Test
    fun bailoutCylinders_returnsCheckedBailoutOnly() {
        val list = listOf(
            plannedCylinder(airCylinder, role = CylinderRole.CCR_DILUENT_AND_BAILOUT, isChecked = true),
            plannedCylinder(nitroxCylinder, role = null, isChecked = true),
            plannedCylinder(trimixCylinder, role = null, isChecked = false),
            plannedCylinder(airCylinder, role = CylinderRole.CCR_OXYGEN, isChecked = true),
        )
        val result = list.bailoutCylinders()
        // Both CCR_DILUENT_AND_BAILOUT and the checked nitroxCylinder are considered available for bailout.
        assertEquals(2, result.size)
    }

    @Test
    fun toAssignedCylinders_mapsAllWithRoles() {
        val list = listOf(
            plannedCylinder(airCylinder, role = CylinderRole.CCR_DILUENT),
            plannedCylinder(nitroxCylinder, role = null),
        )
        val assigned = list.toAssignedCylinders()
        assertEquals(2, assigned.size)
        assertEquals(CylinderRole.CCR_DILUENT, assigned[0].role)
        assertNull(assigned[1].role)
    }

    @Test
    fun truncateAtRuntime_exactFitReturnsUnchanged() {
        val sections = listOf(
            DiveProfileSection(duration = 10, depthInMeters = 20.0, cylinder = airCylinder),
            DiveProfileSection(duration = 5, depthInMeters = 20.0, cylinder = airCylinder),
        )
        val result = sections.truncateAtRuntime(15)
        assertEquals(2, result.size)
        assertEquals(10, result[0].duration)
        assertEquals(5, result[1].duration)
    }

    @Test
    fun truncateAtRuntime_partialTruncatesLastSection() {
        val sections = listOf(
            DiveProfileSection(duration = 10, depthInMeters = 20.0, cylinder = airCylinder),
            DiveProfileSection(duration = 20, depthInMeters = 30.0, cylinder = airCylinder),
        )
        val result = sections.truncateAtRuntime(15)
        assertEquals(2, result.size)
        assertEquals(10, result[0].duration)
        assertEquals(5, result[1].duration)
    }

    @Test
    fun truncateAtRuntime_zeroRuntimeReturnsEmpty() {
        val sections = listOf(
            DiveProfileSection(duration = 10, depthInMeters = 20.0, cylinder = airCylinder),
        )
        val result = sections.truncateAtRuntime(0)
        assertTrue(result.isEmpty())
    }

    @Test
    fun truncateAtRuntime_runtimeExceedsTotalReturnsAll() {
        val sections = listOf(
            DiveProfileSection(duration = 5, depthInMeters = 20.0, cylinder = airCylinder),
            DiveProfileSection(duration = 5, depthInMeters = 30.0, cylinder = airCylinder),
        )
        val result = sections.truncateAtRuntime(100)
        assertEquals(2, result.size)
        assertEquals(5, result[0].duration)
        assertEquals(5, result[1].duration)
    }

    @Test
    fun multiDivePlanInputModel_requiresAtLeastOneDive() {
        assertFailsWith<IllegalArgumentException> {
            MultiDivePlanInputModel(dives = emptyList())
        }
    }

    @Test
    fun multiDivePlanInputModel_updateDive_modifiesSingleDive() {
        val dive1 = divePlanInput()
        val dive2 = divePlanInput()
        val model = MultiDivePlanInputModel(dives = listOf(dive1, dive2))

        val updated = model.updateDive(1) { copy(deeper = true) }

        assertFalse(updated.dives[0].deeper)
        assertTrue(updated.dives[1].deeper)
    }

    @Test
    fun addSegment_referencedCylinderIsLocked() {
        val dive = createDive(
            cylinders = listOf(
                PlannedCylinderModel(cylinder = airCylinder, isChecked = true, isLocked = false),
            ),
            segments = listOf(airSegment)
        )

        val result = dive.addSegment(airSegment)

        assertTrue(result.cylinders[0].isLocked)
    }

    @Test
    fun removeCylinder_lockedCylinderThrows() {
        val dive = createDive()
        assertFailsWith<IllegalStateException> {
            dive.removeCylinder(airCylinder)
        }
    }

    @Test
    fun removeCylinder_unlockedCylinderIsRemoved() {
        val dive = createDive(
            cylinders = listOf(
                PlannedCylinderModel(cylinder = airCylinder, isChecked = true, isLocked = false),
                PlannedCylinderModel(cylinder = nitroxCylinder, isChecked = true, isLocked = false),
            ),
            segments = emptyList(),
        )

        val result = dive.removeCylinder(nitroxCylinder)

        assertEquals(1, result.cylinders.size)
        assertEquals(airCylinder, result.cylinders[0].cylinder)
    }

    @Test
    fun removeCylinder_segmentsReferencingRemovedCylinderAreReassigned() {
        val dive = createDive(
            cylinders = listOf(
                PlannedCylinderModel(
                    cylinder = airCylinder,
                    isChecked = true,
                    isLocked = false,
                    role = CylinderRole.CCR_DILUENT_AND_BAILOUT
                ),
                PlannedCylinderModel(cylinder = nitroxCylinder, isChecked = true, isLocked = false),
            ),
            segments = listOf(
                airSegment,
                DiveProfileSection(duration = 10, depthInMeters = 6.0, cylinder = nitroxCylinder),
            ),
        ).copy(diveMode = DiveMode.CLOSED_CIRCUIT)

        val result = dive.removeCylinder(nitroxCylinder)

        assertEquals(2, result.plannedProfile.size)
        assertEquals(airCylinder, result.plannedProfile[1].cylinder)
    }

    @Test
    fun removeCylinder_lastUnusedCylinderIsRemoved() {
        val dive = createDive(
            cylinders = listOf(
                PlannedCylinderModel(cylinder = airCylinder, isChecked = true, isLocked = false),
            ),
            segments = emptyList(),
        )

        val result = dive.removeCylinder(airCylinder)

        assertTrue(result.cylinders.isEmpty())
    }

    @Test
    fun toggleCylinder_uncheckedCylinderIsEnabled() {
        val dive = createDive(
            cylinders = listOf(
                PlannedCylinderModel(cylinder = airCylinder, isChecked = true, isLocked = true),
                PlannedCylinderModel(cylinder = nitroxCylinder, isChecked = false, isLocked = false),
            ),
            segments = listOf(airSegment)
        )

        val result = dive.toggleCylinder(nitroxCylinder, enabled = true)

        assertTrue(result.cylinders.first { it.cylinder == nitroxCylinder }.isChecked)
    }

    @Test
    fun toggleCylinder_lockedCylinderThrows() {
        val dive = createDive()
        assertFailsWith<IllegalStateException> {
            dive.toggleCylinder(airCylinder, enabled = false)
        }
    }

    @Test
    fun toggleCylinder_checkedUnlockedCylinderIsDisabled() {
        val dive = createDive(
            cylinders = listOf(
                PlannedCylinderModel(cylinder = airCylinder, isChecked = true, isLocked = false),
                PlannedCylinderModel(cylinder = nitroxCylinder, isChecked = true, isLocked = false),
            ),
            segments = emptyList()
        )

        val result = dive.toggleCylinder(nitroxCylinder, enabled = false)

        assertFalse(result.cylinders.first { it.cylinder == nitroxCylinder }.isChecked)
    }

    @Test
    fun recomputeCylinderState_referencedCylinderIsLockedAndUnreferencedIsUnlocked() {
        val dive = createDive(
            cylinders = listOf(
                PlannedCylinderModel(cylinder = airCylinder, isChecked = true, isLocked = false),
                PlannedCylinderModel(cylinder = nitroxCylinder, isChecked = true, isLocked = true),
            ),
            segments = listOf(airSegment)
        )
        val result = dive.recomputeCylinderState()

        assertTrue(result.cylinders[0].isLocked)
        assertFalse(result.cylinders[1].isLocked)
    }

    @Test
    fun recomputeCylinderState_ccrOxygenCylinderIsAutoCheckedAndLocked() {
        val dive = createDive(
            cylinders = listOf(
                PlannedCylinderModel(
                    cylinder = Cylinder(
                        gas = Gas.Oxygen,
                        pressure = 200.0,
                        waterVolume = 3.0
                    ),
                    isChecked = false,
                    isLocked = false,
                    role = CylinderRole.CCR_OXYGEN,
                ),
                PlannedCylinderModel(cylinder = airCylinder, isChecked = true, isLocked = false),
            ),
        ).copy(diveMode = DiveMode.CLOSED_CIRCUIT)

        val result = dive.recomputeCylinderState()

        val oxygenCylinder = result.cylinders.first { it.isCcrOxygen }
        assertTrue(oxygenCylinder.isChecked)
        assertTrue(oxygenCylinder.isLocked)
    }

    @Test
    fun setDiveMode_ccrSwitchAddsNewCcrOxygenCylinderIfRoleMissing() {
        val dive = createDive(
            cylinders = listOf(
                PlannedCylinderModel(cylinder = airCylinder, isChecked = true, isLocked = true),
                PlannedCylinderModel(
                    cylinder = Cylinder.aluminium63Cuft(Gas.Oxygen),
                    isChecked = false,
                    isLocked = false,
                ),
            )
        )

        val result = dive.setDiveMode(DiveMode.CLOSED_CIRCUIT)

        assertEquals(2, result.cylinders.countGas(Gas.Oxygen))
        val oxygen = result.cylinders.first { it.isCcrOxygen }
        assertEquals(CylinderRole.CCR_OXYGEN, oxygen.role)
        assertTrue(oxygen.isChecked)
        assertTrue(oxygen.isLocked)
    }

    @Test
    fun setDiveMode_ccrOxygenCylinderIsCheckedAndLocked() {
        val dive = createDive(
            cylinders = listOf(
                PlannedCylinderModel(cylinder = airCylinder, isChecked = true, isLocked = true),
                PlannedCylinderModel(
                    cylinder = Cylinder(gas = Gas.Oxygen, 200.0, 3.0),
                    isChecked = false,
                    isLocked = false,
                    role = CylinderRole.CCR_OXYGEN,
                ),
            ),
        )

        val result = dive.setDiveMode(DiveMode.CLOSED_CIRCUIT)

        val oxygen = result.cylinders.first { it.isCcrOxygen }
        assertTrue(oxygen.isChecked)
        assertTrue(oxygen.isLocked)
    }

    @Test
    fun setDiveMode_ccrSwitchUpdatesDiveMode() {
        val dive = createDive()

        val result = dive.setDiveMode(DiveMode.CLOSED_CIRCUIT)

        assertEquals(DiveMode.CLOSED_CIRCUIT, result.diveMode)
    }

    @Test
    fun setDiveMode_ccrSwitchAssignsDiluentRole() {
        val dive = createDive()

        val result = dive.setDiveMode(DiveMode.CLOSED_CIRCUIT)

        val diluent = result.cylinders.firstOrNull { it.isCcrDiluent }
        assertNotNull(diluent)
        assertTrue(diluent.isChecked)
        assertTrue(diluent.isLocked)
    }

    @Test
    fun setDiveMode_ccrSwitchCreatesDefaultAirDiluentWhenNoSegments() {
        val dive = createDive(
            segments = emptyList(),
            cylinders = listOf(
                PlannedCylinderModel(cylinder = nitroxCylinder, isChecked = true, isLocked = false),
                PlannedCylinderModel(
                    cylinder = Cylinder.steel3LiterOxygen(),
                    isChecked = false,
                    isLocked = false,
                    role = CylinderRole.CCR_OXYGEN,
                ),
            ),
        )

        val result = dive.setDiveMode(DiveMode.CLOSED_CIRCUIT)

        assertEquals(3, result.cylinders.size)
        val diluent = result.cylinders.first { it.isCcrDiluent }
        assertEquals(Gas.Air, diluent.cylinder.gas)
        assertEquals(CylinderRole.CCR_DILUENT_AND_BAILOUT, diluent.role)
        assertTrue(diluent.isChecked)
        assertTrue(diluent.isLocked)
    }

    @Test
    fun setDiveMode_ccrSwitchReusesExistingDiluentRole() {
        val dive = createDive(
            cylinders = listOf(
                PlannedCylinderModel(
                    cylinder = airCylinder,
                    isChecked = true,
                    isLocked = true,
                    role = CylinderRole.CCR_DILUENT
                ),
                PlannedCylinderModel(
                    cylinder = Cylinder.steel3LiterOxygen(),
                    isChecked = false,
                    isLocked = false,
                    role = CylinderRole.CCR_OXYGEN,
                ),
            ),
        )

        val result = dive.setDiveMode(DiveMode.CLOSED_CIRCUIT)

        val diluent = result.cylinders.first { it.cylinder == airCylinder }
        assertEquals(CylinderRole.CCR_DILUENT, diluent.role)
    }

    @Test
    fun setDiveMode_ocSwitchPreservesOxygenCylinderAndKeepsRole() {
        val dive = createDive().setDiveMode(DiveMode.CLOSED_CIRCUIT)

        val result = dive.setDiveMode(DiveMode.OPEN_CIRCUIT)

        val oxygen = result.cylinders.first { it.cylinder.gas == Gas.Oxygen }
        assertEquals(CylinderRole.CCR_OXYGEN, oxygen.role)
        assertFalse(oxygen.isChecked)
        assertFalse(oxygen.isLocked)
    }

    @Test
    fun setDiveMode_ocSwitchPreservesDiluentCylinderAndKeepsRole() {
        val dive = createDive().setDiveMode(DiveMode.CLOSED_CIRCUIT)

        val result = dive.setDiveMode(DiveMode.OPEN_CIRCUIT)

        val diluent = result.cylinders.first { it.cylinder == airCylinder }
        assertEquals(CylinderRole.CCR_DILUENT_AND_BAILOUT, diluent.role)
        // These are true since the diluent was used in the segment
        assertTrue(diluent.isChecked)
        assertTrue(diluent.isLocked)
    }

    @Test
    fun setDiveMode_roundTripPreservesDiluentRole() {
        val dive = createDive()
            .setDiveMode(DiveMode.CLOSED_CIRCUIT)
            .setDiveMode(DiveMode.OPEN_CIRCUIT)

        val result = dive.setDiveMode(DiveMode.CLOSED_CIRCUIT)

        val diluent = result.cylinders.first { it.cylinder == airCylinder }
        assertEquals(CylinderRole.CCR_DILUENT_AND_BAILOUT, diluent.role)
        assertTrue(diluent.isChecked)
        assertTrue(diluent.isLocked)
    }

    @Test
    fun setDiveMode_ocSwitchUpdatesDiveMode() {
        val dive = createDive().setDiveMode(DiveMode.CLOSED_CIRCUIT)

        val result = dive.setDiveMode(DiveMode.OPEN_CIRCUIT)

        assertEquals(DiveMode.OPEN_CIRCUIT, result.diveMode)
    }

    @Test
    fun setDiveMode_ocSwitchResetsBailout() {
        val dive = createDive()
            .setDiveMode(DiveMode.CLOSED_CIRCUIT)
            .setContingency(deeper = false, longer = false, bailout = true)

        val result = dive.setDiveMode(DiveMode.OPEN_CIRCUIT)

        assertFalse(result.bailout)
    }

    @Test
    fun setDiveMode_sameModeIsNoOp() {
        val dive = createDive()

        val result = dive.setDiveMode(DiveMode.OPEN_CIRCUIT)

        assertEquals(dive, result)
    }

    @Test
    fun setContingency_setsAllFlags() {
        val result = createDive().setContingency(deeper = true, longer = true, bailout = true)

        assertTrue(result.deeper)
        assertTrue(result.longer)
        assertTrue(result.bailout)
    }

    @Test
    fun setContingency_clearsAllFlags() {
        val dive = createDive().copy(deeper = true, longer = true, bailout = true)

        val result = dive.setContingency(deeper = false, longer = false, bailout = false)

        assertFalse(result.deeper)
        assertFalse(result.longer)
        assertFalse(result.bailout)
    }

    @Test
    fun toggleAvailableForBailout_removeBailoutPreservesDiluentRole() {
        val dive = createDive(
            cylinders = listOf(
                PlannedCylinderModel(
                    cylinder = airCylinder,
                    isChecked = true,
                    isLocked = true,
                    role = CylinderRole.CCR_DILUENT_AND_BAILOUT
                ),
                PlannedCylinderModel(cylinder = nitroxCylinder, isChecked = true, isLocked = false),
            ),
        ).copy(diveMode = DiveMode.CLOSED_CIRCUIT)

        val result = dive.toggleAvailableForBailout(airCylinder, false)

        assertEquals(CylinderRole.CCR_DILUENT, result.cylinders.first { it.cylinder == airCylinder }.role)
        assertNull(result.cylinders.first { it.cylinder == nitroxCylinder }.role)
    }

    @Test
    fun toggleAvailableForBailout_addBailoutPreservesDiluentRole() {
        val dive = createDive(
            cylinders = listOf(
                PlannedCylinderModel(
                    cylinder = airCylinder,
                    isChecked = true,
                    isLocked = true,
                    role = CylinderRole.CCR_DILUENT
                ),
                PlannedCylinderModel(cylinder = nitroxCylinder, isChecked = true, isLocked = false),
            ),
        ).copy(diveMode = DiveMode.CLOSED_CIRCUIT)

        val result = dive.toggleAvailableForBailout(airCylinder, true)

        assertEquals(
            CylinderRole.CCR_DILUENT_AND_BAILOUT,
            result.cylinders.first { it.cylinder == airCylinder }.role
        )
        assertTrue(result.cylinders.first { it.cylinder == airCylinder }.isAvailableForBailout)
    }

    private fun plannedCylinder(
        cylinder: Cylinder,
        isChecked: Boolean = true,
        isLocked: Boolean = false,
        role: CylinderRole? = null,
    ) = PlannedCylinderModel(
        cylinder = cylinder,
        isChecked = isChecked,
        isLocked = isLocked,
        role = role,
    )

    private fun divePlanInput() = DivePlanInputModel(
        diveMode = DiveMode.OPEN_CIRCUIT,
        deeper = false,
        longer = false,
        bailout = false,
        plannedProfile = listOf(
            DiveProfileSection(duration = 20, depthInMeters = 20.0, cylinder = airCylinder),
        ),
        cylinders = listOf(plannedCylinder(airCylinder)),
        surfaceIntervalBefore = null,
    )

    private fun createDive(
        segments: List<DiveProfileSection> = listOf(airSegment),
        cylinders: List<PlannedCylinderModel> = listOf(
            PlannedCylinderModel(cylinder = airCylinder, isChecked = true, isLocked = true),
        ),
    ) = DivePlanInputModel(
        diveMode = DiveMode.OPEN_CIRCUIT,
        deeper = false,
        longer = false,
        bailout = false,
        plannedProfile = segments,
        cylinders = cylinders,
        surfaceIntervalBefore = null
    )
}
