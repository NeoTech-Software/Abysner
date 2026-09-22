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

package org.neotech.app.abysner.presentation.theme

import abysner.composeapp.generated.resources.Res
import abysner.composeapp.generated.resources.ic_outline_share_24_android
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.vectorResource

@Composable
internal actual fun backIcon(): ImageVector = Icons.AutoMirrored.Outlined.ArrowBack

@Composable
internal actual fun moreIcon(): ImageVector = Icons.Outlined.MoreVert

@Composable
internal actual fun shareIcon(): ImageVector = vectorResource(Res.drawable.ic_outline_share_24_android)

@Composable
internal actual fun settingsIcon(): ImageVector = Icons.Outlined.Settings

@Composable
internal actual fun deleteIcon(): ImageVector = Icons.Outlined.Delete

@Composable
internal actual fun infoIcon(): ImageVector = Icons.Outlined.Info
