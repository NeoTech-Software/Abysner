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

actual object DecimalFormat {
    actual fun format(fractionDigits: Int, number: Number): String {
        val df = java.text.DecimalFormat()
        df.isGroupingUsed = false
        df.maximumFractionDigits = fractionDigits
        df.minimumFractionDigits = fractionDigits
        df.isDecimalSeparatorAlwaysShown = false
        return df.format(number)
    }
}
