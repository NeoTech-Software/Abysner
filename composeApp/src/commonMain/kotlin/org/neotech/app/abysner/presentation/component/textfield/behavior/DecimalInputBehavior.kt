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
import kotlin.math.pow
import kotlin.math.round

/**
 * TextField behavior that allows for decimal input.
 * Note: This works with [Long] instead of [Double] to avoid rounding issues. To convert this Long
 * into a decimal you need to divide it by 100. This is basically to overcome the lack of a
 * BigDecimal implementation in Kotlin Multi-platform.
 */
open class DecimalInputBehavior(
    private val fractionDigits: Int = 2,
    private val visualTransformation: VisualTransformation
): GenericTextFieldBehavior<Long?> {

    init {
        // Support up to 3 fraction digits.
        require(fractionDigits in 0..3)
    }

    private val divisor = 10.0.pow(fractionDigits).toInt()

    override fun getKeyboardOptions(): KeyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)

    override fun getVisualTransformation(): VisualTransformation = visualTransformation

    private val decimalFormat = DecimalFormatter("0.00").apply {
        setFractionDigits(fractionDigits)
    }
    private val decimalSeparator = decimalFormat.decimalSeparator()

    override fun toString(value: Long?): String {
        return if(value == null) {
            ""
        } else {
            decimalFormat.format(value / divisor.toDouble())
        }
    }

    fun toString(value: Double?): String = toString(fromDecimal(value))

    fun toDecimal(value: Long?): Double? = value?.let { it / divisor.toDouble() }

    fun fromDecimal(value: Double?): Long? = value?.let { round(it * divisor).toLong()}

    override fun fromString(previousValue: Long?, value: String): Long? {
        var indexOfFirstDecimalSeparator = -1
        var removedCount = 0

        fun indexCorrected(index: Int) = index - removedCount

        val filtered = value.filterIndexed { index, c ->
            if(c == decimalSeparator && indexOfFirstDecimalSeparator == -1) {
                indexOfFirstDecimalSeparator = indexCorrected(index)
                true
            } else if(c in '0'..'9' && (indexOfFirstDecimalSeparator == -1 || indexCorrected(index) <= indexOfFirstDecimalSeparator + fractionDigits)) {
                true
            } else {
                // reject everything after the last fraction digit (or that is not a valid character)
                removedCount++
                false
            }
        }

        // Attempt to parse
        val nonDecimalPart: Long?
        val decimalPartString: String?

        if(indexOfFirstDecimalSeparator != -1) {
            // Decimal separator known
            nonDecimalPart = filtered.substring(0, indexOfFirstDecimalSeparator).toLongOrNull()
            decimalPartString = filtered.substring(indexOfFirstDecimalSeparator + 1)
        } else {
            // Decimal separator has been removed or was never there to begin with
            val currentDecimalSeparatorIndex = toString(previousValue).indexOf(decimalSeparator)
            if(currentDecimalSeparatorIndex == -1) {
                // No decimal separator, and no previously known position
                nonDecimalPart = filtered.toLongOrNull()
                decimalPartString = null
            } else if(currentDecimalSeparatorIndex <= filtered.length) {
                // No decimal separator but previous position is known and within bounds
                nonDecimalPart = filtered.substring(0, currentDecimalSeparatorIndex).toLongOrNull()
                decimalPartString = filtered.substring(currentDecimalSeparatorIndex)
            } else {
                nonDecimalPart = filtered.toLongOrNull()
                decimalPartString = null
            }
        }

        val decimalPart = if(decimalPartString?.length == 1) {
            (decimalPartString.toLongOrNull() ?: 0) * (divisor / 10)
        } else {
            decimalPartString?.toLongOrNull() ?: 0
        }

        return if(nonDecimalPart == null) {
            if(decimalPart != 0L) {
                // Still have numbers behind the dot, so add a zero
                decimalPart
            } else {
                // Everything all zeroes? Then we can return null which is similar to having entered no number at all.
                null
            }
        } else {
            (nonDecimalPart * divisor) + decimalPart
        }
    }
}
