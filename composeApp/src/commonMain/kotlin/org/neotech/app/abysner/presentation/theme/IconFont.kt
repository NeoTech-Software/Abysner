import abysner.composeapp.generated.resources.Res
import abysner.composeapp.generated.resources.icon_font
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import org.jetbrains.compose.resources.Font
import org.neotech.app.abysner.presentation.theme.CustomColors

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

@Composable
fun fontFamilyIconFont() = FontFamily(Font(resource = Res.font.icon_font))

internal val LocalIconFont = staticCompositionLocalOf { FontFamily() }

@Composable
fun AnnotatedString.Builder.appendIcon(icon: IconFont, currentTextStyle: TextStyle = LocalTextStyle.current) {
    withStyle(SpanStyle(fontFamily = LocalIconFont.current, fontSize = currentTextStyle.fontSize * 0.9f)) {
        append(icon.unicode)
    }
}

enum class IconFont(val unicode: String) {
    INFO_OUTLINE("\uE90B"),
    ARROW_RIGHT_ALT("\uE902"),
    HOURGLASS_TOP("\uE903"),
    STOP("\uE904"),
    TRENDING_DOWN("\uE906"),
    VERTICAL_ALIGN_BOTTOM("\uE907"),
    TRENDING_UP("\uE908"),
    VERTICAL_ALIGN_TOP("\uE909"),
    TIMER("\uE90A"),
    DANGEROUS("\uE900"),
    WARNING("\uE901");
}
