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

package org.neotech.app.abysner.presentation.component.textfield.behavior

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import org.neotech.app.abysner.presentation.component.textfield.RawTextFieldInputBehavior
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * [RawTextFieldInputBehavior] for `HH:MM` time entry.
 *
 * The `:` separator is always visible. Hours are always 2 digits, minutes are always 2 digits.
 * Deletion replaces the removed digit with `0` rather than shifting.
 */
class DurationInputBehavior : RawTextFieldInputBehavior {

    override fun getKeyboardOptions() = KeyboardOptions(
        keyboardType = KeyboardType.Number,
        imeAction = ImeAction.Done,
    )

    override fun processValue(previousValue: TextFieldValue?, newValue: TextFieldValue): TextFieldValue {
        val previousText = previousValue?.text ?: "00:00"
        val previousColonPosition = previousText.indexOf(':').coerceAtLeast(0)
        val previousHours = previousText.substring(0, previousColonPosition)
        val previousMinutes = previousText.substring(previousColonPosition + 1)
        val isSelection = newValue.selection.start != newValue.selection.end
        val newColonPosition = newValue.text.indexOf(':')

        return if (newColonPosition >= 0) {
            processWithColon(newValue, isSelection, previousHours, newColonPosition)
        } else {
            processWithoutColon(previousValue, newValue, previousColonPosition, previousHours, previousMinutes)
        }
    }

    private fun processWithColon(newValue: TextFieldValue, isSelection: Boolean, previousHours: String, newColonPosition: Int): TextFieldValue {
        val hoursRaw = newValue.text.takeAsciiDigits(endIndex = newColonPosition)
        val minutesRaw = newValue.text.takeAsciiDigits(startIndex = newColonPosition + 1)

        val cursorPosition = newValue.selection.start

        var hours = hoursRaw.take(2)
        val overflow = hoursRaw.drop(2).take(2)

        // Only overflow hours digits into minutes when the cursor is directly at the left of the
        // colon. Typing in the middle of hours simply truncates the extra digit(s).
        val shouldOverflow = overflow.isNotEmpty() && cursorPosition == newColonPosition
        val effectiveOverflow = if (shouldOverflow) { overflow } else { "" }
        var minutes = if (effectiveOverflow.isEmpty()) {
            minutesRaw.take(2)
        } else {
            effectiveOverflow + minutesRaw.takeLast((2 - effectiveOverflow.length).coerceAtLeast(0))
        }

        if (!isSelection && previousHours.length == 2 && hours.length < 2) {
            hours = zeroPad(hours, padLeft = cursorPosition < newColonPosition)
        }


        if (!isSelection && minutes.length < 2) {
            minutes = zeroPad(minutes, padLeft = cursorPosition <= newColonPosition + 1)
        }

        fun mapPos(pos: Int): Int {
            return if (pos > newColonPosition) {
                val minuteChars = newValue.text
                    .countAsciiDigits(newColonPosition + 1, pos)
                    .coerceIn(0, minutes.length)
                hours.length + 1 + minuteChars
            } else {
                newValue.text.countAsciiDigits(endIndex = pos).coerceIn(0, hours.length)
            }
        }

        val (selectionStart, selectionEnd) = if (!isSelection && shouldOverflow) {
            if (effectiveOverflow.all { it == '0' }) {
                // Zero overflow from existing padding — keep cursor where the user typed.
                mapPos(newValue.selection.start) to mapPos(newValue.selection.end)
            } else {
                val advanced = hours.length + 1 + effectiveOverflow.length.coerceAtMost(minutes.length)
                advanced to advanced
            }
        } else {
            mapPos(newValue.selection.start) to mapPos(newValue.selection.end)
        }

        return buildValue(hours, minutes, selectionStart, selectionEnd)
    }

    private fun processWithoutColon(previousValue: TextFieldValue?, newValue: TextFieldValue, previousColon: Int, previousHours: String, previousMinutes: String): TextFieldValue {
        val newDigits = newValue.text.takeAsciiDigits()
        val oldDigits = previousHours + previousMinutes

        if (newDigits == oldDigits) {
            // Only the colon was removed, restore it.
            val previousCursor = previousValue?.selection?.start ?: 0
            return if (previousCursor == previousColon + 1) {
                // Cursor was right after the colon, just hop back over it, no deletion.
                buildValue(previousHours, previousMinutes, previousHours.length, previousHours.length)
            } else if (previousCursor > previousColon) {
                val hours = previousHours.dropLast(1).ifEmpty { "0" }
                buildValue(hours, previousMinutes, hours.length, hours.length)
            } else {
                val cursor = previousCursor.coerceIn(0, previousHours.length)
                buildValue(previousHours, previousMinutes, cursor, cursor)
            }
        }

        // Content changed (for example: select-all and paste/type or cuts that included the colon).
        if (newDigits.isEmpty()) {
            return buildValue("00", "00", 0, 0)
        }

        val hours = newDigits.take(2)
        val minutes = newDigits.drop(2).take(2)

        if (minutes.length < 2) {
            val cursorInHours = newValue.text
                .countAsciiDigits(endIndex = newValue.selection.start)
                .coerceIn(0, hours.length)
            return buildValue(hours, "00", cursorInHours, cursorInHours)
        }

        fun mapPos(pos: Int): Int {
            val digitsBefore = newValue.text
                .countAsciiDigits(endIndex = pos)
                .coerceIn(0, hours.length + minutes.length)
            return if (digitsBefore <= hours.length) digitsBefore
            else hours.length + 1 + (digitsBefore - hours.length).coerceAtMost(minutes.length)
        }

        return buildValue(hours, minutes, mapPos(newValue.selection.start), mapPos(newValue.selection.end),)
    }

    private fun zeroPad(digits: String, padLeft: Boolean): String = when {
        digits.isEmpty() -> "00"
        padLeft -> "0$digits"
        else -> "${digits}0"
    }

    private fun buildValue(hours: String, minutes: String, selectionStart: Int, selectionEnd: Int): TextFieldValue {
        val h = hours.padStart(2, '0')
        val shift = h.length - hours.length
        val display = "$h:$minutes"
        return TextFieldValue(
            text = display,
            selection = TextRange(
                (selectionStart + shift).coerceIn(0, display.length),
                (selectionEnd + shift).coerceIn(0, display.length),
            ),
        )
    }

    companion object {

        /**
         * Converts a [TextFieldValue] in `HH:MM` format to a [Duration]. Returns null if the text
         * cannot be parsed or hours exceeds 99 or minutes exceeds 59.
         */
        fun toDuration(value: TextFieldValue): Duration? {
            val colonIdx = value.text.indexOf(':')
            if (colonIdx < 0) return null
            val hourDigits = value.text.substring(0, colonIdx)
            val minuteDigits = value.text.substring(colonIdx + 1)
            if (hourDigits.isEmpty() || minuteDigits.isEmpty()) return null
            val h = hourDigits.toIntOrNull() ?: return null
            val m = when (minuteDigits.length) {
                1 -> minuteDigits.toIntOrNull()?.times(10) ?: return null
                else -> minuteDigits.toIntOrNull() ?: return null
            }
            if (h > 99 || m > 59) return null
            return (h * 60 + m).minutes
        }

        /**
         * Converts a [Duration] to a [TextFieldValue] in `HH:MM` format with the cursor placed
         * either at position 0 or the end of the text.
         */
        fun fromDuration(duration: Duration?): TextFieldValue {
            val totalMinutes = duration?.inWholeMinutes?.coerceIn(0, 99 * 60 + 59) ?: 0
            val h = (totalMinutes / 60).toString().padStart(2, '0')
            val m = (totalMinutes % 60).toString().padStart(2, '0')
            val display = "$h:$m"
            return TextFieldValue(
                text = display,
                selection = TextRange(if (duration == null || duration == Duration.ZERO) { 0 } else { display.length }),
            )
        }
    }
}

private fun String.takeAsciiDigits(startIndex: Int = 0, endIndex: Int = length): String = buildString {
    for (i in startIndex until endIndex.coerceAtMost(this@takeAsciiDigits.length)) {
        if (this@takeAsciiDigits[i] in '0'..'9') append(this@takeAsciiDigits[i])
    }
}

private fun String.countAsciiDigits(startIndex: Int = 0, endIndex: Int = length): Int {
    var count = 0
    for (i in startIndex until endIndex.coerceAtMost(length)) {
        if (this[i] in '0'..'9'){
            count++
        }
    }
    return count
}
