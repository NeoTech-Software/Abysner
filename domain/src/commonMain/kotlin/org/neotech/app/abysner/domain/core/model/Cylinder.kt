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

import org.neotech.app.abysner.domain.core.physics.GasEquationOfStateModel
import org.neotech.app.abysner.domain.utilities.DecimalFormat
import org.neotech.app.abysner.domain.utilities.generateUUID

data class Cylinder(
    val gas: Gas,
    val pressure: Double,
    val waterVolume: Double,
    val uniqueIdentifier: String = generateUUID()
) {

    constructor(
        gas: Gas,
        pressure: Int,
        waterVolume: Int
    ): this(gas, pressure.toDouble(), waterVolume.toDouble())

    /**
     * Total amount of gas usable from this cylinder (does not take into account minimal operating
     * pressure of a regulator etc.)
     */
    fun capacity(gasEquationOfStateModel: GasEquationOfStateModel = GasEquationOfStateModel.Default): Double = gasEquationOfStateModel.getGasVolume(gas, waterVolume, pressure)

    fun capacityAt(gasEquationOfStateModel: GasEquationOfStateModel = GasEquationOfStateModel.Default, pressure: Double) = gasEquationOfStateModel.getGasVolume(gas, waterVolume, pressure)

    /**
     * Returns the pressure of this cylinder if a given volume of gas at 1 bar is compressed into
     * the cylinder.
     */
    fun pressureAt(gasEquationOfStateModel: GasEquationOfStateModel = GasEquationOfStateModel.Default, volume: Double) = gasEquationOfStateModel.getGasPressure(gas, waterVolume, volume)

    fun pressureAfter(gasEquationOfStateModel: GasEquationOfStateModel = GasEquationOfStateModel.Default, volumeUsage: Double): Double? {
        val leftOver = capacity(gasEquationOfStateModel) - volumeUsage
        return if(leftOver < 0) {
            null
        } else {
            pressureAt(gasEquationOfStateModel, leftOver)
        }
    }

    override fun toString(): String {
        val liters = DecimalFormat.format(1, waterVolume)
        val pressure = DecimalFormat.format(0, pressure)
        return "$gas (${liters}l @ ${pressure}bar)"
    }

    companion object {
        // Imperial tanks capacities/sizes are a bit of an issue
        // An 80 cu.ft is never exactly 80 cu.ft even manufactures seem
        // to have trouble converting between cu.ft tank sizes and liters.
        // Then there is the issue of 'ideal gas capacity' vs 'true capacity'
        //
        // For example in a catalog I found:
        // AL80: capacity 77.4 cu.ft at 207 bars (3000 PSI), or 2191.72 liters at 207 bars
        //
        // This calculates to 2191.72 / 207 = 10.59 liters at 1 bar (surface*)
        //
        // But in another catalog I found:
        // AL80: capacity 77.4 cu.ft at 207 bars (3000 PSI), 11.1 liters at 1 bars
        //
        // The above is weird, since the same pressure leads to a different surface volume in liters?
        //
        // So for aluminium decided to use one manufacturer as the standard: Luxfer
        // Source: https://scubapro.ae/wp-content/uploads/2015/03/Luxfer-Aluminum-Specifications.pdf
        //
        // For steel I went with Faber:
        // Source: https://www.divegearexpress.com/library/articles/calculating-scuba-cylinder-capacities#truecapacitytable

        /**
         * Based on Luxfer LAL100
         */
        val AL100_WATER_VOLUME = 13.2

        /**
         * Based on Luxfer LAL80
         */
        val AL80_WATER_VOLUME = 11.1

        /**
         * Based on Luxfer LAL63
         */
        val AL63_WATER_VOLUME = 9.0

        /**
         * Based on Luxfer LAL50
         */
        val AL50_WATER_VOLUME = 6.9

        /**
         * Based on Luxfer LAL40
         */
        val AL40_WATER_VOLUME = 5.7

        /**
         * Based on Luxfer LAL30
         */
        val AL30_WATER_VOLUME = 4.3

        /**
         * Based on Luxfer LAL19
         */
        val AL19_WATER_VOLUME = 2.9

        /**
         * Based on Luxfer LAL13
         */
        val AL13_WATER_VOLUME = 1.9

        /**
         * Based on Luxfer LAL06
         */
        val AL6_WATER_VOLUME = 0.9

        /**
         * Based on Faber X8-133 HDG
         */
        val HP133_WATER_VOLUME = 17.0

        /**
         * Based on Faber X7-120 HDG
         */
        val HP120_WATER_VOLUME = 15.3

        /**
         * Based on Faber X8-117 HDG
         */
        val HP117_WATER_VOLUME = 15.0

        /**
         * Based on Faber X7-100 HDG
         */
        val HP100_WATER_VOLUME = 12.9

        /**
         * Based on Faber X7-80 HDG
         */
        val HP80_WATER_VOLUME = 10.2

        inline fun steel10Liter(gas: Gas, pressure: Double = 232.0) = Cylinder(gas, pressure, 10.0)
        inline fun steel12Liter(gas: Gas, pressure: Double = 232.0) = Cylinder(gas, pressure, 12.0)
        inline fun aluminium80Cuft(gas: Gas, pressure: Double = 207.0) = Cylinder(gas, pressure, AL80_WATER_VOLUME)
        inline fun aluminium63Cuft(gas: Gas, pressure: Double = 207.0) = Cylinder(gas, pressure, AL63_WATER_VOLUME)


    }
}


