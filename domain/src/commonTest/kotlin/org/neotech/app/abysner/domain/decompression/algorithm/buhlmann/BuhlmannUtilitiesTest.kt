/*
 * Abysner - Dive planner
 * Copyright (C) 2024 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.domain.decompression.algorithm.buhlmann

import kotlin.test.Test
import kotlin.test.assertEquals

class BuhlmannUtilitiesTest {

    @Test
    fun getWaterVapourPressureTest() {
        assertEquals(0.0625993025768047, waterVapourPressureInBars(37.0))
    }
}
