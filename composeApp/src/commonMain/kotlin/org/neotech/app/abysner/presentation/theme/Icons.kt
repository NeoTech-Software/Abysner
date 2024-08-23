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

package org.neotech.app.abysner.presentation.theme

import abysner.composeapp.generated.resources.Res
import abysner.composeapp.generated.resources.ic_outline_share_24_android
import abysner.composeapp.generated.resources.ic_outline_share_24_ios
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.vectorResource

object IconSet {

    val share: ImageVector
        @Composable
        get() = if(platform() == Platform.IOS) {
            vectorResource(Res.drawable.ic_outline_share_24_ios)
        } else {
            vectorResource(Res.drawable.ic_outline_share_24_android)
        }
}
