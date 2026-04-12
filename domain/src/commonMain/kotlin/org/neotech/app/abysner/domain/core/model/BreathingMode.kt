/*
 * Abysner - Dive planner
 * Copyright (C) 2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.domain.core.model

sealed class BreathingMode {

    data object OpenCircuit : BreathingMode()

    data class ClosedCircuit(val setpoint: Double) : BreathingMode() {
        init {
            require(setpoint > 0.0) { "CCR setpoint must be positive, got $setpoint" }
        }
    }

    /**
     * Returns the closed-circuit setpoint or null if the breathing mode is open circuit.
     */
    val ccrSetpoint: Double?
        get() = (this as? ClosedCircuit)?.setpoint
}

