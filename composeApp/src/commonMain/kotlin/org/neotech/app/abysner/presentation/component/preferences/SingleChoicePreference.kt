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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialogCustomContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.neotech.app.abysner.domain.core.model.Salinity
import org.neotech.app.abysner.presentation.component.list.LazyColumnWithScrollIndicators
import org.neotech.app.abysner.utilities.toAnnotatedString

@Preview
@Composable
private fun SingleChoicePreferencePreview() {
    Surface {

        var index by mutableIntStateOf(0)

        SingleChoicePreference(
            label = "Salinity",
            description = "The type of water. Saltier water is heavier and increases pressure at depth.",
            items = Salinity.entries,
            selectedItemIndex = index,
            onItemPicked = {
                index = Salinity.entries.indexOf(it)
            },
        )
    }
}

@Composable
fun <T> SingleChoicePreference(
    modifier: Modifier = Modifier,
    label: String,
    description: String,
    selectedItemIndex: Int = 0,
    items: List<T>,
    itemToStringMapper: (T) -> CharSequence = { it.toString() },
    selectedItemToStringMapper: (T) -> CharSequence = itemToStringMapper,
    onItemPicked: (T) -> Unit,
) {

    var showDialog by remember {
        mutableStateOf(false)
    }

    if (showDialog) {
        SingleChoicePreferenceDialog(
            title = label,
            items = items,
            initialSelectedItemIndex = selectedItemIndex,
            onConfirmButtonClicked = { item, index ->
                if (item != null) {
                    onItemPicked(item)
                }
                showDialog = false
            },
            onCancelButtonClicked = { showDialog = false },
            onDismissRequest = { showDialog = false },
            itemToStringMapper = itemToStringMapper,
        )
    }

    val value = if (selectedItemIndex != -1) {
        selectedItemToStringMapper(items[selectedItemIndex])
    } else {
        null
    }

    BasicPreference(
        modifier = modifier.clickable {
            showDialog = true
        },
        label = label,
        value = description,
        hideDivider = true,
        action = if (value != null) {
            {
                Text(
                    text = value.toAnnotatedString(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        } else {
            null
        }
    )
}

@Composable
fun <T> SingleChoicePreferenceDialog(
    title: String,
    confirmButtonText: String = "OK",
    cancelButtonText: String = "Cancel",
    onConfirmButtonClicked: (T?, Int) -> Unit = { _, _ -> },
    onCancelButtonClicked: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
    initialSelectedItemIndex: Int = 0,
    items: List<T>,
    itemToStringMapper: (T) -> CharSequence = { it.toString() },
) {
    val selectedItemIndex: MutableState<Int> =
        remember(initialSelectedItemIndex) { mutableIntStateOf(initialSelectedItemIndex) }

    AlertDialogCustomContent(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                if (selectedItemIndex.value != -1) {
                    onConfirmButtonClicked(items[selectedItemIndex.value], selectedItemIndex.value)
                } else {
                    onConfirmButtonClicked(null, selectedItemIndex.value)
                }
            }) {
                Text(text = confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelButtonClicked) {
                Text(text = cancelButtonText)
            }
        },
        title = { Text(title) },
        content = {
            LazyColumnWithScrollIndicators {
                itemsIndexed(items) { index, item ->
                    Row(
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .clickable {
                                selectedItemIndex.value = index
                            }
                            .padding(start = 8.dp, end = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedItemIndex.value == index,
                            onClick = {
                                selectedItemIndex.value = index
                            }
                        )
                        Text(text = itemToStringMapper(item).toAnnotatedString())
                    }
                }
            }
        }
    )
}

@Preview
@Composable
fun SingleChoicePreferenceDialogPreview() {
    SingleChoicePreferenceDialog(
        title = "Salinity",
        items = Salinity.entries,
        itemToStringMapper = {
            "${it.humanReadableName} (${it.density} kg/m3)"
        }
    )
}
