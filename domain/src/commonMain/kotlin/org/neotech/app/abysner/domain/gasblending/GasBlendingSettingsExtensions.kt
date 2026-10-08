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
import org.neotech.app.abysner.domain.gasblending.model.GasBlendingPlan
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

/**
 * What blending a set of inputs costs, given the gas prices those inputs carry.
 */
sealed interface BlendCost {

    /** The blend works out and every gas it needs has a price, so it can be compared on cost. */
    data class Known(val total: Double) : BlendCost

    /** The blend works out, but at least one gas it needs has no price filled in yet. */
    data object PriceMissing : BlendCost

    /** The target mix cannot be blended from these gases at all. */
    data object NotBlendable : BlendCost
}

/**
 * Prices the blend these inputs describe. Only reports a total when every gas involved has a known
 * price, since a partial sum is not something two options can be compared on.
 */
fun GasBlendingSettings.blendCost(planner: GasBlendingPlanner = GasBlendingPlanner()): BlendCost {
    val plan = planner.plan(toBlendingInput())
    if (plan !is GasBlendingPlan.Success) {
        return BlendCost.NotBlendable
    }
    val volumePerGas = plan.volumePerGas
    if (volumePerGas.keys.any { gasPrices[it] == null }) {
        return BlendCost.PriceMissing
    }
    return BlendCost.Known(
        total = volumePerGas.entries.sumOf { (gas, volume) -> gasPrices.getValue(gas) * volume }
    )
}
