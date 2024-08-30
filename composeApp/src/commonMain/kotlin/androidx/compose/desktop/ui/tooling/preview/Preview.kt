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

package androidx.compose.desktop.ui.tooling.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * This is a copy of androidx.compose.desktop.ui.tooling.preview.Preview
 * to trick Android Studio (IntelliJ) into showing a gutter icon to get preview
 * capabilities in the commonMain source. It's not perfect, but better then nothing.
 *
 * See: https://github.com/JetBrains/compose-multiplatform/issues/2045
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION
)
@Repeatable
annotation class Preview

/**
 * Improvised fix for: https://github.com/JetBrains/compose-multiplatform/issues/2852
 */
@Composable
fun PreviewWrapper(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalInspectionMode provides true) {
        content()
    }
}
