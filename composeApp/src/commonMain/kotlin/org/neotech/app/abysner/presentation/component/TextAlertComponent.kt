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
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.unit.dp
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.presentation.theme.onWarning
import org.neotech.app.abysner.presentation.theme.warning
import org.neotech.app.abysner.presentation.utilities.toPx


enum class AlertSeverity {
    NONE,
    WARNING,
    ERROR
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
    text: String,
    alertSeverity: AlertSeverity = AlertSeverity.NONE,
) {
    val annotatedString = buildAnnotatedString {
        withAnnotation("alert", "ignored") {
            append(text)
        }
    }

    val (textColor, backgroundColor) = when(alertSeverity) {
        AlertSeverity.NONE -> Color.Unspecified to Color.Unspecified
        AlertSeverity.WARNING -> MaterialTheme.colorScheme.onWarning to MaterialTheme.colorScheme.warning
        AlertSeverity.ERROR -> MaterialTheme.colorScheme.onError to MaterialTheme.colorScheme.error
    }

    var onDraw: DrawScope.() -> Unit by remember { mutableStateOf({}) }

    val horizontalPaddingAndRadius = 4.dp.toPx()

    Text(
        color = textColor,
        maxLines = 1,
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
                val startLineNum = layoutResult.getLineForOffset(annotation.start)
                val endLineNum = layoutResult.getLineForOffset(annotation.end)
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
