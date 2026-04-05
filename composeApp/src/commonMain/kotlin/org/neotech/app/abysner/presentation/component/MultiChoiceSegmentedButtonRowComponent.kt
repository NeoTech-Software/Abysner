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

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Stable
class MultiChoiceSegmentedButtonRowState(initialCheckedItemIndexes: ImmutableList<Int>) {
    var checkedItemIndexes: SnapshotStateList<Int> = initialCheckedItemIndexes.toMutableStateList()
}

@Composable
fun rememberMultiChoiceSegmentedButtonRowState(initialCheckedItemIndexes: ImmutableList<Int> = persistentListOf()): MultiChoiceSegmentedButtonRowState {
    return remember(initialCheckedItemIndexes) {
        MultiChoiceSegmentedButtonRowState(initialCheckedItemIndexes)
    }
}

@Composable
fun <T> MultiChoiceSegmentedButtonRow(
    modifier: Modifier = Modifier,
    items: ImmutableList<T>,
    multiChoiceSegmentedButtonRowState: MultiChoiceSegmentedButtonRowState = rememberMultiChoiceSegmentedButtonRowState(),
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
        items = persistentListOf("+3min", "+3m"),
        onChecked = { _, _, _ ->

        }
    ) { item, _ ->
        Text(text = item)
    }
}
