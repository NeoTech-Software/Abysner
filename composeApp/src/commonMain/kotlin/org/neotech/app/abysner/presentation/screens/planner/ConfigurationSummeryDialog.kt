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

package org.neotech.app.abysner.presentation.screens.planner

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.utilities.DecimalFormat

@Composable
fun ConfigurationSummeryDialog(
    configuration: Configuration,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("OK")
            }
        },
        title = { Text("Calculated with") },
        text = {
            Column {
                Text(
                    text = "Algorithm: ${configuration.algorithm.shortName}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "GF: ${configuration.gf}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Salinity: ${configuration.salinity}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Altitude pressure: ${DecimalFormat.format(0, configuration.environment.atmosphericPressure * 1000.0)} hPa (${DecimalFormat.format(0, configuration.altitude)} meters)",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Max ascent speed: ${configuration.maxAscentRate} m/min",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Max descent speed: ${configuration.maxDescentRate} m/min",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Max PPO2: ${configuration.maxPPO2}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Max Deco PPO2: ${configuration.maxPPO2Deco}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Max END: ${configuration.maxEND}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    )
}
