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

package org.neotech.app.abysner.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction

private fun hasVerticalScrollAction() = hasScrollAction() and
    SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

internal fun hasTextStartingWith(prefix: String) =
    SemanticsMatcher("text starts with '$prefix'") { node ->
        node.config.getOrNull(SemanticsProperties.Text).orEmpty().any { it.text.startsWith(prefix) }
    }

internal fun ComposeTestRule.nodePositionInRoot(tag: String): Offset =
    onNodeWithTag(tag).fetchSemanticsNode().positionInRoot

private fun ComposeTestRule.scrollBy(pixels: Float) {
    onNode(hasVerticalScrollAction())
        .performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, pixels)
        }
    waitForIdle()
}

internal fun ComposeTestRule.scrollToPosition(tag: String, targetYPosition: Float) {
    scrollBy(nodePositionInRoot(tag).y - targetYPosition)
}

internal fun ComposeTestRule.scrollToTop() {
    val scroll = onNode(hasVerticalScrollAction())
        .fetchSemanticsNode()
        .config[SemanticsProperties.VerticalScrollAxisRange]
    scrollBy(-scroll.value())
}
