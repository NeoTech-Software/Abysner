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
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.decompression.model.DiveSegment
import org.neotech.app.abysner.domain.diveplanning.DivePlanner.PlanningException
import org.neotech.app.abysner.domain.diveplanning.model.AssignedCylinder
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
                    other.endPressure >= segment.endPressure && otherTts >= segmentTts -> {
                        // Found a segment at the same or deeper depth with an equal or longer TTS,
                        // so this segment cannot be the worst case.
                        eliminated = true
                        break
                    }
                    other.endPressure < segment.endPressure && otherTts < segmentTts -> {
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
        val segments = divePlan.segmentsCollapsed

        if (configuration.sacRateStress < configuration.sacRate || configuration.sacRateStress < configuration.sacRateDeco) {
            throw PlanningException(
                "sacRateStress (${configuration.sacRateStress}) must be at least as high as both " +
                    "sacRate (${configuration.sacRate}) and sacRateDeco (${configuration.sacRateDeco})."
            )
        }

        val ccrSegments = segments.filter { it.breathingMode is BreathingMode.ClosedCircuit }
        val isCcr = ccrSegments.isNotEmpty()

        return if (isCcr) {
            calculateCcrGasPlan(divePlan, configuration, ccrSegments)
        } else {
            calculateOcGasPlan(divePlan, configuration, segments)
        }
    }

    private fun calculateOcGasPlan(
        divePlan: DivePlan,
        configuration: Configuration,
        segments: List<DiveSegment>,
    ): GasPlan {

        val normalByGas = mutableMapOf<Gas, Double>()
        segments.calculateOpenCircuitGasRequirements(
            normalRate = configuration.sacRate,
            decoRate = configuration.sacRateDeco,
        ).forEach { (cylinder, requirement) ->
            normalByGas.updateOrInsert(cylinder.gas, requirement, Double::plus)
        }

        // Both divers breathe from this supply for the whole ascent and both are stressed:
        // 2 * sacRateStress in total. The donor's own normal consumption over that period is
        // already counted in normalRequirement, so only the rest needs to be added here:
        // 2 * sacRateStress - sacRate. Always positive, since sacRateStress can never be lower
        // than sacRate here.
        val stressRate = 2.0 * configuration.sacRateStress - configuration.sacRate

        // Calculate reserves for an out-of-air buddy using the stress SAC rate, cooling down to the
        // normal (or deco) rate once the stress window elapses.
        val reserveByGas = mutableMapOf<Gas, Double>()
        val outOfAirScenarios = findWorstCaseAscentCandidates(divePlan).map { maxTtsSegment ->
            val ascent = divePlan.alternativeAccents[maxTtsSegment.end]
            ascent?.calculateOpenCircuitGasRequirements(
                normalRate = configuration.sacRate,
                decoRate = configuration.sacRateDeco,
                stressRate = stressRate,
                stress = StressWindow(
                    durationMinutes = configuration.stressDurationMinutes,
                    originMinute = maxTtsSegment.end,
                ),
            ) ?: error("DivePlan does not have alternative ascent for T=${maxTtsSegment.end}, this should not happen and is a developer mistake.")
        }

        // Take the highest gas requirement per cylinder across all scenarios
        outOfAirScenarios.forEach { scenario ->
            scenario.forEach { (cylinder, requirement) ->
                reserveByGas.updateOrInsert(cylinder.gas, requirement) { existing, new -> max(existing, new) }
            }
        }

        return distributeByGas(divePlan.cylinders, normalByGas, reserveByGas).toImmutableList()
    }

    private fun calculateCcrGasPlan(
        divePlan: DivePlan,
        configuration: Configuration,
        ccrSegments: List<DiveSegment>,
    ): GasPlan {

        // In closed-circuit gas mode, no reserves are calculated for an out-of-air buddy. Instead,
        // the bailout reserve is calculated based on a worst-case open-circuit ascent at the stress
        // rate for one diver, cooling down after the stress timeout.
        //
        // Generally speaking, if your closed-circuit rebreather fails, you bail out to your own OC
        // gas supply. If instead an OC buddy needs to borrow your bailout gas, this number is still
        // sized for exactly one diver reaching the surface, not both of you at once, and it does not
        // account for your own CCR then also failing on top of that. Modeling every team and gear
        // combination is out of scope, so sizing for one diver is a reasonable default.
        val reserveByGas = mutableMapOf<Gas, Double>()
        val bailoutScenarios = findWorstCaseAscentCandidates(divePlan, bailout = true).map { maxTtsSegment ->
            val ascent = divePlan.alternativeAccents[maxTtsSegment.end]
                ?: error("DivePlan does not have alternative ascent for T=${maxTtsSegment.end}, this should not happen and is a developer mistake.")
            // The stress timeout starts at the loop-to-open-circuit transition, not at the moment
            // the problem occurs, so problem-solving time on the loop does not consume it.
            val stressOrigin = ascent.firstOrNull { it.breathingMode is BreathingMode.OpenCircuit }?.start
                ?: maxTtsSegment.end
            ascent.calculateOpenCircuitGasRequirements(
                normalRate = configuration.sacRate,
                decoRate = configuration.sacRateDeco,
                stressRate = configuration.sacRateStress,
                stress = StressWindow(
                    durationMinutes = configuration.stressDurationMinutes,
                    originMinute = stressOrigin,
                ),
            )
        }

        // Take the highest gas requirement per cylinder across all scenarios
        bailoutScenarios.forEach { scenario ->
            scenario.forEach { (cylinder, requirement) ->
                reserveByGas.updateOrInsert(cylinder.gas, requirement) { existing, new -> max(existing, new) }
            }
        }

        val bailoutCylinders = divePlan.cylinders.filter { it.isAvailableForBailout }
        val bailoutResult = distributeByGas(bailoutCylinders, emptyMap(), reserveByGas)

        val closedCircuitResult = ccrSegments.calculateClosedCircuitGasRequirements(
            cylinders = divePlan.cylinders.map { it.cylinder },
            oxygenCylinder = divePlan.ccrOxygenCylinder,
            ccrMetabolicOxygenRate = configuration.ccrMetabolicO2LitersPerMinute,
            ccrLoopVolume = configuration.ccrLoopVolumeLiters,
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
        cylinders: List<AssignedCylinder>,
        normalByGas: Map<Gas, Double>,
        reserveByGas: Map<Gas, Double>,
    ): List<CylinderGasRequirements> {
        val cylindersByGas = cylinders.groupBy { it.gas }
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
    ): List<CylinderGasRequirements> {

        // Metabolic oxygen usage
        val totalCcrMinutes = sumOf { it.duration }
        val o2Liters = totalCcrMinutes * ccrMetabolicOxygenRate

        // Diluent usage due to loop expansion
        val diluentLitersByCylinder = mutableMapOf<Cylinder, Double>()
        forEach { segment ->
            val pressureIncrease = segment.endPressure - segment.startPressure
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
     * A window of [durationMinutes] minutes counted from [originMinute], during which the stress
     * rate applies instead of the caller's normal/deco rate.
     */
    private data class StressWindow(
        val durationMinutes: Int,
        val originMinute: Int,
    )

    /**
     * Calculates gas requirements (based on SAC) per cylinder for open-circuit segments.
     * Closed-circuit segments are skipped. [normalRate] applies outside decompression stops,
     * [decoRate] applies to [DiveSegment.Type.DECO_STOP] segments, and both are overridden by
     * [stressRate] for the minutes [stress]'s window covers.
     */
    private fun List<DiveSegment>.calculateOpenCircuitGasRequirements(
        normalRate: Double,
        decoRate: Double,
        stressRate: Double = 0.0,
        stress: StressWindow? = null,
    ): Map<Cylinder, Double> {
        val requiredLitersByCylinder = mutableMapOf<Cylinder, Double>()
        forEach { segment ->
            if (segment.breathingMode is BreathingMode.OpenCircuit) {
                val liters = segment.gasRequirement(normalRate, decoRate, stressRate, stress)
                requiredLitersByCylinder.updateOrInsert(segment.cylinder, liters, Double::plus)
            }
        }
        return requiredLitersByCylinder
    }

    /**
     * Calculates the required gas volume for this segment. The segment may use a combination of the
     * normal/deco rate and the stress rate if [stress] is provided and overlaps with the segment.
     */
    private fun DiveSegment.gasRequirement(
        normalRate: Double,
        decoRate: Double,
        stressRate: Double,
        stress: StressWindow?,
    ): Double {
        val baseRate = if (isDecompressionStop) {
            decoRate
        } else {
            normalRate
        }
        if (stress == null) return duration * baseRate * averagePressure

        val stressMinutes = (stress.durationMinutes - (start - stress.originMinute)).coerceIn(0, duration)
        return when(stressMinutes) {
            0 -> duration * baseRate * averagePressure
            duration -> duration * stressRate * averagePressure
            else -> {
                val boundaryPressure = startPressure - pressureRate * stressMinutes
                // Stress usage for initial part of the segment
                stressMinutes * stressRate * ((startPressure + boundaryPressure) / 2.0) +
                    // Base usage for the rest of the segment
                    (duration - stressMinutes) * baseRate * ((boundaryPressure + endPressure) / 2.0)
            }
        }
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
