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

package org.neotech.app.abysner.presentation.screens.planner.gasplan

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import org.neotech.app.abysner.presentation.PreviewForScreenshotTestsDialog

@PreviewTest
@PreviewForScreenshotTestsDialog
@Composable
fun GasUsageDetailsTwoCylindersPositiveScreenshotTest() {
    TwoCylindersPositivePreview()
}

@PreviewTest
@PreviewForScreenshotTestsDialog
@Composable
fun GasUsageDetailsTwoCylindersWarningScreenshotTest() {
    TwoCylindersWarningPreview()
}

@PreviewTest
@PreviewForScreenshotTestsDialog
@Composable
fun GasUsageDetailsTwoCylindersErrorScreenshotTest() {
    TwoCylindersErrorPreview()
}

@PreviewTest
@PreviewForScreenshotTestsDialog
@Composable
fun GasUsageDetailsOneCylinderPositiveScreenshotTest() {
    OneCylinderPositivePreview()
}

@PreviewTest
@PreviewForScreenshotTestsDialog
@Composable
fun GasUsageDetailsOneCylinderWarningScreenshotTest() {
    OneCylinderWarningPreview()
}

@PreviewTest
@PreviewForScreenshotTestsDialog
@Composable
fun GasUsageDetailsOneCylinderErrorScreenshotTest() {
    OneCylinderErrorPreview()
}
