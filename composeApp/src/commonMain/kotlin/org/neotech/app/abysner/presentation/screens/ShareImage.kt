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

package org.neotech.app.abysner.presentation.screens

import abysner.composeapp.generated.resources.Res
import abysner.composeapp.generated.resources.abysner_logo
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.vectorResource
import org.neotech.app.abysner.domain.core.model.DiveMode
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanSet
import org.neotech.app.abysner.domain.settings.model.SettingsModel
import org.neotech.app.abysner.domain.utilities.format
import org.neotech.app.abysner.presentation.component.InfoPill
import org.neotech.app.abysner.presentation.component.InfoPillSize
import org.neotech.app.abysner.presentation.component.appendBold
import org.neotech.app.abysner.presentation.formatting.toHHMM
import org.neotech.app.abysner.presentation.preview.PreviewData
import org.neotech.app.abysner.presentation.screens.planner.decoplan.DecoPlanOxygenToxicityDisplay
import org.neotech.app.abysner.presentation.screens.planner.decoplan.DecoPlanTable
import org.neotech.app.abysner.presentation.screens.planner.gasplan.CylindersTable
import org.neotech.app.abysner.presentation.screens.planner.gasplan.GasLimitsTable
import org.neotech.app.abysner.presentation.screens.planner.gasplan.GasTotalsTable
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.presentation.theme.platform
import org.neotech.app.abysner.version.VersionInfo
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalLayoutApi::class)
@Composable
fun ShareImage(
    divePlan: DivePlanSet,
    diveNumber: Int,
    surfaceInterval: Duration?,
    settingsModel: SettingsModel,
) {
    // Disable scaling and dark theme and dynamic color, the image should be the same for every device.
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
        AbysnerTheme(darkTheme = false, dynamicColor = false) {
            Card {

                val backgroundImage =
                    rememberVectorPainter(vectorResource(Res.drawable.abysner_logo))

                Column(
                    modifier = Modifier
                        .padding(vertical = 16.dp, horizontal = 16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        text = "Dive plan"
                    )

                    val diveModePill = when (divePlan.diveMode) {
                        DiveMode.OPEN_CIRCUIT -> "OC"
                        DiveMode.CLOSED_CIRCUIT -> if (divePlan.bailout) {
                            "CCR (bailout)"
                        } else {
                            "CCR"
                        }
                    }

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(
                            6.dp,
                            Alignment.CenterHorizontally
                        ),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        InfoPill(label = "Type", value = diveModePill, size = InfoPillSize.SMALL)
                        InfoPill(
                            label = "Dive",
                            value = diveNumber.toOrdinal(),
                            size = InfoPillSize.SMALL
                        )
                        if (diveNumber > 1 && surfaceInterval != null) {
                            InfoPill(
                                label = "Interval",
                                value = surfaceInterval.toHHMM(),
                                size = InfoPillSize.SMALL
                            )
                        }
                    }

                    DecoPlanTable(
                        divePlan = divePlan.base,
                        settings = settingsModel,
                        isCcr = divePlan.isCcr,
                        isBailout = divePlan.bailout
                    )

                    DecoPlanOxygenToxicityDisplay(
                        modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                        cns = divePlan.base.totalCns,
                        otu = divePlan.base.totalOtu
                    )

                    val emergencyLabel = if (divePlan.isCcr) { "Bailout" } else { "Reserve" }
                    val usageLabel = if (divePlan.isCcr) { "Loop" } else { "Used" }

                    // GasPlanBarChart(
                    //     modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    //     gasPlan = divePlan.gasPlan,
                    //     emergencyLabel = emergencyLabel,
                    //     usageLabel = usageLabel,
                    //     balanceHorizontalLayout = true,
                    //     compact = true,
                    // )

                    Text(
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        text = "Totals",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    GasTotalsTable(
                        gasPlan = divePlan.gasPlan,
                        emergencyLabel = emergencyLabel,
                        usageLabel = usageLabel
                    )

                    Text(
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        text = "Cylinders",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    CylindersTable(divePlanSet = divePlan)

                    Text(
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        text = "Limits",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    GasLimitsTable(divePlanSet = divePlan)

                    Text(
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        text = "Configuration",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    val configuration = divePlan.configuration

                    Text(
                        text = buildAnnotatedString {
                            appendBold("Deco model: ")
                            append(configuration.algorithm.shortName)
                            append(" (${configuration.gf})")
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = buildAnnotatedString {
                            appendBold("Salinity: ")
                            append(configuration.environment.salinity.humanReadableName)
                            append(" (${configuration.environment.salinity.density.format(0)} kg/m3)")
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = buildAnnotatedString {
                            appendBold("Atmospheric pressure: ")
                            append(configuration.environment.atmosphericPressure.format(3))
                            append(" hPa")
                            append(" (${configuration.altitude.format(0)} meters)")
                        },
                        style = MaterialTheme.typography.bodySmall
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            modifier = Modifier.padding(end = 16.dp).size(40.dp),
                            painter = backgroundImage,
                            contentDescription = null,
                        )

                        val date = Clock.System.now()
                            .toLocalDateTime(TimeZone.currentSystemDefault()).date.format(LocalDate.Formats.ISO)
                        val platform = platform().humanReadable
                        Text(
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = FontStyle.Italic,
                                color = LocalTextStyle.current.color.copy(alpha = 0.8f)
                            ),
                            textAlign = TextAlign.Center,
                            text = if (LocalInspectionMode.current) {
                                "Created with Abysner for Android\n0.0.0-test (preview) on 2026-04-05"
                            } else {
                                "Created with Abysner for $platform\n${VersionInfo.VERSION_NAME} (${VersionInfo.COMMIT_HASH}) on $date"
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun Int.toOrdinal(): String = when {
    this % 100 in 11..13 -> "${this}th"
    this % 10 == 1 -> "${this}st"
    this % 10 == 2 -> "${this}nd"
    this % 10 == 3 -> "${this}rd"
    else -> "${this}th"
}

@Preview(device = DEVICE_SHARE_IMAGE)
@Composable
fun ShareImagePreview() {
    ShareImage(
        divePlan = PreviewData.divePlan1,
        diveNumber = 1,
        surfaceInterval = null,
        settingsModel = SettingsModel(
            showBasicDecoTable = true,
            termsAndConditionsAccepted = true
        ),
    )
}

@Preview(device = DEVICE_SHARE_IMAGE)
@Composable
fun ShareImagePreviewExtreme() {
    ShareImage(
        divePlan = PreviewData.divePlan2,
        diveNumber = 2,
        surfaceInterval = 60.minutes,
        settingsModel = SettingsModel(
            showBasicDecoTable = true,
            termsAndConditionsAccepted = true
        ),
    )
}

@Preview(device = DEVICE_SHARE_IMAGE)
@Composable
fun ShareImagePreviewCcrBailout() {
    ShareImage(
        divePlan = PreviewData.divePlanCcrBailout,
        diveNumber = 1,
        surfaceInterval = null,
        settingsModel = SettingsModel(
            showBasicDecoTable = true,
            termsAndConditionsAccepted = true
        ),
    )
}

private const val DEVICE_SHARE_IMAGE = "spec:width=411dp,height=2350dp"
