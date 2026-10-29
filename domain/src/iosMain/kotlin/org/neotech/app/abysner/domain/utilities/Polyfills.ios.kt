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

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSUUID
import platform.Foundation.canonicalLocaleIdentifierFromString
import platform.Foundation.componentsFromLocaleIdentifier
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier
import platform.Foundation.localeIdentifierFromComponents

actual object DecimalFormat {
    actual fun format(fractionDigits: Int, number: Number): String {
        val formatter = NSNumberFormatter()
        formatter.minimumFractionDigits = fractionDigits.toULong()
        formatter.maximumFractionDigits = fractionDigits.toULong()
        formatter.numberStyle = 1u //Decimal
        formatter.usesGroupingSeparator = false
        return formatter.stringFromNumber(number as NSNumber)!!
    }
}

actual class DecimalFormatter actual constructor(format: String, locale: FormatLocale) {

    private val decimalFormat = NSNumberFormatter().apply {
        setLocale(locale.platformLocale.withLatinDigits())
        positiveFormat = format
        negativeFormat = format
    }

    actual fun setFractionDigits(digits: Int) {
        decimalFormat.minimumFractionDigits = digits.toULong()
        decimalFormat.maximumFractionDigits = digits.toULong()
    }

    actual fun format(number: Number): String = decimalFormat.stringFromNumber(number as NSNumber)!!

    actual fun decimalSeparator(): Char = decimalFormat.decimalSeparator.first()
}

private fun NSLocale.withLatinDigits(): NSLocale {
    val components = NSLocale.componentsFromLocaleIdentifier(localeIdentifier) + ("numbers" to "latn")
    return NSLocale(localeIdentifier = NSLocale.localeIdentifierFromComponents(components))
}

actual class FormatLocale(internal val platformLocale: NSLocale) {

    actual companion object {
        actual val Default: FormatLocale
            get() = FormatLocale(NSLocale.currentLocale)

        actual fun forLanguageTag(languageTag: String): FormatLocale =
            FormatLocale(NSLocale(localeIdentifier = NSLocale.canonicalLocaleIdentifierFromString(languageTag)))
    }
}

actual fun generateUUID(): String {
    return NSUUID().UUIDString()
}
