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

import org.neotech.app.abysner.domain.core.physics.altitudeToPressure
import org.neotech.app.abysner.domain.persistence.EnumPreference
import kotlin.math.floor
import kotlin.math.max

/**
 * TODO: Eventually this needs to be part of a single planned dive, so we can plan multiple dives allowing multiple configurations.
 */
data class Configuration(
    val sacRate: Double = 20.0,
    val sacRateOutOfAir: Double = 40.0,
    val maxPPO2Deco: Double = 1.6,
    val maxPPO2: Double = 1.4,
    /**
     * Currently not configurable via interface.
     */
    val maxEND: Double = 30.0,
    val maxAscentRate: Double = 5.0,
    val maxDescentRate: Double = 20.0,
    val gfLow: Double = 0.6,
    val gfHigh: Double = 0.7,
    val forceMinimalDecoStopTime: Boolean = true,
    val gasSwitchTime: Int = 1,
    val useDecoGasBetweenSections: Boolean = false,
    val decoStepSize: Int = 3,
    val lastDecoStopDepth: Int = 3,
    val contingencyDeeper: Int = 3,
    val contingencyLonger: Int = 3,
    val salinity: Salinity = Salinity.WATER_FRESH,
    val altitude: Double = 0.0,
    val algorithm: Algorithm = Algorithm.BUHLMANN_ZH16C,
    /**
     * CCR low O2 setpoint (bar) used during descent. Kept low to provide a safety buffer
     * against hypoxia and reduce solenoid firing during depth changes.
     */
    val ccrLowSetpoint: Double = 0.7,
    /**
     * CCR high O2 setpoint (bar) used during bottom time and the entire ascent. A higher
     * setpoint reduces inert gas loading and improves decompression efficiency.
     */
    val ccrHighSetpoint: Double = 1.2,
    /**
     * CCR loop volume in liters (counter-lung + scrubber + hoses). Used to calculate diluent
     * usage from loop expansion during descent.
     */
    val ccrLoopVolumeLiters: Double = 7.0,
    /**
     * Resting metabolic O2 consumption rate in liters per minute. Used to calculate oxygen cylinder
     * usage for CCR dives.
     */
    val ccrMetabolicO2LitersPerMinute: Double = 0.8,
    /**
     * Depth (meters) at which the planner switches from the low setpoint to the high setpoint
     * during descent. Null disables the auto-switch and causes the switch to occur when a bottom
     * section is reached.
     */
    val ccrToHighSetpointSwitchDepth: Int? = null,
    /**
     * Depth (meters) at which the planner switches from the high setpoint to the low setpoint
     * during ascent. Null disables the auto-switch, meaning the high setpoint remains active until
     * the surface is reached or a new descent starts.
     */
    val ccrToLowSetpointSwitchDepth: Int? = null,
) {

    val environment = Environment(salinity, altitudeToPressure(altitude))

    val gf = "${(gfLow * 100).toInt()}/${(gfHigh * 100).toInt()}"

    /**
     * Returns a [SetpointSwitch] for the descent phase (low to high setpoint), or null if
     * the switch is disabled or the dive is not CCR.
     */
    fun descentSetpointSwitch(breathingMode: BreathingMode): SetpointSwitch? =
        ccrToHighSetpointSwitchDepth?.let { depth ->
            (breathingMode as? BreathingMode.ClosedCircuit)?.let {
                SetpointSwitch(depth = depth, toBreathingMode = BreathingMode.ClosedCircuit(ccrHighSetpoint))
            }
        }

    /**
     * Returns a [SetpointSwitch] for the ascent phase (high to low setpoint), or null if
     * the switch is disabled or the dive is not CCR.
     */
    fun ascentSetpointSwitch(breathingMode: BreathingMode): SetpointSwitch? =
        ccrToLowSetpointSwitchDepth?.let { depth ->
            (breathingMode as? BreathingMode.ClosedCircuit)?.let {
                SetpointSwitch(depth = depth, toBreathingMode = BreathingMode.ClosedCircuit(ccrLowSetpoint))
            }
        }

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

    /**
     * Calculate travel time, negative distance is descending, positive is ascending.
     */
    fun travelTime(distance: Double): Int {
        val rate = if(distance > 0) {
            maxAscentRate
        } else {
            -maxDescentRate
        }
        return if(distance == 0.0) {
            0
        } else {
            max(floor(distance / rate).toInt(), 1)
        }
    }
}
