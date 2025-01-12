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

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.AnnotatedString.Builder
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

internal fun Builder.appendBold(
    text: String,
) {
    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
        append(text)
    }
}

internal fun Builder.appendBoldLine(
    text: String,
) {
    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
        appendLine(text)
    }
}


internal inline fun <R: Any> AnnotatedString.Builder.appendBulletPoint(crossinline block: AnnotatedString.Builder.() -> R) {
    withStyle(style = ParagraphStyle(textIndent = TextIndent(firstLine = 4.sp, restLine = 10.sp))) {
        append("\u2022 ")
        block()
    }
}

internal fun CharSequence.toAnnotatedString(): AnnotatedString {
    return if(this is AnnotatedString) {
        return this
    } else {
        AnnotatedString(this.toString())
    }
}
