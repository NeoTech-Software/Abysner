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

package org.neotech.gradle

import java.util.Locale

fun String.capitalizeFirstCharacter(): String {
    return if (isEmpty()) {
        this
    } else {
        this[0].titlecase(Locale.getDefault()) + substring(1)
    }
}
