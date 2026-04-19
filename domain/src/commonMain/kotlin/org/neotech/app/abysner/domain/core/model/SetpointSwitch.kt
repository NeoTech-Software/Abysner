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

/**
 * Describes a CCR setpoint switch that should occur when the diver crosses a specific depth. This
 * can be fed into the [org.neotech.app.abysner.domain.decompression.DecompressionPlanner] to have
 * it automatically switch when certain depths are crossed during the dive.
 */
data class SetpointSwitch(
    val depth: Int,
    val toBreathingMode: BreathingMode.ClosedCircuit,
)

