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

import org.neotech.app.abysner.domain.core.physics.depthInMetersToBar
import kotlin.math.round

/**
 * Returns the best gas in the list for the current depth, based on given O2 MOD [maxPPO2] and END
 * ([maxEND]) constraints.
 *
 * This functions tracks two separate best candidates:
 *  - Ideal: satisfies both O2 MOD and END constraints → highest O2 fraction wins.
 *  - Fallback: if ideal does not satisfy any gas the END constraint is dropped → highest O2 fraction wins.
 *
 * Returns null when no candidate satisfies the O2 MOD. Callers that want automatic fallback
 * behavior can use [findBetterGasOrFallback] instead.
 *
 * A word on density:
 * Enriched gases (e.g. EANx 50) are inherently denser than air at any given depth, because
 * O2 is heavier than N2. Including density as a constraint would therefore always bias the
 * algorithm away from the higher-O2 deco gases that are specifically chosen for their superior
 * off-gassing effect. In practice this is usually a non-issue: a gas's O2 MOD already limits it to
 * depths where its density is within safe limits. However, if the user selects a non-ideal gas for
 * a section of the dive, the decompression ascent planned automatically may ping between different
 * gases due to the density constraints.
 *
 * Regardless, density is surfaced to the user as a warning in the limits table so they can make
 * informed planning choices, but for the above reasons it does not influence automatic gas
 * selection at runtime.
 *
 * @return the best cylinder for the given depth, or null if no cylinder satisfies the O2 MOD.
 */
fun List<Cylinder>.findBestGas(depth: Double, environment: Environment, maxPPO2: Double, maxEND: Double): Cylinder? {
    var ideal: Cylinder? = null
    var fallback: Cylinder? = null
    forEach { candidate ->
        // TODO be safe and use 'floor' instead of 'round'?
        val modOk = depth <= round(candidate.gas.oxygenMod(maxPPO2, environment))
        if (!modOk) {
            // MOD is not ok, skip this candidate don't botter about checking END.
            return@forEach
        }

        val endOk = round(candidate.gas.endInMeters(depth, environment)) <= maxEND

        if (endOk) {
            if (ideal == null || ideal.gas.oxygenFraction < candidate.gas.oxygenFraction) {
                ideal = candidate
            }
        } else {
            if (fallback == null || fallback.gas.oxygenFraction < candidate.gas.oxygenFraction) {
                fallback = candidate
            }
        }
    }
    return ideal ?: fallback
}

/**
 * Last-resort fallback when [findBestGas] returns null (no gas satisfies the O2 MOD at depth). If
 * any non-hypoxic candidates exist (PPO2 ≥ [minPPO2] at [depth]), the one with the lowest O2
 * fraction is returned to minimize O2 toxicity. If all candidates are hypoxic, the one with the
 * highest O2 fraction is returned instead, as it produces the highest PPO2 and is therefore the
 * least hypoxic option available.
 *
 * @return the safest cylinder from an oxygen point of view for the given depth, or null if the list
 * is empty.
 */
internal fun List<Cylinder>.findBreathableFallbackGas(
    depth: Double,
    environment: Environment,
    minPPO2: Double = Gas.MIN_PPO2,
): Cylinder? {
    val pressure = depthInMetersToBar(depth, environment).value
    val nonHypoxic = filter { it.gas.oxygenFraction * pressure >= minPPO2 }
    return if (nonHypoxic.isNotEmpty()) {
        nonHypoxic.minByOrNull { it.gas.oxygenFraction }
    } else {
        maxByOrNull { it.gas.oxygenFraction }
    }
}

/**
 * Convenience combination of [findBestGas] and [findBreathableFallbackGas]. Returns the best gas
 * for the given depth. If no gas satisfies the O2 MOD, falls back to [findBreathableFallbackGas],
 * but only when the fallback gas is genuinely better than [currentCylinder].
 *
 * "Better" is defined by the same criteria [findBreathableFallbackGas] uses to rank candidates: if
 * [currentCylinder] is already an equal or better choice than the fallback, it is returned instead
 * and no switch occurs.
 *
 * @return the best or fallback gas for the given depth, or [currentCylinder] if no switch is
 * warranted, or null if both the list and [currentCylinder] are null/empty.
 */
fun List<Cylinder>.findBetterGasOrFallback(currentCylinder: Cylinder?, depth: Double, environment: Environment, maxPPO2: Double, maxEND: Double, minPPO2: Double = Gas.MIN_PPO2): Cylinder? {
    val best = findBestGas(depth, environment, maxPPO2, maxEND)
    if (best != null) {
        return best
    } else {
        val fallback = findBreathableFallbackGas(depth, environment, minPPO2) ?: return currentCylinder
        // Only switch to the fallback if it is better than the current gas (see findBreathableFallbackGas).
        return if (currentCylinder == null || fallback.gas.oxygenFraction < currentCylinder.gas.oxygenFraction) {
            fallback
        } else {
            currentCylinder
        }
    }
}

