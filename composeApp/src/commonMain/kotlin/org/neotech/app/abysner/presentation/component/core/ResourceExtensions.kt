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

package org.neotech.app.abysner.presentation.component.core

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

interface PluralBuilder {

    @Composable
    fun pluralArgument(resource: PluralStringResource, quantity: Int, value: Any)

    @Composable
    fun pluralInt(resource: PluralStringResource, quantity: Int) =
        pluralArgument(resource, quantity, quantity)
}


@Composable
fun pluralsStringBuilder(resource: StringResource, build: @Composable PluralBuilder.() -> Unit): String {
    val arguments = mutableListOf<String>()
    val builder = object: PluralBuilder {
        @Composable
        override fun pluralArgument(resource: PluralStringResource, quantity: Int, value: Any) {
            arguments.add(pluralStringResource(resource, quantity, value))
        }
    }
    builder.build()
    return stringResource(resource, *arguments.toTypedArray())
}
