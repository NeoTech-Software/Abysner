/*
 * Abysner - Dive planner
 * Copyright (C) 2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.component.list

import androidx.compose.ui.unit.dp

/**
 * Shared metrics for the scrollable option lists inside pickers and preference dialogs, so they all
 * show a comparable number of options.
 */
object SelectionListDefaults {

    /**
     * Height of a single option. Kept at the minimum recommended touch target rather than anything
     * roomier: these lists can hold a dozen or more options, and scrolling past them costs more than
     * the extra breathing room is worth.
     */
    val rowMinHeight = 48.dp

    /**
     * How tall the scrolling part of a picker dialog may get.
     *
     * [androidx.compose.material3.AlertDialogCustomContent] already caps the whole dialog at 700dp,
     * so this leaves room for the title and buttons while using most of what is available.
     */
    val dialogContentMaxHeight = 520.dp
}
