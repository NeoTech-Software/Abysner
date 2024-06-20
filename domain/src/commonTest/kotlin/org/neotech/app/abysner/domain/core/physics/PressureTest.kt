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

import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.tenthAtDecimalPoint
import kotlin.test.Test
import kotlin.test.assertEquals

class PressureTest {

    @Test
    fun testDepthInMetersToBars() {
        // 10 meters (pure water)
        assertEquals(1.9939, depthInMetersToBars(10.0, Environment.Default), DOUBLE_PRECISION)

        // 24 meters (pure water)
        assertEquals(3.3668, depthInMetersToBars(24.0, Environment.Default), DOUBLE_PRECISION)
    }

    @Test
    fun testAltitudeToPressure() {
        assertEquals(ATMOSPHERIC_PRESSURE_AT_SEA_LEVEL, altitudeToPressure(0.0), tenthAtDecimalPoint(4))
        assertEquals(0.7099843196815809, altitudeToPressure(3000.0), DOUBLE_PRECISION)
    }

    @Test
    fun testPressureToAltitude() {
        assertEquals(
            0.0,
            pressureToAltitude(ATMOSPHERIC_PRESSURE_AT_SEA_LEVEL),
            DOUBLE_PRECISION
        )
        assertEquals(3000.0, pressureToAltitude(0.7099843196815809), DOUBLE_PRECISION)
    }
}

private val DOUBLE_PRECISION = tenthAtDecimalPoint(4)
