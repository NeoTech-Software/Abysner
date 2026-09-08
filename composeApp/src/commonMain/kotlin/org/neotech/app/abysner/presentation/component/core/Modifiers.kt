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

package org.neotech.app.abysner.presentation.component.core

import androidx.compose.runtime.MutableIntState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged

fun Modifier.ifTrue(value: Boolean, block: Modifier.() -> Modifier): Modifier {
    return if(value) {
        block()
    } else {
        this
    }
}

fun Modifier.invisible(): Modifier {
    return drawWithContent {  }
}

/**
 * Grows [state] to the widest measured width ever seen, then measures this modifier's content
 * with that width as a minimum, so a set of composables sharing the same [state] all line up on
 * the widest one, regardless of the order they compose in.
 */
fun Modifier.uniformLabelWidth(
    state: MutableIntState,
): Modifier = onSizeChanged { state.intValue = maxOf(state.intValue, it.width) }
    .layout { measurable, constraints ->
        val minWidthPx = state.intValue
        val placeable = measurable.measure(
            constraints.copy(minWidth = maxOf(constraints.minWidth, minWidthPx))
        )
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }
