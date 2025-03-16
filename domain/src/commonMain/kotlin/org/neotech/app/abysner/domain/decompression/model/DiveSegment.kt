/*
 * Abysner - Dive planner
 * Copyright (C) 2025 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.domain.decompression.model

import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.utilities.equalsDelta
import kotlin.math.max

data class DiveSegment(
    /**
     * Minute in which this segment starts.
     */
    val start: Int,

    /**
     * Duration is minutes.
     */
    val duration: Int,
    /**
     * Start depth of this segment (in meters)
     */
    val startDepth: Double,

    /**
     * End depth of this segment (in meters)
     */
    val endDepth: Double,
    /**
     * Selected cylinder & gas for this segment
     */
    val cylinder: Cylinder,

    val gfCeilingAtEnd: Double,

    /**
     * The type of this segment, which describes both the geometry (flat, ascending, descending)
     * and the semantic purpose (deco stop, gas switch).
     */
    val type: Type,

    val travelSpeed: Double = (startDepth - endDepth) / duration.toDouble(),

    /**
     * TODO: This is better removed out of this model, since it is not always available and modifiable.
     */
    var ttsAfter: Int = -1,
) {

    val end = start + duration

    val isDecompressionStop: Boolean
        get() = type == Type.DECO_STOP

    val isGasSwitch: Boolean
        get() = type == Type.GAS_SWITCH

    fun depthAt(duration: Int): Double {
        require(duration >= 0 && duration <= this.duration)
        if (startDepth == endDepth) {
            return startDepth
        } else {
            val depthDifference =
                (endDepth - startDepth) * (duration.toDouble() / this.duration.toDouble())
            return startDepth + depthDifference
        }
    }

    /**
     * Average depth of this section (essentially the depth mid-point).
     */
    val averageDepth: Double = (startDepth + endDepth) / 2.0

    val maxDepth = max(startDepth, endDepth)

    enum class Type {
        /** Flat non-decompression segment (e.g., user-planned bottom time). */
        FLAT,
        /** Descending segment. */
        DECENT,
        /** Ascending segment. */
        ASCENT,
        /** Flat decompression stop. */
        DECO_STOP,
        /** Flat segment representing time spent switching to a different breathing gas. */
        GAS_SWITCH;

        /** Whether this type represents a flat (constant depth) segment. */
        val isFlat: Boolean get() = this == FLAT || this == DECO_STOP || this == GAS_SWITCH
    }
}

fun List<DiveSegment>.subList(fromTimeStamp: Int): List<DiveSegment> {
    val indexOfFirstAffectedDiveSegment = indexOfFirst { it.start >= fromTimeStamp }
    return if(indexOfFirstAffectedDiveSegment == -1) {
        emptyList()
    } else {
        // TODO exact subList at the timestamp by averaging the found dive segment?
        this.subList(indexOfFirstAffectedDiveSegment, this.size)
    }
}

/**
 * Merges adjacent segments that are logically equivalent, reducing the list to a minimal set of
 * dive instructions. Operates in place and returns the same list.
 *
 * Note: some per-segment data becomes imprecise after merging. [DiveSegment.gfCeilingAtEnd] is
 * taken from the last merged segment, so any deeper ceiling that occurred earlier within the
 * merged span is lost.
 *
 * @param compactAscentsBetweenDecoStops If true, a single ascending segment between two deco stops
 *                                       is folded into the shallower stop.
 */
fun MutableList<DiveSegment>.compactSimilarSegments(
    compactAscentsBetweenDecoStops: Boolean = false
): MutableList<DiveSegment> {

    // Maximum delta between travel speeds for them to be considered equal.
    val maxTravelSpeedDelta = 0.0001

    var i = 0
    while (i < size - 1) {
        val currentSegment = this[i]
        val nextSegment = this[i + 1]
        if (
            currentSegment.type.isFlat &&
            currentSegment.type == nextSegment.type &&
            currentSegment.endDepth == nextSegment.startDepth &&
            currentSegment.cylinder == nextSegment.cylinder
        ) {
            val combinedSegment = currentSegment.copy(
                duration = currentSegment.duration + nextSegment.duration,
                gfCeilingAtEnd = nextSegment.gfCeilingAtEnd,
            )
            this[i] = combinedSegment
            this.removeAt(i + 1)
        } else if (
            currentSegment.travelSpeed.equalsDelta(nextSegment.travelSpeed, maxTravelSpeedDelta) &&
            currentSegment.endDepth == nextSegment.startDepth &&
            currentSegment.cylinder == nextSegment.cylinder
        ) {
            val combinedSegment = currentSegment.copy(
                endDepth = nextSegment.endDepth,
                duration = currentSegment.duration + nextSegment.duration,
                gfCeilingAtEnd = nextSegment.gfCeilingAtEnd,
            )
            this[i] = combinedSegment
            this.removeAt(i + 1)
        } else if (
            compactAscentsBetweenDecoStops &&
            currentSegment.type == DiveSegment.Type.ASCENT &&
            nextSegment.isDecompressionStop && getOrNull(i - 1)?.isDecompressionStop == true
        ) {
            // Previous and next segments are both stops, make this segment the next stop instead.
            val combinedSegment = DiveSegment(
                start = currentSegment.start,
                endDepth = nextSegment.endDepth,
                startDepth = nextSegment.startDepth,
                duration = currentSegment.duration + nextSegment.duration,
                cylinder = nextSegment.cylinder,
                gfCeilingAtEnd = nextSegment.gfCeilingAtEnd,
                type = DiveSegment.Type.DECO_STOP,
            )
            this[i] = combinedSegment
            this.removeAt(i + 1)
        } else {
            // Only increment i if nothing changed, otherwise make a pass again and keep compacting
            // at this index until nothing changes anymore.
            i++
        }
    }
    return this
}
