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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.round

@Composable
fun BigNumberDisplay(
    modifier: Modifier = Modifier,
    size: BigNumberSize = BigNumberSize.MEDIUM,
    value: String,
    /**
     * TODO: Would be better if this AutoSizing behavior is not part of the BigNumberDisplay,
     *       instead of string input, a composable input lambda would be better so the behavior can
     *       be adjusted as required. Same is true for the dropdown.
     * @see TextSingleLineAutoSize
     */
    widestEstimatedValue: String? = null,
    label: String,
    onClick: (() -> Unit)? = null,
    showDropDown: Boolean = false
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
        onClick = {
            onClick?.invoke()
        },
        enabled = onClick != null,
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {

        Box(modifier = Modifier.padding(horizontal = paddingHorizontal, vertical = paddingVertical)) {

            // fillMaxWidth is not ideal, would be better to use an intrinsic size here, but that is
            // impossible due to the fact that TextSingleLineAutoSize is internally using subcompose
            // which does not allow intrinsic size.

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                var offsetY by remember { mutableIntStateOf(0) }

                TextSingleLineAutoSize(
                    modifier = Modifier.onGloballyPositioned {
                        offsetY = it.size.height
                    },
                    style = MaterialTheme.typography.bodySmall,
                    text = label,
                )

                TextSingleLineAutoSize(
                    modifier = Modifier.padding(
                        horizontal = if (showDropDown) {
                            24.dp
                        } else {
                            0.dp
                        }
                    ),
                    widestEstimatedText = widestEstimatedValue,
                    color = MaterialTheme.colorScheme.primary,
                    style = style,
                    text = value,
                )
            }
            if(showDropDown) {
                Icon(
                    modifier = Modifier.size(24.dp).align(Alignment.CenterEnd),
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
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
    style: TextStyle = LocalTextStyle.current,
    widestEstimatedText: String? = null
) {
    require(style.fontSize != TextUnit.Unspecified)
    var fontSizeAdjusted by remember(text) { mutableStateOf(style.fontSize) }
    val minSize = 6.sp
    val height = with(LocalDensity.current) { style.lineHeight.toDp() }

    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(height, height)
    ) {

        val layoutDirection = LocalLayoutDirection.current
        val density = LocalDensity.current
        val fontFamilyResolver = LocalFontFamilyResolver.current

        fun BoxWithConstraintsScope.shouldShrink(text: String) = textMeasurer.measure(
            text = text,
            style = style.copy(fontSize = fontSizeAdjusted),
            overflow = TextOverflow.Visible,
            softWrap = false,
            maxLines = 1,
            constraints = constraints,
            layoutDirection = layoutDirection,
            density = density,
            fontFamilyResolver = fontFamilyResolver
        ).didOverflowWidth

        fun decreaseFontSizeIfPossible(): Boolean {
            val reducedSize = fontSizeAdjusted * 0.9f
            if (minSize != TextUnit.Unspecified && reducedSize <= minSize) {
                fontSizeAdjusted = minSize
                return false
            } else {
                fontSizeAdjusted = reducedSize
                return true
            }
        }

        while(shouldShrink(text = widestEstimatedText ?: text)) {
            if(!decreaseFontSizeIfPossible()) {
                break
            }
        }

        if(fontSizeAdjusted != minSize && widestEstimatedText != null) {
            // Do final pass on real text (should the estimate not be good enough)
            while(shouldShrink(text = text)) {
                if(!decreaseFontSizeIfPossible()) {
                    break
                }
            }
        }

        Text(
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            color = color,
            style = style.copy(lineHeight = TextUnit.Unspecified),
            lineHeight = lineHeight,
            textAlign = textAlign,
            textDecoration = textDecoration,
            letterSpacing = letterSpacing,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSize = fontSizeAdjusted,
            text = text,
        )
    }
}

enum class BigNumberSize {
    EXTRA_SMALL,
    SMALL,
    MEDIUM,
    LARGE
}

@Preview
@Composable
private fun GasDisplayPreview() {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BigNumberDisplay(
            size = BigNumberSize.EXTRA_SMALL,
            value = "45m",
            label = "Oxygen MOD",
        )
        BigNumberDisplay(
            size = BigNumberSize.SMALL,
            value = "45m",
            label = "Oxygen MOD",
        )
        BigNumberDisplay(
            size = BigNumberSize.MEDIUM,
            value = "45m",
            label = "Oxygen MOD",
        )
        BigNumberDisplay(
            size = BigNumberSize.LARGE,
            value = "45m",
            label = "Oxygen MOD",
        )

        BigNumberDisplay(
            value = "45m",
            label = "Oxygen MOD",
            showDropDown = true,
        )

        BigNumberDisplay(
            modifier = Modifier.width(200.dp),
            value = "45m",
            label = "Oxygen MOD",
            showDropDown = true,
        )

        BigNumberDisplay(
            modifier = Modifier.width(100.dp),
            value = "100/0",
            label = "Mix",
            showDropDown = true,
        )
    }
}
