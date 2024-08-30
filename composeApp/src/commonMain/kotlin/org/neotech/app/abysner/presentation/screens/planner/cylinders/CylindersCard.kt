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

package org.neotech.app.abysner.presentation.screens.planner.cylinders

import abysner.composeapp.generated.resources.Res
import abysner.composeapp.generated.resources.ic_outline_propane_tank_24
import androidx.compose.desktop.ui.tooling.preview.Preview
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.model.PlannedCylinderModel
import org.neotech.app.abysner.domain.utilities.DecimalFormat
import org.neotech.app.abysner.presentation.component.CheckableListItemComponent
import org.neotech.app.abysner.presentation.component.IconAndTextButton
import org.neotech.app.abysner.presentation.component.TextWithStartIcon
import org.neotech.app.abysner.presentation.component.core.ifTrue
import org.neotech.app.abysner.presentation.component.core.invisible
import org.neotech.app.abysner.presentation.theme.AbysnerTheme

@Composable
fun CylinderSelectionCardComponent(
    modifier: Modifier = Modifier,
    gases: List<PlannedCylinderModel>,
    onAddCylinder: () -> Unit,
    onRemoveCylinder: (cylinder: Cylinder) -> Unit,
    onCylinderChecked: (cylinder: Cylinder, checked: Boolean) -> Unit,
    onEditCylinder: (cylinder: Cylinder) -> Unit
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleLarge,
                text = "Gas & cylinders"
            )

            val grouped = gases.groupBy { it.isInUse }
            val inUse = grouped[true] ?: emptyList()
            val available = grouped[false] ?: emptyList()

            if(inUse.isNotEmpty()) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    text = "In use"
                )
                inUse.forEach { availableGas ->
                    CylinderListItemComponent(
                        modifier = Modifier.clickable {
                            onEditCylinder(availableGas.cylinder)
                        },
                        isChecked = availableGas.isChecked,
                        isInUse = availableGas.isInUse,
                        cylinder = availableGas.cylinder,
                        onDelete = {
                            onRemoveCylinder(availableGas.cylinder)
                        },
                        onChecked = { _, isCheckedChanged ->
                            onCylinderChecked(availableGas.cylinder, isCheckedChanged)
                        }
                    )
                }
            }

            if(available.isNotEmpty()) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    text = "Available"
                )
                available.forEach { availableGas ->
                    CylinderListItemComponent(
                        modifier = Modifier.clickable {
                            onEditCylinder(availableGas.cylinder)
                        },
                        isChecked = availableGas.isChecked,
                        isInUse = availableGas.isInUse,
                        cylinder = availableGas.cylinder,
                        onDelete = {
                            onRemoveCylinder(availableGas.cylinder)
                        },
                        onChecked = { _, isCheckedChanged ->
                            onCylinderChecked(availableGas.cylinder, isCheckedChanged)
                        }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = "Note: Selected gases are available to the decompression algorithm.",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic)
                    )
                }
                IconAndTextButton(
                    onClick = onAddCylinder,
                    text = "Add",
                    imageVector = Icons.Outlined.Add,
                )
            }
        }
    }
}

@Composable
fun CylinderListItemComponent(
    modifier: Modifier = Modifier,
    cylinder: Cylinder,
    isChecked: Boolean,
    isInUse: Boolean,
    onDelete: (cylinder: Cylinder) -> Unit = {},
    onChecked: (cylinder: Cylinder, isChecked: Boolean) -> Unit = { _, _ -> },
) {
    CheckableListItemComponent(
        modifier = modifier,
        enabled = !isInUse,
        checked = isChecked,
        onCheckedChanged = { onChecked(cylinder, it) }
    ) {

        val cylinderSuffix = " - ${DecimalFormat.format(0, cylinder.pressure)} bar (${DecimalFormat.format(1, cylinder.waterVolume)} l)"

        TextWithStartIcon(
            text = "${cylinder.gas}$cylinderSuffix",
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            icon = painterResource(Res.drawable.ic_outline_propane_tank_24)
        )

        IconButton(
            enabled = !isInUse,
            modifier = Modifier.ifTrue(isInUse) {
                invisible()
            },
            onClick = { onDelete(cylinder) }
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete cylinder")
        }
    }
}

@Preview
@Composable
fun CylinderListItemComponentPreview() {
    AbysnerTheme {
        CylinderListItemComponent(
            isChecked = true,
            isInUse = false,
            cylinder = Cylinder.steel12Liter(Gas.Air),
            onDelete = {},
            onChecked = { _, _ -> },
        )
    }
}

@Preview
@Composable
fun CylinderSelectionCardComponentPreview() {
    AbysnerTheme {
        CylinderSelectionCardComponent(
            gases = listOf(
                PlannedCylinderModel(
                    isChecked = true,
                    isInUse = true,
                    cylinder = Cylinder(gas = Gas.Air, 232.0, 12.0)
                ),
                PlannedCylinderModel(
                    isChecked = true,
                    isInUse = false,
                    cylinder = Cylinder(gas = Gas.Nitrox50, 207.0, 11.1)
                ),
                PlannedCylinderModel(
                    isChecked = false,
                    isInUse = false,
                    cylinder = Cylinder(gas = Gas.Nitrox80, 207.0, 9.0)
                )
            ),
            onAddCylinder = {},
            onRemoveCylinder = {},
            onCylinderChecked = { _, _ -> },
            onEditCylinder = {}
        )
    }
}
