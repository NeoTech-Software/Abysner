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

package org.neotech.app.abysner.presentation.component.textfield

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

class SuffixVisualTransformation(private val suffix: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {

        val result = text + buildAnnotatedString {
            withStyle(style = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight(300))) {
                append(suffix)
            }
        }

        val textWithSuffixMapping = object : OffsetMapping {

            override fun originalToTransformed(offset: Int): Int = offset

            override fun transformedToOriginal(offset: Int): Int = when {
                text.isEmpty() -> 0
                offset >= text.length -> text.length
                else -> offset
            }
        }

        return TransformedText(result, textWithSuffixMapping)
    }
}
