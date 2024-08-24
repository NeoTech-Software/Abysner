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

package org.neotech.app.abysner.presentation.screens.planner.decoplan

import abysner.composeapp.generated.resources.Res
import abysner.composeapp.generated.resources.ic_outline_change_circle_24
import abysner.composeapp.generated.resources.ic_outline_stop_circle_24
import abysner.composeapp.generated.resources.ic_outline_trending_down_24
import abysner.composeapp.generated.resources.ic_outline_trending_flat_24
import abysner.composeapp.generated.resources.ic_outline_trending_up_24
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.diveplanning.DivePlanner
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.decompression.model.DiveSegment
import org.neotech.app.abysner.domain.decompression.model.compactSimilarSegments
import org.neotech.app.abysner.domain.gasplanning.model.GasPlan
import org.neotech.app.abysner.domain.diveplanning.model.DivePlan
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanSet
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.settings.model.SettingsModel
import org.neotech.app.abysner.domain.utilities.DecimalFormat
import org.neotech.app.abysner.domain.utilities.DecimalFormatter
import org.neotech.app.abysner.domain.utilities.format
import org.neotech.app.abysner.presentation.component.BigNumberDisplay
import org.neotech.app.abysner.presentation.component.BigNumberSize
import org.neotech.app.abysner.presentation.component.SingleChoiceSegmentedButtonRow
import org.neotech.app.abysner.presentation.component.Table
import org.neotech.app.abysner.presentation.component.TextWithStartIcon
import org.neotech.app.abysner.presentation.component.appendBold
import org.neotech.app.abysner.presentation.component.rememberSingleChoiceSegmentedButtonRowState
import org.neotech.app.abysner.presentation.getUserReadableMessage
import org.neotech.app.abysner.presentation.screens.planner.ConfigurationSummeryDialog
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import kotlin.math.ceil

@Composable
fun DecoPlanCardComponent(
    modifier: Modifier = Modifier,
    divePlanSet: DivePlanSet?,
    settings: SettingsModel,
    planningException: Throwable?,
    isLoading: Boolean,
) {
    val errorMessage: String? = planningException?.getUserReadableMessage()

    Card(modifier) {
        LoadingBoxWithBlur(isLoading) { loadingModifier ->
            Column(modifier = loadingModifier
                .padding(vertical = 16.dp)
                .fillMaxWidth()) {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    text = "Deco plan"
                )

                if ((divePlanSet == null || divePlanSet.isEmpty)) {
                    if (errorMessage != null) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error,
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            text = "Nothing to see here, plan a dive first \uD83D\uDE09",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {

                    val singleChoiceRowState = rememberSingleChoiceSegmentedButtonRowState(0)

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .wrapContentWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
                        items = listOf("Normal", "Contingency"),
                        singleChoiceSegmentedButtonRowState = singleChoiceRowState,
                    ) { item, _ ->
                        Text(text = item, maxLines = 1)
                    }

                    val planToShow = when (singleChoiceRowState.selectedIndex) {
                        1 -> divePlanSet.deeperAndLonger
                        else -> divePlanSet.base
                    }

                    DecoPlanGraph(
                        modifier = Modifier
                            .aspectRatio(3f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        divePlan = planToShow
                    )

                    DecoPlanTable(
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp),
                        divePlan = planToShow,
                        settings = settings
                    )

                    DecoPlanOxygenToxicityDisplay(
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp).fillMaxWidth(),
                        cns = planToShow.totalCns,
                        otu = planToShow.totalOtu
                    )

                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {

                        DecoPlanExtraInfo(
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                            divePlan = planToShow
                        )

                        var showConfigurationInfo by remember { mutableStateOf(false) }

                        IconButton(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            onClick = { showConfigurationInfo = true },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Deco-plan information"
                            )
                        }

                        if (showConfigurationInfo) {
                            ConfigurationSummeryDialog(configuration = divePlanSet.configuration) {
                                showConfigurationInfo = false
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DecoPlanOxygenToxicityDisplay(
    cns: Double,
    otu: Double,
    modifier: Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        BigNumberDisplay(
            modifier = Modifier.width(96.dp),
            size = BigNumberSize.EXTRA_SMALL,
            value = "${ceil(cns).format(0)}%",
            label = "CNS"
        )
        BigNumberDisplay(
            modifier = Modifier.width(96.dp),
            size = BigNumberSize.EXTRA_SMALL,
            value = ceil(otu).format(0),
            label = "OTU"
        )
    }
}


@Composable
fun DecoPlanExtraInfo(
    modifier: Modifier = Modifier,
    divePlan: DivePlan
) {
    Column(modifier = modifier) {

        Text(
            text = buildAnnotatedString {
                appendBold("Average depth: ")
                append(
                    "${DecimalFormat.format(2, divePlan.averageDepth)} meters"
                )
            },
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = buildAnnotatedString {
                appendBold("Total deco time: ")
                append("${divePlan.totalDeco} minutes")
            },
            style = MaterialTheme.typography.bodyMedium
        )
        if(divePlan.firstDeco != -1) {
            Text(
                text = buildAnnotatedString {
                    appendBold("First deco after: ")
                    append("${divePlan.firstDeco} minutes")
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = buildAnnotatedString {
                appendBold("Deepest ceiling: ")
                append(
                    "${DecimalFormat.format(2, divePlan.deepestCeiling)} meter"
                )
            },
            style = MaterialTheme.typography.bodyMedium
        )
        if(divePlan.maxTimeToSurface != null) {
            Text(
                text = buildAnnotatedString {
                    appendBold("Max TTS: ")
                    append("${divePlan.maxTimeToSurface!!.ttsAfter} @ ${divePlan.maxTimeToSurface!!.end} minutes")
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}



@Composable
fun DecoPlanTable(
    modifier: Modifier = Modifier,
    divePlan: DivePlan,
    settings: SettingsModel,
) {
    Table(modifier = modifier,
        header = {
            TextWithStartIcon(
                modifier = Modifier.weight(0.2f),
                text = "Depth",
                icon = ColorPainter(Color.Transparent),
            )
            Text(
                modifier = Modifier.weight(0.2f),
                text = "Runtime",
            )
            Text(
                modifier = Modifier.weight(0.2f),
                text = "Duration",
            )
            TextWithStartIcon(
                modifier = Modifier.weight(0.2f),
                text = "Gas",
                icon = ColorPainter(Color.Transparent),
            )
        }
    ) {

        var currentGas: Gas? = null

        val segments = divePlan.segmentsCollapsed
            .toMutableList()
            .compactSimilarSegments(compactAscentsBetweenDecoStops = settings.showBasicDecoTable)

        segments.forEach { diveSegment ->

            row {
                DecoPlanRow(
                    diveSegment = diveSegment,
                    previousGas = currentGas,
                    runtime = diveSegment.end
                )
                currentGas = diveSegment.cylinder.gas
            }
        }
    }
}

@Composable
fun LoadingBoxWithBlur(
    isLoading: Boolean,
    content: @Composable (modifier: Modifier) -> Unit,
) {
    Box {
        content(Modifier.graphicsLayer {
            if(isLoading) {
                alpha = 0.8f
                renderEffect = BlurEffect(4f, 4f, TileMode.Decal)
            }
        })
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}


@Composable
private fun RowScope.DecoPlanRow(
    diveSegment: DiveSegment,
    previousGas: Gas?,
    runtime: Int
) {
    val typeIcon = when (diveSegment.type) {
        DiveSegment.Type.FLAT -> {
            if (diveSegment.isDecompression) {
                Res.drawable.ic_outline_stop_circle_24
            } else {
                Res.drawable.ic_outline_trending_flat_24
            }
        }

        DiveSegment.Type.DECENT -> Res.drawable.ic_outline_trending_down_24
        DiveSegment.Type.ASCENT -> Res.drawable.ic_outline_trending_up_24
    }

    TextWithStartIcon(
        modifier = Modifier.weight(0.2f),
        text = diveSegment.endDepth.toString(),
        icon = painterResource(resource = typeIcon)
    )
    Text(
        modifier = Modifier.weight(0.2f),
        text = runtime.toString(),
    )
    Text(
        modifier = Modifier.weight(0.2f),
        text = "+${diveSegment.duration}",
    )
    val gasIcon: Painter = if (previousGas != null && previousGas != diveSegment.cylinder.gas) {
        painterResource(resource = Res.drawable.ic_outline_change_circle_24)
    } else {
        ColorPainter(Color.Transparent)
    }
    TextWithStartIcon(
        modifier = Modifier.weight(0.2f),
        text = diveSegment.cylinder.gas.toString(),
        icon = gasIcon
    )
}

@Preview
@Composable
fun DecoPlanCardComponentPreview() {
    AbysnerTheme {

        val divePlan = DivePlanner().apply {
            configuration = Configuration()
        }.getDecoPlan(
            plan = listOf(
                DiveProfileSection(16, 45, Cylinder(gas = Gas.Air, pressure = 232.0, waterVolume = 12.0)),
            ),
            decoGases = listOf(Cylinder.aluminium80Cuft(Gas.Oxygen50)),
        )

        DecoPlanCardComponent(
            divePlanSet = DivePlanSet(divePlan, divePlan, GasPlan(emptyMap(), emptyMap())),
            settings = SettingsModel(),
            planningException = null,
            isLoading = false
        )
    }
}
