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

import org.neotech.app.abysner.domain.persistence.EnumPreference

enum class Salinity(val density: Double, val humanReadableName: String): EnumPreference {
    /**
     * 1000.0 kg/m3 at 0C / 32F (fresh water)
     */
    WATER_FRESH(density = 1000.0, humanReadableName = "Fresh") {
        override val preferenceValue: String = "fresh"
    },
    /**
     * European standard for dive computers, as used by Shearwater.
     */
    WATER_EN13319(density = 1020.0, humanReadableName = "EN13319") {
        override val preferenceValue: String = "EN13319"
    },
    /**
     * 1030.0 kg/m3 at 0C / 32F
     */
    WATER_SALT(density = 1030.0, humanReadableName = "Salt") {
        override val preferenceValue: String = "Salt"
    },

    /**
     * From: https://en.wikipedia.org/wiki/List_of_bodies_of_water_by_salinity
     */
    BALTIC_SEA(density = 1009.0, humanReadableName = "Baltic Sea") {
        override val preferenceValue: String = "Baltic-Sea"
    },

    /**
     * From: https://en.wikipedia.org/wiki/Atlantic_Ocean
     */
    ATLANTIC_OCEAN(density = 1035.0, humanReadableName = "Atlantic Ocean") {
        override val preferenceValue: String = "Atlantic-Ocean"
    },

    /**
     * From: https://en.wikipedia.org/wiki/List_of_bodies_of_water_by_salinity
     */
    MEDITERRANEAN_SEA(density = 1038.0, humanReadableName = "Mediterranean Sea") {
        override val preferenceValue: String = "Mediterranean-Sea"
    },

    /**
     * From: https://en.wikipedia.org/wiki/List_of_bodies_of_water_by_salinity
     *       https://en.wikipedia.org/wiki/Red_Sea
     */
    RED_SEA(density = 1040.0, humanReadableName = "Red Sea") {
        override val preferenceValue: String = "Red-Sea"
    },
}
