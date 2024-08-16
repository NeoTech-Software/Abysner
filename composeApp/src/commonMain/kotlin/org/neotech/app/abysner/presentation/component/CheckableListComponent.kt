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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun CheckableListItemComponent(
    modifier: Modifier = Modifier,
    checked: Boolean = true,
    enabled: Boolean = true,
    onCheckedChanged: (isChecked: Boolean) -> Unit = {},
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Checkbox(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChanged
            )
        }
        content()
    }
}
