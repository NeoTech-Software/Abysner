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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.model.UnitSystem
import org.neotech.app.abysner.domain.core.physics.LITERS_PER_CUBIC_FOOT
import org.neotech.app.abysner.domain.gasblending.GasBlendingPlanner
import org.neotech.app.abysner.domain.gasblending.model.GasBlendingInput
import org.neotech.app.abysner.domain.gasblending.model.GasBlendingPlan
import org.neotech.app.abysner.domain.utilities.DecimalFormat
import org.neotech.app.abysner.presentation.component.CylinderSizeField
import org.neotech.app.abysner.presentation.component.preferences.SettingsSubTitle
import org.neotech.app.abysner.presentation.component.textfield.OutlinedDecimalInputField
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.presentation.utilities.formatVolume
import org.neotech.app.abysner.presentation.utilities.volumeUnitLabel

/**
 * How much of each gas a blend consumes, and what that costs.
 *
 * The cylinder volume lives here rather than with the blend inputs because it is the only thing it
 * affects: the fill pressures in the blending plan are the same whatever the cylinder holds, only
 * the absolute amounts of gas (and therefore the price) scale with it.
 */
@Composable
internal fun GasNeededCard(
    modifier: Modifier = Modifier,
    plan: GasBlendingPlan,
    cylinderSize: Cylinder.Size,
    unitSystem: UnitSystem,
    gasPrices: Map<Gas, Double>,
    onCylinderSizeChanged: (Cylinder.Size) -> Unit,
    onGasPriceChanged: (Gas, Double?) -> Unit,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth()) {

            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleLarge,
                text = "Gas needed",
            )

            SettingsSubTitle(subTitle = "Cylinder")

            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CylinderSizeField(
                    modifier = Modifier.weight(1f),
                    cylinderSize = cylinderSize,
                    unitSystem = unitSystem,
                    onCylinderSizeSelected = onCylinderSizeChanged,
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            val volumePerGas = (plan as? GasBlendingPlan.Success)?.volumePerGas.orEmpty()
            if (volumePerGas.isEmpty()) {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 16.dp),
                    text = "No gas is needed for this blend.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            SettingsSubTitle(subTitle = "Price")

            Text(
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp),
                text = "Price per ${unitSystem.priceVolumeUnitLabel} of gas at 1 bar, the way gas " +
                        "is usually sold. Prices are remembered per gas, so they only need filling " +
                        "in once.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val pricedGases = volumePerGas.filterKeys { gasPrices.containsKey(it) }
            val total = pricedGases.entries.sumOf { (gas, volume) -> gasPrices.getValue(gas) * volume }

            volumePerGas.forEach { (gas, volume) ->
                // Keyed so each row keeps its own text state as the blend (and therefore the set of
                // gases) changes.
                key(gas.oxygenPercentage, gas.heliumPercentage) {
                    GasPriceRow(
                        gas = gas,
                        volume = volume,
                        pricePerLiter = gasPrices[gas],
                        unitSystem = unitSystem,
                        onGasPriceChanged = onGasPriceChanged,
                    )
                }
            }

            if (pricedGases.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Total",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = DecimalFormat.format(2, total),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Composable
private fun GasPriceRow(
    gas: Gas,
    volume: Double,
    pricePerLiter: Double?,
    unitSystem: UnitSystem,
    onGasPriceChanged: (Gas, Double?) -> Unit,
) {
    // Held locally and only pushed outwards: OutlinedDecimalInputField re-keys its text state
    // whenever initialValue changes, so feeding the stored price straight back in would discard
    // partial input on every keystroke.
    var displayPrice: Double? by remember(unitSystem) {
        mutableStateOf(pricePerLiter?.toDisplayPrice(unitSystem))
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(0.9f)) {
            Text(
                text = gas.diveIndustryName(),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Text(
                text = "$gas · ${volume.formatVolume(unitSystem, decimals = if (unitSystem == UnitSystem.IMPERIAL) 1 else 0)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }

        OutlinedDecimalInputField(
            modifier = Modifier.weight(1f),
            initialValue = displayPrice,
            label = "Price / ${unitSystem.priceVolumeUnitLabel}",
            fractionDigits = unitSystem.priceFractionDigits,
            minValue = 0.0,
            maxValue = 9999.0,
            onNumberChanged = { value ->
                displayPrice = value
                onGasPriceChanged(gas, value?.toPricePerLiter(unitSystem))
            },
            supportingText = null,
        )

        Text(
            modifier = Modifier.weight(0.7f),
            textAlign = TextAlign.End,
            text = pricePerLiter?.let { DecimalFormat.format(2, it * volume) } ?: "—",
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
        )
    }
}

/**
 * The denominator prices are entered and shown in. Gas is sold by the cubic meter in metric
 * countries and by the cubic foot in imperial ones, and both give numbers of a sensible size, unlike
 * a price per liter which would come out as something like 0.045.
 */
private val UnitSystem.priceVolumeUnitLabel: String
    get() = when (this) {
        UnitSystem.METRIC -> "m³"
        UnitSystem.IMPERIAL -> "ft³"
    }

/** Cubic meter prices are whole currency units, cubic foot prices are an order of magnitude smaller. */
private val UnitSystem.priceFractionDigits: Int
    get() = when (this) {
        UnitSystem.METRIC -> 2
        UnitSystem.IMPERIAL -> 3
    }

/** Liters of gas at 1 bar in the volume prices are quoted per. */
private val UnitSystem.litersPerPriceUnit: Double
    get() = when (this) {
        UnitSystem.METRIC -> LITERS_PER_CUBIC_METER
        UnitSystem.IMPERIAL -> LITERS_PER_CUBIC_FOOT
    }

/**
 * Prices are stored per liter, the same way pressures are stored in bar, and converted to the
 * denominator the user actually enters them in.
 */
private fun Double.toDisplayPrice(unitSystem: UnitSystem): Double = this * unitSystem.litersPerPriceUnit

private fun Double.toPricePerLiter(unitSystem: UnitSystem): Double = this / unitSystem.litersPerPriceUnit

private const val LITERS_PER_CUBIC_METER = 1000.0

private fun previewPlan(
    startGas: Gas = Gas.Air,
    startPressure: Double = 50.0,
    targetGas: Gas = Gas.Nitrox32,
    topUpGases: List<Gas> = listOf(Gas.Oxygen, Gas.Air),
): GasBlendingPlan = GasBlendingPlanner().plan(
    GasBlendingInput(
        cylinderSize = Cylinder.STEEL_12L,
        startGas = startGas,
        startPressure = startPressure,
        targetGas = targetGas,
        targetPressure = 200.0,
        topUpGases = topUpGases,
    )
)

@Preview
@Composable
fun GasNeededCardPreview() {
    AbysnerTheme {
        GasNeededCard(
            modifier = Modifier.padding(16.dp),
            plan = previewPlan(),
            cylinderSize = Cylinder.STEEL_12L,
            unitSystem = UnitSystem.METRIC,
            // Stored per liter: 8.00 and 1.00 per cubic meter.
            gasPrices = mapOf(Gas.Oxygen to 0.008, Gas.Air to 0.001),
            onCylinderSizeChanged = {},
            onGasPriceChanged = { _, _ -> },
        )
    }
}

@Preview
@Composable
fun GasNeededCardNoPricesPreview() {
    AbysnerTheme {
        GasNeededCard(
            modifier = Modifier.padding(16.dp),
            plan = previewPlan(
                startPressure = 20.0,
                targetGas = Gas.Trimix1845,
                topUpGases = listOf(Gas.Oxygen, Gas.Helium, Gas.Air),
            ),
            cylinderSize = Cylinder.D12,
            unitSystem = UnitSystem.METRIC,
            gasPrices = emptyMap(),
            onCylinderSizeChanged = {},
            onGasPriceChanged = { _, _ -> },
        )
    }
}

@Preview
@Composable
fun GasNeededCardTrimixPricedPreview() {
    AbysnerTheme {
        GasNeededCard(
            modifier = Modifier.padding(16.dp),
            plan = previewPlan(
                startPressure = 20.0,
                targetGas = Gas.Trimix1845,
                topUpGases = listOf(Gas.Oxygen, Gas.Helium, Gas.Air),
            ),
            cylinderSize = Cylinder.D12,
            unitSystem = UnitSystem.METRIC,
            gasPrices = mapOf(Gas.Oxygen to 0.008, Gas.Helium to 0.045, Gas.Air to 0.001),
            onCylinderSizeChanged = {},
            onGasPriceChanged = { _, _ -> },
        )
    }
}

@Preview
@Composable
fun GasNeededCardImperialPreview() {
    AbysnerTheme {
        GasNeededCard(
            modifier = Modifier.padding(16.dp),
            plan = previewPlan(),
            cylinderSize = Cylinder.AL80,
            unitSystem = UnitSystem.IMPERIAL,
            gasPrices = mapOf(Gas.Oxygen to 0.008, Gas.Air to 0.001),
            onCylinderSizeChanged = {},
            onGasPriceChanged = { _, _ -> },
        )
    }
}
