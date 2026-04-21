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

package org.neotech.app.abysner.presentation.screens.planner.decoplan

import abysner.composeapp.generated.resources.Res
import abysner.composeapp.generated.resources.ic_baseline_arrow_down_24
import abysner.composeapp.generated.resources.ic_baseline_arrow_up_24
import abysner.composeapp.generated.resources.ic_outline_lifebuoy_24
import abysner.composeapp.generated.resources.ic_baseline_arrow_flat_24
import abysner.composeapp.generated.resources.ic_baseline_stop_square_24
import abysner.composeapp.generated.resources.ic_baseline_change_24
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.painterResource
import org.neotech.app.abysner.domain.core.model.BreathingMode
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.DiveMode
import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.physics.depthInMetersToBar
import org.neotech.app.abysner.domain.decompression.model.DiveSegment
import org.neotech.app.abysner.domain.decompression.model.compactSimilarSegments
import org.neotech.app.abysner.domain.diveplanning.DivePlanner
import org.neotech.app.abysner.domain.diveplanning.model.DivePlan
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanSet
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.diveplanning.model.assign
import org.neotech.app.abysner.domain.settings.model.SettingsModel
import org.neotech.app.abysner.domain.utilities.DecimalFormat
import org.neotech.app.abysner.domain.utilities.format
import org.neotech.app.abysner.presentation.component.BigNumberDisplay
import org.neotech.app.abysner.presentation.component.BigNumberSize
import org.neotech.app.abysner.presentation.component.MultiChoiceSegmentedButtonRow
import org.neotech.app.abysner.presentation.component.Table
import org.neotech.app.abysner.presentation.component.TextWithStartIcon
import org.neotech.app.abysner.presentation.component.appendBold
import org.neotech.app.abysner.presentation.component.rememberMultiChoiceSegmentedButtonRowState
import org.neotech.app.abysner.presentation.getUserReadableMessage
import org.neotech.app.abysner.presentation.preview.PreviewData
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.presentation.theme.onWarning
import org.neotech.app.abysner.presentation.theme.warning
import kotlin.math.ceil

@Composable
fun DecoPlanCardComponent(
    modifier: Modifier = Modifier,
    divePlanSet: DivePlanSet?,
    settings: SettingsModel,
    planningException: Throwable?,
    isLoading: Boolean,
    onContingencyInputChanged: (deeper: Boolean, longer: Boolean, bailout: Boolean) -> Unit
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
                    text = buildAnnotatedString {
                        append("Deco plan")
                        withStyle(MaterialTheme.typography.titleSmall.toSpanStyle()) {
                            if (divePlanSet?.isDeeper == true) {
                                append(" +${divePlanSet.deeper}m")
                            }
                            if (divePlanSet?.isLonger == true) {
                                append(" +${divePlanSet.longer}min")
                            }
                        }
                    }
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

                    val items = buildList {
                        add("Deeper\u202F+${divePlanSet.configuration.contingencyDeeper}")
                        add("Longer\u202F+${divePlanSet.configuration.contingencyLonger}")
                        if (divePlanSet.isCcr) { add("Bail-out") }
                    }.toImmutableList()

                    val preSelected = buildList {
                        if (divePlanSet.isDeeper) { add(0) }
                        if (divePlanSet.isLonger) { add(1) }
                        if (divePlanSet.isCcr && divePlanSet.bailout) { add(2) }
                    }.toImmutableList()

                    val multiChoiceRowState = rememberMultiChoiceSegmentedButtonRowState(preSelected)

                    MultiChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .wrapContentWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
                        items = items,
                        multiChoiceSegmentedButtonRowState = multiChoiceRowState,
                        onChecked = { _, _, _ ->
                            onContingencyInputChanged(
                                multiChoiceRowState.checkedItemIndexes.contains(0),
                                multiChoiceRowState.checkedItemIndexes.contains(1),
                                divePlanSet.isCcr && multiChoiceRowState.checkedItemIndexes.contains(2),
                            )
                        }
                    ) { item, _ ->
                        Text(text = item, maxLines = 1)
                    }

                    val planToShow = divePlanSet.base

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
                        settings = settings,
                        isCcr = divePlanSet.isCcr,
                        isBailout = divePlanSet.bailout,
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
                            DecoPlanConfigurationSummeryDialog(configuration = divePlanSet.configuration) {
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
        // CNS thresholds based on NOAA Diving Manual (1991): 90% issues a warning color as it
        // approaches the limit, 100% issues an error color as it is at or over the limit. See
        // OxygenToxicityCalculator for details and references.
        val (cnsContainerColor, cnsValueColor) = when {
            cns >= 100.0 -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
            cns >= 90.0  -> MaterialTheme.colorScheme.warning to MaterialTheme.colorScheme.onWarning
            else         -> Color.Unspecified to Color.Unspecified
        }
        val cnsDisplayValue = ceil(cns)
        val cnsDisplay = if (cnsDisplayValue > 999.0) {
            ">999%"
        } else {
            "${cnsDisplayValue.format(0)}%"
        }

        BigNumberDisplay(
            modifier = Modifier.width(96.dp),
            size = BigNumberSize.EXTRA_SMALL,
            value = cnsDisplay,
            valueColor = cnsValueColor,
            containerColor = cnsContainerColor,
            label = "CNS"
        )

        // OTU threshold based on NOAA Diving Manual (1991): 300 OTU is the recommended conservative
        // maximum for a single day of repetitive diving. No error color is used since there is no
        // clear consensus on what value would warrant one for pulmonary oxygen toxicity.
        val (otuContainerColor, otuValueColor) = when {
            otu >= 300.0 -> MaterialTheme.colorScheme.warning to MaterialTheme.colorScheme.onWarning
            else         -> Color.Unspecified to Color.Unspecified
        }
        val otuDisplayValue = ceil(otu)
        val otuDisplay = if (otuDisplayValue > 999.0) {
            ">999"
        } else {
            otuDisplayValue.format(0)
        }

        BigNumberDisplay(
            modifier = Modifier.width(96.dp),
            size = BigNumberSize.EXTRA_SMALL,
            value = otuDisplay,
            valueColor = otuValueColor,
            containerColor = otuContainerColor,
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
        if(divePlan.maxTimeToSurfaceBailout != null) {
            Text(
                text = buildAnnotatedString {
                    appendBold("Max TTS: ")
                    append("${divePlan.maxTimeToSurfaceBailout!!.ttsBailoutAfter} @ ${divePlan.maxTimeToSurfaceBailout!!.end} minutes (bailout)")
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
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
    isCcr: Boolean,
    isBailout: Boolean,
) {
    Table(
        modifier = modifier,
        header = {
            TextWithStartIcon(
                modifier = Modifier.weight(0.23f),
                text = "Depth",
                icon = ColorPainter(Color.Transparent),
            )
            Text(modifier = Modifier.weight(0.30f), text = "Runtime")
            Text(modifier = Modifier.weight(0.25f), text = "Gas")
            Text(modifier = Modifier.weight(0.22f), text = "PPO2")
        }
    ) {
        val segments = divePlan.segmentsCollapsed
            .toMutableList()
            .compactSimilarSegments(compactAscentsAndStops = settings.showBasicDecoTable)
        val environment = divePlan.configuration.environment

        rowsIndexed(segments, key = { _, segment -> segment.start }) { index, diveSegment ->
            // For gas switch segments show the gas the diver is switching to, rather than the gas
            // currently being breathed. The actual switch happens at the end of the section, so the
            // row reads as an instruction (e.g. "switch to Nx50").
            val displayGas = if (diveSegment.isGasSwitch) {
                segments.getOrNull(index + 1)?.cylinder?.gas ?: diveSegment.cylinder.gas
            } else {
                diveSegment.cylinder.gas
            }
            // Show a warning when switching from closed-circuit to open-circuit
            val isBailoutSwitch = isBailout && isCcr &&
                diveSegment.isGasSwitch &&
                diveSegment.breathingMode == BreathingMode.OpenCircuit &&
                segments.getOrNull(index - 1)?.breathingMode is BreathingMode.ClosedCircuit
            DecoPlanRow(
                diveSegment = diveSegment,
                runtime = diveSegment.end,
                gas = displayGas,
                isBailoutSwitch = isBailoutSwitch,
                environment = environment,
            )
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
    runtime: Int,
    gas: Gas,
    isBailoutSwitch: Boolean = false,
    environment: Environment,
) {
    val typeIcon = when (diveSegment.type) {
        DiveSegment.Type.DECO_STOP -> Res.drawable.ic_baseline_stop_square_24
        DiveSegment.Type.GAS_SWITCH -> if (isBailoutSwitch) { Res.drawable.ic_outline_lifebuoy_24 } else { Res.drawable.ic_baseline_change_24 }
        DiveSegment.Type.FLAT -> Res.drawable.ic_baseline_arrow_flat_24
        DiveSegment.Type.DECENT -> Res.drawable.ic_baseline_arrow_down_24
        DiveSegment.Type.ASCENT -> Res.drawable.ic_baseline_arrow_up_24
    }

    TextWithStartIcon(
        modifier = Modifier.weight(0.23f),
        text = diveSegment.endDepth.toInt().toString(),
        icon = painterResource(resource = typeIcon)
    )
    Text(
        modifier = Modifier.weight(0.11f),
        text = runtime.toString(),
    )
    Text(
        modifier = Modifier.weight(0.19f),
        text = "+${diveSegment.duration}",
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
    )
    Text(
        modifier = Modifier.weight(0.25f),
        text = gas.toString(),
    )

    val endAmbientPressure = depthInMetersToBar(diveSegment.endDepth, environment).value

    val ppO2Text = when (val mode = diveSegment.breathingMode) {
        is BreathingMode.ClosedCircuit -> {
            val endMode = diveSegment.breathingModeAtEnd ?: mode
            val startAmbientPressure = depthInMetersToBar(diveSegment.startDepth, environment).value
            val ppO2Start = mode.effectivePpO2(startAmbientPressure)
            val ppO2End = endMode.effectivePpO2(endAmbientPressure)

            formatPpO2Range(ppO2Start, ppO2End)
        }
        is BreathingMode.OpenCircuit -> {
            DecimalFormat.format(1, gas.oxygenFraction * endAmbientPressure)
        }
    }
    Text(
        modifier = Modifier.weight(0.22f),
        text = ppO2Text,
    )
}

private fun formatPpO2Range(ppO2Start: Double, ppO2End: Double): String {
    if (ppO2Start == ppO2End) {
        return DecimalFormat.format(1, ppO2Start)
    }
    val formattedStart = DecimalFormat.format(1, ppO2Start)
    val formattedEnd = DecimalFormat.format(1, ppO2End)
    // Format first before comparing as rounding might make the range unnecessary after all
    return if (formattedStart == formattedEnd) {
        formattedStart
    } else {
        "$formattedStart\u2026$formattedEnd"
    }
}

@Preview
@Composable
fun DecoPlanCardComponentPreview() {
    AbysnerTheme {

        val divePlan = DivePlanner().addDive(
            plan = listOf(
                DiveProfileSection(16, 45, Cylinder(gas = Gas.Air, pressure = 232.0, waterVolume = 12.0)),
            ),
            cylinders = listOf(Cylinder.aluminium80Cuft(Gas.Nitrox50)).assign(),
        )

        DecoPlanCardComponent(
            divePlanSet = DivePlanSet(base = divePlan, deeper = null, longer = null, bailout = false, diveMode = DiveMode.OPEN_CIRCUIT, gasPlan = persistentListOf()),
            settings = SettingsModel(),
            planningException = null,
            isLoading = false,
            onContingencyInputChanged = { _, _, _ -> }
        )
    }
}

@Preview
@Composable
fun DecoPlanCardComponentCcrPreview() {
    AbysnerTheme {
        DecoPlanCardComponent(
            divePlanSet = PreviewData.divePlanCcr,
            settings = SettingsModel(),
            planningException = null,
            isLoading = false,
            onContingencyInputChanged = { _, _, _ -> }
        )
    }
}

@Preview
@Composable
fun DecoPlanCardComponentCcrBailoutPreview() {
    AbysnerTheme {
        DecoPlanCardComponent(
            divePlanSet = PreviewData.divePlanCcrBailout,
            settings = SettingsModel(),
            planningException = null,
            isLoading = false,
            onContingencyInputChanged = { _, _, _ -> }
        )
    }
}
