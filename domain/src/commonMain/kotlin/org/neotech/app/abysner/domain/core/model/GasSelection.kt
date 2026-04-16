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
 * Returns the best gas in the list for the current depth. Filters on oxygen MOD (by [maxPpO2]),
 * then prefers gases that satisfy [maxEND]. If multiple gases satisfy END, then the highest oxygen
 * fraction is chosen, otherwise the highest helium fraction. If nothing satisfies the END
 * constraint, the END constraint is dropped and the highest oxygen fraction gas within MOD is
 * returned (helium as tiebreaker). Returns null if no gas satisfies the oxygen MOD at all.
 *
 * Density is intentionally not included as a constraint here: higher-O2 deco gases are denser, so
 * including density would bias the selection away from exactly the gases chosen for their
 * off-gassing properties. Density warnings are shown to the user separately.
 */
fun List<Cylinder>.findBestGas(depth: Double, environment: Environment, maxPpO2: Double, maxEND: Double): Cylinder? {
    // Step 1: Filter: Don't even consider gas that is beyond the MOD
    return filter {
        // Note: we use round here, since that better matches some divers expectations of certain
        // gases being usable at certain depths. Even if the ppO2 would be ever so slightly above
        // the max. The safer option would be floor, but that is not matching expectations.
        depth <= round(it.gas.oxygenMod(maxPpO2, environment))
    }.maxWithOrNull(
        compareBy(
            // Step 2: Prefer gas that is within END
            { round(it.gas.endInMeters(depth, environment)) <= maxEND },
            // Step 3: Tie? Prefer the higher oxygen fraction (will be valid within MOD)
            { it.gas.oxygenFraction },
            // Step 4: Still a tie? Prefer the higher helium fraction (lower density)
            // TODO prefer the bigger cylinder? Or prefer the helium contents that are still within
            //      density recommendations, but just yet, since helium is expensive? Probably
            //      overkill?
            { it.gas.heliumFraction },
        )
    )
}

/**
 * Last-resort fallback when [findBestGas] returns null (no gas satisfies the O2 MOD at this depth).
 * Prefers the lowest-O2 non-hypoxic candidate to minimize toxicity risk. If everything is hypoxic,
 * picks the highest-O2 option as the least-bad choice. Returns null if the list is empty.
 */
internal fun List<Cylinder>.findBreathableFallbackGas(depth: Double, environment: Environment, minPPO2: Double = Gas.MIN_PPO2, ): Cylinder? {
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
fun List<Cylinder>.findBetterGasOrFallback(
    currentCylinder: Cylinder?,
    depth: Double,
    environment: Environment,
    maxPPO2: Double,
    maxEND: Double,
    minPPO2: Double = Gas.MIN_PPO2
): Cylinder? {
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

