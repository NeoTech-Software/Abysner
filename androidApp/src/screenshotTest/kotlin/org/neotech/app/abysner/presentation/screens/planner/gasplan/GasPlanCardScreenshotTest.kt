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
import org.neotech.app.abysner.presentation.PreviewForScreenshotTestsMaxHeight

/**
 * CCR loop cylinders always carry a zero reserve, and bailout-only cylinders always carry a zero
 * normal requirement. These two previews are the real, planner-produced data for those cases,
 * exercising the pressure track's single-label collapse and right-edge clamp.
 */
@PreviewTest
@PreviewForScreenshotTestsMaxHeight
@Composable
fun GasPlanCardCcrScreenshotTest() {
    GasPlanCardComponentCcrPreview()
}

@PreviewTest
@PreviewForScreenshotTestsMaxHeight
@Composable
fun GasPlanCardCcrBailoutScreenshotTest() {
    GasPlanCardComponentCcrBailoutPreview()
}
