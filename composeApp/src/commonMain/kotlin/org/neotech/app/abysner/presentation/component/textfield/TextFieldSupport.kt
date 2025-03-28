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

package org.neotech.app.abysner.presentation.component.textfield

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.neotech.app.abysner.presentation.component.core.ifTrue
import org.neotech.app.abysner.presentation.component.core.invisible

/**
 * Reusable supporting text composable for text input fields. Shows [message] in the error color,
 * or an invisible placeholder of the same height when [message] is `null`, preventing the
 * layout from jumping as the error appears and disappears.
 */
@Composable
fun ErrorSupportingText(message: String?) {
    Text(
        modifier = Modifier.ifTrue(message == null) { invisible() },
        text = message ?: "Placeholder",
        color = MaterialTheme.colorScheme.error,
    )
}

fun defaultInputFieldLabel(text: String?): (@Composable () -> Unit)? = text?.let {
    {
        Text(
            text = it,
            style = MaterialTheme.typography.bodyLarge.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
            )
        )
    }
}
