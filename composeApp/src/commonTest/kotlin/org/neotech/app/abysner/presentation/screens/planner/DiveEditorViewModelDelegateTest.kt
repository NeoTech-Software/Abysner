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
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanInputModel
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.diveplanning.model.PlannedCylinderModel
import org.neotech.app.abysner.domain.diveplanning.model.countGas
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DiveEditorViewModelDelegateTest {

    private val airCylinder = Cylinder.steel12Liter(gas = Gas.Air, pressure = 232.0)
    private val nitrox50 = Cylinder.aluminium80Cuft(gas = Gas.Nitrox50, pressure = 207.0)
    private val airSegment = DiveProfileSection(duration = 30, depth = 25, cylinder = airCylinder)

    @Test
    fun addSegment_referencedCylinderIsLocked() {
        val dive = createDive(
            cylinders = listOf(
                PlannedCylinderModel(cylinder = airCylinder, isChecked = true, isLocked = false),
            ),
            segments = listOf(airSegment)
        )

        val result = DiveEditorViewModelDelegate.addSegment(dive, airSegment)

        assertTrue(result.cylinders[0].isLocked)
    }

    @Test
    fun removeCylinder_lockedCylinderThrows() {
        val dive = createDive()
        assertFailsWith<IllegalStateException> {
            DiveEditorViewModelDelegate.removeCylinder(dive, airCylinder)
        }
    }

    @Test
    fun removeCylinder_unlockedCylinderIsRemoved() {
        val dive = createDive(
            cylinders = listOf(
                PlannedCylinderModel(cylinder = airCylinder, isChecked = true, isLocked = false),
                PlannedCylinderModel(cylinder = nitrox50, isChecked = true, isLocked = false),
            ),
            segments = emptyList(),
        )

        val result = DiveEditorViewModelDelegate.removeCylinder(dive, nitrox50)

        assertEquals(1, result.cylinders.size)
        assertEquals(airCylinder, result.cylinders[0].cylinder)
    }

    @Test
    fun removeCylinder_segmentsReferencingRemovedCylinderAreReassigned() {
        // TODO this test can be removed if segments allow auto cylinder selection (nullable cylinders)
        //      I need to consider this as something to implement.
        val dive = createDive(
            cylinders = listOf(
                PlannedCylinderModel(
                    cylinder = airCylinder,
                    isChecked = true,
                    isLocked = false,
                    role = CylinderRole.CCR_DILUENT_AND_BAILOUT
                ),
                PlannedCylinderModel(cylinder = nitrox50, isChecked = true, isLocked = false),
            ),
            segments = listOf(
                airSegment,
                DiveProfileSection(duration = 10, depth = 6, cylinder = nitrox50),
            ),
        ).copy(diveMode = DiveMode.CLOSED_CIRCUIT)

        val result = DiveEditorViewModelDelegate.removeCylinder(dive, nitrox50)

        assertEquals(2, result.plannedProfile.size)
        assertEquals(airCylinder, result.plannedProfile[1].cylinder)
    }

    @Test
    fun toggleCylinder_uncheckedCylinderIsEnabled() {
        val dive = createDive(
            cylinders = listOf(
                PlannedCylinderModel(cylinder = airCylinder, isChecked = true, isLocked = true),
                PlannedCylinderModel(cylinder = nitrox50, isChecked = false, isLocked = false),
            ),
            segments = listOf(airSegment)
        )

        val result = DiveEditorViewModelDelegate.toggleCylinder(dive, nitrox50, enabled = true)

        assertTrue(result.cylinders.first { it.cylinder == nitrox50 }.isChecked)
    }

    @Test
    fun toggleCylinder_lockedCylinderThrows() {
        val dive = createDive()
        assertFailsWith<IllegalStateException> {
            DiveEditorViewModelDelegate.toggleCylinder(dive, airCylinder, enabled = false)
        }
    }

    @Test
    fun toggleCylinder_checkedUnlockedCylinderIsDisabled() {
        val dive = createDive(
            cylinders = listOf(
                PlannedCylinderModel(cylinder = airCylinder, isChecked = true, isLocked = false),
                PlannedCylinderModel(cylinder = nitrox50, isChecked = true, isLocked = false),
            ),
            segments = emptyList()
        )

        val result = DiveEditorViewModelDelegate.toggleCylinder(dive, nitrox50, enabled = false)

        assertFalse(result.cylinders.first { it.cylinder == nitrox50 }.isChecked)
    }

    @Test
    fun recomputeCylinderState_referencedCylinderIsLockedAndUnreferencedIsUnlocked() {
        val dive = createDive(
            cylinders = listOf(
                PlannedCylinderModel(cylinder = airCylinder, isChecked = true, isLocked = false),
                PlannedCylinderModel(cylinder = nitrox50, isChecked = true, isLocked = true),
            ),
            segments = listOf(airSegment)
        )
        val result = DiveEditorViewModelDelegate.recomputeCylinderState(dive)

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

        val result = DiveEditorViewModelDelegate.recomputeCylinderState(dive)

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

        val result = DiveEditorViewModelDelegate.setDiveMode(dive, DiveMode.CLOSED_CIRCUIT)

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

        val result = DiveEditorViewModelDelegate.setDiveMode(dive, DiveMode.CLOSED_CIRCUIT)

        val oxygen = result.cylinders.first { it.isCcrOxygen }
        assertTrue(oxygen.isChecked)
        assertTrue(oxygen.isLocked)
    }

    @Test
    fun setDiveMode_ccrSwitchUpdatesDiveMode() {
        val dive = createDive()

        val result = DiveEditorViewModelDelegate.setDiveMode(dive, DiveMode.CLOSED_CIRCUIT)

        assertEquals(DiveMode.CLOSED_CIRCUIT, result.diveMode)
    }

    @Test
    fun setDiveMode_ccrSwitchAssignsDiluentRole() {
        val dive = createDive()

        val result = DiveEditorViewModelDelegate.setDiveMode(dive, DiveMode.CLOSED_CIRCUIT)

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
                PlannedCylinderModel(cylinder = nitrox50, isChecked = true, isLocked = false),
                PlannedCylinderModel(
                    cylinder = Cylinder.steel3LiterOxygen(),
                    isChecked = false,
                    isLocked = false,
                    role = CylinderRole.CCR_OXYGEN,
                ),
            ),
        )

        val result = DiveEditorViewModelDelegate.setDiveMode(dive, DiveMode.CLOSED_CIRCUIT)

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

        val result = DiveEditorViewModelDelegate.setDiveMode(dive, DiveMode.CLOSED_CIRCUIT)

        val diluent = result.cylinders.first { it.cylinder == airCylinder }
        assertEquals(CylinderRole.CCR_DILUENT, diluent.role)
    }

    @Test
    fun setDiveMode_ocSwitchPreservesOxygenCylinderAndKeepsRole() {
        val dive = createDive().let {
            DiveEditorViewModelDelegate.setDiveMode(it, DiveMode.CLOSED_CIRCUIT)
        }

        val result = DiveEditorViewModelDelegate.setDiveMode(dive, DiveMode.OPEN_CIRCUIT)

        val oxygen = result.cylinders.first { it.cylinder.gas == Gas.Oxygen }
        assertEquals(CylinderRole.CCR_OXYGEN, oxygen.role)
        assertFalse(oxygen.isChecked)
        assertFalse(oxygen.isLocked)
    }

    @Test
    fun setDiveMode_ocSwitchPreservesDiluentCylinderAndKeepsRole() {
        val dive = createDive().let {
            DiveEditorViewModelDelegate.setDiveMode(it, DiveMode.CLOSED_CIRCUIT)
        }

        val result = DiveEditorViewModelDelegate.setDiveMode(dive, DiveMode.OPEN_CIRCUIT)

        val diluent = result.cylinders.first { it.cylinder == airCylinder }
        assertEquals(CylinderRole.CCR_DILUENT_AND_BAILOUT, diluent.role)
        // These are true since the diluent was used in the segment
        assertTrue(diluent.isChecked)
        assertTrue(diluent.isLocked)
    }

    @Test
    fun setDiveMode_roundTripPreservesDiluentRole() {
        val dive = createDive().let {
            DiveEditorViewModelDelegate.setDiveMode(it, DiveMode.CLOSED_CIRCUIT)
        }.let {
            DiveEditorViewModelDelegate.setDiveMode(it, DiveMode.OPEN_CIRCUIT)
        }

        val result = DiveEditorViewModelDelegate.setDiveMode(dive, DiveMode.CLOSED_CIRCUIT)

        val diluent = result.cylinders.first { it.cylinder == airCylinder }
        assertEquals(CylinderRole.CCR_DILUENT_AND_BAILOUT, diluent.role)
        assertTrue(diluent.isChecked)
        assertTrue(diluent.isLocked)
    }

    @Test
    fun setDiveMode_ocSwitchUpdatesDiveMode() {
        val dive = createDive().let {
            DiveEditorViewModelDelegate.setDiveMode(it, DiveMode.CLOSED_CIRCUIT)
        }

        val result = DiveEditorViewModelDelegate.setDiveMode(dive, DiveMode.OPEN_CIRCUIT)

        assertEquals(DiveMode.OPEN_CIRCUIT, result.diveMode)
    }

    @Test
    fun setDiveMode_ocSwitchResetsBailout() {
        val dive = createDive().let {
            DiveEditorViewModelDelegate.setDiveMode(it, DiveMode.CLOSED_CIRCUIT)
        }.let {
            DiveEditorViewModelDelegate.setContingency(dive = it, deeper = false, longer = false, bailout = true)
        }

        val result = DiveEditorViewModelDelegate.setDiveMode(dive, DiveMode.OPEN_CIRCUIT)

        assertFalse(result.bailout)
    }

    @Test
    fun setDiveMode_sameModeIsNoOp() {
        val dive = createDive()

        val result = DiveEditorViewModelDelegate.setDiveMode(dive, DiveMode.OPEN_CIRCUIT)

        assertEquals(dive, result)
    }

    @Test
    fun setContingency_setsAllFlags() {
        val result = DiveEditorViewModelDelegate.setContingency(dive = createDive(), deeper = true, longer = true, bailout = true)

        assertTrue(result.deeper)
        assertTrue(result.longer)
        assertTrue(result.bailout)
    }

    @Test
    fun setContingency_clearsAllFlags() {
        val dive = createDive().copy(deeper = true, longer = true, bailout = true)

        val result = DiveEditorViewModelDelegate.setContingency(dive = dive, deeper = false, longer = false, bailout = false)

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
                PlannedCylinderModel(cylinder = nitrox50, isChecked = true, isLocked = false),
            ),
        ).copy(diveMode = DiveMode.CLOSED_CIRCUIT)

        val result = DiveEditorViewModelDelegate.toggleAvailableForBailout(dive, airCylinder, false)

        assertEquals(CylinderRole.CCR_DILUENT, result.cylinders.first { it.cylinder == airCylinder }.role)
        assertNull(result.cylinders.first { it.cylinder == nitrox50 }.role)
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
                PlannedCylinderModel(cylinder = nitrox50, isChecked = true, isLocked = false),
            ),
        ).copy(diveMode = DiveMode.CLOSED_CIRCUIT)

        val result = DiveEditorViewModelDelegate.toggleAvailableForBailout(dive, airCylinder, true)

        assertEquals(
            CylinderRole.CCR_DILUENT_AND_BAILOUT,
            result.cylinders.first { it.cylinder == airCylinder }.role
        )
        assertTrue(result.cylinders.first { it.cylinder == airCylinder }.isAvailableForBailout)
    }

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
