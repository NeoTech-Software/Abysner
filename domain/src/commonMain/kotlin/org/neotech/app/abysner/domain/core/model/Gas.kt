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

import org.neotech.app.abysner.domain.core.physics.ambientPressureToMeters
import org.neotech.app.abysner.domain.core.physics.metersToAmbientPressure
import org.neotech.app.abysner.domain.utilities.DecimalFormat
import kotlin.math.floor
import kotlin.math.round

data class Gas(val oxygenFraction: Double, val heliumFraction: Double) {

    init {
        if((oxygenFraction + heliumFraction) > 1.0) {
            error("Oxygen ($oxygenFraction) and helium ($heliumFraction) fraction add up to more then 1.0!")
        }
    }

    val oxygenPercentage: Int = round(oxygenFraction * 100.0).toInt()
    val heliumPercentage: Int = round(heliumFraction * 100.0).toInt()

    /**
     * The N2 fraction of this gas, calculated by subtracting the helium and oxygen fractions from 1.0.
     */
    val nitrogenFraction = (1.0 - (oxygenFraction + heliumFraction))

    /**
     * Returns the oxygen MOD as absolute ambient pressure in bar.
     */
    fun oxygenModAmbientPressure(ppO2: Double): Double = ppO2 / oxygenFraction

    /**
     * Returns the oxygen MOD in depth meters for the given environment.
     * TODO: this method must be made unit aware when imperial unit support is added.
     */
    fun oxygenMod(ppO2: Double, environment: Environment): Double {
        return ambientPressureToMeters(oxygenModAmbientPressure(ppO2), environment)
    }

    /**
     * Returns the oxygen MOD in depth meters, floored to a whole number while taking into account
     * the [MOD_TOLERANCE], which adds a bit of margin to the allowed ambient pressure for this gas
     * before making the conversion. This allows values like 20.92 meters to become 21 meters,
     * instead of being floored to 20 meters. Which is often closer to what divers expect, as they
     * often work with rules of thumb.
     *
     * This approach is very similar to simply rounding the true MOD in meters to the nearest
     * integer. However since it operates on the raw ambient pressure this will keep working for
     * imperial units as well, however it is probably less important for imperial units since the
     * feet granularity is finer than meters.
     *
     * [findBestGas][org.neotech.app.abysner.domain.core.model.findBestGas] and this method both
     * use the same tolerances, so switch depths are consistent with the MODs shown to the user
     * (except those clamp to a 3 meter or 10 feet step size).
     *
     * TODO: this method must be made unit aware when imperial unit support is added.
     */
    fun oxygenModRounded(ppO2: Double, environment: Environment, modTolerance: Double = MOD_TOLERANCE): Int {
        return floor(ambientPressureToMeters(oxygenModAmbientPressure(ppO2) + modTolerance, environment)).toInt()
    }

    /**
     * Calculates END (Equivalent Narcotic Depth) as absolute ambient pressure in bar. Only oxygen
     * and nitrogen are considered narcotic.
     *
     * https://en.wikipedia.org/wiki/Equivalent_narcotic_depth
     */
    fun endAmbientPressure(ambientPressure: Double): Double {
        val narcIndex = oxygenFraction + nitrogenFraction
        return ambientPressure * narcIndex
    }

    val density: Double by lazy {
        val oxygenDensity = DENSITY_O2 * oxygenFraction
        val heliumDensity = DENSITY_HE * heliumFraction
        val nitrogenDensity = DENSITY_N2 * nitrogenFraction
        oxygenDensity + heliumDensity + nitrogenDensity
    }

    fun densityAtAmbientPressure(ambientPressure: Double): Double = density * ambientPressure

    fun densityAtDepth(depth: Double, environment: Environment): Double {
        return densityAtAmbientPressure(metersToAmbientPressure(depth, environment).value)
    }

    /**
     * Returns the gas density MOD as absolute ambient pressure in bar.
     */
    fun densityModAmbientPressure(maxAllowedDensity: Double = MAX_GAS_DENSITY): Double =
        maxAllowedDensity / density

    /**
     * Returns the gas density MOD in depth meters.
     *
     * TODO: this method must be made unit aware when imperial unit support is added.
     */
    fun densityMod(maxAllowedDensity: Double = MAX_GAS_DENSITY, environment: Environment): Double {
        return ambientPressureToMeters(densityModAmbientPressure(maxAllowedDensity), environment)
    }

    /**
     * Returns the gas density MOD in depth meters, rounded to the nearest integer.
     *
     * TODO: this method must be made unit aware when imperial unit support is added. Note that
     *       rounding here is fine in both metric and imperial units, since there are not really
     *       rules of thumb for density MODs like there are for oxygen MODs. So a tolerance on
     *       pressure would not be required here. In metric we round a bit more compared to
     *       imperial, but that is fine.
     */
    fun densityModRounded(maxAllowedDensity: Double = MAX_GAS_DENSITY, environment: Environment): Int {
        return round(densityMod(maxAllowedDensity, environment)).toInt()
    }

    /**
     * Returns the CCR loop gas composition for the given setpoint and ambient pressure, when this
     * gas is used as diluent. The O2 fraction is the setpoint divided by the ambient pressure, He
     * and N2 scale proportionally in the remaining fraction. No water vapor correction is applied,
     * since this models not the lungs but rather the loop composition.
     */
    fun inspiredGas(ambientPressure: Double, setpoint: Double): Gas {
        val inspiredOxygenFraction = (setpoint / ambientPressure).coerceIn(oxygenFraction, 1.0)
        val inertScale = if (oxygenFraction < 1.0) {
            (1.0 - inspiredOxygenFraction) / (1.0 - oxygenFraction)
        } else {
            0.0
        }
        val inspiredHe = heliumFraction * inertScale
        return Gas(oxygenFraction = inspiredOxygenFraction, heliumFraction = inspiredHe)
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
        const val MIN_PPO2 = 0.16

        /**
         * Pressure tolerance in bar for MOD comparisons. Divers traditionally used rule of thumb
         * methods for MOD calculations and some mixes are generally accepted to be usable at
         * certain depths. For example 100% oxygen is commonly accepted to be usable till 6m/20ft,
         * even though the true MOD is around 5.8 meters. This tolerance allows the app to meet
         * diver expectations and match traditional rules of thumb. The 0.05 bar tolerance
         * corresponds to roughly half a meter. This tolerance matters mostly for metric divers,
         * since imperial units (feet) the granularity is finer to begin with.
         */
        const val MOD_TOLERANCE = 0.05

        val Air = Gas(oxygenFraction = 0.21, heliumFraction = 0.0)
        val Nitrox28 = Gas(oxygenFraction = 0.28, heliumFraction = 0.0)
        val Nitrox32 = Gas(oxygenFraction = 0.32, heliumFraction = 0.0)
        val Nitrox36 = Gas(oxygenFraction = 0.36, heliumFraction = 0.0)
        val Nitrox40 = Gas(oxygenFraction = 0.40, heliumFraction = 0.0)
        val Nitrox50 = Gas(oxygenFraction = 0.5, heliumFraction = 0.0)
        val Nitrox80= Gas(oxygenFraction = 0.8, heliumFraction = 0.0)
        val Oxygen = Gas(oxygenFraction = 1.0, heliumFraction = 0.0)

        // "Recreational Trimix"
        val Trimix3030 = Gas(oxygenFraction = 0.30, heliumFraction = 0.30)
        // Technical Trimix
        val Trimix2135 = Gas(oxygenFraction = 0.21, heliumFraction = 0.35)
        val Trimix1845 = Gas(oxygenFraction = 0.18, heliumFraction = 0.45)
        val Trimix1555 = Gas(oxygenFraction = 0.15, heliumFraction = 0.55)
        val Trimix1070 = Gas(oxygenFraction = 0.10, heliumFraction = 0.70)

        val StandardGases = listOf(
            Air,
            Nitrox28, Nitrox32, Nitrox36, Nitrox40,
            Nitrox50, Nitrox80,
            Oxygen,
            Trimix3030,
            Trimix2135, Trimix1845, Trimix1555, Trimix1070
        )
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
