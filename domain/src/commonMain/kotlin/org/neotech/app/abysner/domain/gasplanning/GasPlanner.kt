/*
 * Abysner - Dive planner
 * Copyright (C) 2024 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.domain.gasplanning

import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.physics.depthInMetersToBars
import org.neotech.app.abysner.domain.decompression.model.DiveSegment
import org.neotech.app.abysner.domain.diveplanning.model.DivePlan
import org.neotech.app.abysner.domain.gasplanning.model.GasPlan
import org.neotech.app.abysner.domain.gasplanning.model.GasUsage
import org.neotech.app.abysner.domain.utilities.mergeInto
import org.neotech.app.abysner.domain.utilities.updateOrInsert
import kotlin.math.max

class GasPlanner {

    /**
     * Given a [DivePlan] find the potential worst-case spots to ascent from, by comparing TTS values
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

            // For each TTS calculated the dive plan should have a accent schedule, retrieve it and use it to calculate gas usage.
            val ascent = divePlan.alternativeAccents[maxTtsSegment.end]
            ascent?.calculateGasRequirementsPerCylinder(
                divePlan.configuration.sacRateOutOfAir,
                divePlan.configuration.environment
            ) ?: error("DivePlan does not have alternative accent for T=${maxTtsSegment.end}, this should not happen and is a developer mistake.")
        }

        val extraRequiredForWorstCaseOutOfAir = mutableMapOf<Cylinder, Double>()
        outOfAirScenarios.forEach { scenario ->
            scenario.mergeInto(extraRequiredForWorstCaseOutOfAir, ::max)
        }

        return baseLine.map {
            // It may happen that for a specific gas no extra is required, hence the default to 0.0
            // liters if that gas is not found.
            GasUsage(it.key, it.value, extraRequiredForWorstCaseOutOfAir[it.key] ?: 0.0)
        }
    }

    private fun List<DiveSegment>.calculateGasRequirementsPerCylinder(sac: Double, environment: Environment): Map<Cylinder, Double> {
        val requiredLitersByGas = mutableMapOf<Cylinder, Double>()
        forEach {
            val pressure = depthInMetersToBars(it.averageDepth, environment)
            val sacAtDepth = sac * pressure
            val liters = it.duration * sacAtDepth
            requiredLitersByGas.updateOrInsert(it.cylinder, liters) { currentValue, newValue ->
                currentValue + newValue
            }
        }
        return requiredLitersByGas
    }
}
