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

package org.neotech.app.abysner.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun <T : PathwayButtonItem> PathwayButtonsComponent(
    modifier: Modifier = Modifier,
    selectedButton: Int,
    buttonLabels: List<T>,
    onClick: (Int, T) -> Unit,
    onAddClicked: () -> Unit = {},
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()).then(modifier),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {

        val lineColor = MaterialTheme.colorScheme.primary
        val lineColorInactive = MaterialTheme.colorScheme.outlineVariant

        buttonLabels.forEachIndexed { index, label ->

            if (selectedButton == index) {
                Button(
                    onClick = { onClick(index, label) },
                ) {
                    Text(label.buttonLabel)
                }
            } else {
                OutlinedButton(
                    onClick = { onClick(index, label) },
                ) {
                    Text(label.buttonLabel)
                }
            }
            if (index < buttonLabels.size - 1) {

                val textMeasurer = rememberTextMeasurer()

                val measuredText = textMeasurer.measure(
                    text = label.nextConnectorLabel,
                    style = MaterialTheme.typography.labelSmall,
                )

                val textColor = LocalTextStyle.current.color

                val width = with(LocalDensity.current) {
                    measuredText.size.width.toDp() + 8.dp
                }

                val connectorColor = if (index == selectedButton - 1) {
                    lineColor
                } else {
                    lineColorInactive
                }

                Canvas(modifier = Modifier.width(width)) {
                    drawLine(
                        color = connectorColor,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 3.dp.toPx()
                    )
                    drawText(
                        textLayoutResult = measuredText,
                        color = textColor,
                        topLeft = Offset(
                            x = (size.width - measuredText.size.width) / 2,
                            y = (size.height / 2) - measuredText.size.height - 2.dp.toPx()
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        OutlinedButton(
            modifier = Modifier.widthIn(min = 0.dp).requiredSizeIn(
                minWidth = 0.dp,
                minHeight = 0.dp,
                maxWidth = ButtonDefaults.MinHeight,
                maxHeight = ButtonDefaults.MinHeight,
            ),
            border = null,
            contentPadding = PaddingValues(0.dp),
            onClick = onAddClicked,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add, contentDescription = "Add another dive"
            )
        }
    }
}

interface PathwayButtonItem {
    val buttonLabel: String
    val nextConnectorLabel: String
}

@Preview
@Composable
fun PathwayButtonsComponentPreview() {
    Surface {
        PathwayButtonsComponent(
            selectedButton = 1,
            buttonLabels = listOf(
                DefaultPathwayButtonItem("Dive 1", "1:40"),
                DefaultPathwayButtonItem("Dive 2", "3:21"),
                DefaultPathwayButtonItem("Dive 3", "1:21")
            ),
            onClick = { index, label ->
                println("Button ${label.buttonLabel} ($index) clicked")
            },
            onAddClicked = {
                println("Add button clicked")
            }
        )
    }
}

data class DefaultPathwayButtonItem(
    override val buttonLabel: String,
    override val nextConnectorLabel: String
) : PathwayButtonItem
