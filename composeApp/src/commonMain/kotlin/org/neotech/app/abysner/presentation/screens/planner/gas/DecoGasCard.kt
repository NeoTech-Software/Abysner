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

package org.neotech.app.abysner.presentation.screens.planner.gas

import androidx.compose.foundation.layout.Column
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
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.presentation.component.CheckableListItemComponent
import org.neotech.app.abysner.presentation.component.IconAndTextButton
import org.neotech.app.abysner.presentation.theme.AbysnerTheme

@Composable
fun DecoGasSelectionCardComponent(
    modifier: Modifier = Modifier,
    gases: List<Pair<Boolean, Gas>>,
    onAddGas: () -> Unit,
    onRemoveGas: (index: Int, gas: Gas) -> Unit,
    onGasChecked: (index: Int, gas: Gas, checked: Boolean) -> Unit
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleLarge,
                text = "Deco gases"
            )
            gases.forEachIndexed { index, (isChecked, gas) ->
                GasListItemComponent(isChecked, gas, onDelete = { onRemoveGas(index, gas) },
                    onChecked = { _, isCheckedChanged -> onGasChecked(index, gas, isCheckedChanged) })
            }
            IconAndTextButton(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .align(Alignment.End),
                onClick = onAddGas,
                text = "Add",
                imageVector = Icons.Outlined.Add,
            )
        }
    }
}

@Composable
fun GasListItemComponent(
    isChecked: Boolean = true,
    gas: Gas = Gas.Air,
    onDelete: (gas: Gas) -> Unit = {},
    onChecked: (gas: Gas, isChecked: Boolean) -> Unit = { _, _ -> },
) {
    CheckableListItemComponent(isChecked = isChecked, onCheckedChanged = { onChecked(gas, it) }) {
        Text(
            style = MaterialTheme.typography.bodyMedium, text = gas.toString(), modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        )
        IconButton(onClick = {
            onDelete(gas)
        }) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete gas")
        }
    }
}

// TODO Make Preview as soon as commonMain supports it
@Composable
fun DecoGasSelectionCardComponentPreview() {
    AbysnerTheme {
        DecoGasSelectionCardComponent(
            gases = listOf(
                true to Gas.Air,
                false to Gas.Nitrox32
            ),
            onAddGas = {},
            onRemoveGas = { _, _ -> },
            onGasChecked = { _, _, _ -> },
        )
    }
}
