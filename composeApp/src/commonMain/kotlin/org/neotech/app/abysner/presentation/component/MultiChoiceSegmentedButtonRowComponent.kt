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

package org.neotech.app.abysner.presentation.component

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier

@Stable
class MultiChoiceSegmentedButtonRowState(initialCheckedItemIndexes: Array<Int>) {
    var checkedItemIndexes: SnapshotStateList<Int> = mutableStateListOf(*initialCheckedItemIndexes)
}

@Composable
fun rememberMultiChoiceSegmentedButtonRowState(initialCheckedItemIndexes: Array<Int> = emptyArray()): MultiChoiceSegmentedButtonRowState {
    return remember {
        MultiChoiceSegmentedButtonRowState(initialCheckedItemIndexes)
    }
}

@Composable
fun <T> MultiChoiceSegmentedButtonRow(
    modifier: Modifier = Modifier,
    items: List<T>,
    multiChoiceSegmentedButtonRowState: MultiChoiceSegmentedButtonRowState = rememberMultiChoiceSegmentedButtonRowState(emptyArray()),
    onChecked: (item: T, index: Int, checked: Boolean) -> Unit =  { _, _, _ -> },
    label: @Composable (item: T, index: Int) -> Unit = { item, _ ->
        Text(text = item.toString(), maxLines = 1)
    }
) {
    androidx.compose.material3.MultiChoiceSegmentedButtonRow(
        modifier = modifier
    ) {
        items.forEachIndexed { index, item ->
            SegmentedButton(
                checked = index in multiChoiceSegmentedButtonRowState.checkedItemIndexes,
                onCheckedChange = {
                    if (it) {
                        multiChoiceSegmentedButtonRowState.checkedItemIndexes.add(index)
                    } else {
                        multiChoiceSegmentedButtonRowState.checkedItemIndexes.remove(index)
                    }
                    onChecked(item, index, it)
                },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size)
            ) {
                label(item, index)
            }
        }
    }
}

@Preview
@Composable
private fun MultiChoiceSegmentedButtonRowPreview() {
    MultiChoiceSegmentedButtonRow(
        items = listOf("+3min", "+3m"),
        onChecked = { _, _, _ ->

        }
    ) { item, _ ->
        Text(text = item)
    }
}
