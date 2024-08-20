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

package org.neotech.app.abysner.presentation.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign

/**
 * This is quite rudimentary still, for example even though the internal TextField is readOnly it
 * still supports text selection, which is kinda weird. I would like to rework this to a more specific
 * drop down component, not a TextField (but with the looks of a TextField).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropDown(
    modifier: Modifier = Modifier,
    label: String? = null,
    selectedValue: T? = null,
    items: List<T>,
    selectedText: (value: T?) -> AnnotatedString,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    dropdownRow: @Composable (index: Int, value: T) -> Unit,
    onSelectionChanged: (index: Int, value: T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    androidx.compose.material3.ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {

        OutlinedTextField(
            value = TextFieldValue(selectedText(selectedValue)),
            onValueChange = {},
            visualTransformation = visualTransformation,
            label = {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        ),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = {
                        dropdownRow.invoke(index, item)
                    },
                    onClick = {
                        expanded = false
                        onSelectionChanged(index, item)
                    }
                )
            }
        }
    }
}
