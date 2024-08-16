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

package org.neotech.app.abysner.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialogCustomContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.neotech.app.abysner.domain.diveplanning.PlanningRepository
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Salinity
import org.neotech.app.abysner.domain.utilities.DecimalFormat
import org.neotech.app.abysner.presentation.component.appendBold
import org.neotech.app.abysner.presentation.component.preferences.BasicPreference
import org.neotech.app.abysner.presentation.component.preferences.NumberPreference
import org.neotech.app.abysner.presentation.component.preferences.SettingsSubTitle
import org.neotech.app.abysner.presentation.component.preferences.SingleChoicePreference
import org.neotech.app.abysner.presentation.component.preferences.SwitchPreference
import org.neotech.app.abysner.presentation.component.textfield.OutlinedNumberInputField
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
        ) {
            Box(
                Modifier
                    .verticalScroll(rememberScrollState())
            ) {

                val configuration by planningRepository.configuration.collectAsState()

                Column(modifier = Modifier.padding(it)) {
                    SettingsSubTitle(subTitle = "Algorithm")

                    SingleChoicePreference(
                        label = "Model",
                        description = "The type decompression model to use.",
                        selectedItemIndex = Configuration.Algorithm.entries.indexOf(
                            configuration.algorithm
                        ),
                        items = Configuration.Algorithm.entries,
                        itemToStringMapper = {
                            it.shortName
                        }
                    ) { algorithm ->
                        planningRepository.updateConfiguration {
                            it.copy(algorithm = algorithm)
                        }
                    }

                    GradientFactorPreference(
                        label = "Gradient factor",
                        description = "The GF low and high settings change the conservatism used by the decompression model.",
                        gfLow = (configuration.gfLow * 100.0).toInt(),
                        gfHigh = (configuration.gfHigh * 100.0).toInt(),
                    ) { gfLowNew, gfHighNew ->
                        planningRepository.updateConfiguration {
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
                        items = Salinity.entries,
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
                        planningRepository.updateConfiguration {
                            it.copy(salinity = salinity)
                        }
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
                        planningRepository.updateConfiguration {
                            it.copy(altitude = altitude.toDouble())
                        }
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
                        planningRepository.updateConfiguration {
                            it.copy(maxAscentRate = ascentRate.toDouble())
                        }
                    }
                    NumberPreference(
                        label = "Descent speed",
                        description = "The speed at which the diver is planning to descent to planned bottom sections.",
                        initialValue = configuration.maxDescentRate.toInt(),
                        minValue = 1,
                        maxValue = 40,
                        valueFormatter = { "$it m/min"},
                        textFieldVisualTransformation = SuffixVisualTransformation(" m/min")
                    ) {descentRate ->
                        planningRepository.updateConfiguration {
                            it.copy(maxDescentRate = descentRate.toDouble())
                        }
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
                        planningRepository.updateConfiguration {
                            it.copy(sacRate = sacRate.toDouble())
                        }
                    }

                    NumberPreference(
                        label = "Gas usage out-of-air",
                        description = "The average amount of gas the diver is breathing per minute at 1 atmosphere during an out-of-air scenario. This is also known as the panic SAC or RMV rate.",
                        initialValue = configuration.sacRateOutOfAir.toInt(),
                        minValue = 5,
                        maxValue = 99,
                        valueFormatter = { "$it l/min"},
                        textFieldVisualTransformation = SuffixVisualTransformation(" l/min")
                    ) { sacRate ->
                        planningRepository.updateConfiguration {
                            it.copy(sacRateOutOfAir = sacRate.toDouble())
                        }
                    }

                    SettingsSubTitle(subTitle = "Decompression & Planing")

                    /*
                    SwitchPreference(
                        label = "Force minimal stop time",
                        value = "If while ascending to a deco stop the diver already off-gassed enough, force a minimal deco stop of 1 minute instead of skipping the stop.",
                        isChecked = configuration.forceMinimalDecoStopTime
                    ) { isChecked ->
                        planningRepository.updateConfiguration {
                            it.copy(forceMinimalDecoStopTime = isChecked)
                        }
                    }
                     */

                    SingleChoicePreference(
                        label = "Deco stop interval",
                        description = "The interval at which to make deco stops.",
                        items = listOf(3, 6, 9),
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
                        planningRepository.updateConfiguration {
                            it.copy(decoStepSize = decoStepSize)
                        }
                    }

                    SingleChoicePreference(
                        label = "Last deco stop",
                        description = "Depth at which the last deco stop will be made.",
                        items = listOf(3, 6, 9),
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
                        planningRepository.updateConfiguration {
                            it.copy(lastDecoStopDepth = lastDecoStopDepth)
                        }
                    }

                    val allowedPPO2values = listOf(1.2, 1.3, 1.4, 1.5, 1.6)

                    fun Iterable<Double>.indexByNearest(target: Double): Int? {
                        var bestMatch: Double? = null
                        var indexOfBestMatch: Int? = null
                        for ((index, element) in this.withIndex()) {
                            val distance = abs(element - target)
                            if(element == target) {
                                return index
                            } else if(bestMatch == null || distance < bestMatch) {
                                bestMatch = distance
                                indexOfBestMatch = index
                            }
                        }
                        return indexOfBestMatch
                    }

                    SingleChoicePreference(
                        label = "Max PPO2",
                        description = "Maximum allowed PPO2 during the dive (except for decompression).",
                        items = allowedPPO2values,
                        selectedItemIndex = allowedPPO2values.indexByNearest(configuration.maxPPO2) ?: 0,
                        itemToStringMapper = {
                            "$it"
                        }
                    ) { maxPPO2Travel ->
                        planningRepository.updateConfiguration {
                            it.copy(maxPPO2 = maxPPO2Travel)
                        }
                    }

                    SingleChoicePreference(
                        label = "Max deco PPO2",
                        description = "Maximum allowed PPO2 during decompression stops and ascents to decompression stops.",
                        items = allowedPPO2values,
                        selectedItemIndex = allowedPPO2values.indexByNearest(configuration.maxPPO2Deco) ?: 0,
                        itemToStringMapper = {
                            "$it"
                        }
                    ) { maxPPO2 ->
                        planningRepository.updateConfiguration {
                            it.copy(maxPPO2Deco = maxPPO2)
                        }
                    }

                    SettingsSubTitle(subTitle = "Multi-level")

                    SwitchPreference(
                        label = "Use deco gas between sections",
                        value = "If ascending from one section of a multi-level dive to another allow the automatic usage of deco gas. Gas will be switched back to the chosen gas for that section once the desired depth is reached.",
                        isChecked = configuration.useDecoGasBetweenSections
                    ) { isChecked ->
                        planningRepository.updateConfiguration {
                            it.copy(useDecoGasBetweenSections = isChecked)
                        }
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
                        planningRepository.updateConfiguration {
                            it.copy(contingencyDeeper = deeper)
                        }
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
                        planningRepository.updateConfiguration {
                            it.copy(contingencyLonger = longer)
                        }
                    }

                }
            }
        }
    }
}
@Composable
fun GradientFactorPreference(
    modifier: Modifier = Modifier,
    label: String,
    description: String,
    gfLow: Int,
    gfHigh: Int,
    onValueChanged: (Int, Int) -> Unit,
) {

    var showDialog by remember {
        mutableStateOf(false)
    }

    if (showDialog) {
        GradientFactorPreferenceDialog(
            title = label,
            gfLow = gfLow,
            gfHigh = gfHigh,
            onConfirmButtonClicked = { gfLowNew, gfHighNew ->
                if (gfLow != gfLowNew || gfHigh != gfHighNew) {
                    onValueChanged(gfLowNew, gfHighNew)
                }
                showDialog = false
            },
            onCancelButtonClicked = { showDialog = false },
            onDismissRequest = { showDialog = false },
        )
    }

    BasicPreference(
        modifier = modifier.clickable {
            showDialog = true
        },
        label = label,
        value = description,
        hideDivider = true,
        action = {
            Text(
                text = "$gfLow/$gfHigh",
                style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    )
}

@Composable
fun GradientFactorPreferenceDialog(
    title: String,
    confirmButtonText: String = "OK",
    cancelButtonText: String = "Cancel",
    onConfirmButtonClicked: (gfLow: Int, gfHigh: Int) -> Unit = { _, _ -> },
    onCancelButtonClicked: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
    gfLow: Int,
    gfHigh: Int,
) {
    val gfLowValue: MutableState<Int?> = remember(gfLow) { mutableStateOf(gfLow) }
    val gfHighValue: MutableState<Int?> = remember(gfHigh) { mutableStateOf(gfHigh) }

    val isGfLowValid = remember { mutableStateOf(false) }
    val isGfHighValid = remember { mutableStateOf(false) }

    AlertDialogCustomContent(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                enabled = isGfLowValid.value && isGfHighValid.value,
                onClick = {
                    onConfirmButtonClicked(gfLowValue.value!!, gfHighValue.value!!)
                }) {
                Text(text = confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelButtonClicked) {
                Text(text = cancelButtonText)
            }
        },
        title = { Text(title) },
        content = {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedNumberInputField(
                    modifier = Modifier.padding(start = 24.dp).weight(1f),
                    minValue = 10,
                    maxValue = 100,
                    initialValue = gfLow,
                    isValid = isGfLowValid,
                    visualTransformation = SuffixVisualTransformation(" low")
                ) {
                    gfLowValue.value = it
                }
                OutlinedNumberInputField(
                    modifier = Modifier.padding(end = 24.dp).weight(1f),
                    minValue = 10,
                    maxValue = 100,
                    initialValue = gfHigh,
                    isValid = isGfHighValid,
                    visualTransformation = SuffixVisualTransformation(" high")
                ) {
                    gfHighValue.value = it
                }
            }
        }

    )
}

@Preview
@Composable
fun GradientFactorPreferenceDialogPreview() {
    GradientFactorPreferenceDialog(
        title = "Gradient factor",
        gfLow = 30,
        gfHigh = 70
    )
}







