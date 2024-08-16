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

package org.neotech.app.abysner.presentation.component.textfield.behavior

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import org.neotech.app.abysner.domain.utilities.DecimalFormatter
import org.neotech.app.abysner.presentation.component.textfield.GenericTextFieldBehavior

open class NumberInputBehavior(
    private val visualTransformation: VisualTransformation = VisualTransformation.None
): GenericTextFieldBehavior<Long?> {

    override fun getVisualTransformation(): VisualTransformation = visualTransformation

    override fun getKeyboardOptions(): KeyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)

    private val decimalFormat = DecimalFormatter("0")

    override fun toString(value: Long?): String {
        return if(value == null) {
            ""
        } else {
            decimalFormat.format(value)
        }
    }

    override fun fromString(previousValue: Long?, value: String): Long? {
        val filtered = value.filter { c ->
            c in '0'..'9'
        }

        // Attempt to parse
        val nonDecimalPart: Long = filtered.toLongOrNull() ?: return null
        return nonDecimalPart
    }
}
