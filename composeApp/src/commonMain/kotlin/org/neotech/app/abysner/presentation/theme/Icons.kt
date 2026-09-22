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

package org.neotech.app.abysner.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

object IconSet {

    val share: ImageVector @Composable get() = shareIcon()

    val back: ImageVector @Composable get() = backIcon()

    val more: ImageVector @Composable get() = moreIcon()

    val settings: ImageVector @Composable get() = settingsIcon()

    val delete: ImageVector @Composable get() = deleteIcon()

    val info: ImageVector @Composable get() = infoIcon()
}

@Composable
internal expect fun backIcon(): ImageVector

@Composable
internal expect fun moreIcon(): ImageVector

@Composable
internal expect fun shareIcon(): ImageVector

@Composable
internal expect fun settingsIcon(): ImageVector

@Composable
internal expect fun deleteIcon(): ImageVector

@Composable
internal expect fun infoIcon(): ImageVector
