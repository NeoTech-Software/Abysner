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

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.coroutineScope

@Composable
fun Modifier.recordLayoutCoordinates(
    key: String,
    validLayoutCoordinates: SnapshotStateMap<String, LayoutCoordinates>
): Modifier {
    return onGloballyPositioned {
        validLayoutCoordinates[key] = it
    }
}

@Composable
fun Modifier.clearFocusOutside(validLayoutCoordinates: SnapshotStateMap<String, LayoutCoordinates>): Modifier {
    val localFocusManager = LocalFocusManager.current
    val parentLayoutCoordinates = mutableStateOf<LayoutCoordinates?>(null)

    val validBounds = derivedStateOf {
        validLayoutCoordinates.values
    }
    return onGloballyPositioned {
        parentLayoutCoordinates.value = it
    }
        .pointerInput(Unit) {
            downPressOutside(validBounds.value, parentLayoutCoordinates) {
                localFocusManager.clearFocus()
            }
        }
}

private suspend fun PointerInputScope.downPressOutside(
    validBounds: MutableCollection<LayoutCoordinates>,
    parentLayoutCoordinates: MutableState<LayoutCoordinates?>,
    onDownPress: () -> Unit,
) = coroutineScope {
    awaitEachGesture {
        val downPointerInputChange = awaitFirstDown(pass = PointerEventPass.Initial)
        val tapPosition = downPointerInputChange.position

        val parentPositionInRoot = parentLayoutCoordinates.value?.positionInRoot()

        if (parentPositionInRoot != null) {
            val inTextField = validBounds.any {
                // TODO somehow cache boundsInRoot?
                it.boundsInRoot().contains(tapPosition.plus(parentPositionInRoot))
            }
            if (!inTextField) {
                onDownPress()
            }
        }
    }
}
