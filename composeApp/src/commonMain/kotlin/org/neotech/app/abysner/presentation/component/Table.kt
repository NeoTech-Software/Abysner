/*
 * Abysner - Dive planner
 * Copyright (C) 2024-2026 Neotech
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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A simple table. Use [TableScope.rows] / [TableScope.row] inside [content] to register
 * rows, following the same DSL pattern as [androidx.compose.foundation.lazy.LazyColumn].
 *
 * **Performance:** the [content] builder runs on every recomposition, but it is plain (non-
 * composable) code, no composition overhead per row during collection. Each row is identified
 * by its [key][TableScope.rows] so Compose can correctly handle additions, removals and
 * reorderings without recomposing unaffected rows.
 */
@Composable
fun Table(
    modifier: Modifier = Modifier,
    header: (@Composable RowScope.() -> Unit)? = null,
    striped: Boolean = true,
    defaultRowModifier: Modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    defaultHeaderModifier: Modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    content: TableScope.() -> Unit,
) {
    val rows = TableScopeInstance(defaultRowModifier).apply(content).rows

    Column(modifier) {
        if (header != null) {
            TableHeader(modifier = defaultHeaderModifier) { header() }
            HorizontalDivider()
        }
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
            rows.forEachIndexed { index, row ->
                // Fall back to index so keyless rows never share the same Compose slot.
                key(row.key ?: index) {
                    val rowModifier = if (striped && index % 2 == 1) {
                        Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    } else {
                        Modifier
                    }
                    Row(rowModifier.then(row.modifier)) {
                        row.content(this)
                    }
                }
            }
        }
    }
}

internal class TableRow(
    val key: Any?,
    val modifier: Modifier,
    val content: @Composable RowScope.() -> Unit,
)

internal class TableScopeInstance(private val defaultRowModifier: Modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) : TableScope {
    val rows = mutableListOf<TableRow>()

    override fun <T> rows(
        items: List<T>,
        key: ((T) -> Any)?,
        modifier: Modifier?,
        itemContent: @Composable RowScope.(T) -> Unit,
    ) {
        items.forEach { item ->
            rows.add(TableRow(
                key = key?.invoke(item),
                modifier = modifier ?: defaultRowModifier,
                content = { itemContent(item) },
            ))
        }
    }

    override fun <T> rowsIndexed(
        items: List<T>,
        key: ((Int, T) -> Any)?,
        modifier: Modifier?,
        itemContent: @Composable RowScope.(index: Int, item: T) -> Unit,
    ) {
        items.forEachIndexed { index, item ->
            rows.add(TableRow(
                key = key?.invoke(index, item),
                modifier = modifier ?: defaultRowModifier,
                content = { itemContent(index, item) },
            ))
        }
    }

    override fun row(key: Any?, modifier: Modifier?, content: @Composable RowScope.() -> Unit) {
        rows.add(TableRow(key = key, modifier = modifier ?: defaultRowModifier, content = content))
    }

    override fun <T> row(key: Any?, item: T, modifier: Modifier?, content: @Composable RowScope.(T) -> Unit) {
        rows.add(TableRow(key = key, modifier = modifier ?: defaultRowModifier, content = { content(item) }))
    }
}

interface TableScope {
    fun <T> rows(items: List<T>, key: ((T) -> Any)? = null, modifier: Modifier? = null, itemContent: @Composable RowScope.(T) -> Unit)

    fun <T> rowsIndexed(items: List<T>, key: ((Int, T) -> Any)? = null, modifier: Modifier? = null, itemContent: @Composable RowScope.(index: Int, item: T) -> Unit)

    fun row(key: Any? = null, modifier: Modifier? = null, content: @Composable RowScope.() -> Unit)

    fun <T> row(key: Any? = null, item: T, modifier: Modifier? = null, content: @Composable RowScope.(T) -> Unit)
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
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .then(modifier)
    ) {
        CompositionLocalProvider(LocalTextStyle provides textStyle) {
            content()
        }
    }
}
