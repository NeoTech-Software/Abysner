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
import org.neotech.app.abysner.domain.core.model.DiveMode
import org.neotech.app.abysner.domain.core.model.SetpointSwitch
import org.neotech.app.abysner.domain.core.physics.ambientPressureToMeters
import org.neotech.app.abysner.domain.core.physics.metersToAmbientPressure
import org.neotech.app.abysner.domain.core.physics.metersToHydrostaticPressure
import org.neotech.app.abysner.domain.decompression.DecoGrid
import org.neotech.app.abysner.domain.decompression.DecompressionPlanner
import org.neotech.app.abysner.domain.decompression.algorithm.DecompressionModel
import org.neotech.app.abysner.domain.decompression.algorithm.buhlmann.Buhlmann
import org.neotech.app.abysner.domain.diveplanning.model.AssignedCylinder
import org.neotech.app.abysner.domain.diveplanning.model.DivePlan
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.gasplanning.OxygenToxicityCalculator
import org.neotech.app.abysner.domain.utilities.removeFloatingPointNoise
import kotlin.time.Duration

/**
 * Builds on top of the [DecompressionPlanner] and [DecompressionModel] and adds algorithms to
 * turn the user provided dive profile into segments usable by the decompression planner. It also
 * adds multi-level dive handling.
 */
class DivePlanner(
    configuration: Configuration = Configuration()
) {

    var configuration: Configuration = configuration
        private set

    private var model: DecompressionModel = createDecompressionModel()

    /**
     * Updates the configuration. Tissue state accumulated across previous dives is preserved, so
     * repetitive dive planning continues correctly after the change.
     */
    fun updateConfiguration(newConfiguration: Configuration) {
        val snapshot = model.snapshot()
        configuration = newConfiguration
        model = createDecompressionModel()
        model.reset(snapshot)
    }

    fun snapshotTissues(): DecompressionModel.Snapshot = model.snapshot()

    fun restoreTissues(snapshot: DecompressionModel.Snapshot) {
        model.reset(snapshot)
    }

    fun addDive(
        plan: List<DiveProfileSection>,
        cylinders: List<AssignedCylinder>,
        diveMode: DiveMode = DiveMode.OPEN_CIRCUIT,
        bailout: Boolean = false,
    ): DivePlan {

        require(!bailout || diveMode.isCcr) {
            "Bailout is only applicable to closed-circuit dives"
        }

        val breathingMode = diveMode.breathingMode(configuration.ccrHighSetpoint)
        val descentBreathingMode = diveMode.breathingMode(configuration.ccrLowSetpoint)
        val isCcr = breathingMode is BreathingMode.ClosedCircuit

        val decoCylinders = if(!diveMode.isCcr) {
            cylinders.map { it.cylinder }
        } else {
            cylinders.filter { it.isAvailableForBailout }.map { it.cylinder }
        }

        // TTS is collected during planning and merged into the segments at the end via copy().
        val ttsBySegmentIndex = mutableMapOf<Int, TtsValues>()

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

        val decompressionPlanner = createDecompressionPlanner(model, configuration)

        decompressionPlanner.setDecoGases(decoCylinders)

        // In CCR mode, all sections breathe the diluent on the loop regardless of what cylinder
        // each section was created with.
        val diluentCylinder = cylinders.firstOrNull { it.isCcrDiluent }?.cylinder
        val effectivePlan = if (isCcr && diluentCylinder != null) {
            plan.map { it.copy(cylinder = diluentCylinder) }
        } else {
            plan
        }

        var currentPressure = configuration.environment.atmosphericPressure
        effectivePlan.forEach {
            val sectionPressure = metersToAmbientPressure(it.depth.toDouble(), configuration.environment).value
            if (sectionPressure != currentPressure) {

                val pressureDifference = currentPressure - sectionPressure

                if(pressureDifference > 0) {
                    // Ascending

                    val runtime = decompressionPlanner.runtime

                    val ascentSwitch = configuration.ascentSetpointSwitch(breathingMode)

                    // Ascending (calculate decompression)
                    if(!configuration.useDecoGasBetweenSections) {
                        // Store current deco gases
                        val gases = decompressionPlanner.getDecoGases()

                        // Only allow the listed bottom gas to get to this segment
                        // This is similar to what MultiDeco does
                        decompressionPlanner.setDecoGases(listOf(it.cylinder))
                        decompressionPlanner.calculateDecompression(toAmbientPressure = sectionPressure, breathingMode = breathingMode, setpointSwitch = ascentSwitch)
                        decompressionPlanner.setDecoGases(gases)
                    } else {
                        decompressionPlanner.calculateDecompression(toAmbientPressure = sectionPressure, breathingMode = breathingMode, setpointSwitch = ascentSwitch)
                    }

                    val timeSpend = decompressionPlanner.runtime - runtime
                    val timeLeftAtPlannedDepth = it.duration - timeSpend

                    // TODO allow 0? And just not add the flat?
                    if (timeLeftAtPlannedDepth < 0) {
                        throw NotEnoughTimeToDecompress()
                    } else if(timeLeftAtPlannedDepth > 0) {
                        decompressionPlanner.addFlat(
                            sectionPressure,
                            it.cylinder,
                            timeLeftAtPlannedDepth,
                            breathingMode,
                        )
                    }
                    decompressionPlanner.collectTts(breathingMode, isCcr, ttsBySegmentIndex, ascentSwitch)

                } else {
                    // Descending
                    val timeToChange = configuration.travelTime(pressureDifference, configuration.environment)

                    val timeLeftAtPlannedDepth = it.duration - timeToChange

                    val descentSwitch = configuration.descentSetpointSwitch(breathingMode)

                    val ascentSwitchForTts = configuration.ascentSetpointSwitch(breathingMode)

                    decompressionPlanner.addDepthChange(
                        currentPressure,
                        sectionPressure,
                        it.cylinder,
                        timeToChange,
                        descentBreathingMode,
                        setpointSwitch = descentSwitch,
                    )

                    // TODO allow 0? And just not add the flat?
                    if (timeLeftAtPlannedDepth < 0) {
                        throw NotEnoughTimeToReachDepth()
                    } else if(timeLeftAtPlannedDepth > 0) {
                        decompressionPlanner.addFlat(
                            sectionPressure,
                            it.cylinder,
                            timeLeftAtPlannedDepth,
                            breathingMode,
                        )
                    }
                    decompressionPlanner.collectTts(breathingMode, isCcr, ttsBySegmentIndex, ascentSwitchForTts)
                }
                currentPressure = sectionPressure
            } else {
                decompressionPlanner.addFlat(
                    sectionPressure,
                    it.cylinder,
                    it.duration,
                    breathingMode,
                )
                val ascentSwitchForTts = configuration.ascentSetpointSwitch(breathingMode)
                decompressionPlanner.collectTts(breathingMode, isCcr, ttsBySegmentIndex, ascentSwitchForTts)
                currentPressure = sectionPressure
            }
        }

        // Bring diver to surface. For bailout, the final ascent is OC. The bailout detection in
        // calculateDecompression will select the best OC (bailout) gas.
        val ascentBreathingMode = if (bailout && isCcr) {
            BreathingMode.OpenCircuit
        } else {
            breathingMode
        }
        val finalAscentSwitch = if (!bailout) {
            configuration.ascentSetpointSwitch(breathingMode)
        } else {
            null
        }
        decompressionPlanner.calculateDecompression(toAmbientPressure = configuration.environment.atmosphericPressure, breathingMode = ascentBreathingMode, setpointSwitch = finalAscentSwitch)

        val segments = decompressionPlanner.getSegments().mapIndexed { index, segment ->
            ttsBySegmentIndex[index]?.let {
                segment.copy(ttsAfter = it.tts, ttsBailoutAfter = it.bailoutTts)
            } ?: segment
        }.toPersistentList()

        return DivePlan(
            segments = segments,
            alternativeAccents = decompressionPlanner.getAlternativeAccents(),
            cylinders = cylinders.toPersistentList(),
            configuration = configuration,
            // TODO should this be part of DivePlan? Or part of DivePlanSet as a OxygenPlan? Like GasPlan?
            totalCns = OxygenToxicityCalculator.calculateCns(segments, configuration.environment),
            totalOtu = OxygenToxicityCalculator.calculateOtu(segments, configuration.environment)
        )
    }

    fun addSurfaceInterval(duration: Duration) {
        model.addSurfaceInterval(duration.inWholeMinutes.toInt())
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

    private fun createDecompressionPlanner(
        model: DecompressionModel,
        configuration: Configuration,
    ): DecompressionPlanner {
        val environment = configuration.environment
        val grid = DecoGrid(
            surfacePressure = environment.atmosphericPressure,
            decoStepSizePressureDelta = metersToHydrostaticPressure(configuration.decoStepSize.toDouble(), environment).value,
            lastDecoStopAmbientPressure = metersToAmbientPressure(configuration.lastDecoStopDepth.toDouble(), environment).value,
            displayUnitPressureDelta = metersToHydrostaticPressure(1.0, environment).value,
        )
        return DecompressionPlanner(
            model = model,
            grid = grid,
            maxPpO2 = configuration.maxPPO2Deco,
            maxEquivalentNarcoticAmbientPressure = metersToAmbientPressure(configuration.maxEND, environment).value,
            ascentRatePressureDelta = metersToHydrostaticPressure(configuration.maxAscentRate, environment).value,
            forceMinimalDecoStopTime = configuration.forceMinimalDecoStopTime,
            gasSwitchTime = configuration.gasSwitchTime,
            pressureToDepth = { pressure ->
                // This is not strictly required to make the UI work, since the UI already formats
                // to zero decimals or 1 maybe 2 decimals at most. However, it makes the current
                // test assertions easier to read, since these are usually in whole meters, without
                // this noise normalization tolerance handling at assertion call sites would be
                // required, or more precise less readable floating point numbers.
                removeFloatingPointNoise(ambientPressureToMeters(pressure, environment))
            },
        )
    }

    private fun DiveMode.breathingMode(setpoint: Double): BreathingMode = when (this) {
        DiveMode.OPEN_CIRCUIT -> BreathingMode.OpenCircuit
        DiveMode.CLOSED_CIRCUIT -> BreathingMode.ClosedCircuit(setpoint)
    }

    private fun DecompressionPlanner.collectTts(
        breathingMode: BreathingMode,
        isCcr: Boolean,
        destination: MutableMap<Int, TtsValues>,
        ascentSwitch: SetpointSwitch? = null,
    ) {
        val segmentIndex = getSegments().lastIndex
        val tts = calculateTimeToSurface(breathingMode, ascentSwitch)
        // OC bailout TTS: time to surface if the diver comes off the loop now. This second call
        // overwrites the alternative ascent stored for this timestamp, which is intentional:
        // the OC bailout ascent is the one that matters for emergency gas planning.
        val bailoutTts = if (isCcr) {
            calculateTimeToSurface(BreathingMode.OpenCircuit)
        } else {
            null
        }
        destination[segmentIndex] = TtsValues(tts, bailoutTts)
    }

    private data class TtsValues(val tts: Int, val bailoutTts: Int?)

    open class PlanningException(message: String? = null) : Exception(message)

    class NotEnoughTimeToReachDepth : PlanningException()

    class NotEnoughTimeToDecompress : PlanningException()
}


