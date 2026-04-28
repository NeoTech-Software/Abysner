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
import org.neotech.app.abysner.domain.core.model.findBetterGasOrFallback
import org.neotech.app.abysner.domain.core.physics.Pressure
import org.neotech.app.abysner.domain.decompression.algorithm.DecompressionModel
import org.neotech.app.abysner.domain.diveplanning.DivePlanner
import org.neotech.app.abysner.domain.utilities.ceilTolerant
import org.neotech.app.abysner.domain.utilities.equalsTolerant
import kotlin.math.floor
import org.neotech.app.abysner.domain.utilities.greaterThanOrEqualTolerant
import org.neotech.app.abysner.domain.utilities.greaterThanTolerant
import org.neotech.app.abysner.domain.utilities.lessThanOrEqualTolerant
import org.neotech.app.abysner.domain.utilities.lessThanTolerant
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

/**
 * The decompression planner makes use of a decompression model and adds algorithms to figure out
 * the exact stop depths, stop times, gas switches and more. This class essentially implements
 * diving standards for decompression based on the ceilings returned by the given decompression
 * model.
 *
 * All internal calculations work in absolute ambient pressure (bar). The [pressureToDepth]
 * callback converts pressure to display-units (meters or feet) for output.
 */
class DecompressionPlanner(
    val model: DecompressionModel,
    val surfacePressure: Double,
    val maxPpO2: Double,
    val maxEquivalentNarcoticAmbientPressure: Double,
    val ascentRatePressureDelta: Double,
    /**
     * The deco step size in pressure (for example about 0.3 bar for 3m steps) used to determine at
     * which pressures deco stops are required. Must be an exact multiple of
     * [displayUnitPressureDelta].
     */
    val decoStepSizePressureDelta: Double,
    /**
     * The ambient pressure of the last allowed deco stop, must be aligned to the deco step size.
     */
    val lastDecoStopAmbientPressure: Double,
    /**
     * The pressure delta that corresponds to the smallest display unit step (for example about
     * 0.1 bar for steps of 1 meter).
     */
    val displayUnitPressureDelta: Double,
    val forceMinimalDecoStopTime: Boolean,
    val gasSwitchTime: Int,
    val pressureToDepth: (Double) -> Double,
) {

    init {
        // Both decoStepSizePressureDelta and lastDecoStopAmbientPressure must align to
        // displayUnitPressureDelta so that deco stops always land on whole display units (e.g.
        // 3, 6 or 9 meter instead of 2.9, 5.8 or 8.7 meter).
        val decoStepInDisplayUnits = decoStepSizePressureDelta / displayUnitPressureDelta
        require(decoStepInDisplayUnits.equalsTolerant(round(decoStepInDisplayUnits))) {
            "decoStepSizePressureDelta ($decoStepSizePressureDelta) must be an exact multiple of displayUnitPressureDelta ($displayUnitPressureDelta)."
        }
        val lastDecoStopDepthPressure = lastDecoStopAmbientPressure - surfacePressure
        val lastDecoStopInDisplayUnits = lastDecoStopDepthPressure / displayUnitPressureDelta
        require(lastDecoStopInDisplayUnits.equalsTolerant(round(lastDecoStopInDisplayUnits))) {
            "lastDecoStopAmbientPressure ($lastDecoStopAmbientPressure) must fall on a display unit grid point relative to surfacePressure ($surfacePressure)."
        }
    }

    private var isLookahead = false

    /**
     * Current dive runtime in minutes.
     */
    var runtime = 0
        private set

    /**
     * Current dive depth in ambient pressure (bar).
     */
    var ambientPressure: Double = surfacePressure
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
    fun getAlternativeAccents(): ImmutableMap<Int, ImmutableList<DiveSegment>> =
        alternativeAccents.mapValues { it.value.toPersistentList() }.toPersistentMap()

    fun getDecoGases(): List<Cylinder> = decoGases.toList()

    fun setDecoGases(gases: List<Cylinder>) {
        this.decoGases.clear()
        this.decoGases.addAll(gases)
    }

    fun addFlat(ambientPressure: Double, gas: Cylinder, timeInMinutes: Int, breathingMode: BreathingMode) {
        return addDepthChangeInternal(ambientPressure, ambientPressure, gas, timeInMinutes, DiveSegment.Type.FLAT, breathingMode)
    }

    private fun addDecoStop(ambientPressure: Double, gas: Cylinder, timeInMinutes: Int, breathingMode: BreathingMode) {
        return addDepthChangeInternal(ambientPressure, ambientPressure, gas, timeInMinutes, DiveSegment.Type.DECO_STOP, breathingMode)
    }

    private fun addGasSwitch(ambientPressure: Double, gas: Cylinder, timeInMinutes: Int, breathingMode: BreathingMode) {
        // A 0-duration segment is still emitted when gasSwitchTime = 0 so the instruction table
        // can consistently show a gas switch row regardless of the configured switch time.
        return addDepthChangeInternal(ambientPressure, ambientPressure, gas, timeInMinutes, DiveSegment.Type.GAS_SWITCH, breathingMode)
    }

    fun addDepthChange(startAmbientPressure: Double, endAmbientPressure: Double, gas: Cylinder, timeInMinutes: Int, breathingMode: BreathingMode, setpointSwitch: SetpointSwitch? = null) {
        require(startAmbientPressure != endAmbientPressure) {
            "startAmbientPressure ($startAmbientPressure) is not different from endAmbientPressure ($endAmbientPressure). Use addFlat() for flat segments."
        }
        // Setpoint switches only apply to CCR, ignore for OC to prevent accidental breathing mode switches.
        val effectiveSetpointSwitch = if (breathingMode is BreathingMode.ClosedCircuit) {
            setpointSwitch
        } else {
            null
        }
        val type = if (startAmbientPressure < endAmbientPressure) {
            DiveSegment.Type.DECENT
        } else {
            DiveSegment.Type.ASCENT
        }
        return addDepthChangeInternal(startAmbientPressure, endAmbientPressure, gas, timeInMinutes, type, breathingMode, effectiveSetpointSwitch)
    }

    private fun addDepthChangeInternal(startAmbientPressure: Double, endAmbientPressure: Double, gas: Cylinder, timeInMinutes: Int, type: DiveSegment.Type, breathingMode: BreathingMode, setpointSwitch: SetpointSwitch? = null) {
        // In lookahead mode we are not interested in minute-level details, apply the change as a whole.
        if(timeInMinutes > 0 && !isLookahead) {
            val diff = startAmbientPressure - endAmbientPressure
            var currentBreathingMode = breathingMode
            repeat(timeInMinutes) { minute ->
                // At callers start and end boundaries use exact pressures to avoid floating point noise.
                val startPressure = if (minute == 0) { startAmbientPressure } else { startAmbientPressure - (diff * (minute / timeInMinutes.toDouble())) }
                val endPressure = if (minute == timeInMinutes - 1) { endAmbientPressure } else { startAmbientPressure - (diff * ((minute + 1) / timeInMinutes.toDouble())) }
                currentBreathingMode = applyPressureChange(startPressure, endPressure, gas, 1, type, currentBreathingMode, setpointSwitch)
            }
        } else {
            applyPressureChange(startAmbientPressure, endAmbientPressure, gas, timeInMinutes, type, breathingMode, setpointSwitch)
        }
    }

    /**
     * Finalizes a pressure change, applies the tissue loading, decompression ceiling changes and
     * other effects of a pressure change to the model, then adds a segment to the list of segments.
     * This also takes care of sub-minute precision setpoint switches, and returns the effective
     * breathing mode at the end of this pressure change.
     */
    private fun applyPressureChange(startAmbientPressure: Double, endAmbientPressure: Double, gas: Cylinder, timeInMinutes: Int, type: DiveSegment.Type, breathingMode: BreathingMode, setpointSwitch: SetpointSwitch? = null): BreathingMode {
        // Check if the setpoint switch pressure is crossed during this segment
        val switchPressure = setpointSwitch?.ambientPressure
        val crossesSwitchPressure = switchPressure != null &&
            (startAmbientPressure < switchPressure) != (endAmbientPressure < switchPressure)

        val breathingModeAtEnd: BreathingMode.ClosedCircuit?
        if (timeInMinutes > 0 && crossesSwitchPressure) {
            // Sub-minute precision: split the model call at the exact switch pressure
            // TODO: See [DiveSegment.breathingModeAtEnd]
            val timeFractionBeforeSwitch = abs(switchPressure - startAmbientPressure) / abs(endAmbientPressure - startAmbientPressure)

            if (timeFractionBeforeSwitch > 0.0) {
                model.addPressureChange(Pressure(startAmbientPressure), Pressure(switchPressure), gas.gas, timeFractionBeforeSwitch * timeInMinutes, breathingMode.ccrSetpointOrNull)
            }
            if (timeFractionBeforeSwitch < 1.0) {
                model.addPressureChange(Pressure(switchPressure), Pressure(endAmbientPressure), gas.gas, (1.0 - timeFractionBeforeSwitch) * timeInMinutes, setpointSwitch.toBreathingMode.ccrSetpointOrNull)
            }
            breathingModeAtEnd = setpointSwitch.toBreathingMode
        } else {
            if (timeInMinutes > 0) {
                model.addPressureChange(Pressure(startAmbientPressure), Pressure(endAmbientPressure), gas.gas, timeInMinutes, breathingMode.ccrSetpointOrNull)
            }
            breathingModeAtEnd = null
        }

        segments.add(
            DiveSegment(
                start = runtime,
                duration = timeInMinutes,
                startPressure = startAmbientPressure,
                endPressure = endAmbientPressure,
                startDepth = pressureToDepth(startAmbientPressure),
                endDepth = pressureToDepth(endAmbientPressure),
                cylinder = gas,
                type = type,
                gfCeilingAtEnd = pressureToDepth(model.getCeiling().value),
                breathingMode = breathingMode,
                breathingModeAtEnd = breathingModeAtEnd,
            )
        )
        this.runtime += timeInMinutes
        this.ambientPressure = endAmbientPressure
        return breathingModeAtEnd ?: breathingMode
    }

    fun calculateTimeToSurface(breathingMode: BreathingMode, setpointSwitch: SetpointSwitch? = null): Int {
        // Prevent nested lookahead (calculateDecompression may trigger another TTS call)
        if(isLookahead) {
            return -1
        }
        if(segments.isEmpty()) {
            return 0
        }
        val decoSegments = lookahead {
            calculateDecompression(toAmbientPressure = surfacePressure, breathingMode = breathingMode, setpointSwitch = setpointSwitch).toList()
        }
        alternativeAccents[runtime] = decoSegments
        return decoSegments.sumOf { it.duration }
    }

    /**
     * Move diver from [fromAmbientPressure] to the next ceiling pressure [toAmbientPressure].
     * During which a gas change may occur as the diver reaches pressures at which a different gas
     * may be better to breathe. Gas switching is skipped when [breathingMode] is
     * [BreathingMode.ClosedCircuit], since the diver stays on the loop.
     */
    private fun addDecoDepthChange(fromAmbientPressure: Double, toAmbientPressure: Double, maxPpO2: Double, maxEquivalentNarcoticAmbientPressure: Double, fromGas: Cylinder, ascentRatePressureDelta: Double, breathingMode: BreathingMode, setpointSwitch: SetpointSwitch? = null): Cylinder {
        val isCcr = breathingMode is BreathingMode.ClosedCircuit
        var gas = fromGas

        // TODO: We could potentially use this.ambientPressure, but this requires some careful
        //       refactoring, and might not be worth it?
        var currentPressure = fromAmbientPressure
        // After crossing the switch pressure, use the switched-to breathing mode for all subsequent segments.
        var effectiveBreathingMode = breathingMode

        fun findBetterGas(currentCylinder: Cylinder?, pressure: Double): Cylinder? =
            decoGases.findBetterGasOrFallback(currentCylinder, pressure, maxPpO2, maxEquivalentNarcoticAmbientPressure)

        while (currentPressure.greaterThanTolerant(toAmbientPressure)) {
            if (!isCcr) {
                // Check if there is a better gas to breathe at the current pressure
                val betterDecoGas = findBetterGas(currentCylinder = gas, pressure = currentPressure)
                // Only start using the better gas when we reach a deco increment point
                if (betterDecoGas != null && betterDecoGas.gas != gas.gas && isAtDecoIncrement(currentPressure)) {
                    // Gas switch time is spent on the old gas: the diver is still breathing the
                    // previous gas while preparing to switch (grabbing regulator, purging, etc.).
                    // The actual switch to the new gas happens after the gas switch time.
                    addGasSwitch(currentPressure, gas, gasSwitchTime, effectiveBreathingMode)
                    gas = betterDecoGas
                }
            }

            // targetPressure is toAmbientPressure, unless there's a better gas to switch to on the way up,
            // then the target pressure may be something between currentPressure and toAmbientPressure.
            var targetPressure = toAmbientPressure

            if (!isCcr) {
                // Figure out if before we reach the targetPressure gas switches are required at any
                // of the deco increments between currentPressure and targetPressure (gas switches
                // are aligned to the decompression stops).
                var nextPressure = findNextDecoStopPressure(currentPressure)
                var nextDecoGas: Cylinder?
                while(nextPressure.greaterThanOrEqualTolerant(targetPressure)) {
                    nextDecoGas = findBetterGas(currentCylinder = gas, pressure = nextPressure)
                    if (nextDecoGas != null && nextDecoGas.gas != gas.gas) {
                        targetPressure = nextPressure
                        break
                    }
                    nextPressure -= decoStepSizePressureDelta
                }
            }

            // At this point we are either at the target pressure, or our target pressure has
            // changed due to a gas switch that needs to happen.

            // Take the diver to this new target pressure, by calculating how much time it will take
            // to get there and then recalculate the current tissue loading.

            // TODO some planners seem to round down (MultiDeco) instead of the more logical ceil
            //      (Subsurface) here? Why is that?
            // Calculate how much time it will take in minutes, rounding up
            val pressureDelta = abs(currentPressure - targetPressure)
            val duration = max(1, ceilTolerant(pressureDelta / ascentRatePressureDelta).toInt())

            addDepthChange(currentPressure, targetPressure, gas, duration, effectiveBreathingMode, setpointSwitch)

            // If the switch pressure was crossed during this segment, update the effective mode
            if (setpointSwitch != null && currentPressure > setpointSwitch.ambientPressure && targetPressure.lessThanOrEqualTolerant(setpointSwitch.ambientPressure)) {
                effectiveBreathingMode = setpointSwitch.toBreathingMode
            }

            currentPressure = targetPressure
        }

        if (!isCcr) {
            val betterDecoGas = findBetterGas(currentCylinder = gas, pressure = currentPressure)
            if (betterDecoGas != null && betterDecoGas.gas != gas.gas && isAtDecoIncrement(currentPressure)) {
                // Gas switch time on the old gas before switching
                addGasSwitch(currentPressure, gas, gasSwitchTime, effectiveBreathingMode)
                gas = betterDecoGas
            }
        }

        return gas
    }

    fun calculateDecompression(
        toAmbientPressure: Double,
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
        // TODO do we need a local fromPressure variable, or can we just use this.ambientPressure?
        val fromPressure: Double
        if (this.segments.isEmpty()) {
            // TODO Instead of throwing an exception (if there are no segments) the current pressure
            //      could be considered surface pressure, hence a simple return would be fine, as no
            //      decompression is required anyways when going from the surface to the surface?
            throw IllegalStateException("Unable to decompress, current depth is 0, have any dive stages been registered?")
        } else {
            fromPressure = ambientPressure
            gas = this.segments[this.segments.size-1].cylinder
        }
        val segmentSizeStart = this.segments.size

        // For bailout (transitioning from CCR to OC), select the best available bailout cylinder
        // and emit a GAS_SWITCH with duration 1 (a minimal problem-solving time for coming off the
        // loop and switching to the bailout regulator). The switch segment is emitted with the
        // gas that is being switched to (unlike OC-to-OC switches), because in a real bailout the
        // diver grabs a bailout regulator directly (skipping the usual team-OC switch procedure).
        // This also avoids incorrectly charging diluent as open-circuit gas consumption in the gas
        // plan (which should not happen if diluent is not available as bailout).
        // TODO: Should this be configurable?
        val isBailout = !isCcr && segments.last().breathingMode is BreathingMode.ClosedCircuit
        if (isBailout) {
            val bestBailoutGas = findBetterGasAtPressure(
                currentCylinder = null,
                ambientPressure = fromPressure,
            )
            if (bestBailoutGas != null) {
                gas = bestBailoutGas
                addGasSwitch(fromPressure, gas, 1, breathingMode)
            }
        }

        if(toAmbientPressure.greaterThanTolerant(fromPressure)) {
            // TODO Should we not even require fromPressure in the public API? Instead we could use this.ambientPressure?
            val fromDepth = pressureToDepth(fromPressure)
            val toDepth = pressureToDepth(toAmbientPressure)
            throw IllegalArgumentException("Cannot calculate decompression as the target depth (${toDepth.toInt()}) is deeper than the current depth (${fromDepth.toInt()}). Add a descending depth change first!")
        }

        // Get the current ceiling:
        var ceiling = findFirstDecoCeiling(fromPressure, maxPpO2, maxEquivalentNarcoticAmbientPressure, gas, ascentRatePressureDelta, breathingMode)

        // Check if there is a better gas to switch to at the current pressure before
        // ascending. Gas switching is skipped in CCR mode (diver stays on the loop).
        if (!isCcr) {
            val betterGas = findBetterGasAtPressure(currentCylinder = gas, ambientPressure = fromPressure)
            if (betterGas != null && betterGas.gas != gas.gas) {
                // Gas switch time on the old gas before switching
                addGasSwitch(fromPressure, gas, gasSwitchTime, breathingMode)
                gas = betterGas
            }
        }

        // Don't allow ceiling below start depth
        // TODO this check feels a bit weird, shouldn't we just throw if this happens?
        if(ceiling.greaterThanTolerant(fromPressure)) {
            // We have to stay at this pressure first, do not change pressures.
            ceiling = fromPressure
        } else {
            // Move the diver to the first ceiling (this may already be the surface)
            gas = addDecoDepthChange(
                fromPressure,
                max(ceiling, toAmbientPressure),
                maxPpO2,
                maxEquivalentNarcoticAmbientPressure,
                gas,
                ascentRatePressureDelta,
                breathingMode,
                effectiveSetpointSwitch
            )
        }

        // After the initial ascent, determine if the switch pressure was already crossed.
        var effectiveBreathingMode = if (effectiveSetpointSwitch != null && fromPressure > effectiveSetpointSwitch.ambientPressure &&
            max(ceiling, toAmbientPressure).lessThanOrEqualTolerant(effectiveSetpointSwitch.ambientPressure)) {
            effectiveSetpointSwitch.toBreathingMode
        } else {
            breathingMode
        }

        // Keep making deco stops until the deco ceiling is at or above the surface
        while (ceiling.greaterThanTolerant(surfacePressure)) {

            val currentPressure = ceiling
            if(currentPressure.lessThanOrEqualTolerant(toAmbientPressure)) {
                break
            }

            // Update effective breathing mode if we're now above the switch pressure
            if (effectiveSetpointSwitch != null && currentPressure.lessThanOrEqualTolerant(effectiveSetpointSwitch.ambientPressure)) {
                effectiveBreathingMode = effectiveSetpointSwitch.toBreathingMode
            }

            // Check the new ceiling
            ceiling = max(this.getDecoCeiling(), toAmbientPressure)

            // Don't allow ceiling below start depth
            // TODO this check feels a bit weird, shouldn't we just throw if this happens?
            if(ceiling.greaterThanTolerant(fromPressure)) {
                ceiling = fromPressure
            }

            // If while moving the diver up to the previous deco ceiling the ceiling itself moved
            // far enough to reach the next deco ceiling, a stop at the previous ceiling would be
            // pointless. Because apparently while traveling the diver has off-gassed enough to no
            // longer need the stop. So instead directly move further up to this next deco ceiling.
            // TODO forceMinimalDecoStopTime should be removed from the code base, it no longer
            //      makes sense to have this as an option, never did?
            if(ceiling.greaterThanOrEqualTolerant(currentPressure) || forceMinimalDecoStopTime) {

                // If minimal deco step times are required, force stops to also be symmetric!
                val nextDecoPressure = if(forceMinimalDecoStopTime) {
                    max(findNextDecoStopPressure(currentPressure), toAmbientPressure)
                } else {
                    max(findNextDecoStopPressure(ceiling), toAmbientPressure)
                }

                // Stop at the current deco stop until we can safely ascend to the next deco stop.
                var stopTime = 0
                while (ceiling.greaterThanTolerant(nextDecoPressure) && ceiling.greaterThanTolerant(toAmbientPressure) || (forceMinimalDecoStopTime && stopTime < 1)) {
                    // Add 1 minute of decompression and test the ceiling again, until the ceiling is higher.
                    this.addDecoStop(currentPressure, gas, 1, effectiveBreathingMode)
                    stopTime++

                    ceiling = max(this.getDecoCeiling(), toAmbientPressure)
                    if(ceiling.lessThanTolerant(nextDecoPressure) && forceMinimalDecoStopTime) {
                        // Ceiling is skipping a deco step, force the ceiling to be deeper to avoid skipping
                        ceiling = nextDecoPressure
                    }

                    if (ceiling.greaterThanTolerant(nextDecoPressure) && ceiling.greaterThanTolerant(toAmbientPressure)) {
                        if (isCeilingClearedDuringAscent(currentPressure, nextDecoPressure, gas, effectiveBreathingMode, effectiveSetpointSwitch)) {
                            ceiling = nextDecoPressure
                            break
                        }
                    }

                    if(stopTime > 1000) {
                        // We are probably in a loop where we cannot off-gas enough within the set
                        // gradient factors to reach the next deco stop. Likely the last deco stop
                        // is too shallow, the deco step size is too big or the gradient factors are
                        // extremely conservative.
                        throw DivePlanner.PlanningException("Unable to reach next deco stop within the set gradient factors. Likely the last deco stop is too shallow, the deco step size is too big or the gradient factors are extremely conservative.")
                    }
                }
            }
            gas = this.addDecoDepthChange(currentPressure, ceiling, maxPpO2, maxEquivalentNarcoticAmbientPressure, gas, ascentRatePressureDelta, effectiveBreathingMode, effectiveSetpointSwitch)
        }

        return if(segments.size == segmentSizeStart) {
            emptyList()
        } else {
            this.segments.subList(segmentSizeStart, segments.size)
        }
    }

    private fun findFirstDecoCeiling(
        fromPressure: Double,
        maxPpO2: Double,
        maxEquivalentNarcoticAmbientPressure: Double,
        gas: Cylinder,
        ascentRatePressureDelta: Double,
        breathingMode: BreathingMode,
    ): Double {

        var ceiling: Double = getDecoCeiling()
        var nextCeiling = ceiling
        do {

            ceiling = nextCeiling

            nextCeiling = lookahead {

                if (ceiling.lessThanOrEqualTolerant(surfacePressure)) {
                    return@lookahead surfacePressure
                }

                // Move diver up
                addDecoDepthChange(
                    fromPressure,
                    ceiling,
                    maxPpO2,
                    maxEquivalentNarcoticAmbientPressure,
                    gas,
                    ascentRatePressureDelta,
                    breathingMode
                )

                // Check new ceiling (could already be higher)
                getDecoCeiling()
            }
        } while(ceiling.greaterThanTolerant(nextCeiling))

        // Check if off-gassing during the ascent to the next shallower deco stop clears the
        // ceiling, allowing the current stop to be skipped entirely.
        val nextDecoPressure = findNextDecoStopPressure(nextCeiling)
        if (nextCeiling.greaterThanTolerant(surfacePressure) && nextDecoPressure.greaterThanOrEqualTolerant(surfacePressure)) {
            if (isCeilingClearedDuringAscent(fromPressure, nextDecoPressure, gas, breathingMode)) {
                return nextDecoPressure
            }
        }

        return nextCeiling
    }

    /**
     * Abysner works in whole minutes (as divers plan in minutes), which means a true ceiling just
     * barely deeper then a deco stop level would keep the diver at the deeper stop level for a full
     * extra minute, even though only a few seconds of off-gassing might be needed to clear that
     * ceiling. This method avoids that minute penalty by simulating the ascent from
     * [fromAmbientPressure] to [targetDecoAmbientPressure]: if the ceiling clears during travel,
     * the stop can be skipped. The model state is rolled back after the simulation.
     */
    private fun isCeilingClearedDuringAscent(
        fromAmbientPressure: Double,
        targetDecoAmbientPressure: Double,
        gas: Cylinder,
        breathingMode: BreathingMode,
        setpointSwitch: SetpointSwitch? = null,
    ): Boolean {
        if (targetDecoAmbientPressure.lessThanTolerant(surfacePressure)) {
            return false
        }
        val ceilingAfterAscent = lookahead {
            addDecoDepthChange(
                fromAmbientPressure,
                targetDecoAmbientPressure.coerceAtLeast(surfacePressure),
                maxPpO2,
                maxEquivalentNarcoticAmbientPressure,
                gas,
                ascentRatePressureDelta,
                breathingMode,
                setpointSwitch
            )
            getDecoCeiling()
        }
        return ceilingAfterAscent.lessThanOrEqualTolerant(targetDecoAmbientPressure)
    }

    private fun getDecoCeiling(): Double {
        val rawCeiling = model.getCeiling().value
        if (rawCeiling.lessThanOrEqualTolerant(surfacePressure)) {
            return surfacePressure
        }

        val depthPressure = rawCeiling - surfacePressure

        // Snap (ceil) to the deco grid (e.g. 3 meter or 10 feet increments). Because
        // decoStepSizePressureDelta is guaranteed to be an exact multiple of
        // displayUnitPressureDelta, this always lands on a display-unit boundary too.
        val decoGridSteps = ceilTolerant(depthPressure / decoStepSizePressureDelta).toInt()
        if (decoGridSteps <= 0) {
            return surfacePressure
        }
        val snappedPressure = surfacePressure + decoGridSteps * decoStepSizePressureDelta

        // If the deco stop falls between surface and the last allowed deco stop depth, clamp to
        // the last allowed deco stop.
        return if (snappedPressure.lessThanTolerant(lastDecoStopAmbientPressure) && snappedPressure.greaterThanTolerant(surfacePressure)) {
            lastDecoStopAmbientPressure
        } else {
            snappedPressure
        }
    }

    private fun findNextDecoStopPressure(fromAmbientPressure: Double): Double {
        val depthPressure = fromAmbientPressure - surfacePressure
        if (depthPressure.lessThanOrEqualTolerant(0.0)) {
            return surfacePressure
        }

        val steps = depthPressure / decoStepSizePressureDelta
        val nearestWholeStep = round(steps)
        val isOnGrid = steps.equalsTolerant(nearestWholeStep)

        // If exactly on a deco-grid point, go one step shallower, so we select the next stop. If
        // not, we are in between deco-grid points. In that case the next stop is the nearest
        // shallower one, so use floor to find it.
        val nextStep = if (isOnGrid) {
            nearestWholeStep.toInt() - 1
        } else {
            // No need for tolerance, if there was any floating-point noise the isOnGrid check would have caught it
            floor(steps).toInt()
        }
        if (nextStep <= 0) {
            // No more deco stops, next step on the grid is surface, return early and exactly
            return surfacePressure
        }

        val nextPressure = surfacePressure + nextStep * decoStepSizePressureDelta

        // If the next stop falls between surface and the configured last deco stop depth, skip to
        // surface (the dive was configured to not stop in between the surface and
        // lastDecoStopAmbientPressure).
        return if (nextPressure.lessThanTolerant(lastDecoStopAmbientPressure)) {
            surfacePressure
        } else {
            nextPressure
        }
    }

    private fun isAtDecoIncrement(pressure: Double): Boolean {
        val steps = (pressure - surfacePressure) / decoStepSizePressureDelta
        val nearestWholeStep = round(steps)
        // If nearest whole step is within tolerance to the actual step we are at, the diver is on a
        // deco grid point.
        return steps.equalsTolerant(nearestWholeStep)
    }

    private fun <T> lookahead(block: () -> T): T {
        val savedRuntime = runtime
        val savedAmbientPressure = ambientPressure
        val savedSegments = segments.toList()
        val savedAlternativeAscents = alternativeAccents.toMap()
        val savedIsLookahead = isLookahead
        isLookahead = true
        val result = model.resetAfter {
            block()
        }
        isLookahead = savedIsLookahead
        runtime = savedRuntime
        ambientPressure = savedAmbientPressure
        segments.clear()
        segments.addAll(savedSegments)
        alternativeAccents.clear()
        alternativeAccents.putAll(savedAlternativeAscents)
        return result
    }

    private fun findBetterGasAtPressure(currentCylinder: Cylinder?, ambientPressure: Double): Cylinder? =
        decoGases.findBetterGasOrFallback(
            currentCylinder = currentCylinder,
            ambientPressure = ambientPressure,
            maxPPO2 = maxPpO2,
            maxEquivalentNarcoticAmbientPressure = maxEquivalentNarcoticAmbientPressure,
        )

    fun getSegments(): ImmutableList<DiveSegment> {
        return segments.toPersistentList()
    }
}

