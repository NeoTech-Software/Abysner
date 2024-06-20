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

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun getColorScheme(dynamicColor: Boolean, isDarkMode: Boolean): ColorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (isDarkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    isDarkMode -> DarkColorScheme
    else -> LightColorScheme
}

@SuppressLint("ComposableNaming")
@Composable
actual fun applyPlatformSpecificThemeConfiguration(colorScheme: ColorScheme, isDarkMode: Boolean) {
    // val view = LocalView.current
    // if (!view.isInEditMode) {
    //     SideEffect {
    //         val window = (view.context as Activity).window
    //         window.statusBarColor = colorScheme.primary.toArgb()
    //         WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isDarkMode
    //     }
    // }
}
