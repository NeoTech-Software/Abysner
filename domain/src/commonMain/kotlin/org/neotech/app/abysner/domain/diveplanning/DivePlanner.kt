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

package org.neotech.app.abysner.domain.diveplanning

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import org.neotech.app.abysner.domain.core.model.BreathingMode
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.DiveMode
import org.neotech.app.abysner.domain.decompression.DecompressionPlanner
import org.neotech.app.abysner.domain.decompression.algorithm.DecompressionModel
import org.neotech.app.abysner.domain.decompression.algorithm.buhlmann.Buhlmann
import org.neotech.app.abysner.domain.diveplanning.model.DivePlan
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.gasplanning.OxygenToxicityCalculator
import kotlin.time.Duration

/**
 * Builds on top of the [DecompressionPlanner] and [DecompressionModel] and adds algorithms to
 * turn the user provided dive profile into segments usable by the decompression planner. It also
 * adds multi-level dive handling.
 */
class DivePlanner(
    var configuration: Configuration = Configuration()
) {

    private var decompressionModelSnapshot: DecompressionModel.Snapshot? = null

    fun addDive(
        plan: List<DiveProfileSection>,
        cylinders: List<Cylinder>,
        diveMode: DiveMode = DiveMode.OPEN_CIRCUIT,
        bailout: Boolean = false,
    ): DivePlan {

        require(!bailout || diveMode == DiveMode.CLOSED_CIRCUIT) {
            "Bailout is only applicable to closed-circuit dives"
        }

        val breathingMode = diveMode.breathingMode(configuration.ccrHighSetpoint)
        val descentBreathingMode = diveMode.breathingMode(configuration.ccrLowSetpoint)
        val isCcr = breathingMode is BreathingMode.ClosedCircuit

        if(plan.isEmpty()) {
            return DivePlan(
                persistentListOf(),
                persistentMapOf(),
                cylinders.toPersistentList(),
                configuration,
                0.0,
                0.0
            )
        }

        val decompressionPlanner = DecompressionPlanner(
            environment = configuration.environment,
            maxPpO2 = configuration.maxPPO2Deco,
            maxEquivalentNarcoticDepth = configuration.maxEND,
            ascentRate = configuration.maxAscentRate,
            decoStepSize = configuration.decoStepSize,
            lastDecoStopDepth = configuration.lastDecoStopDepth,
            forceMinimalDecoStopTime = configuration.forceMinimalDecoStopTime,
            gasSwitchTime = configuration.gasSwitchTime,
            model = createDecompressionModel()
        )
        decompressionModelSnapshot?.let {
            decompressionPlanner.setDecompressionModelSnapshot(it)
        }

        decompressionPlanner.setDecoGasses(cylinders)

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
                        decompressionPlanner.calculateDecompression(toDepth = it.depth, breathingMode = breathingMode)
                        decompressionPlanner.setDecoGasses(gasses)
                    } else {
                        decompressionPlanner.calculateDecompression(toDepth = it.depth, breathingMode = breathingMode)
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
                            breathingMode,
                        )
                    }
                    decompressionPlanner.assignTts(breathingMode, isCcr)

                } else {
                    // Descending
                    val timeToChange = configuration.travelTime(difference)

                    val timeLeftAtPlannedDepth = it.duration - timeToChange

                    decompressionPlanner.addDepthChange(
                        currentDepth,
                        it.depth.toDouble(),
                        it.cylinder,
                        timeToChange,
                        descentBreathingMode,
                    )

                    // TODO allow 0? And just not add the flat?
                    if (timeLeftAtPlannedDepth < 0) {
                        throw NotEnoughTimeToReachDepth()
                    } else if(timeLeftAtPlannedDepth > 0) {
                        decompressionPlanner.addFlat(
                            it.depth.toDouble(),
                            it.cylinder,
                            timeLeftAtPlannedDepth,
                            breathingMode,
                        )
                    }
                    decompressionPlanner.assignTts(breathingMode, isCcr)
                }
                currentDepth = it.depth.toDouble()
            } else {
                decompressionPlanner.addFlat(
                    it.depth.toDouble(),
                    it.cylinder,
                    it.duration,
                    breathingMode,
                )
                decompressionPlanner.assignTts(breathingMode, isCcr)
                currentDepth = it.depth.toDouble()
            }
        }

        // Bring diver to surface. For bailout, the final ascent is OC. The bailout detection in
        // calculateDecompression will select the best OC (bailout) gas.
        val ascentBreathingMode = if (bailout && isCcr) {
            BreathingMode.OpenCircuit
        } else {
            breathingMode
        }
        decompressionPlanner.calculateDecompression(toDepth = 0, breathingMode = ascentBreathingMode)

        val segments = decompressionPlanner.getSegments()

        decompressionModelSnapshot = decompressionPlanner.getDecompressionModelSnapshot()
        return DivePlan(
            segments = segments,
            alternativeAccents = decompressionPlanner.getAlternativeAccents(),
            cylinders = cylinders.toPersistentList(),
            configuration = configuration,
            totalCns = OxygenToxicityCalculator().calculateCns(segments, configuration.environment),
            totalOtu = OxygenToxicityCalculator().calculateOtu(segments, configuration.environment)
        )
    }

    fun addSurfaceInterval(duration: Duration) {
        val model = createDecompressionModel()

        model.reset(decompressionModelSnapshot ?: throw PlanningException("Unable to add surface interval, plan a dive first."))

        model.addSurfaceInterval(duration.inWholeMinutes.toInt())

        decompressionModelSnapshot = model.snapshot()
    }

    private fun createDecompressionModel(): DecompressionModel {
        val version = when(configuration.algorithm) {
            Configuration.Algorithm.BUHLMANN_ZH16C -> Buhlmann.Version.ZH16C
            Configuration.Algorithm.BUHLMANN_ZH16B -> Buhlmann.Version.ZH16B
            Configuration.Algorithm.BUHLMANN_ZH16A -> Buhlmann.Version.ZH16A
        }

        return Buhlmann(
            version = version,
            environment = configuration.environment,
            gfLow = configuration.gfLow,
            gfHigh = configuration.gfHigh,
        )
    }

    private fun DiveMode.breathingMode(setpoint: Double): BreathingMode = when (this) {
        DiveMode.OPEN_CIRCUIT -> BreathingMode.OpenCircuit
        DiveMode.CLOSED_CIRCUIT -> BreathingMode.ClosedCircuit(setpoint)
    }

    private fun DecompressionPlanner.assignTts(breathingMode: BreathingMode, isCcr: Boolean) {
        val lastSegment = getSegments().last()
        lastSegment.ttsAfter = calculateTimeToSurface(breathingMode)
        if (isCcr) {
            // OC bailout TTS: time to surface if the diver comes off the loop now. This second call
            // overwrites the alternative ascent stored for this timestamp, which is intentional:
            // the OC bailout ascent is the one that matters for emergency gas planning.
            lastSegment.ttsBailoutAfter = calculateTimeToSurface(BreathingMode.OpenCircuit)
        }
    }

    open class PlanningException(message: String? = null) : Exception(message)

    class NotEnoughTimeToReachDepth : PlanningException()

    class NotEnoughTimeToDecompress : PlanningException()
}
