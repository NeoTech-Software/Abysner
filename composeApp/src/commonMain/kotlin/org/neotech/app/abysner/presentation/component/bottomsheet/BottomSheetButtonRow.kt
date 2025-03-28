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

package org.neotech.app.abysner.presentation.component.bottomsheet

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BottomSheetButtonRow(
    modifier: Modifier = Modifier,
    primaryLabel: String,
    primaryEnabled: Boolean = true,
    secondaryLabel: String? = null,
    secondaryEnabled: Boolean = true,
    onSecondary: () -> Unit = {},
    onPrimary: () -> Unit,
) {
    Row(modifier = modifier) {
        if (secondaryLabel != null) {
            TextButton(
                enabled = secondaryEnabled,
                modifier = Modifier.weight(1f),
                onClick = onSecondary,
            ) {
                Text(secondaryLabel)
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        Button(
            enabled = primaryEnabled,
            modifier = Modifier.weight(1f),
            onClick = onPrimary,
        ) {
            Text(primaryLabel)
        }
    }
}
