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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CylinderRoleTest {

    @Test
    fun isCcrOxygen_trueOnlyForCcrOxygenRole() {
        assertTrue(CylinderRole.CCR_OXYGEN.isCcrOxygen)
        assertFalse(CylinderRole.CCR_DILUENT.isCcrOxygen)
        assertFalse(CylinderRole.CCR_DILUENT_AND_BAILOUT.isCcrOxygen)
    }

    @Test
    fun isCcrOxygen_falseForNull() {
        val role: CylinderRole? = null
        assertFalse(role.isCcrOxygen)
    }

    @Test
    fun isCcrDiluent_trueForDiluentAndDiluentBailout() {
        assertTrue(CylinderRole.CCR_DILUENT.isCcrDiluent)
        assertTrue(CylinderRole.CCR_DILUENT_AND_BAILOUT.isCcrDiluent)
        assertFalse(CylinderRole.CCR_OXYGEN.isCcrDiluent)
    }

    @Test
    fun isCcrDiluent_falseForNull() {
        val role: CylinderRole? = null
        assertFalse(role.isCcrDiluent)
    }

    @Test
    fun isAvailableForBailout_trueForNullAndDiluentBailout() {
        val nullRole: CylinderRole? = null
        assertTrue(nullRole.isAvailableForBailout)
        assertTrue(CylinderRole.CCR_DILUENT_AND_BAILOUT.isAvailableForBailout)
    }

    @Test
    fun isAvailableForBailout_falseForOxygenAndDiluent() {
        assertFalse(CylinderRole.CCR_OXYGEN.isAvailableForBailout)
        assertFalse(CylinderRole.CCR_DILUENT.isAvailableForBailout)
    }

    @Test
    fun toggleAvailableForBailout_togglesDiluentToBailout() {
        val result = CylinderRole.CCR_DILUENT.toggleAvailableForBailout(true)
        assertEquals(CylinderRole.CCR_DILUENT_AND_BAILOUT, result)
    }

    @Test
    fun toggleAvailableForBailout_togglesBailoutToDiluent() {
        val result = CylinderRole.CCR_DILUENT_AND_BAILOUT.toggleAvailableForBailout(false)
        assertEquals(CylinderRole.CCR_DILUENT, result)
    }

    @Test
    fun toggleAvailableForBailout_noOpForOxygen() {
        val result = CylinderRole.CCR_OXYGEN.toggleAvailableForBailout(true)
        assertEquals(CylinderRole.CCR_OXYGEN, result)
    }

    @Test
    fun toggleAvailableForBailout_noOpForNull() {
        val role: CylinderRole? = null
        val result = role.toggleAvailableForBailout(true)
        assertNull(result)
    }
}
