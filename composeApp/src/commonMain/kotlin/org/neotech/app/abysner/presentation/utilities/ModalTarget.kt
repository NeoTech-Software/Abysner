/*
 * Abysner - Dive planner
 * Copyright (C) 2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.utilities

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Represents the intent to show or hide a modal UI element to add or edit an object (usually a
 * bottom sheet or dialog).
 *
 * It models the intent to 3 different states:
 * - `null` modal is hidden
 * - [Add] modal is visible in add mode
 * - [Edit] modal is visible in edit mode, pre-populated with [Edit.value]
 */
sealed class ModalTarget<out T> {

    data object Add : ModalTarget<Nothing>()

    data class Edit<out T>(val value: T) : ModalTarget<T>()
}

@Composable
fun <T> rememberModalTarget(
    initial: ModalTarget<T>? = null,
): MutableState<ModalTarget<T>?> = remember { mutableStateOf(initial) }



