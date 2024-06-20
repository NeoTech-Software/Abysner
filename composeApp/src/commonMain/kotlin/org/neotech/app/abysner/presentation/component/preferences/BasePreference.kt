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

package org.neotech.app.abysner.presentation.component.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun PreferenceSubTitle() {
    Surface {
        SettingsSubTitle(
            subTitle = "Sub-title"
        )
    }
}

@Composable
fun SettingsSubTitle(
    modifier: Modifier = Modifier,
    subTitle: String,
) {
    Text(
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
        text = subTitle,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Preview
@Composable
private fun BaseTextPreferencePreview() {
    Surface {
        BaseTextPreference(
            label = "Title",
            description = "Description",
            value = "Value"
        ) {

        }
    }
}

@Composable
internal fun BaseTextPreference(
    modifier: Modifier = Modifier,
    label: String,
    description: String,
    value: String? = null,
    onClick: () -> Unit
) {
    BasicPreference(
        modifier = modifier.clickable { onClick() },
        label = label,
        value = description,
        hideDivider = true,
        action = if (value != null) {
            {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        } else {
            null
        }
    )
}

@Composable
internal fun BasicPreference(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    hideDivider: Boolean,
    action: @Composable() (RowScope.() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 64.dp)
            .height(IntrinsicSize.Min)
            .then(modifier)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        if (action != null) {
            if (!hideDivider) {
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp, vertical = 0.dp)
                )
            } else {
                Spacer(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(16.dp)
                )
            }
            action()
        }
    }
}
