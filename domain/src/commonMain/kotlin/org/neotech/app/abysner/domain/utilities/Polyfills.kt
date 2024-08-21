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


fun Number.format(fractionDigits: Int) = DecimalFormat.format(fractionDigits, this)

expect object DecimalFormat {
    fun format(fractionDigits: Int, number: Number): String
}

expect class DecimalFormatter(format: String) {
    fun format(number: Number): String
    fun setFractionDigits(digits: Int)
    fun decimalSeparator(): Char
}

expect fun generateUUID(): String
