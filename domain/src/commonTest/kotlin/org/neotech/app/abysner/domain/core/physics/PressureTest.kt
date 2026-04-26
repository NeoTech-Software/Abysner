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

package org.neotech.app.abysner.domain.core.physics

import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.Salinity
import kotlin.test.Test
import kotlin.test.assertEquals

class PressureTest {

    private val environment = Environment(Salinity.WATER_EN13319, ATMOSPHERIC_PRESSURE_AT_SEA_LEVEL)

    @Test
    fun metersToAmbientPressure_convertsCorrectly() {
        assertEquals(2.0135283, metersToAmbientPressure(10.0, environment).value, DOUBLE_TOLERANCE)
        assertEquals(25.0199292, metersToAmbientPressure(240.0, environment).value, DOUBLE_TOLERANCE)
    }

    @Test
    fun metersToAmbientPressure_zeroDepthReturnsSurfacePressure() {
        assertEquals(environment.atmosphericPressure, metersToAmbientPressure(0.0, environment).value)
    }

    @Test
    fun metersToHydrostaticPressure_excludesAtmosphericPressure() {
        val ambient = metersToAmbientPressure(10.0, environment).value
        val hydrostatic = metersToHydrostaticPressure(10.0, environment).value
        assertEquals(ambient - environment.atmosphericPressure, hydrostatic, DOUBLE_TOLERANCE)
    }

    @Test
    fun ambientPressureToMeters_surfacePressureReturnsZero() {
        assertEquals(0.0, ambientPressureToMeters(environment.atmosphericPressure, environment), DOUBLE_TOLERANCE)
    }

    @Test
    fun feetToAmbientPressure_convertsCorrectly() {
        assertEquals(2.0193699253, feetToAmbientPressure(33.0, environment).value, DOUBLE_TOLERANCE)
        assertEquals(25.0076857936, feetToAmbientPressure(787.0, environment).value, DOUBLE_TOLERANCE)
    }

    @Test
    fun feetToAmbientPressure_zeroDepthReturnsSurfacePressure() {
        assertEquals(environment.atmosphericPressure, feetToAmbientPressure(0.0, environment).value)
    }

    @Test
    fun feetToHydrostaticPressure_excludesAtmosphericPressure() {
        val ambient = feetToAmbientPressure(33.0, environment).value
        val hydrostatic = feetToHydrostaticPressure(33.0, environment).value
        assertEquals(ambient - environment.atmosphericPressure, hydrostatic, DOUBLE_TOLERANCE)
    }

    @Test
    fun ambientPressureToFeet_surfacePressureReturnsZero() {
        assertEquals(0.0, ambientPressureToFeet(environment.atmosphericPressure, environment), DOUBLE_TOLERANCE)
    }
}

private const val DOUBLE_TOLERANCE = 1e-8
