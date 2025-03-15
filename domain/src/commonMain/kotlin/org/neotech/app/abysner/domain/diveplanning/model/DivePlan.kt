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

package org.neotech.app.abysner.domain.diveplanning.model

import org.neotech.app.abysner.domain.decompression.model.DiveSegment
import org.neotech.app.abysner.domain.decompression.model.compactSimilarSegments
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.physics.depthInMetersToBar
import org.neotech.app.abysner.domain.utilities.DecimalFormat
import kotlin.math.ceil

data class DivePlan(
    val segments: List<DiveSegment>,
    val alternativeAccents: Map<Int, List<DiveSegment>>,
    val decoGasses: List<Cylinder>,
    val configuration: Configuration,
    val totalCns: Double,
    val totalOtu: Double,
) {

    /**
     * Minute in which the first deco occurs
     */
    val firstDeco: Int = segments.find { it.gfCeilingAtEnd > 0.0 }?.start ?: -1

    val deepestCeiling: Double = segments.maxOfOrNull { it.gfCeilingAtEnd } ?: 0.0

    val segmentsCollapsed = segments.toMutableList().compactSimilarSegments()

    val isEmpty = segments.isEmpty()

    val maximumDepth: Double = segmentsCollapsed.maxByOrNull { it.startDepth }?.startDepth ?: 0.0
    val runtime: Int = segmentsCollapsed.sumOf { it.duration }

    val maxTimeToSurface: DiveSegment? = segments.maxByOrNull { it.ttsAfter }

    val totalDeco = segmentsCollapsed.totalDeco()
    val averageDepth = segmentsCollapsed.calculateAverageDepth()

    val maximumGasDensities: List<GasAtDepth>
        get() = segmentsCollapsed
            .groupBy { it.cylinder }
            .mapValues { it.value.maxOf { segment -> segment.maxDepth } }
            .map {
                GasAtDepth(it.key.gas, it.value, configuration.environment)
            }

    data class GasAtDepth(
        val gas: Gas,
        val depth: Double,
        val environment: Environment,
    ) {
        val ppo2 = gas.oxygenFraction * depthInMetersToBar(depth, environment).value
        val density: Double = gas.densityAtDepth(depth, environment)
    }

    fun toString(compact: Boolean = false): String {
        val builder = StringBuilder()
        builder.appendLine(segmentsCollapsed.toMutableList().compactSimilarSegments(compact).asString())
        builder.appendLine()
        builder.appendLine("CNS: ${DecimalFormat.format(0, ceil(totalCns))}%")
        builder.appendLine("OTU: ${DecimalFormat.format(0, ceil(totalOtu))}")
        builder.appendLine()
        builder.appendLine("Salinity: ${configuration.salinity.humanReadableName}")
        builder.appendLine("ATM pressure: ${configuration.environment.atmosphericPressure} hPa")
        return builder.toString()
    }

    override fun toString(): String {
        return toString(compact = true)
    }
}

fun List<DiveSegment>.totalDeco(): Int {
    return filter { it.type == DiveSegment.Type.FLAT && it.isDecompression }.sumOf { it.duration }
}

fun List<DiveSegment>.calculateAverageDepth(): Double {
    return sumOf { it.averageDepth * it.duration } / sumOf { it.duration }
}

private fun List<DiveSegment>.asString(): String {
    val builder = StringBuilder()
    val formatter = DecimalFormat
    builder.appendLine("      Depth    Runtime    Duration    Gas")
    forEachIndexed { index, it ->
        val typeChar = when(it.type) {
            DiveSegment.Type.FLAT -> if(it.isDecompression) {
                '⏹'
            } else {
                '▶'
            }
            DiveSegment.Type.DECENT -> '▼'
            DiveSegment.Type.ASCENT -> '▲'
        }
        val depth = formatter.format(0, it.endDepth).toInt()
        builder.appendLine("${index.spaced(5)} $typeChar ${depth.spaced(6)} ${it.end.spaced(10)} ${it.duration.spaced(11)} ${it.cylinder.gas}")
    }
    return builder.toString()
}

private fun Any.spaced(spaces: Int): String {
    val rawString = toString()
    return if(rawString.length == spaces) {
        this.toString()
    } else {
        if(rawString.length > spaces) {
            rawString.padEnd(spaces - 1, ' ').toString() + '…'
        } else {
            rawString.padEnd(spaces, ' ').toString()
        }
    }
}
