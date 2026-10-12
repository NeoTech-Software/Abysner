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

package org.neotech.app.abysner.data.settings.resources

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.neotech.app.abysner.data.SerializableResource

@Serializable
data class SettingsResourceV1(
    val showBasicDecoTable: Boolean,
    val termsAndConditionsAccepted: Boolean,
    val themeMode: ThemeModeResource = ThemeModeResource.SYSTEM,
    val showDiveEditTooltip: Boolean = true,
    val unitSystem: UnitSystemResource? = null,
    val gasBlending: GasBlendingResource = GasBlendingResource(),
): SerializableResource {

    /**
     * Gas composition is stored as whole percentages rather than fractions, so the stored form does
     * not depend on floating point representation.
     */
    @Serializable
    data class GasResource(
        val oxygenPercentage: Int,
        val heliumPercentage: Int,
    )

    @Serializable
    data class GasPriceResource(
        val gas: GasResource,
        val pricePerLiter: Double,
    )

    /**
     * Every field has a default, so settings written by a version without gas blending (or without
     * some of these fields) still parse.
     */
    @Serializable
    data class GasBlendingResource(
        val trimixMode: Boolean = false,
        val cylinderWaterVolume: Double = 12.0,
        val cylinderWorkingPressure: Double = 232.0,
        val startGas: GasResource = GasResource(21, 0),
        val startPressure: Double = 50.0,
        val targetGas: GasResource = GasResource(32, 0),
        val targetPressure: Double = 200.0,
        val firstTopUpGas: GasResource = GasResource(100, 0),
        val secondTopUpGas: GasResource = GasResource(0, 100),
        val finalTopUpGas: GasResource = GasResource(21, 0),
        val gasPrices: List<GasPriceResource> = emptyList(),
    )

    @Serializable
    enum class ThemeModeResource {
        @SerialName("system") SYSTEM,
        @SerialName("light") LIGHT,
        @SerialName("dark") DARK,
    }

    @Serializable
    enum class UnitSystemResource {
        @SerialName("metric") METRIC,
        @SerialName("imperial") IMPERIAL,
    }
}

