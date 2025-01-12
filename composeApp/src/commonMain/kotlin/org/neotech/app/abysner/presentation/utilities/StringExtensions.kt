/*
 * Abysner - Dive planner
 * Copyright (C) 2025 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.utilities

fun String.removeNonAsciiDigits(): String {
    return filter { it in '0'..'9' }
}

fun String.removeLeadingZeros(): String {
    return trimStart('0')
}

inline fun CharSequence.trimStart(startIndex: Int, predicate: (Char) -> Boolean): CharSequence {
    if(startIndex >= this.length) {
        return this
    }

    for(index in startIndex..< this.length)
        if (!predicate(this[index]))
            return "${subSequence(0, startIndex)}${subSequence(index, length)}"

    return ""
}
