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

package org.neotech.app.abysner.presentation.utilities

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/**
 * A `kotlinx.coroutines.runBlocking` replacement for use in `@Preview` functions. The screenshot
 * test renderer's sandboxed classloader has its own bundled kotlinx-coroutines that shadows the
 * project's version and is missing a symbol the compiler emits for `runBlocking`, so calling it
 * there throws `NoSuchMethodError`. Only safe to use on a suspend function that never actually
 * suspends, such as reading a bundled resource.
 */
fun <T> runBlockingInPreview(block: suspend () -> T): T {
    var result: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
    return result!!.getOrThrow()
}
