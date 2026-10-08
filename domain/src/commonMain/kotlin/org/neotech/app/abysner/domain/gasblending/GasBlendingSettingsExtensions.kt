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

package org.neotech.app.abysner.domain.gasblending

import org.neotech.app.abysner.domain.gasblending.model.GasBlendingInput
import org.neotech.app.abysner.domain.settings.model.GasBlendingSettings

/**
 * The remembered blending inputs as something [GasBlendingPlanner] can solve.
 */
fun GasBlendingSettings.toBlendingInput(): GasBlendingInput = GasBlendingInput(
    cylinderSize = cylinderSize,
    startGas = startGas,
    startPressure = startPressure,
    targetGas = targetGas,
    targetPressure = targetPressure,
    topUpGases = topUpGases,
)
