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

package org.neotech.app.abysner.presentation.component.core

import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Dp.toPx() = with(LocalDensity.current) { toPx() }

/**
 * Subtracts from this padding the spacing a component without a visible border already has by
 * means of [LocalMinimumInteractiveComponentSize] (for example an IconButton without an outline).
 * [actualComponentSize] is the visible size, such as the icon size.
 */
@Composable
fun Dp.withoutInteractiveSizeInset(actualComponentSize: Dp): Dp =
    this - (LocalMinimumInteractiveComponentSize.current - actualComponentSize).coerceAtLeast(0.dp) / 2
