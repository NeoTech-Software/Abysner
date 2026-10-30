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

package org.neotech.app.abysner.presentation.screens.blending

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogCustomContent
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.gasblending.BlendCost
import org.neotech.app.abysner.domain.utilities.DecimalFormat
import org.neotech.app.abysner.presentation.component.InfoPill
import org.neotech.app.abysner.presentation.component.InfoPillSize
import org.neotech.app.abysner.presentation.component.AlertSeverity
import org.neotech.app.abysner.presentation.component.TextAlert
import org.neotech.app.abysner.presentation.component.list.SelectionListDefaults
import org.neotech.app.abysner.presentation.component.list.LazyColumnWithScrollIndicators
import org.neotech.app.abysner.presentation.component.textfield.OutlinedInputField
import org.neotech.app.abysner.presentation.component.textfield.number.NumberInputState
import org.neotech.app.abysner.presentation.component.textfield.number.rememberDecimalInputState
import org.neotech.app.abysner.presentation.component.textfield.SuffixVisualTransformation
import org.neotech.app.abysner.presentation.component.textfield.defaultInputFieldLabel
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import kotlin.math.roundToInt

/**
 * Compact single-line gas mix input. Shows the mix as "oxygen/helium" and opens [GasMixDialog] when
 * tapped.
 *
 * Deliberately not built on
 * [GasPickerComponent][org.neotech.app.abysner.presentation.component.GasPickerComponent]: that
 * component is two full-width sliders (too tall to repeat five times on one screen) and it cannot
 * express the gases blending needs, since its oxygen range starts at 4% and it forces nitrogen to
 * stay at or below 79%, making pure helium unreachable.
 */
@Composable
fun GasMixField(
    modifier: Modifier = Modifier,
    label: String,
    gas: Gas,
    /** When false only helium free gases can be picked, as is the case when blending nitrox. */
    allowHelium: Boolean = true,
    /**
     * Asked, per option in the picker, what choosing that gas would do to the blend. Null leaves the
     * options unannotated, which is what the start and target mixes want.
     */
    candidateCost: ((Gas) -> BlendCost)? = null,
    onGasSelected: (Gas) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        modifier = modifier.clickable { showDialog = true },
        value = TextFieldValue(gas.toString()),
        onValueChange = {},
        readOnly = true,
        enabled = false,
        singleLine = true,
        label = defaultInputFieldLabel(label),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            textAlign = TextAlign.Center,
            fontSize = 24.sp
        ),
        colors = OutlinedTextFieldDefaults.colors(
            // The field is only a button, it must not take keyboard input or show a cursor, but it
            // should still look like a normal enabled field. Same trick as CylinderSizeField.
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )

    if (showDialog) {
        GasMixDialog(
            currentGas = gas,
            allowHelium = allowHelium,
            candidateCost = candidateCost,
            onDismiss = { showDialog = false },
            onGasSelected = {
                showDialog = false
                onGasSelected(it)
            },
        )
    }
}

/**
 * Gases offered as presets when blending. Unlike
 * [Gas.StandardGases][org.neotech.app.abysner.domain.core.model.Gas.Companion.StandardGases] this
 * includes pure helium, which is not a breathing gas but is the usual helium source when blending
 * trimix.
 */
internal val blendingGases: List<Gas> = buildList {
    add(Gas.Air)
    add(Gas.Oxygen)
    add(Gas.Helium)
    addAll(
        Gas.StandardGases.filter {
            // Air and oxygen are already listed above. Helitrox 30/30 is dropped as it is not
            // something people actually blend.
            it != Gas.Air && it != Gas.Oxygen && it != Gas.Trimix3030
        }
    )
}

@Composable
internal fun GasMixDialog(
    currentGas: Gas,
    allowHelium: Boolean = true,
    candidateCost: ((Gas) -> BlendCost)? = null,
    onDismiss: () -> Unit,
    onGasSelected: (Gas) -> Unit,
) {
    val presets = if (allowHelium) {
        blendingGases
    } else {
        blendingGases.filter { it.heliumFraction == 0.0 }
    }
    val isPreset = presets.any { it == currentGas }
    val pagerState = rememberPagerState(initialPage = if (isPreset) TAB_PRESETS else TAB_CUSTOM) { 2 }
    val coroutineScope = rememberCoroutineScope()

    val oxygenInput = rememberDecimalInputState(
        initialValue = (currentGas.oxygenFraction * 100.0).roundToInt().toDouble(),
        fractionDigits = 0,
        minValue = 0.0,
        maxValue = 100.0,
    )
    val heliumInput = rememberDecimalInputState(
        initialValue = (currentGas.heliumFraction * 100.0).roundToInt().toDouble(),
        fractionDigits = 0,
        minValue = 0.0,
        maxValue = 100.0,
    )

    val oxygen = oxygenInput.value
    // A gas carried over from trimix mode may still hold helium while the helium field is hidden,
    // so the value has to be dropped rather than merely left out of the UI.
    val helium = if (allowHelium) heliumInput.value else 0.0
    val exceedsHundred = oxygen + helium > 100.0
    val isCustomValid = oxygenInput.isValid && (!allowHelium || heliumInput.isValid) && !exceedsHundred

    AlertDialogCustomContent(
        onDismissRequest = onDismiss,
        title = { Text("Gas mix") },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        confirmButton = {
            if (pagerState.currentPage == TAB_CUSTOM) {
                TextButton(
                    enabled = isCustomValid,
                    onClick = {
                        onGasSelected(
                            Gas(oxygenFraction = oxygen / 100.0, heliumFraction = helium / 100.0)
                        )
                    }
                ) {
                    Text("OK")
                }
            }
        },
        content = {
            Column(modifier = Modifier.heightIn(max = SelectionListDefaults.dialogContentMaxHeight)) {
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Tab(
                        selected = pagerState.currentPage == TAB_PRESETS,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(TAB_PRESETS) }
                        },
                        text = { Text("Presets") },
                    )
                    Tab(
                        selected = pagerState.currentPage == TAB_CUSTOM,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(TAB_CUSTOM) }
                        },
                        text = { Text("Custom") },
                    )
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) { page ->
                    when (page) {
                        TAB_PRESETS -> PresetGasTab(
                            presets = presets,
                            currentGas = currentGas,
                            candidateCost = candidateCost,
                            onGasSelected = onGasSelected,
                        )

                        TAB_CUSTOM -> CustomGasTab(
                            allowHelium = allowHelium,
                            oxygenInput = oxygenInput,
                            heliumInput = heliumInput,
                            exceedsHundred = exceedsHundred,
                            resultingGas = if (isCustomValid) {
                                Gas(oxygen / 100.0, helium / 100.0)
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun PresetGasTab(
    presets: List<Gas>,
    currentGas: Gas,
    candidateCost: ((Gas) -> BlendCost)?,
    onGasSelected: (Gas) -> Unit,
) {
    Column {
        if (candidateCost != null) {
            Text(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                text = "Cost shown is for the whole blend with that gas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumnWithScrollIndicators(modifier = Modifier.weight(1f)) {
            items(presets) { preset ->
                // Solving is microseconds, so re-running it per visible option as the list scrolls
                // is cheaper than caching it and risking a stale answer.
                val cost = candidateCost?.invoke(preset)
                // An option that cannot reach the target is dimmed rather than flagged in the error
                // colour: it is not a mistake, just a choice that does not apply here. It stays
                // selectable so the target can be explored from the other direction.
                val unusable = cost == BlendCost.NotBlendable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = SelectionListDefaults.rowMinHeight)
                        .clickable { onGasSelected(preset) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                        RadioButton(
                            selected = preset == currentGas,
                            onClick = { onGasSelected(preset) },
                        )
                    }
                    Text(
                        // Same "Name (oxygen/helium)" form the dive planner's standard gas picker
                        // uses, which is what tells the various nitrox and trimix presets apart.
                        text = "${preset.diveIndustryName()} ($preset)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (unusable) {
                            MaterialTheme.colorScheme.outline
                        } else {
                            Color.Unspecified
                        },
                        modifier = Modifier.padding(start = 8.dp).weight(1f),
                    )
                    when (cost) {
                        // An actual price is the one thing here worth acting on, so it gets the
                        // same pill treatment values get elsewhere in the app, while the two "no
                        // number available" cases stay as quiet grey text.
                        is BlendCost.Known -> InfoPill(
                            label = null,
                            value = DecimalFormat.format(2, cost.total),
                            size = InfoPillSize.SMALL,
                        )

                        BlendCost.NotBlendable -> CostAnnotation(
                            text = "not possible",
                            color = MaterialTheme.colorScheme.outline,
                        )

                        BlendCost.PriceMissing -> CostAnnotation(
                            text = "price unknown",
                            color = MaterialTheme.colorScheme.outline,
                        )

                        null -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun CostAnnotation(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        textAlign = TextAlign.End,
        maxLines = 1,
    )
}

@Composable
private fun CustomGasTab(
    allowHelium: Boolean,
    oxygenInput: NumberInputState<Double>,
    heliumInput: NumberInputState<Double>,
    exceedsHundred: Boolean,
    resultingGas: Gas?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedInputField(
                modifier = Modifier.weight(1f),
                state = oxygenInput,
                label = "Oxygen",
                visualTransformation = SuffixVisualTransformation(" %"),
                supportingText = null,
            )
            if (allowHelium) {
                OutlinedInputField(
                    modifier = Modifier.weight(1f),
                    state = heliumInput,
                    label = "Helium",
                    visualTransformation = SuffixVisualTransformation(" %"),
                    supportingText = null,
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        if (exceedsHundred) {
            TextAlert(
                modifier = Modifier.fillMaxWidth(),
                text = "Oxygen and helium cannot exceed 100%",
                alertSeverity = AlertSeverity.ERROR,
            )
        } else if (resultingGas != null) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = resultingGas.diveIndustryName(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val TAB_PRESETS = 0
private const val TAB_CUSTOM = 1

@Preview
@Composable
fun GasMixFieldPreview() {
    AbysnerTheme {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GasMixField(modifier = Modifier.weight(1f), label = "Current", gas = Gas.Air, onGasSelected = {})
            GasMixField(modifier = Modifier.weight(1f), label = "Target", gas = Gas.Trimix1845, onGasSelected = {})
        }
    }
}

@Preview
@Composable
fun GasMixDialogPresetsPreview() {
    AbysnerTheme {
        GasMixDialog(currentGas = Gas.Helium, onDismiss = {}, onGasSelected = {})
    }
}

@Preview
@Composable
fun GasMixDialogCustomPreview() {
    AbysnerTheme {
        GasMixDialog(currentGas = Gas(0.27, 0.13), onDismiss = {}, onGasSelected = {})
    }
}

/**
 * The dialog has to pick up whatever theme it is composed in. It only does so while it is created
 * inside [AbysnerTheme]: a Dialog starts its own composition, and one placed outside the theme
 * silently falls back to the default Material colours.
 */
@Preview
@Composable
fun GasMixDialogDarkPreview() {
    AbysnerTheme(darkTheme = true) {
        GasMixDialog(currentGas = Gas.Oxygen, onDismiss = {}, onGasSelected = {})
    }
}

@Preview
@Composable
fun GasMixDialogNitroxOnlyPresetsPreview() {
    AbysnerTheme {
        GasMixDialog(
            currentGas = Gas.Nitrox32,
            allowHelium = false,
            onDismiss = {},
            onGasSelected = {},
        )
    }
}

@Preview
@Composable
fun GasMixDialogNitroxOnlyCustomPreview() {
    AbysnerTheme {
        GasMixDialog(
            currentGas = Gas(0.27, 0.0),
            allowHelium = false,
            onDismiss = {},
            onGasSelected = {},
        )
    }
}
