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

package org.neotech.app.abysner.presentation.component.textfield.number

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import org.neotech.app.abysner.presentation.component.textfield.InputState

@Stable
class NumberInputState<T>(
    behavior: NumberInputBehavior,
    initialValue: Long,
    private val minimum: Long,
    private val maximum: Long,
    private val fromScaled: (Long) -> T,
    private val toScaled: (T) -> Long,
) : InputState<Long>(behavior, initialValue) {

    var value: T
        get() = fromScaled(currentValue)
        set(newValue) = replaceValue(toScaled(newValue))

    override fun isValid(value: Long): Boolean = value in minimum..maximum

    override fun errorMessage(label: String?): String? {
        val subject = label ?: "Value"
        val value = behavior.parse(text.text)
        return when {
            value == null -> "$subject must be between ${behavior.format(minimum)} and ${behavior.format(maximum)}."
            value < minimum -> "$subject must not be lower than ${behavior.format(minimum)}."
            value > maximum -> "$subject must not be higher than ${behavior.format(maximum)}."
            else -> null
        }
    }
}

@Composable
fun rememberDecimalInputState(
    initialValue: Double,
    fractionDigits: Int,
    minValue: Double,
    maxValue: Double,
): NumberInputState<Double> = remember(initialValue, fractionDigits, minValue, maxValue) {
    val behavior = NumberInputBehavior(fractionDigits)
    NumberInputState(
        behavior = behavior,
        minimum = behavior.fromDecimal(minValue),
        maximum = behavior.fromDecimal(maxValue),
        initialValue = behavior.fromDecimal(initialValue),
        fromScaled = behavior::toDecimal,
        toScaled = behavior::fromDecimal,
    )
}

@Composable
fun rememberIntegerInputState(
    initialValue: Int,
    minValue: Int,
    maxValue: Int,
): NumberInputState<Int> = remember(initialValue, minValue, maxValue) {
    NumberInputState(
        behavior = NumberInputBehavior(fractionDigits = 0),
        minimum = minValue.toLong(),
        maximum = maxValue.toLong(),
        initialValue = initialValue.toLong(),
        fromScaled = Long::toInt,
        toScaled = Int::toLong,
    )
}
