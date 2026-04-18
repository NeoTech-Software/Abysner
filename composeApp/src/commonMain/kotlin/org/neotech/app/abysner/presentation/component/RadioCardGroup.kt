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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.neotech.app.abysner.presentation.theme.AbysnerTheme

@Composable
fun RadioCardGroup(
    modifier: Modifier = Modifier,
    items: ImmutableList<RadioCardItem>,
    selectedIndex: Int,
    onSelectionChanged: (index: Int) -> Unit,
) {
    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEachIndexed { index, item ->
            RadioCard(
                title = item.title,
                description = item.description,
                selected = index == selectedIndex,
                onClick = { onSelectionChanged(index) },
            )
        }
    }
}

@Composable
private fun RadioCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) { MaterialTheme.colorScheme.primary } else { MaterialTheme.colorScheme.outline }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            ),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Radio button itself is not clickable and purely a visual indicator
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                RadioButton(selected = selected, onClick = null)
            }
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview
@Composable
private fun RadioCardGroupPreview() {
    AbysnerTheme {
        var selected by remember { mutableIntStateOf(0) }
        RadioCardGroup(
            modifier = Modifier.padding(16.dp),
            items = persistentListOf(
                RadioCardItem(
                    title = "Open Circuit",
                    description = "Recreational and multi-gas technical diving, with reserve gas planning.",
                ),
                RadioCardItem(
                    title = "Closed Circuit Rebreather",
                    description = "Rebreather diving with configurable high/low setpoints and bail-out planning.",
                ),
            ),
            selectedIndex = selected,
            onSelectionChanged = { selected = it },
        )
    }
}

data class RadioCardItem(
    val title: String,
    val description: String,
)
