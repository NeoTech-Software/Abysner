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

private data class PluralData(val resource: PluralStringResource, val quantity: Int, val value: Any)

interface PluralBuilder {

    fun pluralArgument(resource: PluralStringResource, quantity: Int, value: Any)

    fun pluralInt(resource: PluralStringResource, quantity: Int) =
        pluralArgument(resource, quantity, quantity)
}

@Composable
fun pluralsStringBuilder(resource: StringResource, build: PluralBuilder.() -> Unit): String {
    val pluralArgs = mutableListOf<PluralData>()
    val builder = object : PluralBuilder {
        override fun pluralArgument(resource: PluralStringResource, quantity: Int, value: Any) {
            pluralArgs.add(PluralData(resource, quantity, value))
        }
    }
    builder.build()

    val arguments = pluralArgs.map { (resource, quantity, value) ->
        pluralStringResource(resource, quantity, value)
    }
    return stringResource(resource, *arguments.toTypedArray())
}
