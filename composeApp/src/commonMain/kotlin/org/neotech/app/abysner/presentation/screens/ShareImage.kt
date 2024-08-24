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

import abysner.composeapp.generated.resources.Res
import abysner.composeapp.generated.resources.abysner_logo
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.vectorResource
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanSet
import org.neotech.app.abysner.domain.settings.model.SettingsModel
import org.neotech.app.abysner.domain.utilities.format
import org.neotech.app.abysner.presentation.component.BigNumberDisplay
import org.neotech.app.abysner.presentation.component.BigNumberSize
import org.neotech.app.abysner.presentation.component.appendBold
import org.neotech.app.abysner.presentation.preview.PreviewData
import org.neotech.app.abysner.presentation.screens.planner.decoplan.DecoPlanOxygenToxicityDisplay
import org.neotech.app.abysner.presentation.screens.planner.decoplan.DecoPlanTable
import org.neotech.app.abysner.presentation.screens.planner.gasplan.CylindersTable
import org.neotech.app.abysner.presentation.screens.planner.gasplan.GasLimitsTable
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.presentation.theme.platform
import org.neotech.app.abysner.version.VersionInfo

@Composable
fun ShareImage(
    divePlan: DivePlanSet,
    settingsModel: SettingsModel,
) {
    // Disable scaling and dark theme and dynamic color, the image should be the same for every device.
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
        AbysnerTheme(darkTheme = false, dynamicColor = false) {
            Card {

                val backgroundImage = rememberVectorPainter(vectorResource(Res.drawable.abysner_logo))

                Column(
                    modifier = Modifier
                        .drawBehind {
                            val vectorSize = backgroundImage.intrinsicSize.times(2f)
                            translate(
                                left = (size.width - vectorSize.width) / 2f,
                                top = (size.height - vectorSize.height) / 2f
                            ) {
                                with(backgroundImage) {
                                    draw(size = backgroundImage.intrinsicSize.times(2f), alpha = 0.05f)
                                }
                            }
                        }
                        .padding(vertical = 16.dp, horizontal = 16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        text = "Dive plan"
                    )
                    DecoPlanTable(
                        divePlan = divePlan.base,
                        settings = settingsModel
                    )

                    DecoPlanOxygenToxicityDisplay(
                        modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                        cns = divePlan.base.totalCns,
                        otu = divePlan.base.totalOtu
                    )

                    // DecoPlanExtraInfo(
                    //     modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp),
                    //     divePlan = divePlan.base
                    // )

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

                    val date =
                        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                    Text(
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = FontStyle.Italic,
                            color = LocalTextStyle.current.color.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(top = 16.dp)
                            .align(Alignment.CenterHorizontally),
                        textAlign = TextAlign.Center,
                        text = "Created with Abysner for ${platform().humanReadable} ${VersionInfo.VERSION_NAME} (${VersionInfo.COMMIT_HASH})\non ${date.format(LocalDate.Formats.ISO)}"
                    )
                }
            }
        }
    }
}


@Preview
@Composable
private fun ShareImagePreview() {
    ShareImage(
        divePlan = PreviewData.divePlan,
        settingsModel = SettingsModel(
            showBasicDecoTable = true,
            termsAndConditionsAccepted = true
        )
    )
}
