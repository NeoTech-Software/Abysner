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

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.neotech.app.abysner.domain.core.model.Gas

@Composable
fun BigNumberDisplay(
    modifier: Modifier = Modifier,
    size: BigNumberSize = BigNumberSize.MEDIUM,
    value: String,
    label: String,
) {

    val style = when (size) {
        BigNumberSize.SMALL -> MaterialTheme.typography.headlineMedium
        BigNumberSize.MEDIUM -> MaterialTheme.typography.displayMedium
        BigNumberSize.LARGE -> MaterialTheme.typography.displayLarge
    }

    val paddingHorizontal = when (size) {
        BigNumberSize.SMALL -> 16.dp
        BigNumberSize.MEDIUM -> 16.dp
        BigNumberSize.LARGE -> 16.dp
    }

    val paddingVertical = when (size) {
        BigNumberSize.SMALL -> 8.dp
        BigNumberSize.MEDIUM -> 16.dp
        BigNumberSize.LARGE -> 16.dp
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(
                start = paddingHorizontal,
                end = paddingHorizontal,
                top = paddingVertical,
                bottom = paddingVertical
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                text = label
            )

            // TODO generalize the below auto-sizing code potentially extracting it?
            var textStyle by remember { mutableStateOf(style) }
            var isTextMeasured by remember { mutableStateOf(false) }
            val minSize = 6.sp

            Text(
                modifier = Modifier.drawWithContent {
                    if (isTextMeasured) {
                        drawContent()
                    }
                },
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
                color = MaterialTheme.colorScheme.primary,
                style = textStyle,
                text = value,
                onTextLayout = {
                    fun constrain() {
                        val reducedSize = textStyle.fontSize * 0.9f
                        if (minSize != TextUnit.Unspecified && reducedSize <= minSize) {
                            textStyle = textStyle.copy(fontSize = minSize)
                            isTextMeasured = true
                        } else {
                            textStyle = textStyle.copy(fontSize = reducedSize)
                        }
                    }
                    if (it.didOverflowWidth) {
                        constrain()
                    } else {
                        isTextMeasured = true
                    }
                }
            )
        }
    }
}

enum class BigNumberSize {
    SMALL,
    MEDIUM,
    LARGE
}

@Composable
fun GasDisplay(
    modifier: Modifier = Modifier,
    size: BigNumberSize = BigNumberSize.MEDIUM,
    oxygenPercentage: Int = 21,
    heliumPercentage: Int = 0
) {
    BigNumberDisplay(
        modifier = modifier,
        size = size,
        value = "$oxygenPercentage/$heliumPercentage",
        label = "Mix (O2/He)"
    )
}

@Composable
fun GasDisplay(
    modifier: Modifier = Modifier,
    size: BigNumberSize = BigNumberSize.MEDIUM,
    gas: Gas = Gas.Trimix2135
) {
    GasDisplay(
        modifier = modifier,
        size = size,
        oxygenPercentage = (gas.oxygenFraction * 100.0).toInt(),
        heliumPercentage = (gas.heliumFraction * 100.0).toInt()
    )
}

@Preview
@Composable
private fun GasDisplayPreview() {
    GasDisplay()
}
