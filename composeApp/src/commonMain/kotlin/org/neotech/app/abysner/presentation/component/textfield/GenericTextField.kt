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

package org.neotech.app.abysner.presentation.component.textfield

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation

interface GenericTextFieldBehavior<T> {

    fun getVisualTransformation(): VisualTransformation = VisualTransformation.None

    fun getKeyboardOptions(): KeyboardOptions = KeyboardOptions.Default

    fun toString(value: T?): String

    fun fromString(previousValue: T?, value: String): T
}

/**
 * Default [GenericTextFieldBehavior] that does not apply any transformation to the raw string, it's
 * output type is therefor [String]. It does also not apply any [VisualTransformation] nor
 * [KeyboardOptions].
 */
class TextInputBehavior(private val keyboardOptions: KeyboardOptions = KeyboardOptions.Default): GenericTextFieldBehavior<String> {

    override fun getKeyboardOptions(): KeyboardOptions = keyboardOptions

    override fun toString(value: String?): String = value ?: ""

    override fun fromString(previousValue: String?, value: String): String = value
}

/**
 * A basic [TextField] that supports a [GenericTextFieldBehavior]. This means
 * [VisualTransformation] is not a direct argument anymore but is instead controlled by the given
 * [behavior], this is also true for [KeyboardOptions]. The rest of the arguments are completely the
 * same as a normal [TextField] including the defaults.
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
        colors = colors
    )
}
