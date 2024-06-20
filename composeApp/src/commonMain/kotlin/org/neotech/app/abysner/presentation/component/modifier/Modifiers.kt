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

package org.neotech.app.abysner.presentation.component.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent

fun Modifier.ifTrue(value: Boolean, block: Modifier.() -> Modifier): Modifier {
    return if(value) {
        block()
    } else {
        this
    }
}

fun Modifier.invisible(): Modifier {
    return drawWithContent {  }
}