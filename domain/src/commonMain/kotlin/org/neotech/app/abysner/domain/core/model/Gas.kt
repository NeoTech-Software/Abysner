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

import org.neotech.app.abysner.domain.core.physics.barToDepthInMeters
import org.neotech.app.abysner.domain.core.physics.depthInMetersToBars
import org.neotech.app.abysner.domain.utilities.DecimalFormat
import kotlin.math.round

data class Gas(val oxygenFraction: Double, val heliumFraction: Double) {

    init {
        if((oxygenFraction + heliumFraction) > 1.0) {
            error("Oxygen ($oxygenFraction) and helium ($heliumFraction) fraction add up to more then 1.0!")
        }
    }

    /**
     * The N2 fraction of this gas, calculated by subtracting the helium and oxygen fractions from 1.0.
     */
    val nitrogenFraction = (1.0 - (oxygenFraction + heliumFraction))

    /**
     * Returns the oxygen MOD in meters.
     */
    fun oxygenMod(ppO2: Double, environment: Environment): Double {
        return barToDepthInMeters(ppO2 / this.oxygenFraction, environment)
    }

    /**
     * Returns the oxygen MOD in meters, rounded to the nearest integer.
     */
    fun oxygenModRounded(ppO2: Double, environment: Environment): Int {
        return round(oxygenMod(ppO2, environment)).toInt()
    }

    /**
     * Calculates END (Equivalent Narcotic Depth). Only Oxygen and Nitrogen are considered in this
     * calculation.
     *
     * https://en.wikipedia.org/wiki/Equivalent_narcotic_depth
     */
    fun endInMeters(depth: Double, environment: Environment): Double {
        // Helium has a narc factor of 0 while N2 and O2 have a narc factor of 1
        val narcIndex = (this.oxygenFraction) + (this.nitrogenFraction)

        val bars = depthInMetersToBars(depth, environment)
        val equivalentBars = bars * narcIndex
        return barToDepthInMeters(equivalentBars, environment)
    }

    val density: Double by lazy {
        val oxygenDensity = DENSITY_O2 * oxygenFraction
        val heliumDensity = DENSITY_HE * heliumFraction
        val nitrogenDensity = DENSITY_N2 * nitrogenFraction
        oxygenDensity + heliumDensity + nitrogenDensity
    }

    fun densityAtDepth(depth: Double, environment: Environment): Double {
        val bar = depthInMetersToBars(depth, environment)
        return density * bar
    }

    /**
     * Returns the gas density MOD in meters.
     */
    fun densityMod(maxAllowedDensity: Double = MAX_GAS_DENSITY, environment: Environment): Double {
        val bar = maxAllowedDensity / density
        return barToDepthInMeters(bar, environment)
    }

    /**
     * Returns the gas density MOD in meters, rounded to the nearest integer.
     */
    fun densityModRounded(maxAllowedDensity: Double = MAX_GAS_DENSITY, environment: Environment): Int {
        return round(densityMod(maxAllowedDensity, environment)).toInt()
    }

    fun diveIndustryName(): String {

        // Neox and Hydreliox, Hydrox are not supported, since this app does not support hydrogen, argon or neon as a gas.
        return when {

            // One gas types (no helium/nitrogen)
            heliumFraction == 0.0 && nitrogenFraction == 0.0 -> "Oxygen" // Divox

            // Two gas types (no helium)
            heliumFraction == 0.0 && oxygenFraction == 0.21 -> "Air"
            heliumFraction == 0.0 && oxygenFraction > 0.21 -> "Nitrox"

            // Not really a normal gas blend though... and gas picker should not allow a gas like this
            heliumFraction == 0.0 && oxygenFraction < 0.21 -> "Hypoxic"

            // Helium types
            heliumFraction > 0.0 && nitrogenFraction == 0.0 -> "Heliox"
            heliumFraction > 0.0 && oxygenFraction <= 0.21 && nitrogenFraction > 0.0 -> "Trimix"
            heliumFraction > 0.0 && oxygenFraction > 0.21 && nitrogenFraction > 0.0 -> "Helitrox" //Triox (Hyperoxic Trimix)
            else -> error("Gas mixture ($this) should always match a name. This is a developer mistake and must be considered a bug!")
        }
    }

    override fun toString(): String {
        val oxygen = DecimalFormat.format(0, oxygenFraction * 100.0)
        val helium = DecimalFormat.format(0, heliumFraction * 100.0)

        return "$oxygen/$helium"
    }

    companion object {

        const val MAX_RECOMMENDED_GAS_DENSITY = 5.2
        const val MAX_GAS_DENSITY = 6.2
        const val MAX_PPO2 = 1.6

        val Air = Gas(oxygenFraction = 0.21, heliumFraction = 0.0)
        val Nitrox32 = Gas(oxygenFraction = 0.32, heliumFraction = 0.0)
        val Oxygen50 = Gas(oxygenFraction = 0.5, heliumFraction = 0.0)
        val Oxygen80= Gas(oxygenFraction = 0.8, heliumFraction = 0.0)
        val Oxygen100 = Gas(oxygenFraction = 1.0, heliumFraction = 0.0)
        val Trimix2135 = Gas(oxygenFraction = 0.21, heliumFraction = 0.35)
        val Trimix1845 = Gas(oxygenFraction = 0.18, heliumFraction = 0.45)
        val Trimix1555 = Gas(oxygenFraction = 0.15, heliumFraction = 0.55)
        val Trimix1060 = Gas(oxygenFraction = 0.10, heliumFraction = 0.60)
    }
}

/**
 * Density O2 = 31.998 g/mole x 1 mole/22.4 L = 1.428 g/L
 */
private const val DENSITY_O2 = 1.428

/**
 * Density N2 = 28.014 g/mole x 1 mole/22.4 L = 1.251 g/L
 */
private const val DENSITY_N2 = 1.251

/**
 * Density He = 4.00 g/mole x 1 mole/22.4 L = 0.178 g/L
 */
private const val DENSITY_HE = 0.178

/**
 * Returns the best gas in the list based on MOD and END.
 *
 * @return the best gas for the given depth, salinity, maximum ppo2 and maximum END, this may return
 *         null if no gasses match these criteria. If multiple gasses match the gas with the highest
 *         PPO2 is chosen (to have the maximum off gassing effect).
 */
fun List<Gas>.findBestDecoGas(depth: Double, environment: Environment, maxPPO2: Double, maxEND: Double): Gas? {
    var bestGas: Gas? = null
    forEach { candidateGas ->
        // TODO be safe and use 'floor' instead of 'round'?
        val mod = round(candidateGas.oxygenMod(maxPPO2, environment))
        val end = round(candidateGas.endInMeters(depth, environment))
        if (depth <= mod && end <= maxEND) {
            if (bestGas == null || bestGas!!.oxygenFraction < candidateGas.oxygenFraction) {
                bestGas = candidateGas
            }
        }
    }
    return bestGas
}
