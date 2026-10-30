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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.model.UnitSystem
import org.neotech.app.abysner.domain.gasblending.GasBlendingPlanner
import org.neotech.app.abysner.domain.gasblending.model.GasBlendingInput
import org.neotech.app.abysner.domain.gasblending.model.GasBlendingPlan
import org.neotech.app.abysner.presentation.component.AlertSeverity
import org.neotech.app.abysner.presentation.component.Table
import org.neotech.app.abysner.presentation.component.TextAlert
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.presentation.utilities.formatPressure
import org.neotech.app.abysner.presentation.utilities.pressureUnitLabel

@Composable
internal fun GasBlendingResultCard(
    modifier: Modifier = Modifier,
    plan: GasBlendingPlan,
    unitSystem: UnitSystem,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp),
                style = MaterialTheme.typography.titleLarge,
                text = "Blending plan",
            )

            when (plan) {
                GasBlendingPlan.DegenerateTopUpGases -> Explanation(
                    text = "These top-up gases cannot be told apart by the calculation, so there " +
                            "is no single answer. Pick top-up gases with different compositions, " +
                            "and make sure one of them contains helium when blending trimix.",
                    isError = true,
                )

                GasBlendingPlan.ImpossibleWithTheseGases -> Explanation(
                    text = "This mix cannot be blended from these top-up gases, not even starting " +
                            "from an empty cylinder. One of the gases the target needs is missing.",
                    isError = true,
                )

                GasBlendingPlan.HeliumNotBalanced -> Explanation(
                    text = "Helium is involved, which needs three top-up gases to balance. " +
                            "Switch to Trimix.",
                    isError = true,
                )

                is GasBlendingPlan.Success -> SuccessContent(plan = plan, unitSystem = unitSystem)
            }
        }
    }
}

@Composable
private fun Explanation(text: String, isError: Boolean = false) {
    Text(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 16.dp),
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

@Composable
private fun SuccessContent(
    plan: GasBlendingPlan.Success,
    unitSystem: UnitSystem,
) {
    val drainToPressure = plan.drainToPressure
    if (drainToPressure != null) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)) {
            TextAlert(
                text = "Drain to ${drainToPressure.formatPressure(unitSystem, decimals = unitSystem.blendingPressureDecimals)} first",
                alertSeverity = AlertSeverity.WARNING,
                textStyle = MaterialTheme.typography.bodyLarge,
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = "The cylinder holds more of something than the target mix allows, so it " +
                        "cannot be reached by topping up alone. This is the highest pressure the " +
                        "blend still works from, so it wastes as little gas as possible.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (plan.steps.isEmpty()) {
        if (drainToPressure == null) {
            Explanation(text = "The cylinder already holds the target mix at the target pressure.")
        }
        return
    }

    Table(
        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp),
        header = {
            Text(modifier = Modifier.weight(0.3f), maxLines = 1, text = "Top up with")
            Text(
                modifier = Modifier.weight(0.4f),
                maxLines = 1,
                text = headerWithUnit("Up to", unitSystem.pressureUnitLabel),
            )
            Text(modifier = Modifier.weight(0.3f), maxLines = 1, text = "Mix")
        }
    ) {
        rows(plan.steps, key = { it.gas to it.pressureAfter }) { step ->
            Text(modifier = Modifier.weight(0.3f), maxLines = 1, text = step.gas.toString())
            Text(
                modifier = Modifier.weight(0.4f),
                maxLines = 1,
                text = step.pressureAfter.formatPressure(
                    unitSystem = unitSystem,
                    includeUnit = false,
                    decimals = unitSystem.blendingPressureDecimals,
                ),
            )
            Text(modifier = Modifier.weight(0.3f), maxLines = 1, text = step.mixAfter.toString())
        }
    }
}

/**
 * Blending fill pressures are worth a decimal, since a few tenths of a bar is a meaningful amount of
 * gas. Not in imperial: one decimal of bar is roughly 1.5 psi, so whole psi is already finer.
 */
private val UnitSystem.blendingPressureDecimals: Int
    get() = when (this) {
        UnitSystem.METRIC -> 1
        UnitSystem.IMPERIAL -> 0
    }

@Composable
private fun headerWithUnit(label: String, unit: String) = buildAnnotatedString {
    append(label)
    append(" ")
    withStyle(
        SpanStyle(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = MaterialTheme.typography.labelSmall.fontSize,
        )
    ) {
        append(unit)
    }
}

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
fun GasBlendingResultCardPreview() {
    AbysnerTheme {
        GasBlendingResultCard(
            modifier = Modifier.padding(16.dp),
            plan = previewPlan(),
            unitSystem = UnitSystem.METRIC,
        )
    }
}

@Preview
@Composable
fun GasBlendingResultCardTrimixPreview() {
    AbysnerTheme {
        GasBlendingResultCard(
            modifier = Modifier.padding(16.dp),
            plan = previewPlan(
                startPressure = 20.0,
                targetGas = Gas.Trimix1845,
                topUpGases = listOf(Gas.Oxygen, Gas.Helium, Gas.Air),
            ),
            unitSystem = UnitSystem.METRIC,
        )
    }
}

@Preview
@Composable
fun GasBlendingResultCardDrainPreview() {
    AbysnerTheme {
        GasBlendingResultCard(
            modifier = Modifier.padding(16.dp),
            plan = previewPlan(startGas = Gas.Nitrox50, startPressure = 100.0),
            unitSystem = UnitSystem.METRIC,
        )
    }
}

@Preview
@Composable
fun GasBlendingResultCardImpossiblePreview() {
    AbysnerTheme {
        GasBlendingResultCard(
            modifier = Modifier.padding(16.dp),
            plan = previewPlan(startPressure = 0.0, topUpGases = listOf(Gas.Nitrox50, Gas.Oxygen)),
            unitSystem = UnitSystem.METRIC,
        )
    }
}

@Preview
@Composable
fun GasBlendingResultCardImperialPreview() {
    AbysnerTheme {
        GasBlendingResultCard(
            modifier = Modifier.padding(16.dp),
            plan = previewPlan(),
            unitSystem = UnitSystem.IMPERIAL,
        )
    }
}
