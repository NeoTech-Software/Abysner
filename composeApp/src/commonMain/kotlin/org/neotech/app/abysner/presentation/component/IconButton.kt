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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.neotech.app.abysner.presentation.theme.AbysnerTheme


@Composable
@Preview
fun IconAndTextButtonPreview() {
    AbysnerTheme {
        IconAndTextButton(imageVector = Icons.Outlined.Add, text = "Add") {

        }
    }
}

@Composable
fun IconAndTextButton(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    text: String,
    outlined: Boolean = false,
    onClick: () -> Unit,
) {
    val content = @Composable {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
        Text(style = MaterialTheme.typography.bodyMedium, text = text)
    }

    if(!outlined) {
        Button(
            modifier = modifier,
            onClick = onClick,
            contentPadding = PaddingValues(start = 16.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)
        ) {
            content()
        }
    } else {
        OutlinedButton(
            modifier = modifier,
            onClick = onClick,
            contentPadding = PaddingValues(start = 16.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)
        ) {
            content()
        }
    }
}
