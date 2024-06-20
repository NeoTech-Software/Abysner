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

package org.neotech.app.abysner.domain.core.model

import org.neotech.app.abysner.domain.core.physics.ATMOSPHERIC_PRESSURE_AT_SEA_LEVEL

/**
 * Model that holds the environmental configuration of a dive, such as the density of the water
 * (salinity) and atmospheric pressure at the surface.
 */
data class Environment(
    val salinity: Salinity,
    val atmosphericPressure: Double,
) {
    companion object {

        /**
         * This is not part of the Environment constructor to avoid mistakes. Since it would then be
         * very easy to skip certain parameters, which could drastically affect calculations.
         */
        val Default = Environment(
            salinity = Salinity.WATER_FRESH,
            atmosphericPressure = ATMOSPHERIC_PRESSURE_AT_SEA_LEVEL
        )

        val SeaLevelFresh = Environment(
            salinity = Salinity.WATER_FRESH,
            atmosphericPressure = ATMOSPHERIC_PRESSURE_AT_SEA_LEVEL
        )

        val SeaLevelSalt = Environment(
            salinity = Salinity.WATER_SALT,
            atmosphericPressure = ATMOSPHERIC_PRESSURE_AT_SEA_LEVEL
        )
    }
}
