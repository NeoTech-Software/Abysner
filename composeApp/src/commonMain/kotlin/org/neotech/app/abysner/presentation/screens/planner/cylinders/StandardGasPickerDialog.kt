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

package org.neotech.app.abysner.presentation.screens.planner.cylinders

import androidx.compose.runtime.Composable
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.presentation.component.preferences.SingleChoicePreferenceDialog

@Composable
fun StandardGasPickerDialog(
    onGasSelected: (Gas) -> Unit,
    onDismissRequest: () -> Unit,
) {
    SingleChoicePreferenceDialog(
        title = "Standard gases",
        confirmButtonText = null,
        onItemSelected = { item, _ ->
            item?.let{
                onGasSelected(it)
            }
            onDismissRequest()
        },
        onCancelButtonClicked = onDismissRequest,
        onDismissRequest = onDismissRequest,
        initialSelectedItemIndex = 0,
        items = Gas.StandardGasses,
        itemToStringMapper = {
            "${it.diveIndustryName()} ($it)"
        }
    )
}
