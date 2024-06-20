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

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun getColorScheme(dynamicColor: Boolean, isDarkMode: Boolean): ColorScheme = when(isDarkMode) {
    true -> DarkColorScheme
    false -> LightColorScheme
}

@Composable
actual fun applyPlatformSpecificThemeConfiguration(colorScheme: ColorScheme, isDarkMode: Boolean) = Unit