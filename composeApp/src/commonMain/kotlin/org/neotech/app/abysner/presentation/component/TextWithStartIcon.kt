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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun TextWithStartIcon(
    modifier: Modifier = Modifier,
    text: String,
    icon: Painter,
    textStyle: TextStyle = LocalTextStyle.current,
) {
    Row(modifier = modifier) {
        if(text.isNotEmpty()) {
            Icon(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(16.dp),
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        Text(
            style = textStyle,
            text = text,
            modifier = Modifier
                .padding(start = 4.dp),

            )
    }
}
