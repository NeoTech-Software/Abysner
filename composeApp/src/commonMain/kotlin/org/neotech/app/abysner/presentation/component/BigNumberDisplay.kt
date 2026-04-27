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

package org.neotech.app.abysner.presentation.component

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import org.neotech.app.abysner.presentation.component.core.ifUnspecified
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BigNumberDisplay(
    modifier: Modifier = Modifier,
    size: BigNumberSize = BigNumberSize.MEDIUM,
    value: String,
    label: String,
    valueColor: Color = Color.Unspecified,
    containerColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    showDropDown: Boolean = false
) {

    val style = when (size) {
        BigNumberSize.EXTRA_SMALL -> MaterialTheme.typography.headlineSmall
        BigNumberSize.SMALL -> MaterialTheme.typography.headlineMedium
        BigNumberSize.MEDIUM -> MaterialTheme.typography.displayMedium
        BigNumberSize.LARGE -> MaterialTheme.typography.displayLarge
    }

    val paddingHorizontal = when (size) {
        BigNumberSize.EXTRA_SMALL -> 12.dp
        BigNumberSize.SMALL -> 12.dp
        BigNumberSize.MEDIUM -> 12.dp
        BigNumberSize.LARGE -> 12.dp
    }

    val paddingVertical = when (size) {
        BigNumberSize.EXTRA_SMALL -> 8.dp
        BigNumberSize.SMALL -> 8.dp
        BigNumberSize.MEDIUM -> 16.dp
        BigNumberSize.LARGE -> 16.dp
    }

    val autoSizeLabel = TextAutoSize.StepBased(minFontSize = 6.sp, maxFontSize = MaterialTheme.typography.bodySmall.fontSize)
    val autoSizeValue = TextAutoSize.StepBased(minFontSize = 6.sp, maxFontSize = style.fontSize)

    // The value height should match the line height of the text style used so that when the text
    // is auto-sized down, the height remains the same across multiple differently sized
    // instances.
    val valueHeight = with(LocalDensity.current) { style.lineHeight.toDp() }

    Surface(
        onClick = {
            onClick?.invoke()
        },
        enabled = onClick != null,
        modifier = modifier,
        color = containerColor.ifUnspecified(MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.medium,
    ) {

        Box(modifier = Modifier.padding(horizontal = paddingHorizontal, vertical = paddingVertical)) {

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Text(
                    maxLines = 1,
                    autoSize = autoSizeLabel,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    text = label,
                )

                Box(
                    modifier = Modifier
                        .height(valueHeight)
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (showDropDown) {
                                24.dp
                            } else {
                                0.dp
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        modifier = Modifier.wrapContentHeight(unbounded = true),
                        maxLines = 1,
                        autoSize = autoSizeValue,
                        overflow = TextOverflow.Clip,
                        color = valueColor.ifUnspecified(MaterialTheme.colorScheme.primary),
                        style = style,
                        text = value,
                    )
                }
            }
            if(showDropDown) {
                Icon(
                    modifier = Modifier.size(24.dp).align(Alignment.CenterEnd),
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
        }
    }
}

enum class BigNumberSize {
    EXTRA_SMALL,
    SMALL,
    MEDIUM,
    LARGE
}

@Preview
@Composable
private fun GasDisplayPreview() {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BigNumberDisplay(
            size = BigNumberSize.EXTRA_SMALL,
            value = "45m",
            label = "Oxygen MOD",
        )
        BigNumberDisplay(
            size = BigNumberSize.SMALL,
            value = "45m",
            label = "Oxygen MOD",
        )
        BigNumberDisplay(
            size = BigNumberSize.MEDIUM,
            value = "45m",
            label = "Oxygen MOD",
        )
        BigNumberDisplay(
            size = BigNumberSize.LARGE,
            value = "45m",
            label = "Oxygen MOD",
        )

        BigNumberDisplay(
            value = "45m",
            label = "Oxygen MOD",
            showDropDown = true,
        )

        BigNumberDisplay(
            modifier = Modifier.width(200.dp),
            value = "45m",
            label = "Oxygen MOD",
            showDropDown = true,
        )

        BigNumberDisplay(
            modifier = Modifier.width(100.dp),
            value = "100/0",
            label = "Mix",
            showDropDown = true,
        )
    }
}
