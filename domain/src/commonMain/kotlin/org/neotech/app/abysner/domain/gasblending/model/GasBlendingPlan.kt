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

package org.neotech.app.abysner.domain.gasblending.model

import org.neotech.app.abysner.domain.core.model.Gas

/**
 * The result of [org.neotech.app.abysner.domain.gasblending.GasBlendingPlanner.plan].
 */
sealed interface GasBlendingPlan {

    /**
     * A complete, workable blending procedure. May legitimately contain no [steps] at all, when the
     * cylinder already holds the target gas, or when reaching the target is purely a matter of
     * draining (see [drainToPressure]).
     */
    data class Success(
        /**
         * Non-null when the cylinder holds too much of something to be topped up to the target, and
         * has to be drained down to this pressure (in bar) before blending starts. The gas vented is
         * wasted, hence it being called out separately rather than hidden inside [steps].
         */
        val drainToPressure: Double?,
        /** Gas in the cylinder when blending starts (after any drain), in liters at 1 bar. */
        val startVolume: Double,
        /** Gas in the cylinder when blending is done, in liters at 1 bar. */
        val targetVolume: Double,
        /** In the order the gases are added. Gases that are not needed are omitted. */
        val steps: List<BlendStep>,
    ) : GasBlendingPlan {

        /** Total gas added across all steps, in liters at 1 bar. */
        val totalAdded: Double by lazy { steps.sumOf { it.volume } }

        /**
         * How much of each gas is consumed in total, in liters at 1 bar. Keyed by gas, since the same
         * gas may legitimately appear in more than one step.
         */
        val volumePerGas: Map<Gas, Double> by lazy {
            steps.groupBy { it.gas }.mapValues { (_, steps) -> steps.sumOf { it.volume } }
        }
    }

    /**
     * The top-up gases cannot be told apart by the balance equations (for example two identical
     * gases, or three helium-free gases when blending trimix), so there is no unique solution.
     */
    data object DegenerateTopUpGases : GasBlendingPlan

    /**
     * No combination of the top-up gases reaches the target, not even starting from an empty
     * cylinder. The target needs a gas that none of the top-up gases can supply.
     */
    data object ImpossibleWithTheseGases : GasBlendingPlan

    /**
     * Two top-up gases can only balance total volume and oxygen, so the helium in the start, target
     * or top-up gases cannot be accounted for. A third top-up gas is required.
     */
    data object HeliumNotBalanced : GasBlendingPlan
}

/**
 * One top-up: add [volume] liters (at 1 bar) of [gas], ending up with [mixAfter] at
 * [pressureAfter].
 */
data class BlendStep(
    val gas: Gas,
    val volume: Double,
    val mixAfter: Gas,
    val pressureAfter: Double,
)
