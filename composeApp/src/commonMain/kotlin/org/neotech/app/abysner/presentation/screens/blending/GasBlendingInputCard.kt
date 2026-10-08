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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.model.UnitSystem
import org.neotech.app.abysner.domain.settings.model.GasBlendingSettings
import org.neotech.app.abysner.domain.core.physics.asBarToPsi
import org.neotech.app.abysner.domain.core.physics.asPsiToBar
import org.neotech.app.abysner.presentation.component.SingleChoiceSegmentedButtonRow
import org.neotech.app.abysner.presentation.component.preferences.SettingsSubTitle
import org.neotech.app.abysner.presentation.component.rememberSingleChoiceSegmentedButtonRowState
import org.neotech.app.abysner.presentation.component.textfield.OutlinedDecimalInputField
import org.neotech.app.abysner.presentation.component.textfield.SuffixVisualTransformation
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.presentation.utilities.pressureUnitLabel

@Composable
internal fun GasBlendingInputCard(
    modifier: Modifier = Modifier,
    blending: GasBlendingSettings,
    unitSystem: UnitSystem,
    callbacks: GasBlendingCallbacks,
) {
    // Nitrox is blended from two gases, which can only balance total volume and oxygen, so helium
    // has no place in any of the mixes while that mode is selected.
    val allowHelium = blending.trimixMode
    val mode = if (blending.trimixMode) BlendingMode.TRIMIX else BlendingMode.NITROX

    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth()) {

            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleLarge,
                text = "Blend",
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 16.dp),
                items = BlendingMode.entries.toImmutableList(),
                singleChoiceSegmentedButtonRowState = rememberSingleChoiceSegmentedButtonRowState(
                    initialSelectedIndex = mode.ordinal
                ),
                onClick = { mode, _ -> callbacks.onModeChanged(mode) },
                label = { mode, _ -> Text(text = mode.humanReadableName, maxLines = 1) },
            )

            SettingsSubTitle(subTitle = "Currently in the cylinder")

            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PressureField(
                    modifier = Modifier.weight(1f),
                    label = "Pressure",
                    pressureInBar = blending.startPressure,
                    unitSystem = unitSystem,
                    onPressureChanged = callbacks.onStartPressureChanged,
                )
                GasMixField(
                    modifier = Modifier.weight(1f),
                    label = "Mix",
                    gas = blending.startGas,
                    allowHelium = allowHelium,
                    onGasSelected = callbacks.onStartGasChanged,
                )
            }

            SettingsSubTitle(subTitle = "Target")

            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PressureField(
                    modifier = Modifier.weight(1f),
                    label = "Pressure",
                    pressureInBar = blending.targetPressure,
                    unitSystem = unitSystem,
                    onPressureChanged = callbacks.onTargetPressureChanged,
                )
                GasMixField(
                    modifier = Modifier.weight(1f),
                    label = "Mix",
                    gas = blending.targetGas,
                    allowHelium = allowHelium,
                    onGasSelected = callbacks.onTargetGasChanged,
                )
            }

            SettingsSubTitle(subTitle = "Top-up gases")

            Text(
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp),
                text = "Added in this order. The amounts do not depend on the order, but the " +
                        "pressures you fill to obviously do.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GasMixField(
                    modifier = Modifier.fillMaxWidth(),
                    label = "First",
                    gas = blending.firstTopUpGas,
                    allowHelium = allowHelium,
                    onGasSelected = { callbacks.onTopUpGasChanged(TopUpSlot.FIRST, it) },
                )
                AnimatedVisibility(visible = blending.trimixMode) {
                    GasMixField(
                        modifier = Modifier.fillMaxWidth(),
                        label = "Second",
                        gas = blending.secondTopUpGas,
                        onGasSelected = { callbacks.onTopUpGasChanged(TopUpSlot.SECOND, it) },
                    )
                }
                GasMixField(
                    modifier = Modifier.fillMaxWidth(),
                    label = "Final",
                    gas = blending.finalTopUpGas,
                    allowHelium = allowHelium,
                    onGasSelected = { callbacks.onTopUpGasChanged(TopUpSlot.FINAL, it) },
                )
            }
        }
    }
}

/**
 * Pressure input that shows and accepts the unit the user has configured, while reporting bar.
 *
 * The displayed value is kept in local state and only pushed outwards, never fed back in:
 * [OutlinedDecimalInputField] re-keys its internal text state whenever `initialValue` changes, so
 * round-tripping every keystroke through the view model would discard partial input.
 */
@Composable
private fun PressureField(
    modifier: Modifier = Modifier,
    label: String,
    pressureInBar: Double,
    unitSystem: UnitSystem,
    onPressureChanged: (Double) -> Unit,
) {
    var displayValue: Double? by remember(unitSystem) {
        mutableStateOf(
            when (unitSystem) {
                UnitSystem.METRIC -> pressureInBar
                UnitSystem.IMPERIAL -> pressureInBar.asBarToPsi()
            }
        )
    }

    OutlinedDecimalInputField(
        modifier = modifier,
        initialValue = displayValue,
        label = label,
        fractionDigits = 0,
        minValue = 0.0,
        maxValue = when (unitSystem) {
            UnitSystem.METRIC -> MAX_PRESSURE_BAR
            UnitSystem.IMPERIAL -> MAX_PRESSURE_PSI
        },
        visualTransformation = SuffixVisualTransformation(" ${unitSystem.pressureUnitLabel}"),
        onNumberChanged = { value ->
            displayValue = value
            if (value != null) {
                onPressureChanged(
                    when (unitSystem) {
                        UnitSystem.METRIC -> value
                        UnitSystem.IMPERIAL -> value.asPsiToBar()
                    }
                )
            }
        },
        supportingText = null,
    )
}

/**
 * Well below the ~930 bar where the real gas model's iterative pressure solve stops converging, and
 * far above any cylinder a diver fills.
 */
private const val MAX_PRESSURE_BAR = 300.0
private const val MAX_PRESSURE_PSI = 4500.0

@Preview
@Composable
fun GasBlendingInputCardPreview() {
    AbysnerTheme {
        GasBlendingInputCard(
            modifier = Modifier.padding(16.dp),
            blending = GasBlendingSettings(),
            unitSystem = UnitSystem.METRIC,
            callbacks = GasBlendingCallbacks(),
        )
    }
}

@Preview
@Composable
fun GasBlendingInputCardTrimixPreview() {
    AbysnerTheme {
        GasBlendingInputCard(
            modifier = Modifier.padding(16.dp),
            blending = GasBlendingSettings(
                trimixMode = true,
                targetGas = Gas.Trimix1845,
            ),
            unitSystem = UnitSystem.METRIC,
            callbacks = GasBlendingCallbacks(),
        )
    }
}
