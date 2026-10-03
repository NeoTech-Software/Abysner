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

import abysner.composeapp.generated.resources.Res
import abysner.composeapp.generated.resources.ic_outline_chevron_back_24_ios
import abysner.composeapp.generated.resources.ic_outline_delete_24_ios
import abysner.composeapp.generated.resources.ic_outline_info_24_ios
import abysner.composeapp.generated.resources.ic_outline_more_horiz_24_ios
import abysner.composeapp.generated.resources.ic_outline_settings_24_ios
import abysner.composeapp.generated.resources.ic_outline_share_24_android
import abysner.composeapp.generated.resources.ic_outline_share_24_ios
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.vectorResource

object IconSet {

    val share: ImageVector @Composable get() = shareIcon()

    val back: ImageVector @Composable get() = backIcon()

    val more: ImageVector @Composable get() = moreIcon()

    val settings: ImageVector @Composable get() = settingsIcon()

    val delete: ImageVector @Composable get() = deleteIcon()

    val info: ImageVector @Composable get() = infoIcon()
}

@Composable
private fun backIcon(): ImageVector = if (platform() == Platform.IOS) {
    vectorResource(Res.drawable.ic_outline_chevron_back_24_ios)
} else {
    Icons.AutoMirrored.Outlined.ArrowBack
}

@Composable
private fun moreIcon(): ImageVector = if (platform() == Platform.IOS) {
    vectorResource(Res.drawable.ic_outline_more_horiz_24_ios)
} else {
    Icons.Outlined.MoreVert
}

@Composable
private fun shareIcon(): ImageVector = if (platform() == Platform.IOS) {
    vectorResource(Res.drawable.ic_outline_share_24_ios)
} else {
    vectorResource(Res.drawable.ic_outline_share_24_android)
}

@Composable
private fun settingsIcon(): ImageVector = if (platform() == Platform.IOS) {
    vectorResource(Res.drawable.ic_outline_settings_24_ios)
} else {
    Icons.Outlined.Settings
}

@Composable
private fun deleteIcon(): ImageVector = if (platform() == Platform.IOS) {
    vectorResource(Res.drawable.ic_outline_delete_24_ios)
} else {
    Icons.Outlined.Delete
}

@Composable
private fun infoIcon(): ImageVector = if (platform() == Platform.IOS) {
    vectorResource(Res.drawable.ic_outline_info_24_ios)
} else {
    Icons.Outlined.Info
}
