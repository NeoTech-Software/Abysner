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

package org.neotech.app.abysner.presentation.utilities

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * Improvised fix for: https://github.com/JetBrains/compose-multiplatform/issues/2852
 */
@Composable
fun PreviewWrapper(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalInspectionMode provides true) {
        content()
    }
}
