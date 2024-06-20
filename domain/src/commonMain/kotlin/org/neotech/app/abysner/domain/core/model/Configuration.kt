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

import org.neotech.app.abysner.domain.core.physics.altitudeToPressure
import org.neotech.app.abysner.domain.persistence.EnumPreference

/**
 * TODO: Eventually this needs to be part of a single planned dive, so we can plan multiple dives allowing multiple configurations.
 */
data class Configuration(
    val sacRate: Double = 15.0,
    val sacRateOutOfAir: Double = 40.0,
    val maxPPO2Deco: Double = 1.6,
    val maxPPO2: Double = 1.4,
    val maxEND: Double = 30.0,
    val maxAscentRate: Double = 5.0,
    val maxDescentRate: Double = 20.0,
    val gfLow: Double = 0.3,
    val gfHigh: Double = 0.7,
    val forceMinimalDecoStopTime: Boolean = true,
    val useDecoGasBetweenSections: Boolean = false,
    val decoStepSize: Int = 3,
    val lastDecoStopDepth: Int = 3,
    val contingencyDeeper: Int = 3,
    val contingencyLonger: Int = 3,
    val salinity: Salinity = Salinity.WATER_FRESH,
    val altitude: Double = 0.0,
    val algorithm: Algorithm = Algorithm.BUHLMANN_ZH16C
) {

    val environment = Environment(salinity, altitudeToPressure(altitude))

    val gf = "${(gfLow * 100).toInt()}/${(gfHigh * 100).toInt()}"

    enum class Algorithm(val shortName: String): EnumPreference {
        BUHLMANN_ZH16C("ZHL-16C-GF") {
            override val preferenceValue: String = "BUHLMANN-ZHL-16C-GF"
        },
        BUHLMANN_ZH16B("ZHL-16B-GF") {
            override val preferenceValue: String = "BUHLMANN-ZHL-16B-GF"
        },
        BUHLMANN_ZH16A("ZHL-16A-GF") {
            override val preferenceValue: String = "BUHLMANN-ZHL-16A-GF"
        },
    }
}
