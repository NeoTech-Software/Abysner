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

package org.neotech.app.abysner.domain.gasplanning

import kotlinx.collections.immutable.toImmutableList
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.physics.depthInMetersToBar
import org.neotech.app.abysner.domain.decompression.model.DiveSegment
import org.neotech.app.abysner.domain.diveplanning.model.DivePlan
import org.neotech.app.abysner.domain.gasplanning.model.GasPlan
import org.neotech.app.abysner.domain.gasplanning.model.CylinderGasRequirements
import org.neotech.app.abysner.domain.utilities.mergeInto
import org.neotech.app.abysner.domain.utilities.updateOrInsert
import kotlin.math.max

class GasPlanner {

    /**
     * Given a [DivePlan] find the potential worst-case spots to ascend from, by comparing TTS values
     * at various depths. This returns a List of [DiveSegments][DiveSegment] with the highest TTS
     * values, as well as spots during the dive that have a lower TTS value but are deeper.
     */
    fun findPotentialWorstCaseTtsPoints(divePlan: DivePlan): List<DiveSegment> {

        // Find all segments with a TTS value
        val ttsValues = divePlan.segments.filter { it.ttsAfter != -1 }.toMutableList()

        val candidates = mutableListOf<DiveSegment>()
        var segmentIndex = 0
        while (segmentIndex < ttsValues.size) {
            val segment = ttsValues[segmentIndex++]
            var isCandidate = true

            val iterator = ttsValues.listIterator()
            while (iterator.hasNext()) {
                val other = iterator.next()
                if (other !== segment) {
                    if (other.endDepth >= segment.endDepth && other.ttsAfter >= segment.ttsAfter) {
                        // Found segment at same or deeper depth with an equal or longer TTS, thus this segment cannot be the worst case gas scenario.
                        isCandidate = false
                        break
                    } else if (other.ttsAfter < segment.ttsAfter && other.endDepth < segment.endDepth) {
                        // Found segment that is also not a worst case scenario, since it is shallower and has a shorter TTS.
                        // We can already remove this segment, as an optimization (no need to check this segment again).
                        // Correct main loop index if the removal happens before the current segment
                        if(iterator.previousIndex() < segmentIndex) {
                            segmentIndex--
                        }
                        iterator.remove()
                    }
                }
            }
            if (isCandidate) {
                candidates.add(segment)
            }
        }
        return candidates
    }

    fun calculateGasPlan(divePlan: DivePlan): GasPlan {
        // Calculate base gas usage for a normal dive for one diver
        val baseLine = divePlan.segmentsCollapsed.calculateGasRequirementsPerCylinder(divePlan.configuration.sacRate, divePlan.configuration.environment)

        val worstCaseGasLossScenarios = findPotentialWorstCaseTtsPoints(divePlan)

        // Calculate gas usage from multiple depths to the surface, at the point where the
        // diver is at the maximum TTS at those depths
        val outOfAirScenarios = worstCaseGasLossScenarios.map { maxTtsSegment ->

            // For each TTS calculated the dive plan should have an ascent schedule, retrieve it and use it to calculate gas usage.
            val ascent = divePlan.alternativeAccents[maxTtsSegment.end]

            // This only calculates the gas needed for the emergency ascent itself, for the diver
            // that is out-of-air. Any out-of-air event occurring earlier in the dive would require
            // ascending from a shallower depth or lower TTS, and therefore less emergency gas,
            // so computing from the worst-case point is inherently conservative.
            ascent?.calculateGasRequirementsPerCylinder(
                divePlan.configuration.sacRateOutOfAir,
                divePlan.configuration.environment
            ) ?: error("DivePlan does not have alternative ascent for T=${maxTtsSegment.end}, this should not happen and is a developer mistake.")
        }

        val extraRequiredForWorstCaseOutOfAir = mutableMapOf<Cylinder, Double>()
        outOfAirScenarios.forEach { scenario ->
            scenario.mergeInto(extraRequiredForWorstCaseOutOfAir, ::max)
        }

        // Pool total gas requirements by gas mix rather than by individual cylinder identity.
        //
        // The decompression planner currently always selects one representative cylinder per gas
        // mix via List<Cylinder>.findBestDecoGas(). When the user has multiple cylinders with the
        // same mix (e.g. doubles for back mount diving or sidemount diving), the other cylinder(s)
        // never appear in any DiveSegment and are therefore invisible to the raw baseLine gas
        // requirement map.
        //
        // By pooling requirements by Gas and then redistributing proportionally to each cylinder's
        // capacity, we correctly spread the usage across all same-mix cylinders.
        //
        // Note: this does not address the scenario where, once a cylinder is empty, a less-than-ideal
        // gas may still be breathed for the remainder of the dive. Fixing that requires a significant
        // change in the planner.
        val normalByGas = mutableMapOf<Gas, Double>()
        baseLine.forEach { (cylinder, req) ->
            normalByGas.updateOrInsert(cylinder.gas, req, Double::plus)
        }
        val emergencyByGas = mutableMapOf<Gas, Double>()
        extraRequiredForWorstCaseOutOfAir.forEach { (cylinder, req) ->
            emergencyByGas.updateOrInsert(cylinder.gas, req, Double::plus)
        }

        val cylindersByGas = divePlan.cylinders.groupBy { it.gas }

        return cylindersByGas
            // Some cylinders may never appear in any segment (planner did not use the cylinder), in
            // which case it has no entry in normalByGas and no gas requirement to report. Thus we
            // filter those out.
            .filter { (gas, _) -> gas in normalByGas }
            .flatMap { (gas, cylinders) ->
                distributeProportionally(
                    cylinders = cylinders,
                    totalNormal = normalByGas.getValue(gas),
                    totalEmergency = emergencyByGas[gas] ?: 0.0,
                )
            }
            .toImmutableList()
    }

    private fun distributeProportionally(
        cylinders: List<Cylinder>,
        totalNormal: Double,
        totalEmergency: Double,
    ): List<CylinderGasRequirements> {
        val totalCapacity = cylinders.sumOf { it.capacity() }
        return cylinders.map { cylinder ->
            val fraction = cylinder.capacity() / totalCapacity
            CylinderGasRequirements(cylinder, totalNormal * fraction, totalEmergency * fraction)
        }
    }

    private fun List<DiveSegment>.calculateGasRequirementsPerCylinder(sac: Double, environment: Environment): Map<Cylinder, Double> {
        val requiredLitersByGas = mutableMapOf<Cylinder, Double>()
        forEach {
            val pressure = depthInMetersToBar(it.averageDepth, environment)
            val sacAtDepth = sac * pressure.value
            val liters = it.duration * sacAtDepth
            requiredLitersByGas.updateOrInsert(it.cylinder, liters, Double::plus)
        }
        return requiredLitersByGas
    }
}
