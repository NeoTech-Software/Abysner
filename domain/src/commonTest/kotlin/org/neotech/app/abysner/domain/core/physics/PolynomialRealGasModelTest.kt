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

package org.neotech.app.abysner.domain.core.physics

import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.tenthAtDecimalPoint
import kotlin.test.Test
import kotlin.test.assertEquals

class PolynomialRealGasModelTest {

    @Test
    fun test_pressure_to_volume_and_back() {
        val model = PolynomialRealGasModel()

        fun test(pressure: Double) {
            val cylinder = Cylinder(Gas.Air, pressure, 10.0)
            assertEquals(model.getGasPressure(cylinder, model.getGasVolume(cylinder)), pressure, DOUBLE_TOLERANCE)
        }

        // Test 0 to 300 bar in increments of 30 bar
        repeat(11) {
            test(it * 30.0)
        }
    }
}

private val DOUBLE_TOLERANCE = tenthAtDecimalPoint(6)
