/*
 * Abysner - Dive planner
 * Copyright (C) 2025-2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.component.graphs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A label anchored to a position on a [LabeledTrack], expressed as a fraction (0 = start of the
 * track, 1 = end of the track).
 */
internal class TrackLabel(
    val fraction: Float,
    val content: @Composable () -> Unit,
)

/**
 * Layout's a horizontal [track] with an optional label above and below it, each anchored to a
 * position on the track. Labels are clamped to stay within the track's horizontal bounds.
 */
@Composable
internal fun LabeledTrack(
    modifier: Modifier = Modifier,
    labelSpacing: Dp = 2.dp,
    aboveLabel: TrackLabel? = null,
    belowLabel: TrackLabel? = null,
    track: @Composable () -> Unit,
) {
    val labelSpacingPx = with(LocalDensity.current) { labelSpacing.roundToPx() }
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            Box { track() }
            Box { aboveLabel?.content?.invoke() }
            Box { belowLabel?.content?.invoke() }
        }
    ) { measurables, constraints ->
        val trackPlaceable = measurables[0].measure(Constraints.fixedWidth(constraints.maxWidth))
        val looseConstraints = Constraints(maxWidth = constraints.maxWidth)
        val abovePlaceable = measurables[1].measure(looseConstraints)
        val belowPlaceable = measurables[2].measure(looseConstraints)

        fun clampedX(fraction: Float, labelWidth: Int): Int {
            val target = trackPlaceable.width * fraction - labelWidth / 2f
            return target.roundToInt().coerceIn(0, (trackPlaceable.width - labelWidth).coerceAtLeast(0))
        }

        val totalHeight = abovePlaceable.height + labelSpacingPx + trackPlaceable.height + labelSpacingPx + belowPlaceable.height

        layout(constraints.maxWidth, totalHeight) {
            val aboveX = if (aboveLabel != null) clampedX(aboveLabel.fraction, abovePlaceable.width) else 0
            val belowX = if (belowLabel != null) clampedX(belowLabel.fraction, belowPlaceable.width) else 0

            abovePlaceable.place(aboveX, 0)
            trackPlaceable.place(0, abovePlaceable.height + labelSpacingPx)
            belowPlaceable.place(belowX, abovePlaceable.height + labelSpacingPx + trackPlaceable.height + labelSpacingPx)
        }
    }
}
