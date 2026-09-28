/*
 * Abysner - Dive planner
 * Copyright (C) 2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.component.appbar

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.neotech.app.abysner.presentation.theme.IconSet

@Composable
fun DetailScreenTopAppBar(
    title: String,
    showBackButton: Boolean,
    onNavigateUp: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.background) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                if (showBackButton) {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = IconSet.back,
                            contentDescription = "Back"
                        )
                    }
                }
            },
            actions = actions,
        )
    }
}
