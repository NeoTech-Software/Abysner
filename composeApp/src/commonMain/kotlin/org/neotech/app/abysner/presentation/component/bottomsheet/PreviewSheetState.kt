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

package org.neotech.app.abysner.presentation.component.bottomsheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Creates a [SheetState] that starts fully expanded with no animation, for use in previews and
 * screenshot tests where the bottom sheet content needs to be visible immediately.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberExpandedSheetState(): SheetState {
    val density = LocalDensity.current
    return SheetState(
        skipPartiallyExpanded = true,
        initialValue = SheetValue.Expanded,
        positionalThreshold = { with(density) { 56.dp.toPx() } },
        velocityThreshold = { with(density) { 125.dp.toPx() } },
    )
}



