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

package org.neotech.app.abysner.presentation.component.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.round

/**
 * Arrangement that distributes all leftover space (if any) evenly around a single child.
 */
@Suppress("UnusedReceiverParameter")
fun Arrangement.Horizontal.spaceAround(itemIndex: Int): Arrangement.Horizontal = ArrangementSpaceAround(itemIndex)

/**
 * Arrangement that distributes all leftover space (if any) evenly around a single child.
 */
@Suppress("UnusedReceiverParameter")
fun Arrangement.HorizontalOrVertical.spaceAround(itemIndex: Int): Arrangement.HorizontalOrVertical = ArrangementSpaceAround(itemIndex)

private class ArrangementSpaceAround(private val itemIndex: Int) : Arrangement.HorizontalOrVertical {

    override val spacing = 0.dp

    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        layoutDirection: LayoutDirection,
        outPositions: IntArray,
    ) = if (layoutDirection == LayoutDirection.Ltr) {
        placeNthInCenter(totalSize, sizes, outPositions, reverseInput = false)
    } else {
        placeNthInCenter(totalSize, sizes, outPositions, reverseInput = true)
    }

    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        outPositions: IntArray,
    ) = placeNthInCenter(totalSize, sizes, outPositions, reverseInput = false)

    private fun placeNthInCenter(
        totalSize: Int,
        sizes: IntArray,
        outPosition: IntArray,
        reverseInput: Boolean,
    ) {
        val totalOccupiedSpace = sizes.sum()
        val totalGapSize = if (totalSize == totalOccupiedSpace) {
            // No space to give away to the first item
            0
        } else {
            (totalSize - totalOccupiedSpace)
        }

        var current = 0f
        val correctedForDirection = if(reverseInput) {
            sizes.reversedArray()
        } else {
            sizes
        }
        correctedForDirection.forEachIndexed { index, item ->

            if(index == itemIndex) {
                val currentTemp = round(current + (totalGapSize / 2f))
                outPosition[index] = currentTemp.toInt()
                // Add the whole gap size as a whole number (to avoid rounding issues)
                current += totalGapSize
            } else {
                outPosition[index] = current.toInt()
            }
            current += item
        }
    }
}
