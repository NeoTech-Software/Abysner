/*
 * Abysner - Dive planner
 * Copyright (C) 2024-2026 Neotech
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
 * Returns the best gas in the list for the given ambient pressure. Filters on oxygen MOD (by
 * [maxPpO2]), then prefers gases that satisfy the END constraint
 * ([maxEquivalentNarcoticAmbientPressure]). If multiple gases satisfy END, the highest oxygen
 * fraction within MOD is chosen (helium as tiebreaker). If nothing satisfies the END constraint,
 * the END constraint is dropped and the highest oxygen fraction gas within MOD is returned (helium
 * as tiebreaker). Returns null if no gas satisfies the oxygen MOD at all.
 *
 * Density is intentionally not included as a constraint here: higher-O2 deco gases are denser, so
 * including density would bias the selection away from exactly the gases chosen for their
 * off-gassing properties. Density warnings are shown to the user separately.
 *
 * @param modTolerance small pressure allowance (bar) added to the oxygen MOD limit. Divers expect
 * some common gasses to be usable certain depths for example oxygen at 6 meters with ppO2 1.6,
 * even when the true MOD is shallower. Defaults to [Gas.MOD_TOLERANCE].
 */
fun List<Cylinder>.findBestGas(
    ambientPressure: Double,
    maxPpO2: Double,
    maxEquivalentNarcoticAmbientPressure: Double,
    modTolerance: Double = Gas.MOD_TOLERANCE,
): Cylinder? {
    // Step 1: Filter: Don't even consider gas that is beyond the MOD
    return filter {
        ambientPressure <= it.gas.oxygenModAmbientPressure(maxPpO2) + modTolerance
    }.maxWithOrNull(
        compareBy(
            // Step 2: Prefer gas that is within END
            { it.gas.endAmbientPressure(ambientPressure) <= maxEquivalentNarcoticAmbientPressure },
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
 * Last-resort fallback when [findBestGas] returns null (no gas satisfies the O2 MOD at this
 * ambient pressure). Prefers the lowest-O2 non-hypoxic candidate to minimize toxicity risk. If
 * everything is hypoxic, picks the highest-O2 option as the least-bad choice. Returns null if the
 * list is empty.
 */
internal fun List<Cylinder>.findBreathableFallbackGas(ambientPressure: Double, minPPO2: Double = Gas.MIN_PPO2): Cylinder? {
    val nonHypoxic = filter { it.gas.oxygenFraction * ambientPressure >= minPPO2 }
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
    ambientPressure: Double,
    maxPPO2: Double,
    maxEquivalentNarcoticAmbientPressure: Double,
    modTolerance: Double = Gas.MOD_TOLERANCE,
    minPPO2: Double = Gas.MIN_PPO2
): Cylinder? {
    val best = findBestGas(ambientPressure, maxPPO2, maxEquivalentNarcoticAmbientPressure, modTolerance)
    if (best != null) {
        return best
    } else {
        val fallback = findBreathableFallbackGas(ambientPressure, minPPO2) ?: return currentCylinder
        // Only switch to the fallback if it is better than the current gas (see findBreathableFallbackGas).
        return if (currentCylinder == null || fallback.gas.oxygenFraction < currentCylinder.gas.oxygenFraction) {
            fallback
        } else {
            currentCylinder
        }
    }
}

