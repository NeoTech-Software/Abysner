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

package org.neotech.app.abysner.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.neotech.app.abysner.presentation.screens.planner.decoplan.DecoPlanCardComponentPreview
import org.neotech.app.abysner.presentation.screens.planner.decoplan.GasPieChartPreview
import org.neotech.app.abysner.presentation.screens.planner.gas.DecoGasSelectionCardComponentPreview

@Preview(widthDp = 500)
@Composable
private fun DecoGasSelectionCardComponentAndroidPreview() {
    DecoGasSelectionCardComponentPreview()
}

@Preview(showBackground = true)
@Composable
private fun GasPieChartAndroidPreview() {
    GasPieChartPreview()
}

@Preview
@Composable
private fun DecoPlanCardComponentAndroidPreview() {
    DecoPlanCardComponentPreview()
}