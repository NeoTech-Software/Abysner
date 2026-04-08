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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
    vertical: Boolean = false,
    selectedButton: Int,
    buttonLabels: List<T>,
    onClick: (Int, T) -> Unit,
    onAddClicked: () -> Unit = {},
) {
    val buttonModifier = if (vertical) {
        Modifier.fillMaxWidth()
    } else {
        Modifier
    }

    if (vertical) {
        Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
            PathwayButtonsList(
                selectedButton = selectedButton,
                buttonLabels = buttonLabels,
                onClick = onClick,
                onAddClicked = onAddClicked,
                buttonModifier = buttonModifier,
                connector = { label, isActive -> VerticalConnector(label, isActive) },
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .then(modifier),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PathwayButtonsList(
                selectedButton = selectedButton,
                buttonLabels = buttonLabels,
                onClick = onClick,
                onAddClicked = onAddClicked,
                connector = { label, isActive -> HorizontalConnector(label, isActive) },
            )
        }
    }
}

@Composable
private fun <T : PathwayButtonItem> PathwayButtonsList(
    selectedButton: Int,
    buttonLabels: List<T>,
    onClick: (Int, T) -> Unit,
    onAddClicked: () -> Unit = {},
    buttonModifier: Modifier = Modifier,
    connector: @Composable (label: String, isActive: Boolean) -> Unit,
) {
    buttonLabels.forEachIndexed { index, label ->
        SelectableButton(
            index = index,
            label = label,
            isSelected = index == selectedButton,
            modifier = buttonModifier,
            onClick = onClick,
        )
        if (index < buttonLabels.size - 1) {
            connector(label.nextConnectorLabel, index == selectedButton - 1)
        }
    }

    Spacer(modifier = Modifier.size(4.dp))
    TextButton(modifier = buttonModifier, onClick = onAddClicked) {
        Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
        Text(modifier = Modifier.padding(start = ButtonDefaults.IconSpacing), text = "Add dive")
    }
}

@Composable
private fun <T : PathwayButtonItem> SelectableButton(
    index: Int,
    label: T,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (Int, T) -> Unit,
) {
    if (isSelected) {
        Button(modifier = modifier, onClick = { onClick(index, label) }) {
            Text(label.buttonLabel)
        }
    } else {
        OutlinedButton(modifier = modifier, onClick = { onClick(index, label) }) {
            Text(label.buttonLabel)
        }
    }
}

@Composable
private fun HorizontalConnector(label: String, isActive: Boolean) {
    val connectorColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val textMeasurer = rememberTextMeasurer()
    val measuredText = textMeasurer.measure(
        text = label,
        style = MaterialTheme.typography.labelSmall,
    )
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val width = with(LocalDensity.current) { measuredText.size.width.toDp() + 24.dp }

    Canvas(modifier = Modifier.width(width)) {
        drawLine(
            color = connectorColor,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 3.dp.toPx(),
        )
        drawText(
            textLayoutResult = measuredText,
            color = textColor,
            topLeft = Offset(
                x = (size.width - measuredText.size.width) / 2,
                y = size.height / 2 - measuredText.size.height - 2.dp.toPx(),
            ),
        )
    }
}

@Composable
private fun VerticalConnector(label: String, isActive: Boolean) {
    val connectorColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val textMeasurer = rememberTextMeasurer()
    val measuredText = textMeasurer.measure(
        text = label,
        style = MaterialTheme.typography.labelSmall,
    )
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val connectorHeight = with(LocalDensity.current) {
        measuredText.size.height.toDp() + 32.dp
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(connectorHeight)) {
        val lineX = size.width / 2f
        // This is a bit hacky, but the buttons used have some invisible touch-target padding on
        // the bottom and top, so we extend the lines 4dp in both directions.
        // (minimumInteractiveComponentSize pads (48-40) / 2 = 4 dp per side)
        val buttonTouchTargetOverhang = 4.dp.toPx()
        drawLine(
            color = connectorColor,
            start = Offset(lineX, -buttonTouchTargetOverhang),
            end = Offset(lineX, size.height + buttonTouchTargetOverhang),
            strokeWidth = 3.dp.toPx(),
        )
        drawText(
            textLayoutResult = measuredText,
            color = textColor,
            topLeft = Offset(
                x = lineX + 6.dp.toPx(),
                y = (size.height - measuredText.size.height) / 2f,
            ),
        )
    }
}

interface PathwayButtonItem {
    val buttonLabel: String
    val nextConnectorLabel: String
}

@Immutable
data class DefaultPathwayButtonItem(
    override val buttonLabel: String,
    override val nextConnectorLabel: String,
) : PathwayButtonItem

@Preview(widthDp = 500)
@Composable
private fun HorizontalPathwayButtonsComponentPreview() {
    Surface {
        PathwayButtonsComponent(
            vertical = false,
            selectedButton = 1,
            buttonLabels = listOf(
                DefaultPathwayButtonItem("Dive 1", "1:40"),
                DefaultPathwayButtonItem("Dive 2", "3:21"),
                DefaultPathwayButtonItem("Dive 3", "1:21"),
            ),
            onClick = { _, _ -> },
            onAddClicked = {},
        )
    }
}

@Preview(widthDp = 180)
@Composable
private fun VerticalPathwayButtonsComponentPreview() {
    Surface {
        PathwayButtonsComponent(
            vertical = true,
            selectedButton = 1,
            buttonLabels = listOf(
                DefaultPathwayButtonItem("Dive 1", "1:40"),
                DefaultPathwayButtonItem("Dive 2", "3:21"),
                DefaultPathwayButtonItem("Dive 3", "1:21"),
            ),
            onClick = { _, _ -> },
            onAddClicked = {},
        )
    }
}
