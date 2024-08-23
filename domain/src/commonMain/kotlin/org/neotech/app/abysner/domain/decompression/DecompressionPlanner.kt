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

package org.neotech.app.abysner.domain.decompression

import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.decompression.model.DiveSegment
import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.findBestDecoGas
import org.neotech.app.abysner.domain.decompression.algorithm.DecompressionModel
import org.neotech.app.abysner.domain.decompression.algorithm.SnapshotScope
import org.neotech.app.abysner.domain.decompression.algorithm.SnapshotScopeImpl
import org.neotech.app.abysner.domain.diveplanning.DivePlanner
import org.neotech.app.abysner.domain.decompression.model.subList
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.round

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
) {

    private var isCalculatingTts = false

    var runtime = 0
        private set
    private val decoGasses = mutableListOf<Cylinder>()
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
    fun getAlternativeAccents(): Map<Int, List<DiveSegment>> = alternativeAccents

    fun getDecoGasses(): List<Cylinder> = decoGasses.toList()

    fun setDecoGasses(gasses: List<Cylinder>) {
        this.decoGasses.clear()
        this.decoGasses.addAll(gasses)
    }

    fun addCylinder(cylinder: Cylinder) {
        this.decoGasses.add(cylinder)
    }

    fun addFlat(depth: Double, gas: Cylinder, timeInMinutes: Int, isDecompression: Boolean) {
        return addDepthChangePerMinute(depth, depth, gas, timeInMinutes, isDecompression)
    }

    fun addDepthChangePerMinute(startDepth: Double, endDepth: Double, gas: Cylinder, timeInMinutes: Int, isDecompression: Boolean) {
        if(calculateTissueChangesPerMinute && !isCalculatingTts) {
            val diff = startDepth - endDepth
            repeat(timeInMinutes) {
                val statDepthForThisMinute = startDepth - (diff * ((it) / timeInMinutes.toDouble()))
                val endDepthForThisMinute = startDepth - (diff * ((it + 1) / timeInMinutes.toDouble()))
                addDepthChange(statDepthForThisMinute, endDepthForThisMinute, gas, 1, isDecompression)
            }
        } else {
            addDepthChange(startDepth, endDepth, gas, timeInMinutes, isDecompression)
        }
    }

    private fun addDepthChange(startDepth: Double, endDepth: Double, gas: Cylinder, timeInMinutes: Int, isDecompression: Boolean) {

        model.addDepthChange(startDepth, endDepth, gas.gas, timeInMinutes)

        val ceiling = model.getCeiling()

        //store this as a stage
        this.segments.add(
            DiveSegment(
                start = runtime,
                duration = timeInMinutes,
                startDepth = startDepth,
                endDepth = endDepth,
                cylinder = gas,
                isDecompression = isDecompression,
                gfCeilingAtEnd = ceiling,
                ttsAfter = -1
            )
        )
        this.runtime += timeInMinutes
    }

    fun calculateTimeToSurface(): Int {
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
            calculateDecompression(toDepth = 0).sumOf { it.duration }.also {
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
     */
    private fun addDecoDepthChange(fromDepth: Double, toDepth: Double, maxppO2: Double, maxEND: Double, fromGas: Cylinder, ascentRateInMetersPerMinute: Double): Cylinder {
        var gas = fromGas
        var currentDepth = fromDepth

        while (currentDepth > toDepth) {
            // Check if there is a better gas to breath at the current depth
            val betterDecoGas = decoGasses.findBestDecoGas(currentDepth, environment, maxppO2, maxEND)
            // Only start using the better gas when we reach a deco increment point

            if (betterDecoGas != null && betterDecoGas != gas && currentDepth.toInt() % decoStepSize == 0) {
                gas = betterDecoGas
            }


            // targetDepth is to toDepth, unless there's a better gas to switch to on the way up,
            // then the target depth may be something between currentDepth and toDepth.
            var targetDepth = toDepth

            // Figure out if before we reach the targetDepth anything needs to happen (gas switch)
            // We do this by descending with increments of 1 meter and checking if there is a better
            // gas available at each depth.
            var nextDepth = currentDepth - 1
            var nextDecoGas: Cylinder?
            while(nextDepth >= targetDepth) {
                nextDecoGas = decoGasses.findBestDecoGas(nextDepth, environment, maxppO2, maxEND)
                // TODO don't hardcode 3 here, instead use the configuration!
                if (nextDecoGas != null && nextDecoGas != gas && nextDepth.toInt() % 3 == 0) {
                    targetDepth = nextDepth //Only carry us up to the point where we can use this better gas.
                    break
                }
                nextDepth--
            }

            // At this point we are either at the target depth, or our target depth has changed due to a gas switch that needs to happen.

            // Take the diver to this new target depth, by calculating how much time it will take to
            // get there and then recalculate the current tissue loading.

            // TODO some planners seem to round down (MultiDeco) instead of the more logical ceil
            //      (Subsurface) here? Why is that?
            // Calculate how much time it will take in minutes, rounding up
            val depthChange = abs(currentDepth - targetDepth)
            val duration = max(1, ceil(depthChange / ascentRateInMetersPerMinute).toInt())

            // println("Moving diver from $fromDepth to $targetDepth on gas $gas over $duration minutes.")
            addDepthChangePerMinute(currentDepth, targetDepth, gas, duration, true)

            // TODO Add gas switch time (settings)
            //if(foundBetterGas && nextDecoGas != null) {
            //    addFlat(targetDepth, nextDecoGas, 1, isDecompression = true)
            //}

            currentDepth = targetDepth
        }

        val betterDecoGasName = decoGasses.findBestDecoGas(currentDepth, environment, maxppO2, maxEND)
        // TODO remove hardcoded number 3
        if (betterDecoGasName != null && betterDecoGasName != gas && currentDepth.toInt() % 3 == 0) {
            gas = betterDecoGasName
        }
        return gas
    }

    fun calculateDecompression(
        toDepth: Int,
    ): List<DiveSegment> {
        var gas: Cylinder
        val fromDepth: Double
        if (this.segments.size == 0) {
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

        if(toDepth > fromDepth) {
            throw IllegalArgumentException("Cannot calculate decompression as the target depth ($toDepth meter) is deeper then the current depth ($fromDepth meter). Add an descending depth change first!")
        }

        // Get the current ceiling:
        var ceiling = findFirstDecoCeiling(fromDepth, decoStepSize, lastDecoStopDepth, maxPpO2, maxEquivalentNarcoticDepth, gas, ascentRate)

        // Don't allow ceiling below start depth
        // TODO get this depth from the previous section endDepth?
        if(ceiling > fromDepth) {
            // We have to stay at this depth first, do not change depths, but look for better gas.
            ceiling = fromDepth.toInt()
            gas = decoGasses.findBestDecoGas(fromDepth, environment, maxPpO2, maxEquivalentNarcoticDepth) ?: gas
        } else {
            gas = decoGasses.findBestDecoGas(fromDepth, environment, maxPpO2, maxEquivalentNarcoticDepth) ?: gas
            // Move the diver to the first ceiling (this may already be the surface)

            gas = addDecoDepthChange(
                fromDepth,
                max(ceiling.toDouble(), toDepth.toDouble()),
                maxPpO2,
                maxEquivalentNarcoticDepth,
                gas,
                ascentRate
            )
        }
        // Keep making deco stops until the deco ceiling is above the surface
        while (ceiling > 0) {

            val currentDepth = ceiling.toDouble()
            if(currentDepth <= toDepth) {
                break
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
                    this.addFlat(currentDepth, gas, 1, isDecompression = true)
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
                    if(stopTime > 1000) {
                        // We are probably in a loop where we cannot off-gas enough within the set gradient factors
                        // to reach the next deco stop. Likely the last deco stop is too shallow, the deco step size is too
                        // big or the gradient factors are extremely conservative.
                        throw DivePlanner.PlanningException("Unable to reach next deco stop within the set gradient factors. Likely the last deco stop is too shallow, the deco step size is too big or the gradient factors are extremely conservative.")
                    }
                }
            }
            gas = this.addDecoDepthChange(currentDepth, ceiling.toDouble(), maxPpO2, maxEquivalentNarcoticDepth, gas, ascentRate)
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
        ascentRate: Double
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
                    ascentRate
                )

                // Check new ceiling (could already be higher)
                getDecoCeiling(
                    decoStepSize,
                    lastDecoStopDepth
                )
            }
        } while(ceiling > nextCeiling)

        return nextCeiling
    }

    private fun getDecoCeiling(decoStepSize: Int, lastDecoStopDepth: Int): Int {
        var ceiling = round(model.getCeiling()).toInt()
        // Divers like to do deco stops in increments of 10 feet or 3 meters.
        // This finds the closest to the ceiling increment of 3 (lower or at the ceiling,
        // never higher).
        // This increment thing may not be required, especially if a conservative gradient factor is
        // chosen, since you will never be close to an M value. But since this is almost an industry
        // standard now...
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

    private fun <T> resetAfter(block: SnapshotScope.() -> T): T {
        val savedRuntime = runtime
        val savedSegments = segments.toList()
        val savedAlternativeAscents = alternativeAccents.toMap()
        val scope = SnapshotScopeImpl()
        val result = model.resetAfter {
            scope.block()
        }
        if(!scope.keepChanges) {
            runtime = savedRuntime
            segments.clear()
            segments.addAll(savedSegments)
            alternativeAccents.clear()
            alternativeAccents.putAll(savedAlternativeAscents)
        }
        return result
    }

    fun getSegments(): List<DiveSegment> {
        return segments.toList()
    }
}

private const val calculateTissueChangesPerMinute: Boolean = true
