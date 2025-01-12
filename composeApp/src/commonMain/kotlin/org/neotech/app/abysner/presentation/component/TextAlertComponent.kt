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

package org.neotech.app.abysner.presentation.component

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.unit.dp
import org.neotech.app.abysner.presentation.component.core.toPx
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.presentation.theme.onWarning
import org.neotech.app.abysner.presentation.theme.warning
import kotlin.math.min

enum class AlertSeverity {
    NONE,
    POSITIVE,
    WARNING,
    ERROR
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun TextAlert(
    modifier: Modifier = Modifier,
    text: String,
    alertSeverity: AlertSeverity = AlertSeverity.NONE,
) {
    TextAlert(
        modifier = modifier,
        text = AnnotatedString(text),
        alertSeverity = alertSeverity
    )
}

/**
 * Text composable that has the ability to draw a rounded corner background behind the text in
 * case a [AlertSeverity] is given that is anything other then [AlertSeverity.NONE].
 *
 * It only draws behind the text, even if the view is wider then the text, in the future I may want
 * to extend this to support all kinds of custom spans. Currently it is quite limited, as it does
 * not properly support multiple lines (it forces a single line).
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun TextAlert(
    modifier: Modifier = Modifier,
    text: AnnotatedString,
    textStyle: TextStyle = LocalTextStyle.current,
    alertSeverity: AlertSeverity = AlertSeverity.NONE,
) {
    val annotatedString = buildAnnotatedString {
        withAnnotation("alert", "ignored") {
            append(text)
        }
    }

    val (textColor, backgroundColor) = when(alertSeverity) {
        AlertSeverity.NONE -> Color.Unspecified to Color.Unspecified
        AlertSeverity.POSITIVE -> MaterialTheme.colorScheme.onPrimary to MaterialTheme.colorScheme.primary
        AlertSeverity.WARNING -> MaterialTheme.colorScheme.onWarning to MaterialTheme.colorScheme.warning
        AlertSeverity.ERROR -> MaterialTheme.colorScheme.onError to MaterialTheme.colorScheme.error
    }

    var onDraw: DrawScope.() -> Unit by remember { mutableStateOf({}) }

    val horizontalPaddingAndRadius = 4.dp.toPx()

    Text(
        color = textColor,
        maxLines = 1,
        style = textStyle,
        modifier = modifier.drawBehind { onDraw() },
        text = annotatedString,
        onTextLayout = { layoutResult ->
            if (backgroundColor.isUnspecified) {
                onDraw = {}
                return@Text
            }
            val annotation =
                annotatedString.getStringAnnotations("alert", 0, annotatedString.length)
                    .firstOrNull()
            if (annotation == null) {
                onDraw = {}
            } else {
                // Clamp to lineCount - 1 (since offset is 0 based) this avoids
                // java.lang.IllegalArgumentException: lineIndex(1) is out of bounds [0, 1)
                //     at androidx.compose.ui.text.MultiParagraph.requireLineIndexInRange(MultiParagraph.kt:919)
                //     at androidx.compose.ui.text.MultiParagraph.getLineBottom(MultiParagraph.kt:828)
                //     at androidx.compose.ui.text.TextLayoutResult.getLineBottom(TextLayoutResult.kt:437)
                //
                val startLineNum = min(layoutResult.getLineForOffset(annotation.start), layoutResult.lineCount - 1)
                val endLineNum = min(layoutResult.getLineForOffset(annotation.end), layoutResult.lineCount - 1)
                val textBounds = Rect(
                    top = layoutResult.getLineTop(startLineNum),
                    bottom = layoutResult.getLineBottom(endLineNum),
                    left = layoutResult.getHorizontalPosition(annotation.start, true) - horizontalPaddingAndRadius,
                    right = layoutResult.getHorizontalPosition(annotation.end, true) + horizontalPaddingAndRadius
                )
                onDraw = {
                    drawRoundRect(
                        backgroundColor,
                        topLeft = textBounds.topLeft,
                        size = textBounds.size,
                        cornerRadius = CornerRadius(horizontalPaddingAndRadius, horizontalPaddingAndRadius)
                    )
                }
            }
        }
    )
}

@Composable
@Preview
private fun TextAlertPreview() {
    AbysnerTheme {
        TextAlert(
            text = "test",
            alertSeverity = AlertSeverity.ERROR,
        )
    }
}
