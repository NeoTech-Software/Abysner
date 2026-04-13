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

package org.neotech.app.abysner.domain.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CylinderTest {

    @Test
    fun capacityAt_returnsCorrectCapacity() {
        // Steel 12 liter tank
        val cylinder = Cylinder(Gas.Air, 232.0, 12.0)

        assertEquals(2316.0, cylinder.capacityAt(pressure = 200.0), 1.0)
    }

    @Test
    fun pressureAt_returnsCorrectPressure() {
        // Steel 12 liter tank
        val cylinder = Cylinder(Gas.Air, 232.0, 12.0)

        assertEquals(99.0, cylinder.pressureAt(volume = 1200.0), 1.0)
    }
}
