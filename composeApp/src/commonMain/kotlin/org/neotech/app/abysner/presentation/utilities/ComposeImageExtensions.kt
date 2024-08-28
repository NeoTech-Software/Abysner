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

package org.neotech.app.abysner.presentation.utilities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class RenderTask(
    val composable: @Composable () -> Unit,
    val width: Int?,
    val height: Int?,
    val onComplete: suspend (ImageBitmap) -> Unit
)

class BitmapRenderController {

    private val _channel = Channel<RenderTask>()
    internal val events = _channel.receiveAsFlow()

    suspend fun renderBitmap(
        width: Int?,
        height: Int?,
        onRendered: suspend (ImageBitmap) -> Unit,
        composable: @Composable () -> Unit,
    ) {
        _channel.send(
            RenderTask(
                width = width,
                height = height,
                onComplete = onRendered,
                composable = composable
            )
        )
    }
}

val LocalBitmapRenderController = staticCompositionLocalOf {
    BitmapRenderController()
}

@Composable
fun BitmapRenderRoot(content: @Composable () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val render = remember { BitmapRenderController() }

    val coroutineScope = rememberCoroutineScope()

    val actionState: MutableState<RenderTask?> = remember { mutableStateOf(null) }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main) {
                render.events.collect {
                    actionState.value = it
                }
            }
        }
    }

    val action = actionState.value
    if (action != null) {
        RenderBitmap(
            width = action.width,
            height = action.height,
            onRendered = {
                actionState.value = null
                coroutineScope.launch {
                    action.onComplete(it)
                }
            }
        ) {
            action.composable.invoke()
        }
    }

    CompositionLocalProvider(LocalBitmapRenderController provides render) {
        content()
    }
}

@Composable
private fun RenderBitmap(
    width: Int?,
    height: Int?,
    onRendered: (ImageBitmap) -> Unit,
    composable: @Composable () -> Unit,
) {
    val graphicsLayer = rememberGraphicsLayer()

    Box(
        modifier = Modifier
            .layout { measurable, constraints ->

                val newConstraints = if (width != null && height != null) {
                    constraints.copy(
                        minWidth = 0,
                        maxWidth = width,
                        minHeight = 0,
                        maxHeight = height
                    )
                } else if (width != null) {
                    constraints.copy(
                        minWidth = 0,
                        maxWidth = width,
                        minHeight = 0,
                        maxHeight = Constraints.Infinity
                    )
                } else if (height != null) {
                    constraints.copy(
                        minHeight = 0,
                        maxHeight = height,
                        minWidth = 0,
                        maxWidth = Constraints.Infinity
                    )
                } else {
                    constraints
                }

                val placeable = measurable.measure(newConstraints)

                val layoutWidth = width ?: placeable.width
                val layoutHeight = height ?: placeable.height

                layout(layoutWidth, layoutHeight) {
                    placeable.place(0, 0)
                }
            }
            .background(Color.Transparent)
            .drawWithContent {
                graphicsLayer.record {
                    this@drawWithContent.drawContent()
                }
            }
    ) {
        composable()
    }

    LaunchedEffect(true) {
        // Not sure if this is entirely save to do, but it seems to work regardless.
        val bitmap = withContext(Dispatchers.IO) {
            graphicsLayer.toImageBitmap()
        }
        onRendered(bitmap)
    }
}
