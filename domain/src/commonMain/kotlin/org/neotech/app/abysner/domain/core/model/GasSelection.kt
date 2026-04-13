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
 * Returns the best gas in the list for the current depth, based on O2 MOD [maxPPO2] and END
 * [maxEND] constraints. Picks the highest O2 fraction that satisfies both. If nothing satisfies
 * the END constraint, the END constraint is dropped and the highest-O2 gas within MOD is returned
 * instead. Returns null if no gas satisfies the O2 MOD at all.
 *
 * Density is intentionally not included as a constraint here: higher-O2 deco gases are denser, so
 * including density would bias the selection away from exactly the gases chosen for their
 * off-gassing properties. Density warnings are shown to the user separately.
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
 * Last-resort fallback when [findBestGas] returns null (no gas satisfies the O2 MOD at this depth).
 * Prefers the lowest-O2 non-hypoxic candidate to minimize toxicity risk. If everything is hypoxic,
 * picks the highest-O2 option as the least-bad choice. Returns null if the list is empty.
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
 * Combines [findBestGas] with [findBreathableFallbackGas]. Only switches to the fallback if it is
 * actually a better choice than [currentCylinder] (lower O2 fraction when MOD is already exceeded).
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

