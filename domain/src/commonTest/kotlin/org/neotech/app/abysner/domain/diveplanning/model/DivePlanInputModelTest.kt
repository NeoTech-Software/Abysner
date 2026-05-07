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
}
