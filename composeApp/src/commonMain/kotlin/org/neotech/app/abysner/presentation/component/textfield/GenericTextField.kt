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

package org.neotech.app.abysner.presentation.component.textfield

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation

/**
 * A [GenericTextFieldBehavior] specialised for [TextFieldValue], giving full, low-level access to
 * the cursor position, selection, and composition. Use this instead of a plain
 * [GenericTextFieldBehavior] when you need to control cursor placement or selection as part of the
 * processing logic.
 *
 * Default implementations of [toString] and [fromString] are provided so that concrete classes only
 * need to implement [processValue]. Both [GenericTextField] and [OutlinedGenericInputField]
 * detect this interface and route through [processValue] instead of [fromString] so that cursor
 * information is never lost.
 */
interface RawTextFieldInputBehavior : GenericTextFieldBehavior<TextFieldValue> {

    /**
     * Called on every `onValueChange` event from the underlying text field. Transform [newValue]
     * into the value that should actually be stored/displayed, using [previousValue] for context.
     * Return [newValue] unchanged for pass-through behavior.
     */
    fun processValue(previousValue: TextFieldValue?, newValue: TextFieldValue): TextFieldValue = newValue

    /**
     * Extracts the plain text from the [TextFieldValue], or defaults to an empty string if the
     * value is null.
     */
    override fun toString(value: TextFieldValue?): String = value?.text ?: ""

    override fun fromString(previousValue: TextFieldValue?, value: String): TextFieldValue =
        error("fromString must not be called on a RawTextFieldInputBehavior, use processValue instead.")
}

interface GenericTextFieldBehavior<T> {

    fun getVisualTransformation(): VisualTransformation = VisualTransformation.None

    fun getKeyboardOptions(): KeyboardOptions = KeyboardOptions.Default

    fun toString(value: T?): String

    fun fromString(previousValue: T?, value: String): T

    fun isEmpty(value: T?): Boolean = toString(value).isEmpty()
}

/**
 * Default [GenericTextFieldBehavior] that does not apply any transformation to the raw string, it's
 * output type is therefor [String]. It does also not apply any [VisualTransformation] nor
 * [KeyboardOptions].
 */
class TextInputBehavior(private val keyboardOptions: KeyboardOptions = KeyboardOptions.Default) :
    GenericTextFieldBehavior<String> {

    override fun getKeyboardOptions(): KeyboardOptions = keyboardOptions

    override fun toString(value: String?): String = value ?: ""

    override fun fromString(previousValue: String?, value: String): String = value
}

/**
 * A basic [TextField]
 * [VisualTransformation] is not a direct argument anymore but is instead controlled by the given
 * [behavior], this is also true for [KeyboardOptions]. The rest of the arguments are completely the
 * same as a normal [TextField] including the defaults.
 *
 * When [behavior] is a [RawTextFieldInputBehavior] the field automatically switches to the
 * [TextFieldValue] overload and routes every change through [RawTextFieldInputBehavior.processValue],
 * preserving cursor position and selection.
 */
@Composable
fun <T> GenericTextField(
    modifier: Modifier = Modifier,
    value: T? = null,
    onValueChanged: (value: T) -> Unit = {},
    behavior: GenericTextFieldBehavior<T>,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors()
) {
    if (behavior is RawTextFieldInputBehavior) {
        TextField(
            modifier = modifier,
            value = value as TextFieldValue,
            onValueChange = {
                @Suppress("UNCHECKED_CAST")
                val processed = behavior.processValue(value as TextFieldValue, it)
                @Suppress("UNCHECKED_CAST")
                onValueChanged(processed as T)
            },
            keyboardOptions = behavior.getKeyboardOptions(),
            visualTransformation = behavior.getVisualTransformation(),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,
            isError = isError,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            interactionSource = interactionSource,
            shape = shape,
            colors = colors,
        )
    } else {
        TextField(
            modifier = modifier,
            value = behavior.toString(value),
            onValueChange = {
                onValueChanged(behavior.fromString(value, it))
            },
            keyboardOptions = behavior.getKeyboardOptions(),
            visualTransformation = behavior.getVisualTransformation(),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,
            isError = isError,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            interactionSource = interactionSource,
            shape = shape,
            colors = colors,
        )
    }
}

/**
 * An [OutlinedTextField] that supports a [GenericTextFieldBehavior]. Mirrors [GenericTextField]
 * but uses the outlined Material 3 style and always operates in single-line mode.
 *
 * When [behavior] is a [RawTextFieldInputBehavior] the field uses the [TextFieldValue] overload
 * and routes every change through [RawTextFieldInputBehavior.processValue], preserving cursor
 * position and selection. The call site is responsible for holding the [TextFieldValue] state.
 */
@Composable
fun <T> OutlinedGenericInputField(
    modifier: Modifier = Modifier,
    initialValue: T,
    behavior: GenericTextFieldBehavior<T>,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors().copy(errorTextColor = Color.Red),
    textStyle: TextStyle = LocalTextStyle.current,
    errorMessage: String? = null,
    supportingText: (@Composable (message: String?) -> Unit)? = { ErrorSupportingText(it) },
    onValueChanged: (T) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current

    if (behavior is RawTextFieldInputBehavior) {
        OutlinedTextField(
            modifier = modifier,
            singleLine = true,
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            supportingText = { supportingText?.invoke(errorMessage) },
            label = label,
            isError = isError,
            colors = colors,
            textStyle = textStyle,
            value = @Suppress("UNCHECKED_CAST") (initialValue as TextFieldValue),
            keyboardOptions = behavior.getKeyboardOptions(),
            visualTransformation = behavior.getVisualTransformation(),
            onValueChange = { new ->
                @Suppress("UNCHECKED_CAST")
                val processed = behavior.processValue(initialValue as? TextFieldValue, new)
                @Suppress("UNCHECKED_CAST")
                onValueChanged(processed as T)
            }
        )
    } else {
        OutlinedTextField(
            modifier = modifier,
            singleLine = true,
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            supportingText = { supportingText?.invoke(errorMessage) },
            label = label,
            isError = isError,
            colors = colors,
            textStyle = textStyle,
            value = behavior.toString(initialValue),
            keyboardOptions = behavior.getKeyboardOptions(),
            visualTransformation = behavior.getVisualTransformation(),
            onValueChange = { new ->
                onValueChanged(behavior.fromString(initialValue, new))
            }
        )
    }
}
