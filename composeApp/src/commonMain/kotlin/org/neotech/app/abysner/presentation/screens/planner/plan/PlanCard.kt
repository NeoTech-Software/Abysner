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

package org.neotech.app.abysner.presentation.screens.planner.plan

import abysner.composeapp.generated.resources.Res
import abysner.composeapp.generated.resources.ic_outline_propane_tank_24
import abysner.composeapp.generated.resources.ic_outline_timer_24
import abysner.composeapp.generated.resources.ic_outline_vertical_align_bottom_24
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.presentation.component.IconAndTextButton
import org.neotech.app.abysner.presentation.component.TextWithStartIcon
import org.neotech.app.abysner.presentation.theme.AbysnerTheme

@Composable
fun PlanSelectionCardComponent(
    modifier: Modifier = Modifier,
    segments: List<DiveProfileSection>,
    onAddSegment: () -> Unit,
    onRemoveSegment: (index: Int, segment: DiveProfileSection) -> Unit,
    onEditSegment: (index: Int, segment: DiveProfileSection) -> Unit
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleLarge,
                text = "Dive profile"
            )
            segments.forEachIndexed { index, diveSegment ->
                PlanListItemComponent(
                    modifier = Modifier.clickable {
                        onEditSegment(index, diveSegment)
                    },
                    diveProfileSection = diveSegment,
                    onDelete = {
                        onRemoveSegment(index, diveSegment)
                    }
                )
            }

            val planRunTime = segments.sumOf { it.duration }
            val planMaxDepth = segments.maxOfOrNull { it.depth } ?: 0

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Planned runtime: $planRunTime min",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Max depth: $planMaxDepth m",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconAndTextButton(
                    onClick = onAddSegment,
                    text = "Add",
                    imageVector = Icons.Outlined.Add,
                )
            }
        }
    }
}

@Composable
fun PlanListItemComponent(
    modifier: Modifier,
    diveProfileSection: DiveProfileSection = DiveProfileSection(10, 15, Gas.Air),
    onDelete: (diveProfileSection: DiveProfileSection) -> Unit = {},
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextWithStartIcon(
            modifier = Modifier.padding(start = 16.dp),
            text = "${diveProfileSection.depth} m",
            icon = painterResource(resource = Res.drawable.ic_outline_vertical_align_bottom_24)
        )
        TextWithStartIcon(
            modifier = Modifier.padding(start = 8.dp),
            text = "${diveProfileSection.duration} min",
            icon = painterResource(resource = Res.drawable.ic_outline_timer_24)
        )
        TextWithStartIcon(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
            text = "${diveProfileSection.gas}",
            icon = painterResource(resource = Res.drawable.ic_outline_propane_tank_24)
        )
        IconButton(onClick = {
            onDelete(diveProfileSection)
        }) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete gas")
        }
    }
}

//@Preview(widthDp = 500)
@Preview
@Composable
fun PlanSelectionCardComponentPreview() {
    AbysnerTheme {
        PlanSelectionCardComponent(
            segments = listOf(
                DiveProfileSection(5, 20, Gas.Air),
                DiveProfileSection(15, 15, Gas.Air),
                DiveProfileSection(30, 10, Gas.Air)
            ),
            onAddSegment = {},
            onRemoveSegment = { _, _ -> },
            onEditSegment = { _, _ -> }
        )
    }
}
