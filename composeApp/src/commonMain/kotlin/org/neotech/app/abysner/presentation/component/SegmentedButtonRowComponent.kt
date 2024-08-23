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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Stable
class SingleChoiceSegmentedButtonRowState(initialSelectedIndex: Int) {
    var selectedIndex: Int by mutableIntStateOf(initialSelectedIndex)
}

@Composable
fun rememberSingleChoiceSegmentedButtonRowState(initialSelectedIndex: Int): SingleChoiceSegmentedButtonRowState {
    return remember {
        SingleChoiceSegmentedButtonRowState(initialSelectedIndex)
    }
}

@Composable
fun <T> SingleChoiceSegmentedButtonRow(
    modifier: Modifier = Modifier,
    items: List<T>,
    singleChoiceSegmentedButtonRowState: SingleChoiceSegmentedButtonRowState = rememberSingleChoiceSegmentedButtonRowState(0),
    onClick: (item: T, index: Int) -> Unit =  { _, _ -> },
    label: @Composable (item: T, index: Int) -> Unit
) {
    androidx.compose.material3.SingleChoiceSegmentedButtonRow(
        modifier = modifier
    ) {
        items.forEachIndexed { index, item ->
            SegmentedButton(
                selected = index == singleChoiceSegmentedButtonRowState.selectedIndex,
                onClick = {
                    singleChoiceSegmentedButtonRowState.selectedIndex = index
                    onClick(item, index)
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
fun SingleChoiceSegmentedButtonRowPreview() {
    SingleChoiceSegmentedButtonRow(
        items = listOf("All", "Basics"),
        onClick = { _, _ ->

        }
    ) { item, _ ->
        Text(text = item)
    }
}
