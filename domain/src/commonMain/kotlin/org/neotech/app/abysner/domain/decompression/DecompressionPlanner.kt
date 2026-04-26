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

package org.neotech.app.abysner.domain.decompression

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import org.neotech.app.abysner.domain.core.model.BreathingMode
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.SetpointSwitch
import org.neotech.app.abysner.domain.decompression.model.DiveSegment
import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.findBetterGasOrFallback
import org.neotech.app.abysner.domain.core.physics.ambientPressureToMeters
import org.neotech.app.abysner.domain.core.physics.metersToAmbientPressure
import org.neotech.app.abysner.domain.decompression.algorithm.DecompressionModel
import org.neotech.app.abysner.domain.diveplanning.DivePlanner
import org.neotech.app.abysner.domain.decompression.model.subList
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * The decompression planner makes use of a decompression model and adds algorithms to figure out
 * the exact stop depths, stop times, gas switch depths and more. This class essentially implements
 * diving standards for decompression based on the ceilings returned by the given decompression
 * model.
 */
class DecompressionPlanner(
    val model: DecompressionModel,
    val environment: Environment,
    val maxPpO2: Double,
    val maxEquivalentNarcoticDepth: Double,
    val ascentRate: Double,
    val decoStepSize: Int,
    val lastDecoStopDepth: Int,
    val forceMinimalDecoStopTime: Boolean,
    val gasSwitchTime: Int,
) {

    private var isCalculatingTts = false

    var runtime = 0
        private set
    private val decoGases = mutableListOf<Cylinder>()
    private val segments = mutableListOf<DiveSegment>()
    private val alternativeAccents: MutableMap<Int, List<DiveSegment>> = mutableMapOf()

    /**
     * Returns a map with alternative final accents that have been calculated for this dive. Where
     * the key represents the minute in the dive the accent started and the value is a list of
     * alternative dive segments for this accent.
     *
     * Note: These alternative accents are not calculated minute, but as an optimization
     * calculated per section.
     */
    fun getAlternativeAccents(): ImmutableMap<Int, ImmutableList<DiveSegment>> = alternativeAccents.mapValues { it.value.toPersistentList() }.toPersistentMap()

    fun getDecoGases(): List<Cylinder> = decoGases.toList()

    fun setDecoGases(gases: List<Cylinder>) {
        this.decoGases.clear()
        this.decoGases.addAll(gases)
    }


    fun addFlat(depth: Double, gas: Cylinder, timeInMinutes: Int, breathingMode: BreathingMode) {
        return addDepthChangeInternal(depth, depth, gas, timeInMinutes, DiveSegment.Type.FLAT, breathingMode)
    }

    private fun addDecoStop(depth: Double, gas: Cylinder, timeInMinutes: Int, breathingMode: BreathingMode) {
        return addDepthChangeInternal(depth, depth, gas, timeInMinutes, DiveSegment.Type.DECO_STOP, breathingMode)
    }

    private fun addGasSwitch(depth: Double, gas: Cylinder, timeInMinutes: Int, breathingMode: BreathingMode) {
        // A 0-duration segment is still emitted when gasSwitchTime = 0 so the instruction table
        // can consistently show a gas switch row regardless of the configured switch time.
        return addDepthChangeInternal(depth, depth, gas, timeInMinutes, DiveSegment.Type.GAS_SWITCH, breathingMode)
    }

    fun addDepthChange(startDepth: Double, endDepth: Double, gas: Cylinder, timeInMinutes: Int, breathingMode: BreathingMode, setpointSwitch: SetpointSwitch? = null) {
        require(startDepth != endDepth) { "Use addFlat() for flat segments, startDepth and endDepth must differ." }
        // Setpoint switches only apply to CCR, ignore for OC to prevent accidental breathing mode switches.
        val effectiveSetpointSwitch = if (breathingMode is BreathingMode.ClosedCircuit) {
            setpointSwitch
        } else {
            null
        }
        val type = if (startDepth < endDepth) DiveSegment.Type.DECENT else DiveSegment.Type.ASCENT
        return addDepthChangeInternal(startDepth, endDepth, gas, timeInMinutes, type, breathingMode, effectiveSetpointSwitch)
    }

    private fun addDepthChangeInternal(startDepth: Double, endDepth: Double, gas: Cylinder, timeInMinutes: Int, type: DiveSegment.Type, breathingMode: BreathingMode, setpointSwitch: SetpointSwitch? = null) {
        if(timeInMinutes > 0 && calculateTissueChangesPerMinute && !isCalculatingTts) {
            val diff = startDepth - endDepth
            var currentBreathingMode = breathingMode
            repeat(timeInMinutes) { minute ->
                val startDepthForThisMinute = startDepth - (diff * ((minute) / timeInMinutes.toDouble()))
                val endDepthForThisMinute = startDepth - (diff * ((minute + 1) / timeInMinutes.toDouble()))
                currentBreathingMode = applyDepthChange(startDepthForThisMinute, endDepthForThisMinute, gas, 1, type, currentBreathingMode, setpointSwitch)
            }
        } else {
            applyDepthChange(startDepth, endDepth, gas, timeInMinutes, type, breathingMode, setpointSwitch)
        }
    }

    /**
     * Finalizes a pressure change, applies the tissue loading, decompression ceiling changes and
     * other effects of a pressure change to the model, then adds a segment to the list of segments.
     * This also takes care of sub-minute precision setpoint switches, and returns the effective
     * breathing mode at the end of this pressure change.
     */
    private fun applyDepthChange(startDepth: Double, endDepth: Double, gas: Cylinder, timeInMinutes: Int, type: DiveSegment.Type, breathingMode: BreathingMode, setpointSwitch: SetpointSwitch? = null): BreathingMode {

        val startPressure = metersToAmbientPressure(startDepth, environment)
        val endPressure = metersToAmbientPressure(endDepth, environment)

        // Check if the setpoint switch depth is crossed during this segment
        val switchDepth = setpointSwitch?.depth?.toDouble()
        val crossesSwitchDepth = switchDepth != null &&
            (startDepth < switchDepth) != (endDepth < switchDepth)

        val breathingModeAtEnd: BreathingMode.ClosedCircuit?
        if (timeInMinutes > 0 && crossesSwitchDepth) {
            // Sub-minute precision: split the model call at the exact switch depth
            // TODO: See [DiveSegment.breathingModeAtEnd]
            val timeFractionBeforeSwitch = abs(setpointSwitch.depth - startDepth) / abs(endDepth - startDepth)

            val pressureSwitch = metersToAmbientPressure(setpointSwitch.depth.toDouble(), environment)

            if (timeFractionBeforeSwitch > 0.0) {
                model.addPressureChange(startPressure, pressureSwitch, gas.gas, timeFractionBeforeSwitch * timeInMinutes, breathingMode.ccrSetpointOrNull)
            }
            if (timeFractionBeforeSwitch < 1.0) {
                model.addPressureChange(pressureSwitch, endPressure, gas.gas, (1.0 - timeFractionBeforeSwitch) * timeInMinutes, setpointSwitch.toBreathingMode.ccrSetpointOrNull)
            }
            breathingModeAtEnd = setpointSwitch.toBreathingMode
        } else {
            if (timeInMinutes > 0) {
                model.addPressureChange(startPressure, endPressure, gas.gas, timeInMinutes, breathingMode.ccrSetpointOrNull)
            }
            breathingModeAtEnd = null
        }

        val ceiling = ambientPressureToMeters(model.getCeiling().value, environment)

        segments.add(
            DiveSegment(
                start = runtime,
                duration = timeInMinutes,
                startDepth = startDepth,
                endDepth = endDepth,
                cylinder = gas,
                type = type,
                gfCeilingAtEnd = ceiling,
                breathingMode = breathingMode,
                breathingModeAtEnd = breathingModeAtEnd,
            )
        )
        this.runtime += timeInMinutes
        return breathingModeAtEnd ?: breathingMode
    }

    fun calculateTimeToSurface(breathingMode: BreathingMode, setpointSwitch: SetpointSwitch? = null): Int {
        // TODO this is not an ideal method to acquire this information, as we generate a lot
        //      of info we don't need.

        // This call is very expensive as it could be recursive
        if(isCalculatingTts) {
            return -1
        }
        if(segments.isEmpty()) {
            return 0
        }
        isCalculatingTts = true
        var alternativeAscentSegments: List<DiveSegment> = emptyList()
        val start = runtime
        val result = resetAfter {
            calculateDecompression(toDepth = 0, breathingMode = breathingMode, setpointSwitch = setpointSwitch).sumOf { it.duration }.also {
                alternativeAscentSegments = segments.subList(start).toList()
            }
        }
        alternativeAccents[start] = alternativeAscentSegments
        isCalculatingTts = false
        return result
    }

    /**
     * Move diver from [fromDepth] to the next ceiling depth [toDepth]. During which a gas change
     * may occur as the diver reaches depths at which a different gas may be better to breath.
     * Gas switching is skipped when [breathingMode] is [BreathingMode.ClosedCircuit], since the
     * diver stays on the loop.
     */
    private fun addDecoDepthChange(fromDepth: Double, toDepth: Double, maxppO2: Double, maxEND: Double, fromGas: Cylinder, ascentRateInMetersPerMinute: Double, breathingMode: BreathingMode, setpointSwitch: SetpointSwitch? = null): Cylinder {
        val isCcr = breathingMode is BreathingMode.ClosedCircuit
        var gas = fromGas
        var currentDepth = fromDepth
        // After crossing the switch depth, use the switched-to breathing mode for all subsequent segments.
        var effectiveBreathingMode = breathingMode

        while (currentDepth > toDepth) {
            if (!isCcr) {
                // Check if there is a better gas to breath at the current depth
                val betterDecoGas = decoGases.findBetterGasOrFallback(currentCylinder = gas, ambientPressure = metersToAmbientPressure(currentDepth, environment).value, maxPPO2 = maxppO2, maxEquivalentNarcoticAmbientPressure = metersToAmbientPressure(maxEND, environment).value)
                // Only start using the better gas when we reach a deco increment point

                if (betterDecoGas != null && betterDecoGas.gas != gas.gas && currentDepth.toInt() % decoStepSize == 0) {
                    // Gas switch time is spent on the old gas: the diver is still breathing the
                    // previous gas while preparing to switch (grabbing regulator, purging, etc.).
                    // The actual switch to the new gas happens after the gas switch time.
                    addGasSwitch(currentDepth, gas, gasSwitchTime, effectiveBreathingMode)
                    gas = betterDecoGas
                }
            }

            // targetDepth is to toDepth, unless there's a better gas to switch to on the way up,
            // then the target depth may be something between currentDepth and toDepth.
            var targetDepth = toDepth

            if (!isCcr) {
                // Figure out if before we reach the targetDepth anything needs to happen (gas switch)
                // We do this by descending with increments of 1 meter and checking if there is a better
                // gas available at each depth.
                var nextDepth = currentDepth - 1
                var nextDecoGas: Cylinder?
                while(nextDepth >= targetDepth) {
                    nextDecoGas = decoGases.findBetterGasOrFallback(currentCylinder = gas, ambientPressure = metersToAmbientPressure(nextDepth, environment).value, maxPPO2 = maxppO2, maxEquivalentNarcoticAmbientPressure = metersToAmbientPressure(maxEND, environment).value)
                    if (nextDecoGas != null && nextDecoGas.gas != gas.gas && nextDepth.toInt() % decoStepSize == 0) {
                        targetDepth = nextDepth
                        break
                    }
                    nextDepth--
                }
            }

            // At this point we are either at the target depth, or our target depth has changed due to a gas switch that needs to happen.

            // Take the diver to this new target depth, by calculating how much time it will take to
            // get there and then recalculate the current tissue loading.

            // TODO some planners seem to round down (MultiDeco) instead of the more logical ceil
            //      (Subsurface) here? Why is that?
            // Calculate how much time it will take in minutes, rounding up
            val depthChange = abs(currentDepth - targetDepth)
            val duration = max(1, ceil(depthChange / ascentRateInMetersPerMinute).toInt())

            addDepthChange(currentDepth, targetDepth, gas, duration, effectiveBreathingMode, setpointSwitch)

            // If the switch depth was crossed during this segment, update the effective mode
            if (setpointSwitch != null && currentDepth > setpointSwitch.depth && targetDepth <= setpointSwitch.depth) {
                effectiveBreathingMode = setpointSwitch.toBreathingMode
            }

            currentDepth = targetDepth
        }

        if (!isCcr) {
            val betterDecoGas = decoGases.findBetterGasOrFallback(currentCylinder = gas, ambientPressure = metersToAmbientPressure(currentDepth, environment).value, maxPPO2 = maxppO2, maxEquivalentNarcoticAmbientPressure = metersToAmbientPressure(maxEND, environment).value)
            if (betterDecoGas != null && betterDecoGas.gas != gas.gas && currentDepth.toInt() % decoStepSize == 0) {
                // Gas switch time on the old gas before switching
                addGasSwitch(currentDepth, gas, gasSwitchTime, effectiveBreathingMode)
                gas = betterDecoGas
            }
        }

        return gas
    }

    fun calculateDecompression(
        toDepth: Int,
        breathingMode: BreathingMode,
        setpointSwitch: SetpointSwitch? = null,
    ): List<DiveSegment> {
        // Setpoint switches only apply to CCR, ignore for OC to prevent accidental breathing mode switches
        val effectiveSetpointSwitch = if (breathingMode is BreathingMode.ClosedCircuit) {
            setpointSwitch
        } else {
            null
        }

        val isCcr = breathingMode is BreathingMode.ClosedCircuit
        var gas: Cylinder
        val fromDepth: Double
        if (this.segments.isEmpty()) {
            // TODO
            //   Instead of throwing an exception (if there are no segments) the current depth could
            //   be considered 0 meters, hence a simple return would be fine, as no decompression is
            //   required anyways when going from 0 meters to 0 meters?
            throw IllegalStateException("Unable to decompress, current depth is 0, have any dive stages been registered?")
        } else {
            fromDepth = this.segments[this.segments.size-1].endDepth
            gas = this.segments[this.segments.size-1].cylinder
        }
        val segmentSizeStart = this.segments.size

        // For bailout (transitioning from CCR to OC), select the best available bailout cylinder
        // and emit a GAS_SWITCH with duration 1 (a minimal problem-solving time for coming off the
        // loop and switching to the bailout regulator).
        // TODO: Should this be configurable, or even tied to the existing Configuration.gasSwitchTime?
        val isBailout = !isCcr && segments.last().breathingMode is BreathingMode.ClosedCircuit
        if (isBailout) {
            val bestBailoutGas = decoGases.findBetterGasOrFallback(
                currentCylinder = null,
                ambientPressure = metersToAmbientPressure(fromDepth, environment).value,
                maxPPO2 = maxPpO2,
                maxEquivalentNarcoticAmbientPressure = metersToAmbientPressure(maxEquivalentNarcoticDepth, environment).value
            )
            if (bestBailoutGas != null) {
                addGasSwitch(fromDepth, gas, 1, breathingMode)
                gas = bestBailoutGas
            }
        }

        if(toDepth > fromDepth) {
            throw IllegalArgumentException("Cannot calculate decompression as the target depth ($toDepth meter) is deeper then the current depth ($fromDepth meter). Add an descending depth change first!")
        }

        // Get the current ceiling:
        var ceiling = findFirstDecoCeiling(fromDepth, decoStepSize, lastDecoStopDepth, maxPpO2, maxEquivalentNarcoticDepth, gas, ascentRate, breathingMode)

        // Check if there is a better gas to switch to at the current depth before
        // ascending. Gas switching is skipped in CCR mode (diver stays on the loop).
        if (!isCcr) {
            val betterGas = decoGases.findBetterGasOrFallback(currentCylinder = gas, ambientPressure = metersToAmbientPressure(fromDepth, environment).value, maxPPO2 = maxPpO2, maxEquivalentNarcoticAmbientPressure = metersToAmbientPressure(maxEquivalentNarcoticDepth, environment).value)
            if (betterGas != null && betterGas.gas != gas.gas) {
                // Gas switch time on the old gas before switching
                addGasSwitch(fromDepth, gas, gasSwitchTime, breathingMode)
                gas = betterGas
            }
        }

        // Don't allow ceiling below start depth
        // TODO get this depth from the previous section endDepth?
        if(ceiling > fromDepth) {
            // We have to stay at this depth first, do not change depths.
            ceiling = fromDepth.toInt()
        } else {
            // Move the diver to the first ceiling (this may already be the surface)

            gas = addDecoDepthChange(
                fromDepth,
                max(ceiling.toDouble(), toDepth.toDouble()),
                maxPpO2,
                maxEquivalentNarcoticDepth,
                gas,
                ascentRate,
                breathingMode,
                effectiveSetpointSwitch
            )
        }

        // After the initial ascent, determine if the switch depth was already crossed.
        var effectiveBreathingMode = if (effectiveSetpointSwitch != null && fromDepth > effectiveSetpointSwitch.depth &&
            max(ceiling.toDouble(), toDepth.toDouble()) <= effectiveSetpointSwitch.depth) {
            effectiveSetpointSwitch.toBreathingMode
        } else {
            breathingMode
        }

        // Keep making deco stops until the deco ceiling is above the surface
        while (ceiling > 0) {

            val currentDepth = ceiling.toDouble()
            if(currentDepth <= toDepth) {
                break
            }

            // Update effective breathing mode if we're now above the switch depth
            if (effectiveSetpointSwitch != null && currentDepth <= effectiveSetpointSwitch.depth) {
                effectiveBreathingMode = effectiveSetpointSwitch.toBreathingMode
            }

            // Check the new ceiling
            ceiling = max(
                this.getDecoCeiling(decoStepSize, lastDecoStopDepth),
                toDepth
            )

            // Don't allow ceiling below start depth
            // TODO get this depth from the previous section endDepth?
            if(ceiling > fromDepth) {
                ceiling = fromDepth.toInt()
            }

            // If while moving the diver up to the previous deco ceiling the ceiling itself moved
            // far enough to reach the next deco ceiling, a stop at the previous ceiling would be
            // pointless. Because apparently while traveling the diver has off-gassed enough to no
            // longer need the stop. So instead directly move further up to this next deco ceiling.
            if(ceiling >= currentDepth || forceMinimalDecoStopTime) {

                // If minimal deco step times are required, force stops to also be symmetric!
                val nextDecoDepth = if(forceMinimalDecoStopTime) {
                    max(
                        findNextDecoDepth(currentDepth.toInt(), decoStepSize, lastDecoStopDepth),
                        toDepth
                    )
                } else {
                    max(findNextDecoDepth(ceiling, decoStepSize, lastDecoStopDepth), toDepth)
                }

                // Stop at the current deco depth until we can safely ascent to the next deco depth.
                var stopTime = 0
                while (ceiling > nextDecoDepth && ceiling > toDepth || (forceMinimalDecoStopTime && stopTime < 1)) {
                    // Add 1 minute of decompression and test the ceiling again, until the ceiling is higher.
                    this.addDecoStop(currentDepth, gas, 1, effectiveBreathingMode)
                    stopTime++

                    // TODO: Should we calculate the gf based on the current stop depth, or the next stop depth we want to reach?
                    // Because getDecoCeiling may return a deeper ceiling to honor the [decoStepSize]
                    // the diver could potentially already move up to [toDepth], however to allow
                    // for some additional conservatism, we only move the diver to toDepth if the nearest
                    // shallower deco ceiling is cleared as well.
                    ceiling = max(
                        this.getDecoCeiling(decoStepSize, lastDecoStopDepth),
                        toDepth
                    )
                    if(ceiling < nextDecoDepth && forceMinimalDecoStopTime) {
                        // ceiling is skipping a deco step, force the ceiling to be deeper to avoid skipping
                        ceiling = nextDecoDepth
                    }

                    if (ceiling > nextDecoDepth && ceiling > toDepth) {
                        if (isCeilingClearedDuringAscent(currentDepth, nextDecoDepth, gas, effectiveBreathingMode, effectiveSetpointSwitch)) {
                            ceiling = nextDecoDepth
                            break
                        }
                    }

                    if(stopTime > 1000) {
                        // We are probably in a loop where we cannot off-gas enough within the set gradient factors
                        // to reach the next deco stop. Likely the last deco stop is too shallow, the deco step size is too
                        // big or the gradient factors are extremely conservative.
                        throw DivePlanner.PlanningException("Unable to reach next deco stop within the set gradient factors. Likely the last deco stop is too shallow, the deco step size is too big or the gradient factors are extremely conservative.")
                    }
                }
            }
            gas = this.addDecoDepthChange(currentDepth, ceiling.toDouble(), maxPpO2, maxEquivalentNarcoticDepth, gas, ascentRate, effectiveBreathingMode, effectiveSetpointSwitch)
        }

        return if(segments.size == segmentSizeStart) {
            emptyList()
        } else {
            this.segments.subList(segmentSizeStart, segments.size)
        }
    }


    private fun findFirstDecoCeiling(
        fromDepth: Double,
        decoStepSize: Int,
        lastDecoStopDepth: Int,
        maxPpO2: Double,
        maxEquivalentNarcoticDepth: Double,
        gas: Cylinder,
        ascentRate: Double,
        breathingMode: BreathingMode,
    ): Int {

        var ceiling: Int = getDecoCeiling(decoStepSize, lastDecoStopDepth)
        var nextCeiling = ceiling
        do {

            ceiling = nextCeiling

            nextCeiling = resetAfter {

                if (ceiling == 0) {
                    return@resetAfter 0
                }

                // Move diver up
                addDecoDepthChange(
                    fromDepth,
                    ceiling.toDouble(),
                    maxPpO2,
                    maxEquivalentNarcoticDepth,
                    gas,
                    ascentRate,
                    breathingMode
                )

                // Check new ceiling (could already be higher)
                getDecoCeiling(
                    decoStepSize,
                    lastDecoStopDepth
                )
            }
        } while(ceiling > nextCeiling)

        val nextDecoDepth = findNextDecoDepth(nextCeiling, decoStepSize, lastDecoStopDepth)
        if (nextCeiling > 0 && nextDecoDepth >= 0) {
            if (isCeilingClearedDuringAscent(fromDepth, nextDecoDepth, gas, breathingMode)) {
                return nextDecoDepth
            }
        }

        return nextCeiling
    }

    /**
     * Abysner works in whole minutes (as divers plan in minutes), which means a true ceiling of
     * 3.1 meter would keep the diver at 6 meter for a full extra minute, even though only a few
     * seconds of off-gassing might be needed to clear that ceiling (to 3 meters). This method
     * avoids that minute penalty by simulating the ascent from [fromDepth] to [targetDecoDepth], if
     * the ceiling clears during travel, the stop can be skipped. The model state is rolled back
     * after the simulation.
     */
    private fun isCeilingClearedDuringAscent(
        fromDepth: Double,
        targetDecoDepth: Int,
        gas: Cylinder,
        breathingMode: BreathingMode,
        setpointSwitch: SetpointSwitch? = null,
    ): Boolean {
        if (targetDecoDepth < 0) return false
        val ceilingAfterAscent = resetAfter {
            addDecoDepthChange(
                fromDepth,
                targetDecoDepth.toDouble().coerceAtLeast(0.0),
                maxPpO2,
                maxEquivalentNarcoticDepth,
                gas,
                ascentRate,
                breathingMode,
                setpointSwitch
            )
            getDecoCeiling(decoStepSize, lastDecoStopDepth)
        }
        return ceilingAfterAscent <= targetDecoDepth
    }

    private fun getDecoCeiling(decoStepSize: Int, lastDecoStopDepth: Int): Int {
        // Ceil to the next whole meter (or foot) so the ceiling is never shallower than what the
        // model reports. Using round() here would allow the diver up to 0.5m shallower than the
        // true ceiling (e.g. a raw ceiling of 3.2m would round to 3m, violating the ceiling).
        var ceiling = ceil(ambientPressureToMeters(model.getCeiling().value, environment)).toInt()
        // Snap up to the nearest deco grid point (e.g. 3m or 10ft increments). The ceiling must
        // never be shallower than the model ceiling, so we only move deeper.
        while (ceiling % decoStepSize != 0) {
            ceiling += 1
        }
        if(ceiling > 0 && ceiling in 1..lastDecoStopDepth) {
            ceiling = lastDecoStopDepth
        }
        return ceiling
    }

    private fun findNextDecoDepth(currentDepth: Int, decoStepSize: Int, lastDecoStopDepth: Int): Int {
        val modulo = currentDepth % decoStepSize
        val decoStop = if(modulo != 0) {
            // Current depth not valid deco stop
            currentDepth - (currentDepth % decoStepSize)
        } else {
            // Current depth valid deco stop
            currentDepth - decoStepSize
        }
        return if(decoStop in 1 ..< lastDecoStopDepth) {
            0
        } else {
            decoStop
        }
    }

    private fun <T> resetAfter(block: () -> T): T {
        val savedRuntime = runtime
        val savedSegments = segments.toList()
        val savedAlternativeAscents = alternativeAccents.toMap()
        val result = model.resetAfter {
            block()
        }
        runtime = savedRuntime
        segments.clear()
        segments.addAll(savedSegments)
        alternativeAccents.clear()
        alternativeAccents.putAll(savedAlternativeAscents)
        return result
    }

    fun getSegments(): ImmutableList<DiveSegment> {
        return segments.toPersistentList()
    }
}

private const val calculateTissueChangesPerMinute: Boolean = true
