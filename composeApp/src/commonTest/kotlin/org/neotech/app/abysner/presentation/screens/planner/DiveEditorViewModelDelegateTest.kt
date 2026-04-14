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
import org.neotech.app.abysner.domain.diveplanning.model.countGas
import org.neotech.app.abysner.domain.diveplanning.model.hasGas
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
                    isLocked = false
                ),
                PlannedCylinderModel(cylinder = airCylinder, isChecked = true, isLocked = false),
            ),
        ).copy(diveMode = DiveMode.CLOSED_CIRCUIT)

        val result = DiveEditorViewModelDelegate.recomputeCylinderState(dive)

        val oxygenCylinder = result.cylinders.first { it.cylinder.gas == Gas.Oxygen }
        assertTrue(oxygenCylinder.isChecked)
        assertTrue(oxygenCylinder.isLocked)
    }

    @Test
    fun setDiveMode_ccrSwitchAddsOxygenCylinderIfMissing() {
        val dive = createDive()

        val result = DiveEditorViewModelDelegate.setDiveMode(dive, DiveMode.CLOSED_CIRCUIT)

        assertTrue(result.cylinders.hasGas(Gas.Oxygen))
    }

    @Test
    fun setDiveMode_ccrSwitchDoesNotDuplicateExistingOxygenCylinder() {
        val oxygenCylinder = Cylinder(Gas.Oxygen, pressure = 200.0, waterVolume = 3.0)
        val dive = createDive(
            cylinders = listOf(
                PlannedCylinderModel(cylinder = airCylinder, isChecked = true, isLocked = true),
                PlannedCylinderModel(
                    cylinder = oxygenCylinder,
                    isChecked = false,
                    isLocked = false
                ),
            )
        )

        val result = DiveEditorViewModelDelegate.setDiveMode(dive, DiveMode.CLOSED_CIRCUIT)

        assertEquals(1, result.cylinders.countGas(Gas.Oxygen))
    }

    @Test
    fun setDiveMode_ccrOxygenCylinderIsCheckedAndLocked() {
        val dive = createDive()

        val result = DiveEditorViewModelDelegate.setDiveMode(dive, DiveMode.CLOSED_CIRCUIT)

        val oxygen = result.cylinders.first { it.cylinder.gas == Gas.Oxygen }
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
    fun setDiveMode_ocSwitchPreservesOxygenCylinder() {
        val dive = createDive().let {
            DiveEditorViewModelDelegate.setDiveMode(it, DiveMode.CLOSED_CIRCUIT)
        }

        val result = DiveEditorViewModelDelegate.setDiveMode(dive, DiveMode.OPEN_CIRCUIT)

        assertTrue(result.cylinders.hasGas(Gas.Oxygen))
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
            DiveEditorViewModelDelegate.setContingency(
                dive = it,
                deeper = false,
                longer = false,
                bailout = true
            )
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
        val result = DiveEditorViewModelDelegate.setContingency(
            dive = createDive(),
            deeper = true,
            longer = true,
            bailout = true
        )

        assertTrue(result.deeper)
        assertTrue(result.longer)
        assertTrue(result.bailout)
    }

    @Test
    fun setContingency_clearsAllFlags() {
        val dive = createDive().copy(deeper = true, longer = true, bailout = true)

        val result = DiveEditorViewModelDelegate.setContingency(
            dive,
            deeper = false,
            longer = false,
            bailout = false
        )

        assertFalse(result.deeper)
        assertFalse(result.longer)
        assertFalse(result.bailout)
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
