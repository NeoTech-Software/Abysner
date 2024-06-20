package org.neotech.app.abysner.domain.decompression.model

import org.neotech.app.abysner.domain.core.model.Gas
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
     * Selected gas for this segment (usually travel or bottom gas)
     */
    val gas: Gas,

    val gfCeilingAtEnd: Double,

    val isDecompression: Boolean,

    val travelSpeed: Double = (startDepth - endDepth) / duration.toDouble(),

    /**
     * TODO: This is better removed out of this model, since it is not always available and modifiable.
     */
    var ttsAfter: Int = -1,
) {

    val end = start + duration

    val isDecompressionStop: Boolean
        get() = type == Type.FLAT && isDecompression

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

    val type = when {
        startDepth == endDepth -> Type.FLAT
        startDepth < endDepth -> Type.DECENT
        else -> Type.ASCENT
    }

    enum class Type {
        FLAT,
        DECENT,
        ASCENT
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
 * Compacts adjacent segments together if they logically the same. Segments are only merged with
 * adjacent segments and the resulting segments may be merged again to other adjacent segments.
 *
 * Merging is done in 2 (or 3) situations
 * - If segments are flat, at the same depth, use the same gas and [DiveSegment.isDecompression] is the same.
 * - If segments are both either descending or ascending:
 *   - Start and end depth are matching
 *   - Use the same gas
 *   - Use the same ascent/descent speeds
 *
 *  If [compactAscentsBetweenDecoStops] is true, then if a single ascending segment exists between
 *  two deco stops, then this segment is merged with the shallowest deco stop.
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
            currentSegment.type == DiveSegment.Type.FLAT &&
            nextSegment.type == DiveSegment.Type.FLAT &&
            currentSegment.endDepth == nextSegment.startDepth &&
            currentSegment.gas == nextSegment.gas &&
            currentSegment.isDecompression == nextSegment.isDecompression
        ) {
            val combinedSegment = currentSegment.copy(
                duration = currentSegment.duration + nextSegment.duration,
            )
            this[i] = combinedSegment
            this.removeAt(i + 1)
        } else if (
            currentSegment.travelSpeed.equalsDelta(nextSegment.travelSpeed, maxTravelSpeedDelta) &&
            currentSegment.endDepth == nextSegment.startDepth &&
            currentSegment.gas == nextSegment.gas
        ) {
            val combinedSegment = currentSegment.copy(
                endDepth = nextSegment.endDepth,
                duration = currentSegment.duration + nextSegment.duration,
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
                gas = nextSegment.gas,
                gfCeilingAtEnd = nextSegment.gfCeilingAtEnd,
                isDecompression = true,
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
