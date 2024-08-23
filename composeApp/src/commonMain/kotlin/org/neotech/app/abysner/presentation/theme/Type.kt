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
import abysner.composeapp.generated.resources.Roboto_Bold
import abysner.composeapp.generated.resources.Roboto_BoldItalic
import abysner.composeapp.generated.resources.Roboto_Italic
import abysner.composeapp.generated.resources.Roboto_Medium
import abysner.composeapp.generated.resources.Roboto_MediumItalic
import abysner.composeapp.generated.resources.Roboto_Regular
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
private fun fontFamilyRoboto() = FontFamily(
    Font(Res.font.Roboto_Regular, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(Res.font.Roboto_Italic, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(Res.font.Roboto_Medium, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(Res.font.Roboto_MediumItalic, weight = FontWeight.Medium, style = FontStyle.Italic),
    Font(Res.font.Roboto_Bold, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(Res.font.Roboto_BoldItalic, weight = FontWeight.Bold, style = FontStyle.Italic)
)

private val baseline = Typography()

// Set of Material typography styles to start with
@Composable
fun getTypography(): Typography {

    val montserrat = fontFamilyMontserrat()
    val roboto = fontFamilyRoboto()

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

        labelLarge = baseline.labelLarge.copy(fontFamily = roboto, fontWeight = FontWeight.Medium),
        labelMedium = baseline.labelMedium.copy(fontFamily = roboto, fontWeight = FontWeight.Medium),
        labelSmall = baseline.labelSmall.copy(fontFamily = roboto, fontWeight = FontWeight.Medium),

        bodyLarge = baseline.bodyLarge.copy(fontFamily = roboto, fontWeight = FontWeight.Normal),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = roboto, fontWeight = FontWeight.Normal),
        // Roboto with default font size of 12.sp is just a bit too small
        bodySmall = baseline.bodySmall.copy(fontFamily = roboto, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    )
}

internal val LocalCustomTypography = staticCompositionLocalOf { CustomTypography() }

data class CustomTypography(
    val bodyExtraLarge: TextStyle = baseline.bodyLarge.copy(fontSize = 24.sp),
)

val Typography.bodyExtraLarge: TextStyle
    @Composable
    get() = LocalCustomTypography.current.bodyExtraLarge
