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
import org.neotech.app.abysner.domain.core.physics.PSI_PER_BAR
import org.neotech.app.abysner.domain.utilities.DecimalFormat
import org.neotech.app.abysner.domain.utilities.generateUUID

data class Cylinder(
    val gas: Gas,
    val pressure: Double,
    val size: Size,
    val uniqueIdentifier: String = generateUUID()
) {

    data class Size(
        val name: String? = null,
        val waterVolume: Double,
        val workingPressure: Double,
    ) {
        fun fill(gas: Gas, pressure: Double = workingPressure) = Cylinder(gas, pressure, this)

        /**
         * Capacity in liters (uses real-gas properties based on Air at working pressure). This is
         * what manufacturers might list on spec sheets as the cylinder's true capacity.
         *
         * For an exact fill capacity for a specific gas use: [Cylinder.capacity] or
         * [Cylinder.capacityAt].
         */
        fun ratedCapacity(
            gasEquationOfStateModel: GasEquationOfStateModel = GasEquationOfStateModel.Default
        ): Double =
            gasEquationOfStateModel.getGasVolume(Gas.Air, waterVolume, workingPressure)

        companion object {

            /**
             * Finds a matching standard size (if any) for a given water volume and working
             * pressure. Matches within 0.05L and 0.5 bar tolerance.
             */
            fun findMatching(waterVolume: Double, workingPressure: Double): Size? =
                StandardSizes.firstOrNull {
                    kotlin.math.abs(it.waterVolume - waterVolume) < 0.05 &&
                        kotlin.math.abs(it.workingPressure - workingPressure) < 0.5
                }
        }
    }

    val waterVolume: Double get() = size.waterVolume
    val workingPressure: Double get() = size.workingPressure

    constructor(
        gas: Gas,
        pressure: Double,
        waterVolume: Double,
        workingPressure: Double = pressure,
        uniqueIdentifier: String = generateUUID()
    ) : this(
        gas = gas,
        pressure = pressure,
        size = Size(waterVolume = waterVolume, workingPressure = workingPressure),
        uniqueIdentifier = uniqueIdentifier
    )

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

        private const val BAR_3000_PSI = 3000.0 / PSI_PER_BAR
        private const val BAR_3300_PSI = 3300.0 / PSI_PER_BAR
        private const val BAR_3442_PSI = 3442.0 / PSI_PER_BAR

        // Common (EU) metric steel cylinders (not based on any specific manufacturer's specs, but
        // very consistently available in Europe)

        val STEEL_3L = Size(name = "3L", waterVolume = 3.0, workingPressure = 232.0)
        val STEEL_7L = Size(name = "7L", waterVolume = 7.0, workingPressure = 232.0)
        val STEEL_8_5L = Size(name = "8.5L", waterVolume = 8.5, workingPressure = 232.0)
        val STEEL_10L = Size(name = "10L", waterVolume = 10.0, workingPressure = 232.0)
        val STEEL_12L = Size(name = "12L", waterVolume = 12.0, workingPressure = 232.0)
        val STEEL_15L = Size(name = "15L", waterVolume = 15.0, workingPressure = 232.0)

        // Common (EU) metric twinset configurations (doubles)

        val D7 = Size(name = "D7", waterVolume = 14.0, workingPressure = 232.0)
        val D8_5 = Size(name = "D8.5", waterVolume = 17.0, workingPressure = 232.0)
        val D10 = Size(name = "D10", waterVolume = 20.0, workingPressure = 232.0)
        val D12 = Size(name = "D12", waterVolume = 24.0, workingPressure = 232.0)

        // Aluminium cylinders (Luxfer specs)
        // Source: https://scubapro.ae/wp-content/uploads/2015/03/Luxfer-Aluminum-Specifications.pdf
        // Note: Luxfer lists working pressures in both bar and PSI, but they don't round-trip
        // exactly, since working pressure is more important in imperial mode, the exact PSI values
        // are used as reference.

        val AL6 = Size(name = "AL6", waterVolume = 0.9, workingPressure = BAR_3000_PSI)
        val AL13 = Size(name = "AL13", waterVolume = 1.9, workingPressure = BAR_3000_PSI)
        val AL19 = Size(name = "AL19", waterVolume = 2.9, workingPressure = BAR_3000_PSI)
        val AL30 = Size(name = "AL30", waterVolume = 4.3, workingPressure = BAR_3000_PSI)
        val AL40 = Size(name = "AL40", waterVolume = 5.7, workingPressure = BAR_3000_PSI)
        val AL50 = Size(name = "AL50", waterVolume = 6.9, workingPressure = BAR_3000_PSI)
        val AL63 = Size(name = "AL63", waterVolume = 9.0, workingPressure = BAR_3000_PSI)
        val AL80 = Size(name = "AL80", waterVolume = 11.1, workingPressure = BAR_3000_PSI)
        val AL100 = Size(name = "AL100", waterVolume = 13.2, workingPressure = BAR_3300_PSI)

        // Imperial steel cylinders (Faber specs)
        // Source: https://www.divegearexpress.com/library/articles/calculating-scuba-cylinder-capacities#truecapacitytable
        // Note: Faber lists working pressures in both bar and PSI, but they don't round-trip
        // exactly, since working pressure is more important in imperial mode, the exact PSI values
        // are used as reference.

        val HP80 = Size(name = "HP80", waterVolume = 10.2, workingPressure = BAR_3442_PSI)
        val HP100 = Size(name = "HP100", waterVolume = 12.9, workingPressure = BAR_3442_PSI)
        val HP117 = Size(name = "HP117", waterVolume = 15.0, workingPressure = BAR_3442_PSI)
        val HP120 = Size(name = "HP120", waterVolume = 15.3, workingPressure = BAR_3442_PSI)
        val HP133 = Size(name = "HP133", waterVolume = 17.0, workingPressure = BAR_3442_PSI)

        val StandardMetricSizes = listOf(
            STEEL_3L, STEEL_7L, STEEL_8_5L, STEEL_10L, STEEL_12L, STEEL_15L,
            D7, D8_5, D10, D12,
        )

        val StandardImperialSizes = listOf(
            AL40, AL63, AL80, AL100,
            HP80, HP100, HP117, HP120, HP133,
        )

        val StandardSizes = StandardMetricSizes + StandardImperialSizes

        fun steel3LiterOxygen(pressure: Double = 200.0) = STEEL_3L.fill(Gas.Oxygen, pressure)
        fun steel10Liter(gas: Gas, pressure: Double = 232.0) = STEEL_10L.fill(gas, pressure)
        fun steel12Liter(gas: Gas, pressure: Double = 232.0) = STEEL_12L.fill(gas, pressure)
        fun aluminium80Cuft(gas: Gas, pressure: Double = BAR_3000_PSI) = AL80.fill(gas, pressure)
        fun aluminium63Cuft(gas: Gas, pressure: Double = BAR_3000_PSI) = AL63.fill(gas, pressure)
    }
}
