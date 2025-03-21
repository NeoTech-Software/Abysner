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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun Table(
    modifier: Modifier = Modifier,
    header: (@Composable RowScope.() -> Unit)?,
    content: @Composable TableScope.() -> Unit
) {
    Column(modifier) {
        if (header != null) {
            TableHeader { header() }
            Divider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.height(1.dp))
        }

        val items = mutableStateListOf<RowComposable>()

        TableScopeInstance(items).apply {
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
                items.clear()
                content()
                items.forEach {
                    it()
                }
            }
        }
    }
}

typealias RowComposable = @Composable () -> Unit

internal class TableScopeInstance(private val items: SnapshotStateList<RowComposable>) :
    TableScope {

    @Composable
    override fun row(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
        val color = if (items.size % 2 == 1) {
            MaterialTheme.colorScheme.surfaceColorAtElevation(LocalAbsoluteTonalElevation.current + 2.dp)
        } else {
            Color.Transparent
        }
        val row: RowComposable = {
            Row(
                Modifier.background(color).then(modifier)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                content()
            }
        }
        items.add(row)
    }
}

interface TableScope {

    @Composable
    fun row(modifier: Modifier, content: @Composable RowScope.() -> Unit)

    @Composable
    fun row(content: @Composable RowScope.() -> Unit) = row(Modifier, content)
}

@Composable
private fun TableHeader(
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSecondaryContainer
    ),
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        CompositionLocalProvider(LocalTextStyle provides textStyle) {
            content()
        }
    }
}
