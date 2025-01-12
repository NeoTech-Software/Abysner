/*
 * Abysner - Dive planner
 * Copyright (C) 2025 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.theme

import androidx.compose.desktop.ui.tooling.preview.PreviewWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val LightColorScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

internal val DarkColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

@Composable
fun AbysnerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = getColorScheme(dynamicColor, darkTheme)

    applyPlatformSpecificThemeConfiguration(colorScheme, darkTheme)

    val customColors = if (darkTheme) {
        CustomColors(warning = warningDark, onWarning = onWarningDark)
    } else {
        CustomColors(warning = warningLight, onWarning = onWarningLight)
    }

    val typography = getTypography()

    val customTypography = CustomTypography(
        bodyExtraLarge = typography.bodyLarge.copy(fontSize = 24.sp)
    )

    val iconFont = fontFamilyIconFont()

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme,
        LocalIconFont provides iconFont,
        LocalCustomColors provides customColors,
        LocalCustomTypography provides customTypography
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

internal val LocalIsDarkTheme = staticCompositionLocalOf { false }


@Composable
expect fun getColorScheme(dynamicColor: Boolean, isDarkMode: Boolean): ColorScheme

@Composable
expect fun applyPlatformSpecificThemeConfiguration(colorScheme: ColorScheme, isDarkMode: Boolean)

enum class Platform(
    val humanReadable: String
) {
    ANDROID("Android"),
    DESKTOP("Desktop"),
    IOS("iOS");
}

expect fun platform(): Platform

private data class ColorPairing(
    val name: String,
    val background: Color,
    val foreground: Color? = null,
)

@androidx.compose.desktop.ui.tooling.preview.Preview
@Composable
private fun ThemePreview() = PreviewWrapper {
    AbysnerTheme(darkTheme = true, dynamicColor = false) {

        val colors = listOf(
            ColorPairing("primary",  MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary),
            ColorPairing("primaryContainer",  MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer),

            ColorPairing("secondary",  MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary),
            ColorPairing("secondaryContainer",  MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer),

            ColorPairing("tertiary",  MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary),
            ColorPairing("tertiaryContainer",  MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer),

            ColorPairing("error",  MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError),
            ColorPairing("errorContainer",  MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer),
            ColorPairing("warning",  MaterialTheme.colorScheme.warning, MaterialTheme.colorScheme.onWarning),

            ColorPairing("background",  MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.onBackground),

            ColorPairing("surface",  MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface),
            ColorPairing("surfaceVariant",  MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant),

            ColorPairing("outline",  MaterialTheme.colorScheme.outline),
            ColorPairing("outlineVariant",  MaterialTheme.colorScheme.outlineVariant),
            ColorPairing("scrim", MaterialTheme.colorScheme.scrim),

            ColorPairing("inverseSurface",  MaterialTheme.colorScheme.inverseSurface, MaterialTheme.colorScheme.inverseOnSurface),
            ColorPairing("inversePrimary",  MaterialTheme.colorScheme.inversePrimary),

            ColorPairing("surfaceDim", MaterialTheme.colorScheme.surfaceDim),
            ColorPairing("surfaceBright", MaterialTheme.colorScheme.surfaceDim),
            ColorPairing("surfaceContainerLowest", MaterialTheme.colorScheme.surfaceContainerLowest),
            ColorPairing("surfaceContainerLow", MaterialTheme.colorScheme.surfaceContainerLow),
            ColorPairing("surfaceContainer", MaterialTheme.colorScheme.surfaceContainer),
            ColorPairing("surfaceContainerHigh", MaterialTheme.colorScheme.surfaceContainerHigh),
            ColorPairing("surfaceContainerHighest", MaterialTheme.colorScheme.surfaceContainerHighest),
        )

        fun Color.contrastingOnColor(): Color {
            // Choose white for darker surfaces and black for lighter surfaces
            return if (this.luminance() > 0.5) Color.Black else Color.White
        }

        LazyVerticalGrid(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            columns = GridCells.Adaptive(minSize = 96.dp)
        ) {
            colors.forEach { (name, background, foreground) ->
                item(name) {
                    Column(Modifier.background(background).padding(8.dp).aspectRatio(1f)) {
                        Text(
                            style = MaterialTheme.typography.labelSmall,
                            text = name,
                            maxLines = 1,
                            color = foreground ?: background.contrastingOnColor(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if(foreground != null) {
                            Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp).background(foreground, shape = RoundedCornerShape(16.dp)))
                        }
                    }
                }
            }
        }
    }
}
