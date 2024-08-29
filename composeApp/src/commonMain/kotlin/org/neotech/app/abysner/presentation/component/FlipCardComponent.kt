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

package org.neotech.app.abysner.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay


@Composable
fun FlipCardComponent(
    showBack: MutableState<Boolean> = remember { mutableStateOf(false) },
    modifier: Modifier = Modifier,
    animateBackAutomatically: Boolean = true,
    front: @Composable (modifier: Modifier, onClick: () -> Unit) -> Unit = { _, _, ->},
    back: @Composable (modifier: Modifier, onClick: () -> Unit) -> Unit = {_, _, -> },
) {

    LaunchedEffect(showBack.value) {
        if (showBack.value && animateBackAutomatically) {
            delay(4000)
            showBack.value = false
        }
    }

    val rotation = animateFloatAsState(
        targetValue = if (showBack.value) {
            180f
        } else {
            0f
        },
        animationSpec = tween(delayMillis = 0)
    )


    val onClick = remember { { showBack.value = !showBack.value} }

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation.value
            },
    ) {
        if (rotation.value <= 90f) {
            front(Modifier.fillMaxSize(), onClick)
        } else {
            back(Modifier.fillMaxSize().graphicsLayer {
                rotationY = 180f
            }, onClick)
        }
    }
}
