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

    data class ClosedCircuit(
        /**
         * The setpoint in bars that the CCR is set to maintain, this is not the effective setpoint
         * used for tissue loading or oxygen toxicity calculations, which may be influenced by the
         * ambient pressure.
         */
        val setpoint: Double
    ) : BreathingMode() {
        init {
            require(setpoint > 0.0) { "CCR setpoint must be positive, got $setpoint" }
        }

        /**
         * Returns the effective ppO2 for tissue loading: the setpoint capped at ambient pressure.
         * This assumes pure oxygen as injection gas.
         *
         * @see org.neotech.app.abysner.domain.decompression.algorithm.buhlmann.ccrSchreinerInputs
         */
        fun effectivePpO2(ambientPressure: Double): Double = minOf(setpoint, ambientPressure)
    }

    val ccrSetpointOrNull: Double?
        get() = (this as? ClosedCircuit)?.setpoint

    companion object {

        fun ccr(setpoint: Double) = ClosedCircuit(setpoint)

        fun oc() = BreathingMode.OpenCircuit
    }
}

