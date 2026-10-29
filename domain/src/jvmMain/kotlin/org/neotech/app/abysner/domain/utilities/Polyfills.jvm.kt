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

package org.neotech.app.abysner.domain.utilities

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.UUID

actual object DecimalFormat {
    actual fun format(fractionDigits: Int, number: Number): String {
        @Suppress("RemoveRedundantQualifierName")
        val df = java.text.DecimalFormat()
        df.isGroupingUsed = false
        df.maximumFractionDigits = fractionDigits
        df.minimumFractionDigits = fractionDigits
        df.isDecimalSeparatorAlwaysShown = false
        return df.format(number)
    }
}

actual class DecimalFormatter actual constructor(format: String, locale: FormatLocale) {

    private val decimalFormat = DecimalFormat(
        format,
        DecimalFormatSymbols.getInstance(locale.platformLocale).apply { zeroDigit = '0' },
    )

    actual fun setFractionDigits(digits: Int) {
        decimalFormat.minimumFractionDigits = digits
        decimalFormat.maximumFractionDigits = digits
    }

    actual fun format(number: Number): String = decimalFormat.format(number)

    actual fun decimalSeparator(): Char = decimalFormat.decimalFormatSymbols.decimalSeparator
}

actual class FormatLocale(internal val platformLocale: Locale) {

    actual companion object {
        actual val Default: FormatLocale
            get() = FormatLocale(Locale.getDefault(Locale.Category.FORMAT))

        actual fun forLanguageTag(languageTag: String): FormatLocale =
            FormatLocale(Locale.forLanguageTag(languageTag))
    }
}

actual fun generateUUID(): String {
    return UUID.randomUUID().toString()
}
