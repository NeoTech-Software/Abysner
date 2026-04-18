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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.neotech.app.abysner.presentation.component.core.ifUnspecified
import org.neotech.app.abysner.presentation.theme.AbysnerTheme

enum class InfoPillSize(
    internal val minHeight: Dp,
    internal val horizontalPadding: Dp,
) {
    DEFAULT(minHeight = 40.dp, horizontalPadding = 16.dp),
    SMALL(minHeight = 24.dp, horizontalPadding = 12.dp),
}

@Composable
fun InfoPill(
    modifier: Modifier = Modifier,
    label: String?,
    value: String?,
    size: InfoPillSize = InfoPillSize.DEFAULT,
    containerColor: Color = Color.Unspecified,
    labelColor: Color = Color.Unspecified,
    valueColor: Color = Color.Unspecified,
) {
    val textStyle: TextStyle = when (size) {
        InfoPillSize.DEFAULT -> MaterialTheme.typography.labelMedium
        InfoPillSize.SMALL -> MaterialTheme.typography.labelSmall
    }
    Surface(
        modifier = modifier.heightIn(min = size.minHeight),
        color = containerColor.ifUnspecified(MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(100),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = size.horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            label?.let {
                Text(
                    text = it,
                    style = textStyle,
                    maxLines = 1,
                    color = labelColor.ifUnspecified(MaterialTheme.colorScheme.secondary),
                )
            }
            value?.let {
                Text(
                    text = it,
                    style = textStyle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = valueColor.ifUnspecified(MaterialTheme.colorScheme.onSecondaryContainer),
                )
            }
        }
    }
}

@Preview
@Composable
private fun InfoPillPreview() {
    AbysnerTheme {
        Surface {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoPill(label = "Diluent", value = "18/45")
                InfoPill(label = "Setpoint", value = "1.3 bar")
                InfoPill(label = "ppO2", value = "1.30")
            }
        }
    }
}

@Preview
@Composable
private fun InfoPillSmallPreview() {
    AbysnerTheme {
        Surface {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoPill(label = "Diluent", value = "18/45", size = InfoPillSize.SMALL)
                InfoPill(label = null, value = "+ bail-out", size = InfoPillSize.SMALL)
            }
        }
    }
}
