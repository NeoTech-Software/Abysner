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

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.DiveMode
import org.neotech.app.abysner.domain.core.model.Gas
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DivePlanSetTest {

    private val configuration = Configuration()
    private val airCylinder = Cylinder.steel12Liter(Gas.Air)

    @Test
    fun isDeeper_trueWhenDeeperNotNull() {
        val set = divePlanSet(deeper = 5)
        assertTrue(set.isDeeper)
    }

    @Test
    fun isDeeper_falseWhenDeeperNull() {
        val set = divePlanSet(deeper = null)
        assertFalse(set.isDeeper)
    }

    @Test
    fun isLonger_trueWhenLongerNotNull() {
        val set = divePlanSet(longer = 10)
        assertTrue(set.isLonger)
    }

    @Test
    fun isLonger_falseWhenLongerNull() {
        val set = divePlanSet(longer = null)
        assertFalse(set.isLonger)
    }

    @Test
    fun isCcr_trueWhenDiveModeIsClosedCircuit() {
        val set = divePlanSet(diveMode = DiveMode.CLOSED_CIRCUIT)
        assertTrue(set.isCcr)
    }

    @Test
    fun isCcr_falseWhenDiveModeIsOpenCircuit() {
        val set = divePlanSet(diveMode = DiveMode.OPEN_CIRCUIT)
        assertFalse(set.isCcr)
    }

    @Test
    fun isEmpty_trueWhenBasePlanHasNoSegments() {
        val set = divePlanSet()
        assertTrue(set.isEmpty)
    }

    @Test
    fun configuration_delegatesToBasePlan() {
        val set = divePlanSet()
        assertEquals(configuration, set.configuration)
    }

    @Test
    fun multiDivePlanSet_isEmpty_trueWhenAllPlansEmpty() {
        val multi = MultiDivePlanSet(listOf(divePlanSet(), divePlanSet()))
        assertTrue(multi.isEmpty)
    }

    @Test
    fun multiDivePlanSet_configuration_returnsFirstPlanConfiguration() {
        val multi = MultiDivePlanSet(listOf(divePlanSet(), divePlanSet()))
        assertEquals(configuration, multi.configuration)
    }

    private fun divePlanSet(
        deeper: Int? = null,
        longer: Int? = null,
        diveMode: DiveMode = DiveMode.OPEN_CIRCUIT,
    ) = DivePlanSet(
        base = DivePlan(
            segments = persistentListOf(),
            alternativeAccents = persistentMapOf(),
            cylinders = persistentListOf(AssignedCylinder(airCylinder)),
            configuration = configuration,
            totalCns = 0.0,
            totalOtu = 0.0,
        ),
        deeper = deeper,
        longer = longer,
        bailout = false,
        diveMode = diveMode,
        gasPlan = persistentListOf(),
    )
}
