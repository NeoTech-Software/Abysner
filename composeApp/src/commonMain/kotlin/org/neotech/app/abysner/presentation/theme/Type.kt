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

import abysner.composeapp.generated.resources.Montserrat_Bold
import abysner.composeapp.generated.resources.Montserrat_BoldItalic
import abysner.composeapp.generated.resources.Montserrat_Italic
import abysner.composeapp.generated.resources.Montserrat_Medium
import abysner.composeapp.generated.resources.Montserrat_MediumItalic
import abysner.composeapp.generated.resources.Montserrat_Regular
import abysner.composeapp.generated.resources.Res
import abysner.composeapp.generated.resources.SourceSans3_Bold
import abysner.composeapp.generated.resources.SourceSans3_Italic
import abysner.composeapp.generated.resources.SourceSans3_Medium
import abysner.composeapp.generated.resources.SourceSans3_MediumItalic
import abysner.composeapp.generated.resources.SourceSans3_Regular
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font

@Composable
private fun fontFamilyMontserrat() = FontFamily(
    Font(Res.font.Montserrat_Regular, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(Res.font.Montserrat_Italic, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(Res.font.Montserrat_Medium, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(Res.font.Montserrat_MediumItalic, weight = FontWeight.Medium, style = FontStyle.Italic),
    Font(Res.font.Montserrat_Bold, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(Res.font.Montserrat_BoldItalic, weight = FontWeight.Bold, style = FontStyle.Italic),
)

@Composable
private fun fontFamilySourceSans() = FontFamily(
    Font(Res.font.SourceSans3_Regular, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(Res.font.SourceSans3_Italic, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(Res.font.SourceSans3_Medium, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(Res.font.SourceSans3_MediumItalic, weight = FontWeight.Medium, style = FontStyle.Italic),
    Font(Res.font.SourceSans3_Bold, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(Res.font.SourceSans3_Italic, weight = FontWeight.Bold, style = FontStyle.Italic)
)

private val baseline = Typography()

// Set of Material typography styles to start with
@Composable
fun getTypography(): Typography {

    val montserrat = fontFamilyMontserrat()
    val sourceSans = fontFamilySourceSans()

    return Typography(
        titleLarge = baseline.titleLarge.copy(fontFamily = montserrat, fontWeight = FontWeight.Medium),
        titleMedium = baseline.titleMedium.copy(fontFamily = montserrat, fontWeight = FontWeight.Medium),
        titleSmall = baseline.titleSmall.copy(fontFamily = montserrat, fontWeight = FontWeight.Medium),

        headlineLarge = baseline.headlineLarge.copy(fontFamily = montserrat, fontWeight = FontWeight.Medium),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = montserrat, fontWeight = FontWeight.Medium),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = montserrat, fontWeight = FontWeight.Medium),

        displayLarge = baseline.displayLarge.copy(fontFamily = montserrat, fontWeight = FontWeight.Medium),
        displayMedium = baseline.displayMedium.copy(fontFamily = montserrat, fontWeight = FontWeight.Medium),
        displaySmall = baseline.displaySmall.copy(fontFamily = montserrat, fontWeight = FontWeight.Medium),

        labelLarge = baseline.labelLarge.copy(fontFamily = sourceSans),
        labelMedium = baseline.labelMedium.copy(fontFamily = sourceSans),
        labelSmall = baseline.labelSmall.copy(fontFamily = sourceSans)
    )
}

internal val LocalCustomTypography = staticCompositionLocalOf { CustomTypography() }

data class CustomTypography(
    val bodyExtraLarge: TextStyle = baseline.bodyLarge.copy(fontSize = 24.sp),
)

val Typography.bodyExtraLarge: TextStyle
    @Composable
    get() = LocalCustomTypography.current.bodyExtraLarge
