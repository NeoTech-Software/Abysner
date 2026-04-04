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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Lock
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.model.markdownPadding
import org.jetbrains.compose.resources.painterResource
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.model.PlannedCylinderModel
import org.neotech.app.abysner.domain.utilities.DecimalFormat
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
    var showLockedExplanation by remember { mutableStateOf(false) }

    if (showLockedExplanation) {
        GasInUseDialog(onDismiss = { showLockedExplanation = false })
    }

    Card(modifier = modifier) {
        Column(modifier = Modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleLarge,
                text = "Gas & cylinders"
            )

            gases.forEach { availableGas ->
                CylinderListItemComponent(
                    modifier = Modifier.clickable {
                        onEditCylinder(availableGas.cylinder)
                    },
                    isChecked = availableGas.isChecked,
                    isLocked = availableGas.isLocked,
                    cylinder = availableGas.cylinder,
                    onDelete = {
                        onRemoveCylinder(availableGas.cylinder)
                    },
                    onChecked = { _, isCheckedChanged ->
                        onCylinderChecked(availableGas.cylinder, isCheckedChanged)
                    },
                    onLockedClick = {
                        showLockedExplanation = true
                    }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp)
            ) {
                val message = if (gases.isEmpty()) {
                    "Add at least one cylinder to start planning your dive."
                } else {
                    "Checked cylinders are offered to the decompression algorithm for ascent planning."
                }
                Text(
                    modifier = Modifier.weight(1f).padding(end = 16.dp),
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic)
                )
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
private fun GasInUseDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gas in use") },
        text = {
            Markdown(
                modifier = Modifier.fillMaxWidth(),
                content = """
                    This cylinder's gas is **used in the dive profile** and cannot be unchecked or removed.
                    
                    **To free it up:**
                    - Remove all segments using this gas.
                    - Switch those segments to a different gas.
                    - Add an additional cylinder with the same gas.
                """.trimIndent(),
                colors = markdownColor(),
                padding = markdownPadding(
                    list = 0.dp,
                    block = 4.dp,
                    listItemTop = 1.dp,
                    listItemBottom = 1.dp,
                ),
                typography = markdownTypography(),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
fun CylinderListItemComponent(
    modifier: Modifier = Modifier,
    cylinder: Cylinder,
    isChecked: Boolean,
    isLocked: Boolean,
    onDelete: (cylinder: Cylinder) -> Unit = {},
    onChecked: (cylinder: Cylinder, isChecked: Boolean) -> Unit = { _, _ -> },
    onLockedClick: () -> Unit = {},
) {
    val cylinderSuffix = " - ${DecimalFormat.format(0, cylinder.pressure)} bar (${DecimalFormat.format(1, cylinder.waterVolume)} l)"

    Row(
        modifier = modifier.padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onLockedClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Gas in use",
                        modifier = Modifier.alpha(0.38f)
                    )
                }
            } else {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { onChecked(cylinder, it) }
                )
            }
        }

        TextWithStartIcon(
            text = "${cylinder.gas}$cylinderSuffix",
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            icon = painterResource(Res.drawable.ic_outline_propane_tank_24)
        )

        IconButton(
            enabled = !isLocked,
            modifier = Modifier.ifTrue(isLocked) { invisible() },
            onClick = { onDelete(cylinder) }
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete cylinder")
        }
    }
}

@Preview
@Composable
private fun GasInUseDialogPreview() {
    AbysnerTheme {
        GasInUseDialog(onDismiss = {})
    }
}

@Preview
@Composable
fun CylinderListItemComponentPreview() {
    AbysnerTheme {
        CylinderListItemComponent(
            isChecked = true,
            isLocked = false,
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
                    isLocked = true,
                    cylinder = Cylinder(gas = Gas.Air, 232.0, 12.0)
                ),
                PlannedCylinderModel(
                    isChecked = true,
                    isLocked = false,
                    cylinder = Cylinder(gas = Gas.Nitrox50, 207.0, 11.1)
                ),
                PlannedCylinderModel(
                    isChecked = false,
                    isLocked = false,
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
