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

package org.neotech.app.abysner.domain.diveplanning

import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.decompression.DecompressionPlanner
import org.neotech.app.abysner.domain.gasplanning.OxygenToxicityCalculator
import org.neotech.app.abysner.domain.decompression.algorithm.buhlmann.Buhlmann
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.model.DivePlan
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

/**
 * Builds on top of the [DecompressionPlanner] and [DecompressionModel] and adds algorithms to
 * turn the user provided dive profile into segments usable by the decompression planner. It also
 * adds multi-level dive handling.
 */
class DivePlanner {

    var configuration = Configuration()

    fun getDecoPlan(
        plan: List<DiveProfileSection>,
        decoGases: List<Cylinder>,
    ): DivePlan {

        if(plan.isEmpty()) {
            return DivePlan(
                emptyList(),
                emptyMap(),
                decoGases,
                configuration,
                0.0,
                0.0
            )
        }

        val version = when(configuration.algorithm) {
            Configuration.Algorithm.BUHLMANN_ZH16C -> Buhlmann.Version.ZH16C
            Configuration.Algorithm.BUHLMANN_ZH16B -> Buhlmann.Version.ZH16B
            Configuration.Algorithm.BUHLMANN_ZH16A -> Buhlmann.Version.ZH16A
        }

        val decompressionPlanner = DecompressionPlanner(
            environment = configuration.environment,
            maxPpO2 = configuration.maxPPO2Deco,
            maxEquivalentNarcoticDepth = configuration.maxEND,
            ascentRate = configuration.maxAscentRate,
            decoStepSize = configuration.decoStepSize,
            lastDecoStopDepth = configuration.lastDecoStopDepth,
            forceMinimalDecoStopTime = configuration.forceMinimalDecoStopTime,
            model = Buhlmann(
                version = version,
                environment = configuration.environment,
                gfLow = configuration.gfLow,
                gfHigh = configuration.gfHigh,
            )
        )

        decoGases.forEach {
            decompressionPlanner.addCylinder(it)
        }

        var currentDepth = 0.0
        plan.forEach {
            if (it.depth.toDouble() != currentDepth) {

                val difference = currentDepth - it.depth

                if(difference > 0) {
                    // Ascending

                    val runtime = decompressionPlanner.runtime

                    // Ascending (calculate decompression)
                    if(!configuration.useDecoGasBetweenSections) {
                        // Store current deco gasses
                        val gasses = decompressionPlanner.getDecoGasses()

                        // Only allow the listed bottom gas to get to this segment
                        // This is similar to what MultiDeco does
                        decompressionPlanner.setDecoGasses(listOf(it.cylinder))
                        decompressionPlanner.calculateDecompression(toDepth = it.depth)
                        decompressionPlanner.setDecoGasses(gasses)
                    } else {
                        decompressionPlanner.calculateDecompression(toDepth = it.depth)
                    }

                    val timeSpend = decompressionPlanner.runtime - runtime
                    val timeLeftAtPlannedDepth = it.duration - timeSpend

                    // TODO allow 0? And just not add the flat?
                    if (timeLeftAtPlannedDepth < 0) {
                        throw NotEnoughTimeToDecompress()
                    } else if(timeLeftAtPlannedDepth > 0) {
                        decompressionPlanner.addFlat(
                            it.depth.toDouble(),
                            it.cylinder,
                            timeLeftAtPlannedDepth,
                            isDecompression = false,
                        )
                    }
                    decompressionPlanner.getSegments().last().ttsAfter = decompressionPlanner.calculateTimeToSurface()

                } else {
                    // Descending
                    val timeToChange = configuration.travelTime(difference)

                    val timeLeftAtPlannedDepth = it.duration - timeToChange

                    decompressionPlanner.addDepthChangePerMinute(
                        currentDepth,
                        it.depth.toDouble(),
                        it.cylinder,
                        timeToChange,
                        isDecompression = false,
                    )

                    // TODO allow 0? And just not add the flat?
                    if (timeLeftAtPlannedDepth < 0) {
                        throw NotEnoughTimeToReachDepth()
                    } else if(timeLeftAtPlannedDepth > 0) {
                        decompressionPlanner.addFlat(
                            it.depth.toDouble(),
                            it.cylinder,
                            timeLeftAtPlannedDepth,
                            isDecompression = false,
                        )
                    }
                    decompressionPlanner.getSegments().last().ttsAfter = decompressionPlanner.calculateTimeToSurface()
                }
                currentDepth = it.depth.toDouble()
            } else {
                decompressionPlanner.addFlat(
                    it.depth.toDouble(),
                    it.cylinder,
                    it.duration,
                    isDecompression = false,
                )
                decompressionPlanner.getSegments().last().ttsAfter = decompressionPlanner.calculateTimeToSurface()
                currentDepth = it.depth.toDouble()
            }
        }

        // Bring diver to surface
        decompressionPlanner.calculateDecompression(toDepth = 0)

        val segments = decompressionPlanner.getSegments()
        return DivePlan(
            segments = segments,
            alternativeAccents = decompressionPlanner.getAlternativeAccents(),
            decoGasses = decoGases,
            configuration = configuration,
            totalCns = OxygenToxicityCalculator().calculateCns(segments, configuration.environment),
            totalOtu = OxygenToxicityCalculator().calculateOtu(segments, configuration.environment)
        )
    }

    open class PlanningException(message: String? = null) : Exception(message)

    class NotEnoughTimeToReachDepth : PlanningException()

    class NotEnoughTimeToDecompress : PlanningException()
}
