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

package org.neotech.app.abysner.presentation.component.preferences

import androidx.compose.foundation.clickable
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview

//@SuppressLint("UnrememberedMutableState")
@Preview
@Composable
private fun SwitchPreferencePreview() {
    Surface {

        var isChecked by mutableStateOf(true)

        SwitchPreference(
            label = "Force minimal stop time",
            value = "If while ascending to a deco stop the diver already off-gassed enough, force a minimal deco stop of 1 minute instead of skipping the stop.",
            isChecked = isChecked
        ) {
            isChecked = it
        }
    }
}

@Composable
fun SwitchPreference(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    isChecked: Boolean,
    onCheckedChanged: ((Boolean) -> Unit)
) {
    BasicPreference(
        modifier = modifier.clickable {
            onCheckedChanged(!isChecked)
        },
        label = label,
        value = value,
        hideDivider = false
    ) {
        Switch(checked = isChecked, onCheckedChange = onCheckedChanged)
    }
}
