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
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.physics.depthInMetersToBar
import org.neotech.app.abysner.domain.decompression.model.DiveSegment
import org.neotech.app.abysner.domain.diveplanning.model.DivePlan
import org.neotech.app.abysner.domain.gasplanning.model.GasPlan
import org.neotech.app.abysner.domain.gasplanning.model.CylinderGasRequirements
import org.neotech.app.abysner.domain.utilities.updateOrInsert
import kotlin.math.max

class GasPlanner {

    /**
     * Returns [DiveSegment] that are candidates for the worst-case ascent to the surface, based on
     * TTS. Segments are eliminated as candidate if another segment exists that is both deeper and
     * has a longer TTS, since this would for sure produce a higher gas requirement. However, a
     * segment is not eliminated if the other segment is deeper but has a shorter TTS, since the
     * shallower segment may require more gas (since its TTS is longer).
     *
     * Note: TTS is used as a filter to narrow down candidates, the true worst case still requires
     * calculating the actual gas usage for each ascent.
     */
    fun findWorstCaseAscentCandidates(divePlan: DivePlan, bailout: Boolean = false): List<DiveSegment> {
        val tts: (DiveSegment) -> Int? = if (bailout) { it -> it.ttsBailoutAfter } else { it -> it.ttsAfter }

        // Check only segments that have a TTS value to begin with
        val remaining = divePlan.segments.mapNotNull { segment -> tts(segment)?.let { segment to it } }.toMutableList()

        val candidates = mutableListOf<DiveSegment>()
        var index = 0
        while (index < remaining.size) {
            val (segment, segmentTts) = remaining[index++]
            var eliminated = false

            val iterator = remaining.listIterator()
            while (iterator.hasNext()) {
                val (other, otherTts) = iterator.next()
                when {
                    other === segment -> continue
                    other.endDepth >= segment.endDepth && otherTts >= segmentTts -> {
                        // Found a segment at the same or deeper depth with an equal or longer TTS,
                        // so this segment cannot be the worst case.
                        eliminated = true
                        break
                    }
                    other.endDepth < segment.endDepth && otherTts < segmentTts -> {
                        // This other segment is shallower and has a shorter TTS, so it can never be
                        // the worst case either. Remove it early so we don't check it again.
                        // Adjust the main loop index if the removal shifts elements before it.
                        if (iterator.previousIndex() < index) {
                            index--
                        }
                        iterator.remove()
                    }
                }
            }
            if (!eliminated) {
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

        return if (isCcr) {
            calculateCcrGasPlan(divePlan, configuration, environment, ccrSegments)
        } else {
            calculateOcGasPlan(divePlan, configuration, environment, segments)
        }
    }

    private fun calculateOcGasPlan(
        divePlan: DivePlan,
        configuration: Configuration,
        environment: Environment,
        segments: List<DiveSegment>,
    ): GasPlan {

        val normalByGas = mutableMapOf<Gas, Double>()
        segments.calculateOpenCircuitGasRequirements(configuration.sacRate, environment).forEach { (cylinder, requirement) ->
            normalByGas.updateOrInsert(cylinder.gas, requirement, Double::plus)
        }

        // Calculate reserves for an out-of-air buddy using the panic SAC rate
        val reserveByGas = mutableMapOf<Gas, Double>()
        val outOfAirScenarios = findWorstCaseAscentCandidates(divePlan).map { maxTtsSegment ->
            val ascent = divePlan.alternativeAccents[maxTtsSegment.end]
            ascent?.calculateOpenCircuitGasRequirements(configuration.sacRateOutOfAir, environment)
                ?: error("DivePlan does not have alternative ascent for T=${maxTtsSegment.end}, this should not happen and is a developer mistake.")
        }

        // Take the highest gas requirement per cylinder across all scenarios
        outOfAirScenarios.forEach { scenario ->
            scenario.forEach { (cylinder, requirement) ->
                reserveByGas.updateOrInsert(cylinder.gas, requirement) { existing, new -> max(existing, new) }
            }
        }

        return distributeByGas(divePlan, normalByGas, reserveByGas).toImmutableList()
    }

    private fun calculateCcrGasPlan(
        divePlan: DivePlan,
        configuration: Configuration,
        environment: Environment,
        ccrSegments: List<DiveSegment>,
    ): GasPlan {

        // In closed-circuit gas mode no reserves are calculated for an out-of-air buddy. Generally
        // speaking if your closed-circuit rebreather fails, you bail out to your own gas supply.
        // Perhaps if your diluent tank is also your bailout (recreational rebreather setup) then
        // you might need your buddies bailout, but if you assume that buddy dives the same setup,
        // you should be just fine with that bailout? It would also be quite complex to account for
        // all possible team setups etc.

        // TODO choice: calculate bailout with panic mode?
        //      a bailout is usually not as stressed as a true out-of-air? Since the loop
        //      usually remains somewhat breathable for a short while? Unless it's a dramatic
        //      flood perhaps?
        // Bailout reserve: worst-case open-circuit ascent at normal SAC rate.
        val reserveByGas = mutableMapOf<Gas, Double>()
        val bailoutScenarios = findWorstCaseAscentCandidates(divePlan, bailout = true).map { maxTtsSegment ->
            val ascent = divePlan.alternativeAccents[maxTtsSegment.end]
            ascent?.calculateOpenCircuitGasRequirements(configuration.sacRate, environment)
                ?: error("DivePlan does not have alternative ascent for T=${maxTtsSegment.end}, this should not happen and is a developer mistake.")
        }

        // Take the highest gas requirement per cylinder across all scenarios
        bailoutScenarios.forEach { scenario ->
            scenario.forEach { (cylinder, requirement) ->
                reserveByGas.updateOrInsert(cylinder.gas, requirement) { existing, new -> max(existing, new) }
            }
        }

        val bailoutResult = distributeByGas(divePlan, emptyMap(), reserveByGas)

        val closedCircuitResult = ccrSegments.calculateClosedCircuitGasRequirements(
            cylinders = divePlan.cylinders.map { it.cylinder },
            oxygenCylinder = divePlan.ccrOxygenCylinder,
            ccrMetabolicOxygenRate = configuration.ccrMetabolicO2LitersPerMinute,
            ccrLoopVolume = configuration.ccrLoopVolumeLiters,
            environment = environment,
        )

        return closedCircuitResult.merge(bailoutResult).toImmutableList()
    }

    /**
     * Pools gas requirements by mix and distributes them proportionally across same-mix cylinders.
     *
     * The decompression planner always selects one representative cylinder per gas mix via
     * List<Cylinder>.findBestDecoGas(). When the user has multiple cylinders with the same mix
     * (e.g. doubles or sidemount), the other cylinder(s) never appear in any DiveSegment and are
     * invisible to the raw requirement maps. Pooling by Gas and then redistributing proportionally
     * to each cylinder's capacity correctly spreads the usage across all same-mix cylinders.
     *
     * Note: this does not address the scenario where, once a cylinder is empty, a less-than-ideal
     * gas may still be breathed for the remainder of the dive. Fixing that requires a significant
     * change in the planner, which is now based on 'best-gas' not on 'make it work'.
     */
    private fun distributeByGas(
        divePlan: DivePlan,
        normalByGas: Map<Gas, Double>,
        reserveByGas: Map<Gas, Double>,
    ): List<CylinderGasRequirements> {
        val cylindersByGas = divePlan.cylinders.groupBy { it.gas }
        return cylindersByGas
            .filter { (gas, _) -> gas in normalByGas || gas in reserveByGas }
            .flatMap { (gas, cylinders) ->
                distributeProportionally(
                    cylinders = cylinders.map { it.cylinder },
                    totalNormal = normalByGas[gas] ?: 0.0,
                    totalEmergency = reserveByGas[gas] ?: 0.0,
                )
            }
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

    /**
     * Distributes the given gas usage evenly across the cylinders, assumes cylinder are of the same mix.
     */
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
