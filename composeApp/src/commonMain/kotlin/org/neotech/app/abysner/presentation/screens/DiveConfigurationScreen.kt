/*
 * Abysner - Dive planner
 * Copyright (C) 2025-2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import androidx.compose.ui.tooling.preview.Preview
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Salinity
import org.neotech.app.abysner.domain.diveplanning.PlanningRepository
import org.neotech.app.abysner.domain.utilities.DecimalFormat
import org.neotech.app.abysner.presentation.component.appendBold
import org.neotech.app.abysner.presentation.component.preferences.CcrSetpointPreference
import org.neotech.app.abysner.presentation.component.preferences.DecimalNumberPreference
import org.neotech.app.abysner.presentation.component.preferences.GradientFactorPreference
import org.neotech.app.abysner.presentation.component.preferences.NumberPreference
import org.neotech.app.abysner.presentation.component.preferences.SettingsSubTitle
import org.neotech.app.abysner.presentation.component.preferences.SingleChoicePreference
import org.neotech.app.abysner.presentation.component.preferences.SwitchPreference
import org.neotech.app.abysner.presentation.component.textfield.SuffixVisualTransformation
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import kotlin.math.abs


typealias DiveConfigurationScreen = @Composable (navController: NavHostController) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Inject
@Composable
fun DiveConfigurationScreen(
    planningRepository: PlanningRepository,
    @Assisted navController: NavHostController = rememberNavController()
) {
    val configuration by planningRepository.configuration.collectAsState()
    DiveConfigurationScreen(
        navController = navController,
        configuration = configuration,
        updateConfiguration = planningRepository::updateConfiguration,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiveConfigurationScreen(
    navController: NavHostController = rememberNavController(),
    configuration: Configuration,
    updateConfiguration: ((Configuration) -> Configuration) -> Unit,
) {
    AbysnerTheme {
        Scaffold(
            topBar = {
                Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.background) {
                    TopAppBar(
                        title = { Text("Dive configuration") },
                        navigationIcon = {

                            val currentBackStackEntry by navController.currentBackStackEntryAsState()
                            if (currentBackStackEntry != null || LocalInspectionMode.current) {
                                IconButton(onClick = {
                                    navController.navigateUp()
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        },
                    )

                }
            }
        ) { paddingValues ->
            Box(
                Modifier
                    .verticalScroll(rememberScrollState())
            ) {


                Column(modifier = Modifier.padding(paddingValues)) {
                    SettingsSubTitle(subTitle = "Algorithm")

                    SingleChoicePreference(
                        label = "Model",
                        description = "The type decompression model to use.",
                        selectedItemIndex = Configuration.Algorithm.entries.indexOf(
                            configuration.algorithm
                        ),
                        items = Configuration.Algorithm.entries.toImmutableList(),
                        itemToStringMapper = {
                            it.shortName
                        }
                    ) { algorithm ->
                        updateConfiguration { it.copy(algorithm = algorithm) }
                    }

                    GradientFactorPreference(
                        label = "Gradient factor",
                        description = "The GF low and high settings change the conservatism used by the decompression model.",
                        gfLow = (configuration.gfLow * 100.0).toInt(),
                        gfHigh = (configuration.gfHigh * 100.0).toInt(),
                    ) { gfLowNew, gfHighNew ->
                        updateConfiguration {
                            it.copy(
                                gfLow = gfLowNew / 100.0,
                                gfHigh = gfHighNew / 100.0
                            )
                        }
                    }

                    SettingsSubTitle(subTitle = "Environment")

                    SingleChoicePreference(
                        label = "Salinity",
                        description = "The type of water. Saltier water is heavier and increases pressure at depth.",
                        selectedItemIndex = Salinity.entries.indexOf(configuration.salinity),
                        items = Salinity.entries.toImmutableList(),
                        itemToStringMapper = {
                            buildAnnotatedString {
                                appendBold(it.humanReadableName)
                                append(" (${DecimalFormat.format(1, it.density)} kg/m3)")
                            }
                        },
                        selectedItemToStringMapper = {
                            it.humanReadableName
                        }
                    ) { salinity ->
                        updateConfiguration { it.copy(salinity = salinity) }
                    }

                    NumberPreference(
                        label = "Altitude",
                        description = "The altitude of the water surface at which the dive is taking place, in most cases this will be 0 meter (sea level).",
                        initialValue = configuration.altitude.toInt(),
                        minValue = -450,
                        maxValue = 3000,
                        valueFormatter = { "$it m"},
                        textFieldVisualTransformation = SuffixVisualTransformation(" m")
                    ) { altitude ->
                        updateConfiguration { it.copy(altitude = altitude.toDouble()) }
                    }


                    SettingsSubTitle(subTitle = "Diver")
                    NumberPreference(
                        label = "Ascent speed",
                        description = "The speed at which the diver is planning to ascent to stops or the surface.",
                        initialValue = configuration.maxAscentRate.toInt(),
                        minValue = 1,
                        maxValue = 18,
                        valueFormatter = { "$it m/min"},
                        textFieldVisualTransformation = SuffixVisualTransformation(" m/min")
                    ) { ascentRate ->
                        updateConfiguration { it.copy(maxAscentRate = ascentRate.toDouble()) }
                    }
                    NumberPreference(
                        label = "Descent speed",
                        description = "The speed at which the diver is planning to descent to planned bottom sections.",
                        initialValue = configuration.maxDescentRate.toInt(),
                        minValue = 1,
                        maxValue = 40,
                        valueFormatter = { "$it m/min"},
                        textFieldVisualTransformation = SuffixVisualTransformation(" m/min")
                    ) { descentRate ->
                        updateConfiguration { it.copy(maxDescentRate = descentRate.toDouble()) }
                    }
                    NumberPreference(
                        label = "Gas usage",
                        description = "The average amount of gas the diver is breathing per minute at 1 atmosphere during normal diving conditions. This is also known as SAC or RMV rate.",
                        initialValue = configuration.sacRate.toInt(),
                        minValue = 5,
                        maxValue = 99,
                        valueFormatter = { "$it l/min"},
                        textFieldVisualTransformation = SuffixVisualTransformation(" l/min")
                    ) { sacRate ->
                        updateConfiguration { it.copy(sacRate = sacRate.toDouble()) }
                    }

                    NumberPreference(
                        label = "Gas usage emergency",
                        description = "The average amount of gas a diver is breathing per minute at 1 atmosphere during an emergency scenario. This is also known as the panic SAC or RMV rate.",
                        initialValue = configuration.sacRateOutOfAir.toInt(),
                        minValue = 5,
                        maxValue = 99,
                        valueFormatter = { "$it l/min"},
                        textFieldVisualTransformation = SuffixVisualTransformation(" l/min")
                    ) { sacRate ->
                        updateConfiguration { it.copy(sacRateOutOfAir = sacRate.toDouble()) }
                    }

                    SettingsSubTitle(subTitle = "Decompression & Planing")

                    /*
                    SwitchPreference(
                        label = "Force minimal stop time",
                        value = "If while ascending to a deco stop the diver already off-gassed enough, force a minimal deco stop of 1 minute instead of skipping the stop.",
                        isChecked = configuration.forceMinimalDecoStopTime
                    ) { isChecked ->
                        onConfigurationChanged(configuration.copy(forceMinimalDecoStopTime = isChecked))
                    }
                     */

                    SingleChoicePreference(
                        label = "Deco stop interval",
                        description = "The interval at which to make deco stops.",
                        items = persistentListOf(3, 6, 9),
                        selectedItemIndex = when (configuration.decoStepSize) {
                            3 -> 0
                            6 -> 1
                            9 -> 2
                            else -> 1
                        },
                        itemToStringMapper = {
                            "$it m"
                        }
                    ) { decoStepSize ->
                        updateConfiguration { it.copy(decoStepSize = decoStepSize) }
                    }

                    SingleChoicePreference(
                        label = "Last deco stop",
                        description = "Depth at which the last deco stop will be made.",
                        items = persistentListOf(3, 6, 9),
                        selectedItemIndex = when (configuration.lastDecoStopDepth) {
                            3 -> 0
                            6 -> 1
                            9 -> 2
                            else -> 0
                        },
                        itemToStringMapper = {
                            "$it m"
                        }
                    ) { lastDecoStopDepth ->
                        updateConfiguration { it.copy(lastDecoStopDepth = lastDecoStopDepth) }
                    }

                    NumberPreference(
                        label = "Gas switch time",
                        description = "Adds a flat section to the profile at each gas switch, to account for the time needed to switch gases in open-circuit/bailout mode.",
                        initialValue = configuration.gasSwitchTime,
                        minValue = 0,
                        maxValue = 5,
                        valueFormatter = { "$it min"},
                        textFieldVisualTransformation = SuffixVisualTransformation(" min")
                    ) { gasSwitchTime ->
                        updateConfiguration { it.copy(gasSwitchTime = gasSwitchTime) }
                    }

                    val allowedPpO2values = persistentListOf(1.2, 1.3, 1.4, 1.5, 1.6)

                    fun Iterable<Double>.selectedIndex(value: Double): Int? =
                        withIndex().minByOrNull { abs(it.value - value) }?.index

                    SingleChoicePreference(
                        label = "Max PPO2",
                        description = "Maximum allowed PPO2 during the dive (except for decompression).",
                        items = allowedPpO2values,
                        selectedItemIndex = allowedPpO2values.selectedIndex(configuration.maxPPO2) ?: 2,
                        itemToStringMapper = {
                            "$it"
                        }
                    ) { maxPPO2Travel ->
                        updateConfiguration { it.copy(maxPPO2 = maxPPO2Travel) }
                    }

                    SingleChoicePreference(
                        label = "Max deco PPO2",
                        description = "Maximum allowed PPO2 during decompression stops and ascents to decompression stops.",
                        items = allowedPpO2values,
                        selectedItemIndex = allowedPpO2values.selectedIndex(configuration.maxPPO2Deco) ?: 3,
                        itemToStringMapper = {
                            "$it"
                        }
                    ) { maxPPO2 ->
                        updateConfiguration { it.copy(maxPPO2Deco = maxPPO2) }
                    }

                    SettingsSubTitle(subTitle = "Multi-level")

                    SwitchPreference(
                        label = "Use deco gas between sections",
                        value = "If ascending from one section of a multi-level dive to another allow the automatic usage of deco gas. Gas will be switched back to the chosen gas for that section once the desired depth is reached.",
                        isChecked = configuration.useDecoGasBetweenSections
                    ) { isChecked ->
                        updateConfiguration { it.copy(useDecoGasBetweenSections = isChecked) }
                    }

                    SettingsSubTitle(subTitle = "Contingency plan")

                    NumberPreference(
                        label = "Deeper",
                        description = "How much deeper the contingency plan should be, this is added to the deepest section of the planned dive.",
                        initialValue = configuration.contingencyDeeper,
                        minValue = 0,
                        maxValue = 5,
                        valueFormatter = { "$it m"},
                        textFieldVisualTransformation = SuffixVisualTransformation(" m")
                    ) { deeper ->
                        updateConfiguration { it.copy(contingencyDeeper = deeper) }
                    }

                    NumberPreference(
                        label = "Longer",
                        description = "How much longer the contingency plan should be, this is added to the deepest section of the planned dive.",
                        initialValue = configuration.contingencyLonger,
                        minValue = 0,
                        maxValue = 5,
                        valueFormatter = { "$it min"},
                        textFieldVisualTransformation = SuffixVisualTransformation(" min")
                    ) { longer ->
                        updateConfiguration { it.copy(contingencyLonger = longer) }
                    }

                    SettingsSubTitle(subTitle = "CCR")

                    CcrSetpointPreference(
                        label = "Low setpoint",
                        description = "The CCR setpoint used during descent, with optional auto-switch depth to the high setpoint.",
                        setpoint = configuration.ccrLowSetpoint,
                        switchDepth = configuration.ccrToHighSetpointSwitchDepth,
                    ) { setpoint, switchDepth ->
                        updateConfiguration { it.copy(ccrLowSetpoint = setpoint, ccrToHighSetpointSwitchDepth = switchDepth) }
                    }

                    CcrSetpointPreference(
                        label = "High setpoint",
                        description = "The CCR setpoint used during bottom time and ascent, with optional auto-switch depth to the low setpoint.",
                        setpoint = configuration.ccrHighSetpoint,
                        switchDepth = configuration.ccrToLowSetpointSwitchDepth,
                    ) { setpoint, switchDepth ->
                        updateConfiguration { it.copy(ccrHighSetpoint = setpoint, ccrToLowSetpointSwitchDepth = switchDepth) }
                    }

                    DecimalNumberPreference(
                        label = "Loop volume",
                        description = "Total internal loop volume in liters (counter-lung, scrubber, hoses). Used to calculate diluent usage from loop expansion during descent.",
                        initialValue = configuration.ccrLoopVolumeLiters,
                        minValue = 0.1,
                        maxValue = 20.0,
                        fractionDigits = 1,
                        valueFormatter = { "$it L" },
                        textFieldVisualTransformation = SuffixVisualTransformation(" L"),
                    ) { volume ->
                        updateConfiguration { it.copy(ccrLoopVolumeLiters = volume) }
                    }

                    DecimalNumberPreference(
                        label = "Metabolic oxygen rate",
                        description = "Oxygen consumption rate in liters per minute. Used to calculate oxygen usage for CCR dives.",
                        initialValue = configuration.ccrMetabolicO2LitersPerMinute,
                        minValue = 0.1,
                        maxValue = 3.0,
                        fractionDigits = 1,
                        valueFormatter = { "$it L/min" },
                        textFieldVisualTransformation = SuffixVisualTransformation(" L/min"),
                    ) { rate ->
                        updateConfiguration { it.copy(ccrMetabolicO2LitersPerMinute = rate) }
                    }
                }
            }
        }
    }
}


@Preview
@Composable
fun DiveConfigurationScreenPreview() {
    DiveConfigurationScreen(
        configuration = Configuration(),
        updateConfiguration = {}
    )
}

