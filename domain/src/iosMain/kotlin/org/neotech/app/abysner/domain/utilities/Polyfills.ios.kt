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

package org.neotech.app.abysner.domain.utilities

import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSUUID

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

actual class DecimalFormatter actual constructor(format: String) {

    private val decimalFormat = NSNumberFormatter().apply {
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


actual fun generateUUID(): String {
    return NSUUID().UUIDString()
}
