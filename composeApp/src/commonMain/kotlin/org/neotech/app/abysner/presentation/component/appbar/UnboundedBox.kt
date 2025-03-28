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

package org.neotech.app.abysner.presentation.component.appbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * A box like layout that does not constrain its children in either the horizontal or vertical
 * direction or both, but does constrain its own size. Thus allowing overflow to happen, if
 * [Modifier.clipToBounds] is used the overflow will be clipped.
 */
@Composable
fun UnboundedBox(
    modifier: Modifier = Modifier,
    constrainChildrenHorizontal: Boolean = true,
    constrainChildrenVertical: Boolean = true,
    alignment: Alignment = Alignment.TopStart,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->

        require(constraints.maxWidth != Constraints.Infinity || constraints.maxHeight != Constraints.Infinity) {
            "UnboundedBox requires a finite maximum width and maximum height!"
        }

        val parentSize = IntSize(constraints.maxWidth, constraints.maxHeight)

        val placeables = measurables.map {
            val placeable = it.measure(
                constraints.copy(
                    maxHeight = if (constrainChildrenVertical) constraints.maxHeight else Constraints.Infinity,
                    maxWidth = if (constrainChildrenHorizontal) constraints.maxWidth else Constraints.Infinity
                )
            )

            val offset: IntOffset = alignment.align(
                size = IntSize(placeable.width, placeable.height),
                space = parentSize,
                layoutDirection = layoutDirection
            )
            placeable to offset
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach {
                it.first.placeRelative(it.second)
            }
        }
    }
}

