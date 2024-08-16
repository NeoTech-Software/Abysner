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

import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas

/**
 * Common interface for models that describe the relationship between pressure and volume for a
 * given gas at a constant temperature. Specific to pressure cylinders using in scuba.
 *
 * Note: Temperature is not a variable anywhere, it is assumed implementations use some logical
 * constant value that is suited for common scuba use.
 */
interface GasEquationOfStateModel {

    /**
     * Returns the total volume of gas in the given cylinder in liters.
     */
    fun getGasVolume(cylinder: Cylinder): Double =
        getGasVolume(cylinder.gas, cylinder.waterVolume, cylinder.pressure)

    /**
     * Returns the total volume of gas in the given cylinder in liters.
     */
    fun getGasVolume(gas: Gas, cylinderSize: Double, pressure: Double): Double

    /**
     * Returns the pressure of the cylinder
     */
    fun getGasPressure(cylinder: Cylinder, gasVolume: Double): Double =
        getGasPressure(cylinder.gas, cylinder.waterVolume, gasVolume)

    /**
     * Returns the pressure of the cylinder
     */
    fun getGasPressure(gas: Gas, cylinderSize: Double, gasVolume: Double): Double

    companion object {
        val Default = PolynomialRealGasModel()
    }
}
