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
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiveEditorViewModelDelegateTest {

    private val airCylinder = Cylinder.steel12Liter(gas = Gas.Air, pressure = 232.0)
    private val nitrox50 = Cylinder.aluminium80Cuft(gas = Gas.Nitrox50, pressure = 207.0)
    private val airSegment = DiveProfileSection(duration = 30, depth = 25, cylinder = airCylinder)

    private fun createDive(
        segments: List<DiveProfileSection> = listOf(airSegment),
        cylinders: List<PlannedCylinderModel> = listOf(
            PlannedCylinderModel(cylinder = airCylinder, isChecked = true, isLocked = true),
        ),
    ) = DivePlanInputModel(
        deeper = false,
        longer = false,
        plannedProfile = segments,
        cylinders = cylinders,
        surfaceIntervalBefore = null
    )

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
    fun removeCylinder_whenLockedThrows() {
        val dive = createDive()
        assertFailsWith<IllegalStateException> {
            DiveEditorViewModelDelegate.removeCylinder(dive, airCylinder)
        }
    }

    @Test
    fun removeCylinder_whenUnlockedCylinderIsRemoved() {
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
    fun toggleCylinder_whenNonLockedCylinderIsEnabled() {
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
    fun toggleCylinder_whenLockedThrows() {
        val dive = createDive(
            cylinders = listOf(
                PlannedCylinderModel(cylinder = airCylinder, isChecked = true, isLocked = true),
            ),
            segments = listOf(airSegment)
        )

        assertFailsWith<IllegalStateException> {
            DiveEditorViewModelDelegate.toggleCylinder(dive, airCylinder, enabled = false)
        }
    }

    @Test
    fun toggleCylinder_whenNonLockedCylinderIsDisabled() {
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

        // Cylinder with air must be locked, since it is referenced by a segment.
        assertTrue(result.cylinders[0].isLocked)
        // Cylinder with nitrox should not be locked, since it is not referenced by a segment.
        assertFalse(result.cylinders[1].isLocked)
    }
}
