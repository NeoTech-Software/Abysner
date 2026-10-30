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

import abysner.composeapp.generated.resources.Res
import abysner.composeapp.generated.resources.ic_outline_add_circle_outline_24
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.zacsweers.metro.Inject
import org.jetbrains.compose.resources.painterResource
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.model.UnitSystem
import org.neotech.app.abysner.domain.settings.SettingsRepository
import org.neotech.app.abysner.presentation.component.appbar.DetailScreenTopAppBar
import org.neotech.app.abysner.presentation.component.textfield.LiveDecimalInputField
import org.neotech.app.abysner.presentation.component.textfield.SuffixVisualTransformation
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.presentation.theme.IconSet

// Metro supports @Inject on top-level functions, but the generated types are not resolved by the
// IDE, causing "Unresolved reference" errors. This wrapper class avoids those IDE errors.
// See: https://zacsweers.github.io/metro/latest/installation/#ide-support
@Inject
class GasPricesScreen(
    private val settingsRepository: SettingsRepository,
) {
    @Composable
    operator fun invoke(navController: NavHostController) {
        val settings by settingsRepository.settings.collectAsState()
        GasPricesScreen(
            navController = navController,
            gasPrices = settings.gasBlending.gasPrices,
            unitSystem = settings.unitSystem,
            onGasPriceChanged = { gas, pricePerLiter ->
                settingsRepository.updateSettings { current ->
                    val prices = current.gasBlending.gasPrices.toMutableMap()
                    if (pricePerLiter == null) {
                        prices.remove(gas)
                    } else {
                        prices[gas] = pricePerLiter
                    }
                    current.copy(gasBlending = current.gasBlending.copy(gasPrices = prices))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GasPricesScreen(
    navController: NavHostController = rememberNavController(),
    gasPrices: Map<Gas, Double>,
    unitSystem: UnitSystem,
    onGasPriceChanged: (Gas, Double?) -> Unit,
) {
    var showGasPicker by remember { mutableStateOf(false) }

    AbysnerTheme {
        Scaffold(
            topBar = {
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                DetailScreenTopAppBar(
                    title = "Gas prices",
                    showBackButton = currentBackStackEntry != null || LocalInspectionMode.current,
                    onNavigateUp = { navController.navigateUp() },
                )
            }
        ) { scaffoldPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp),
                    text = "What you pay per ${unitSystem.priceVolumeUnitLabel} of gas at 1 bar. " +
                            "Used to price a blend, and to show what each gas would cost when " +
                            "picking top-up gases.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val gases = gasPrices.keys
                    .sortedWith(compareBy({ it.oxygenPercentage }, { it.heliumPercentage }))

                if (gases.isEmpty()) {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 24.dp),
                        text = "No gases priced yet. Add the ones you buy.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                gases.forEach { gas ->
                    key(gas.oxygenPercentage, gas.heliumPercentage) {
                        GasPriceEntry(
                            gas = gas,
                            pricePerLiter = gasPrices.getValue(gas),
                            unitSystem = unitSystem,
                            onPriceChanged = { onGasPriceChanged(gas, it) },
                            onDelete = { onGasPriceChanged(gas, null) },
                        )
                    }
                }

                TextButton(
                    modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp),
                    onClick = { showGasPicker = true },
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(resource = Res.drawable.ic_outline_add_circle_outline_24),
                        contentDescription = null,
                    )
                    Text(modifier = Modifier.padding(start = 8.dp), text = "Add gas")
                }
            }
        }

        // Must stay inside AbysnerTheme: a Dialog starts its own composition, so anything placed
        // outside the theme falls back to the default Material colours and ignores dark mode.
        if (showGasPicker) {
            GasMixDialog(
                // Picking a gas that is already priced simply leaves its existing price alone.
                currentGas = Gas.Air,
                onDismiss = { showGasPicker = false },
                onGasSelected = { gas ->
                    showGasPicker = false
                    // Added at no cost, which is both a sane starting point and the honest value
                    // for air out of your own compressor.
                    if (gasPrices[gas] == null) {
                        onGasPriceChanged(gas, 0.0)
                    }
                },
            )
        }
    }
}

@Composable
private fun GasPriceEntry(
    gas: Gas,
    pricePerLiter: Double,
    unitSystem: UnitSystem,
    onPriceChanged: (Double) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp).padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = gas.diveIndustryName(),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Text(
                text = gas.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }

        // Clearing the text reports nothing, so a price being retyped never makes the row
        // disappear mid-edit. Removing a gas is what the delete button is for.
        LiveDecimalInputField(
            modifier = Modifier.weight(1f),
            initialValue = pricePerLiter.toDisplayPrice(unitSystem),
            resetKey = unitSystem,
            label = null,
            fractionDigits = unitSystem.priceFractionDigits,
            minValue = 0.0,
            maxValue = 9999.0,
            visualTransformation = SuffixVisualTransformation(" /${unitSystem.priceVolumeUnitLabel}"),
            supportingText = null,
            onValueChanged = { price -> onPriceChanged(price.toPricePerLiter(unitSystem)) },
        )

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = IconSet.delete,
                contentDescription = "Remove ${gas.diveIndustryName()}",
            )
        }
    }
}

@Preview
@Composable
fun GasPricesScreenPreview() {
    GasPricesScreen(
        gasPrices = mapOf(Gas.Oxygen to 0.008, Gas.Helium to 0.045, Gas.Air to 0.001),
        unitSystem = UnitSystem.METRIC,
        onGasPriceChanged = { _, _ -> },
    )
}

@Preview
@Composable
fun GasPricesScreenEmptyPreview() {
    GasPricesScreen(
        gasPrices = emptyMap(),
        unitSystem = UnitSystem.METRIC,
        onGasPriceChanged = { _, _ -> },
    )
}

@Preview
@Composable
fun GasPricesScreenImperialPreview() {
    GasPricesScreen(
        gasPrices = mapOf(
            Gas.Oxygen to 0.008,
            Gas.Air to 0.001,
            Gas(oxygenFraction = 0.4, heliumFraction = 0.0) to 0.0015,
        ),
        unitSystem = UnitSystem.IMPERIAL,
        onGasPriceChanged = { _, _ -> },
    )
}
