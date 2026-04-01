/*
 * Abysner - Dive planner
 * Copyright (C) 2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.screens.planner.decoplan

import io.github.koalaplot.core.xygraph.DefaultPoint
import org.neotech.app.abysner.domain.decompression.model.DiveSegment

/**
 * Returns a copy of each point shifted by [x] and [y].
 */
fun List<DefaultPoint<Float, Float>>.offset(x: Float = 0f, y: Float = 0f): List<DefaultPoint<Float, Float>> =
    map { DefaultPoint(it.x + x, it.y + y) }

/**
 * Returns a copy of each point with x and y coerced to the given bounds. Only axes you name are
 * coerced.
 */
fun List<DefaultPoint<Float, Float>>.coerceIn(
    xMin: Float = Float.NEGATIVE_INFINITY,
    xMax: Float = Float.POSITIVE_INFINITY,
    yMin: Float = Float.NEGATIVE_INFINITY,
    yMax: Float = Float.POSITIVE_INFINITY,
): List<DefaultPoint<Float, Float>> =
    map { DefaultPoint(it.x.coerceIn(xMin, xMax), it.y.coerceIn(yMin, yMax)) }

/**
 * Returns the (time, depth) plot points for the GF ceiling area, where x is time in minutes
 * and y is the negative ceiling depth (0 = surface, more negative = deeper).
 */
fun buildGfCeilingPlotPoints(segments: List<DiveSegment>): List<DefaultPoint<Float, Float>> {
    // Skip zero-duration segments (GAS_SWITCH etc.) to avoid unnecessary duplicate x-coordinates.
    val nonZeroSegments = segments.filter { it.duration > 0 }
    
    // Drop all leading zero-ceiling segments except the one directly before the first
    // non-zero ceiling (keeps a single clean zero-height starting point for the filled area).
    val subListStart = (nonZeroSegments.indexOfFirst { it.gfCeilingAtEnd > 0.0 } - 1).coerceAtLeast(0)

    return buildList {
        // Rare edge case: if ceiling is already > 0 from the very first segment (diver starts with
        // residual loading). DiveSegment.end of the first segment is higher than 0, so we need an
        // explicit (0, 0) anchor to close the left edge of the filled graph area.
        if ((nonZeroSegments.firstOrNull()?.gfCeilingAtEnd ?: 0.0) > 0.0) {
            add(DefaultPoint(0f, 0f))
        }
        
        // Use segment.end as x (gfCeilingAtEnd belongs at the end of the segment).
        nonZeroSegments.subList(subListStart, nonZeroSegments.size).mapTo(this) { segment ->
            DefaultPoint(segment.end.toFloat(), -segment.gfCeilingAtEnd.toFloat())
        }
    }
}

/**
 * Returns the (time, depth) plot points for the depth profile line, where x is elapsed time
 * in minutes and y is the negative depth (0 = surface).
 */
fun buildDepthProfilePlotPoints(segments: List<DiveSegment>): List<DefaultPoint<Float, Float>> {
    var runtime = 0L
    return buildList {
        segments.mapTo(this) { segment ->
            DefaultPoint(runtime.toFloat(), -segment.startDepth.toFloat()).also {
                runtime += segment.duration
            }
        }
        // Add a final point at the last segment's end depth so the line reaches the surface.
        add(DefaultPoint(runtime.toFloat(), -segments.last().endDepth.toFloat()))
    }
}

/**
 * Returns the (time, depth) plot points for the running-average depth line, where x is elapsed
 * time in minutes and y is the negative running-average depth.
 */
fun buildAverageDepthPlotPoints(segments: List<DiveSegment>): List<DefaultPoint<Float, Float>> {
    var runningTotal = 0.0
    var runningDuration = 0L
    return segments.map { segment ->
        runningTotal += segment.averageDepth * segment.duration
        runningDuration += segment.duration
        DefaultPoint(segment.end.toFloat(), -(runningTotal / runningDuration).toFloat())
    }
}
