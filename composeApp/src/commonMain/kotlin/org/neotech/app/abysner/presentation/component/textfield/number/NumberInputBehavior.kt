/*
 * Abysner - Dive planner
 * Copyright (C) 2024-2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.component.textfield.number

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import org.neotech.app.abysner.domain.utilities.DecimalFormatter
import org.neotech.app.abysner.domain.utilities.FormatLocale
import org.neotech.app.abysner.presentation.component.textfield.InputBehavior
import kotlin.math.pow
import kotlin.math.round

/**
 * Input behavior for whole and decimal numbers with up to [fractionDigits] digits after the
 * decimal separator.
 *
 * Note: Values are a [Long] scaled by 10^[fractionDigits] instead of a [Double] to avoid rounding
 * issues. This is to overcome the lack of a BigDecimal implementation in Kotlin Multiplatform. Use
 * [toDecimal] and [fromDecimal] to convert between the storage format [Long] and decimal format
 * [Double], but be aware of precision loss.
 */
class NumberInputBehavior(
    private val fractionDigits: Int,
    locale: FormatLocale = FormatLocale.Default,
) : InputBehavior<Long> {

    init {
        require(fractionDigits in 0..MAXIMUM_FRACTION_DIGITS)
    }

    private val divisor = 10.0.pow(fractionDigits).toLong()

    private val formatter = DecimalFormatter("0", locale).apply {
        setFractionDigits(fractionDigits)
    }

    private val decimalSeparator = formatter.decimalSeparator()

    override val keyboardOptions = KeyboardOptions.Default.copy(
        keyboardType = if (fractionDigits == 0) {
            KeyboardType.Number
        } else {
            KeyboardType.Decimal
        },
        imeAction = ImeAction.Done,
    )

    /**
     * Transforms [newValue] according to the following rules:
     * - Only digits and separators are kept, everything else is removed.
     * - A change that results in more than one separator is rejected as a whole.
     * - "." and "," are both accepted as decimal separator, but replaced with the separator of the
     *   locale.
     * - The fraction never has more than [fractionDigits] digits, extra digits are dropped. So
     *   placing a separator in "120" after the "1" gives "1.2" with one fraction digit.
     * - If configured with zero fraction digits the separator is never allowed in the first place
     *   and dropped from any input.
     * - The integer part never has more than [MAXIMUM_INTEGER_DIGITS] digits, if exceeded the
     *   change is rejected as a whole.
     * - The cursor stays at the same place relative to the characters that are kept.
     */
    override fun transformInput(previousValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue {
        // Remove everything except digits, and separators if fraction digits are allowed
        var normalizedChars = newValue.text.filter(::isAllowed)

        // Reject the change if there is more than one separator
        if (normalizedChars.count(::isSeparator) > 1) {
            return previousValue
        }

        // Drop fraction digits beyond the allowed maximum
        val separatorPosition = normalizedChars.indexOfFirst(::isSeparator)
        normalizedChars = if (separatorPosition == -1) {
            normalizedChars
        } else {
            normalizedChars.take(separatorPosition + 1 + fractionDigits)
        }

        // Replace the separator with the decimal separator of the current locale
        normalizedChars = normalizedChars.map {
            if (isSeparator(it)) {
                decimalSeparator
            } else {
                it
            }
        }.joinToString("")

        // Reject the change if the result has too many integer digits
        if (split(normalizedChars).first.length > MAXIMUM_INTEGER_DIGITS) {
            return previousValue
        }

        // Move the cursor back by the number of characters removed before it
        fun transformedIndex(index: Int) = newValue.text
            .take(index)
            .count(::isAllowed)
            .coerceAtMost(normalizedChars.length)

        return newValue.copy(
            text = normalizedChars,
            selection = TextRange(transformedIndex(newValue.selection.start), transformedIndex(newValue.selection.end)),
            composition = if (normalizedChars == newValue.text) {
                newValue.composition
            } else {
                null
            },
        )
    }

    private fun isAllowed(char: Char) = char in '0'..'9' || (fractionDigits > 0 && isSeparator(char))

    private fun isSeparator(char: Char) = char == '.' || char == ',' || char == decimalSeparator

    override fun parse(text: String): Long? {
        val (integerPart, fraction) = split(text)
        if (integerPart.isEmpty() && fraction.isEmpty()) {
            return null
        }
        val digits = (integerPart + fraction).toLong()
        return digits * 10.0.pow(fractionDigits - fraction.length).toLong()
    }

    override fun format(value: Long): String {
        val formatted = formatter.format(toDecimal(value))
        if (fractionDigits == 0) {
            return formatted
        }
        // Remove trailing zeroes, but always keep at least one fraction.
        val trimmed = formatted.trimEnd('0')
        return if (trimmed.endsWith(decimalSeparator)) {
            trimmed + "0"
        } else {
            trimmed
        }
    }

    fun toDecimal(value: Long): Double = value / divisor.toDouble()

    fun fromDecimal(value: Double): Long = round(value * divisor).toLong()

    private fun split(text: String): Pair<String, String> {
        val separatorIndex = text.indexOf(decimalSeparator)
        return if (separatorIndex == -1) {
            text to ""
        } else {
            text.substring(0, separatorIndex) to text.substring(separatorIndex + 1)
        }
    }
}

private const val MAXIMUM_FRACTION_DIGITS = 4

/**
 * Limits the number of integer digits so a scaled value always fits in a [Long]. The exact number
 * is quite randomly chosen, but good enough for this app.
 */
private const val MAXIMUM_INTEGER_DIGITS = 9
