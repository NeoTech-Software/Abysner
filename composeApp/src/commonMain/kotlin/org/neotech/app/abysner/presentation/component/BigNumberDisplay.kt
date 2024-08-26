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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.neotech.app.abysner.domain.core.model.Gas
import kotlin.math.min

@Composable
fun BigNumberDisplay(
    modifier: Modifier = Modifier,
    size: BigNumberSize = BigNumberSize.MEDIUM,
    value: String,
    label: String,
) {

    val style = when (size) {
        BigNumberSize.EXTRA_SMALL -> MaterialTheme.typography.headlineSmall
        BigNumberSize.SMALL -> MaterialTheme.typography.headlineMedium
        BigNumberSize.MEDIUM -> MaterialTheme.typography.displayMedium
        BigNumberSize.LARGE -> MaterialTheme.typography.displayLarge
    }

    val paddingHorizontal = when (size) {
        BigNumberSize.EXTRA_SMALL -> 12.dp
        BigNumberSize.SMALL -> 12.dp
        BigNumberSize.MEDIUM -> 12.dp
        BigNumberSize.LARGE -> 12.dp
    }

    val paddingVertical = when (size) {
        BigNumberSize.EXTRA_SMALL -> 8.dp
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
            modifier = Modifier
                .wrapContentWidth()
                .padding(horizontal = paddingHorizontal, vertical = paddingVertical),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextSingleLineAutoSize(
                style = MaterialTheme.typography.bodySmall,
                text = label,
            )

            TextSingleLineAutoSize(
                color = MaterialTheme.colorScheme.primary,
                style = style,
                text = value,
            )
        }
    }
}

@Composable
private fun TextSingleLineAutoSize(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    style: TextStyle = LocalTextStyle.current
) {
    require(style.fontSize != TextUnit.Unspecified)

    var fontSizeAdjusted by remember { mutableStateOf(style.fontSize) }
    var isTextMeasured by remember { mutableStateOf(false) }
    val minSize = 6.sp

    Text(
        modifier = modifier.drawWithContent {
            if (isTextMeasured) {
                drawContent()
            }
        },
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Visible,
        color = color,
        style = style,
        lineHeight = lineHeight,
        textAlign = textAlign,
        textDecoration = textDecoration,
        letterSpacing = letterSpacing,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        fontSize = fontSizeAdjusted,
        text = text,
        onTextLayout = {
            fun constrain() {
                val reducedSize = fontSizeAdjusted * 0.9f
                if (minSize != TextUnit.Unspecified && reducedSize <= minSize) {
                    fontSizeAdjusted = minSize
                    isTextMeasured = true
                } else {
                    fontSizeAdjusted = reducedSize
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


enum class BigNumberSize {
    EXTRA_SMALL,
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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GasDisplay()
        BigNumberDisplay(
            value = "45m",
            label = "Oxygen MOD",
        )
    }
}
