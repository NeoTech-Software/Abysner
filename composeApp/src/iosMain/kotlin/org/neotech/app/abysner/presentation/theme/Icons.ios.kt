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
import abysner.composeapp.generated.resources.ic_outline_chevron_back_24_ios
import abysner.composeapp.generated.resources.ic_outline_delete_24_ios
import abysner.composeapp.generated.resources.ic_outline_info_24_ios
import abysner.composeapp.generated.resources.ic_outline_more_horiz_24_ios
import abysner.composeapp.generated.resources.ic_outline_settings_24_ios
import abysner.composeapp.generated.resources.ic_outline_share_24_ios
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.vectorResource

@Composable
internal actual fun backIcon(): ImageVector = vectorResource(Res.drawable.ic_outline_chevron_back_24_ios)

@Composable
internal actual fun moreIcon(): ImageVector = vectorResource(Res.drawable.ic_outline_more_horiz_24_ios)

@Composable
internal actual fun shareIcon(): ImageVector = vectorResource(Res.drawable.ic_outline_share_24_ios)

@Composable
internal actual fun settingsIcon(): ImageVector = vectorResource(Res.drawable.ic_outline_settings_24_ios)

@Composable
internal actual fun deleteIcon(): ImageVector = vectorResource(Res.drawable.ic_outline_delete_24_ios)

@Composable
internal actual fun infoIcon(): ImageVector = vectorResource(Res.drawable.ic_outline_info_24_ios)
