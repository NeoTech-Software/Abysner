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

package org.neotech.app.abysner.presentation.component.bottomsheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Column of [header] plus scrollable [content], capped to leave [topClearance] clear above the
 * sheet. Content taller than that scrolls instead of growing the sheet further, which keeps a
 * [androidx.compose.material3.ModalBottomSheet] from wrapping to a height close enough to the
 * screen's to hunt between wrapping and covering the status bar.
 */
@Composable
fun ModalBottomSheetScaffold(
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    val topClearance = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
        .asPaddingValues()
        .calculateTopPadding()
    BoxWithConstraints(modifier) {
        Column(Modifier.heightIn(max = maxHeight - topClearance)) {
            header()
            Box(Modifier.weight(1f, fill = false).verticalScroll(scrollState)) {
                content()
            }
        }
    }
}
