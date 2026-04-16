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
import org.neotech.app.abysner.domain.core.model.BreathingMode
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
        val ttsValues = divePlan.segments.filter { it.ttsAfter != null }.toMutableList()

        val candidates = mutableListOf<DiveSegment>()
        var segmentIndex = 0
        while (segmentIndex < ttsValues.size) {
            val segment = ttsValues[segmentIndex++]
            val segmentTts = segment.ttsAfter!!
            var isCandidate = true

            val iterator = ttsValues.listIterator()
            while (iterator.hasNext()) {
                val other = iterator.next()
                if (other !== segment) {
                    val otherTts = other.ttsAfter!!
                    if (other.endDepth >= segment.endDepth && otherTts >= segmentTts) {
                        // Found segment at same or deeper depth with an equal or longer TTS, thus this segment cannot be the worst case gas scenario.
                        isCandidate = false
                        break
                    } else if (otherTts < segmentTts && other.endDepth < segment.endDepth) {
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
        val configuration = divePlan.configuration
        val environment = configuration.environment
        val segments = divePlan.segmentsCollapsed

        val ccrSegments = segments.filter { it.breathingMode is BreathingMode.ClosedCircuit }
        val isCcr = ccrSegments.isNotEmpty()
        val hasOcSegments = segments.any { it.breathingMode is BreathingMode.OpenCircuit }

        val baseLineOpenCircuit = segments.calculateOpenCircuitGasRequirements(configuration.sacRate, environment)

        // Only calculated for open-circuit, you wouldn't share a closed-circuit loop with a buddy.
        val emergencyOpenCircuit = mutableMapOf<Cylinder, Double>()
        if (hasOcSegments) {
            val outOfAirScenarios = findPotentialWorstCaseTtsPoints(divePlan).map { maxTtsSegment ->
                val ascent = divePlan.alternativeAccents[maxTtsSegment.end]
                ascent?.calculateOpenCircuitGasRequirements(configuration.sacRateOutOfAir, environment) ?: error("DivePlan does not have alternative ascent for T=${maxTtsSegment.end}, this should not happen and is a developer mistake.")
            }
            outOfAirScenarios.forEach { scenario ->
                scenario.mergeInto(emergencyOpenCircuit, ::max)
            }
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
        // Note: this does not address the scenario where, once a cylinder is empty, a
        // less-than-ideal gas may still be breathed for the remainder of the dive. Fixing that
        // requires a significant change in the planner.
        val baseLineOpenCircuitByGas = mutableMapOf<Gas, Double>()
        baseLineOpenCircuit.forEach { (cylinder, requirement) ->
            baseLineOpenCircuitByGas.updateOrInsert(cylinder.gas, requirement, Double::plus)
        }
        val emergencyOpenCircuitByGas = mutableMapOf<Gas, Double>()
        emergencyOpenCircuit.forEach { (cylinder, requirement) ->
            emergencyOpenCircuitByGas.updateOrInsert(cylinder.gas, requirement, Double::plus)
        }

        val cylindersByGas = divePlan.cylinders.groupBy { it.gas }

        // Distribute the open circuit requirements proportionally across same-gas cylinders.
        val openCircuitResult = cylindersByGas
            .filter { (gas, _) -> gas in baseLineOpenCircuitByGas || gas in emergencyOpenCircuitByGas }
            .flatMap { (gas, cylinders) ->
                distributeProportionally(
                    cylinders = cylinders.map { it.cylinder },
                    totalNormal = baseLineOpenCircuitByGas[gas] ?: 0.0,
                    totalEmergency = emergencyOpenCircuitByGas[gas] ?: 0.0,
                )
            }

        if (!isCcr) {
            return openCircuitResult.toImmutableList()
        }

        val closedCircuitResult = ccrSegments.calculateClosedCircuitGasRequirements(
            cylinders = divePlan.cylinders.map { it.cylinder },
            oxygenCylinder = divePlan.ccrOxygenCylinder,
            ccrMetabolicOxygenRate = configuration.ccrMetabolicO2LitersPerMinute,
            ccrLoopVolume = configuration.ccrLoopVolumeLiters,
            environment = environment,
        )

        return closedCircuitResult.merge(openCircuitResult).toImmutableList()
    }

    /**
     * Calculates gas requirements for closed-circuit segments. Oxygen requirements are calculated
     * using the provided [ccrMetabolicOxygenRate] rate, and diluent requirements are calculated
     * based on loop expansion during descent using the provided [ccrLoopVolume].
     *
     * If not provided the oxygen cylinder is assumed to be the first pure-oxygen cylinder, or if
     * not found no oxygen requirements are returned.
     */
    private fun List<DiveSegment>.calculateClosedCircuitGasRequirements(
        cylinders: List<Cylinder>,
        oxygenCylinder: Cylinder? = cylinders.filter { it.gas == Gas.Oxygen }.minByOrNull { it.waterVolume },
        ccrMetabolicOxygenRate: Double,
        ccrLoopVolume: Double,
        environment: Environment,
    ): List<CylinderGasRequirements> {

        // Metabolic oxygen usage
        val totalCcrMinutes = sumOf { it.duration }
        val o2Liters = totalCcrMinutes * ccrMetabolicOxygenRate

        // Diluent usage due to loop expansion
        val diluentLitersByCylinder = mutableMapOf<Cylinder, Double>()
        forEach { segment ->
            val startPressure = depthInMetersToBar(segment.startDepth, environment).value
            val endPressure = depthInMetersToBar(segment.endDepth, environment).value
            val pressureIncrease = endPressure - startPressure
            if (pressureIncrease > 0.0) {
                val expansion = pressureIncrease * ccrLoopVolume
                diluentLitersByCylinder.updateOrInsert(segment.cylinder, expansion, Double::plus)
            }
        }

        return buildList {
            oxygenCylinder?.let {
                add(CylinderGasRequirements(it, o2Liters, 0.0))
            }
            diluentLitersByCylinder.forEach { (cylinder, liters) ->
                add(CylinderGasRequirements(cylinder, liters, 0.0))
            }
        }
    }

    /**
     * Calculates gas requirements (based on SAC) per cylinder for open-circuit segments.
     * Closed-circuit segments are skipped.
     */
    private fun List<DiveSegment>.calculateOpenCircuitGasRequirements(sac: Double, environment: Environment): Map<Cylinder, Double> {
        val requiredLitersByCylinder = mutableMapOf<Cylinder, Double>()
        forEach { segment ->
            if (segment.breathingMode is BreathingMode.OpenCircuit) {
                val pressure = depthInMetersToBar(segment.averageDepth, environment)
                val sacAtDepth = sac * pressure.value
                val liters = segment.duration * sacAtDepth
                requiredLitersByCylinder.updateOrInsert(segment.cylinder, liters, Double::plus)
            }
        }
        return requiredLitersByCylinder
    }

    private fun List<CylinderGasRequirements>.merge(
        other: List<CylinderGasRequirements>
    ): List<CylinderGasRequirements> {
        val merged = associateByTo(linkedMapOf()) { it.cylinder }
        other.forEach { entry ->
            merged.updateOrInsert(entry.cylinder, entry) { current, new ->
                CylinderGasRequirements(
                    cylinder = current.cylinder,
                    normalRequirement = current.normalRequirement + new.normalRequirement,
                    extraEmergencyRequirement = current.extraEmergencyRequirement + new.extraEmergencyRequirement,
                )
            }
        }
        return merged.values.toList()
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
}
