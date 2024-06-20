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

package org.neotech.app.abysner.presentation.component

import abysner.composeapp.generated.resources.Res
import abysner.composeapp.generated.resources.ic_outline_add_circle_outline_24
import abysner.composeapp.generated.resources.ic_outline_remove_circle_outline_24
import androidx.annotation.IntRange
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SliderWithButtons(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0)
    steps: Int = 0,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {

    val addEnabled = value < valueRange.endInclusive
    val removeEnabled = value > valueRange.start

    Row(modifier = modifier) {
        IconButton(
            enabled = removeEnabled && enabled,
            onClick = { onValueChange(value - 1) },
        ) {
            Icon(
                painter = painterResource(resource = Res.drawable.ic_outline_remove_circle_outline_24),
                contentDescription = ""
            )
        }
        Slider(
            modifier = Modifier.weight(1f),
            value = value,
            colors = colors,
            steps = steps,
            interactionSource = interactionSource,
            enabled = enabled,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
        IconButton(
            enabled = addEnabled && enabled,
            onClick = { onValueChange(value + 1) },
        ) {
            Icon(
                painter = painterResource(resource = Res.drawable.ic_outline_add_circle_outline_24),
                contentDescription = ""
            )
        }
    }
}

@Preview
@Composable
fun SliderWithButtonsPreview() {

    var oxygenPercentage: Int by remember {
        mutableIntStateOf(21)
    }

    Surface {
        SliderWithButtons(
            value = oxygenPercentage.toFloat(),
            valueRange = 4f..100f,
            onValueChange = { oxygenPercentage = it.toInt() }
        )
    }
}