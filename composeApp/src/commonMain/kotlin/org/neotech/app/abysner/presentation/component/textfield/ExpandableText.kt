/*
 * Abysner - Dive planner
 * Copyright (C) 2025 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.component.textfield

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ExpandableText(
    modifier: Modifier = Modifier,
    annotatedText: AnnotatedString,
    showMoreText: String = "Show more",
    minimizedMaxLines: Int = 3,
    style: TextStyle = LocalTextStyle.current
) {
    var expanded by remember { mutableStateOf(false) }
    var hasVisualOverflow by remember { mutableStateOf(false) }
    Box(modifier = modifier
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = { expanded = !expanded }
        )
    ) {

        val textCollapsed = remember(annotatedText) {
            // Replace all double whitespace with a single space
            // Replace all new lines
            // Keep existing spannable
            buildAnnotatedString {
                var lastWasWhitespace = false
                val originalText = annotatedText.text
                val spanStyles = annotatedText.spanStyles
                val modifiedSpans = spanStyles.toMutableList()

                originalText.forEachIndexed { i, char ->
                    // If the character is a whitespace or newline, collapse multiple spaces/newlines into one
                    if (char.isWhitespace() || char == '\n') {
                        if (lastWasWhitespace) {
                            // Already added a whitespace, skip this one (or line), and shift the spannable's accordingly.
                            modifiedSpans.forEachIndexed { index, span ->
                                if (span.start >= i) {
                                    // If the span starts at this character index or after it, shift both start and end
                                    modifiedSpans[index] = AnnotatedString.Range(span.item, span.start - 1, span.end - 1)
                                } else if (span.start < i && span.end > i) {
                                    // If the current index is within the span range, shift the end only
                                    modifiedSpans[index] = AnnotatedString.Range(span.item, span.start, span.end - 1)
                                }
                            }
                        } else {
                            append(" ")
                            lastWasWhitespace = true
                        }
                    } else {
                        append(char)
                        lastWasWhitespace = false
                    }
                }

                modifiedSpans.forEach {
                    addStyle(it.item, it.start, it.end)
                }
            }
        }

        Text(
            text = if(expanded) { annotatedText } else { textCollapsed },
            maxLines = if (expanded) { Int.MAX_VALUE } else { minimizedMaxLines },
            onTextLayout = { hasVisualOverflow = it.hasVisualOverflow },
            style = style
        )
        if (hasVisualOverflow) {
            val fadeSize = 48.dp
            val fadeSizeInPx = with(LocalDensity.current) { fadeSize.toPx() }
            Text(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .drawWithContent {
                        // Clear everything below this composable (except for the fade area)
                        drawRect(
                            brush = SolidColor(Color.Transparent),
                            topLeft = Offset(fadeSizeInPx, 0.0f),
                            blendMode = BlendMode.Clear
                        )

                        // Draw the fade area
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Black, Color.Transparent),
                                endX = fadeSizeInPx
                            ),
                            size = Size(height = size.height, width = fadeSizeInPx),
                            blendMode = BlendMode.DstIn
                        )
                        drawContent()
                    }
                    .padding(start = fadeSize),
                text = showMoreText,
                maxLines = 1,
                color = MaterialTheme.colorScheme.primary,
                style = style.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}
