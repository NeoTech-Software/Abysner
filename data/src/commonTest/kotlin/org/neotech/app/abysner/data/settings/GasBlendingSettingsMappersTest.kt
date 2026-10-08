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

package org.neotech.app.abysner.data.settings

import kotlinx.serialization.json.Json
import org.neotech.app.abysner.data.settings.resources.SettingsResourceV1
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.model.UnitSystem
import org.neotech.app.abysner.domain.settings.model.GasBlendingSettings
import org.neotech.app.abysner.domain.settings.model.SettingsModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GasBlendingSettingsMappersTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun SettingsModel.roundTrip(): SettingsModel = json
        .decodeFromString<SettingsResourceV1>(json.encodeToString(toResource()))
        .toModel(UnitSystem.METRIC)

    @Test
    fun everyBlendingInput_survivesARoundTripThroughJson() {
        val blending = GasBlendingSettings(
            trimixMode = true,
            cylinderSize = Cylinder.D8,
            startGas = Gas.Nitrox28,
            startPressure = 73.0,
            targetGas = Gas.Trimix1845,
            targetPressure = 232.0,
            firstTopUpGas = Gas.Oxygen,
            secondTopUpGas = Gas.Helium,
            finalTopUpGas = Gas.Air,
            gasPrices = mapOf(Gas.Oxygen to 0.008, Gas.Helium to 0.045, Gas.Air to 0.001),
        )

        val restored = SettingsModel(gasBlending = blending).roundTrip().gasBlending

        assertEquals(blending, restored)
    }

    @Test
    fun cylinderSize_comesBackAsTheNamedPreset() {
        // Only volume and working pressure are stored, so the preset identity has to be resolved
        // again on the way back, otherwise the cylinder picker would show it as a custom size.
        val restored = SettingsModel(
            gasBlending = GasBlendingSettings(cylinderSize = Cylinder.D8)
        ).roundTrip().gasBlending.cylinderSize

        assertEquals("D8", restored.name)
        assertEquals(16.0, restored.waterVolume)
        assertEquals(300.0, restored.workingPressure)
    }

    @Test
    fun customCylinderSize_survivesWithoutAName() {
        val custom = Cylinder.Size(waterVolume = 9.4, workingPressure = 207.0)

        val restored = SettingsModel(
            gasBlending = GasBlendingSettings(cylinderSize = custom)
        ).roundTrip().gasBlending.cylinderSize

        assertEquals(null, restored.name)
        assertEquals(9.4, restored.waterVolume)
        assertEquals(207.0, restored.workingPressure)
    }

    @Test
    fun settingsStoredBeforeGasBlendingExisted_stillParse() {
        // Older installs have no gasBlending field at all, which must not break settings loading.
        val stored = """
            {"showBasicDecoTable":false,"termsAndConditionsAccepted":true,"themeMode":"dark","unitSystem":"metric"}
        """.trimIndent()

        val restored = json.decodeFromString<SettingsResourceV1>(stored).toModel(UnitSystem.METRIC)

        assertTrue(restored.termsAndConditionsAccepted)
        assertEquals(GasBlendingSettings(), restored.gasBlending)
    }

    @Test
    fun impossibleStoredGasesAreReplacedOrDroppedRatherThanThrowing() {
        // Gas rejects fractions that add up to more than 1.0, so corrupt entries must not take the
        // whole settings load down with them.
        val resource = SettingsResourceV1(
            showBasicDecoTable = false,
            termsAndConditionsAccepted = true,
            gasBlending = SettingsResourceV1.GasBlendingResource(
                startGas = SettingsResourceV1.GasResource(60, 60),
                targetGas = SettingsResourceV1.GasResource(-5, 0),
                gasPrices = listOf(
                    SettingsResourceV1.GasPriceResource(SettingsResourceV1.GasResource(60, 60), 1.0),
                    SettingsResourceV1.GasPriceResource(SettingsResourceV1.GasResource(21, 0), 0.001),
                ),
            ),
        )

        val restored = resource.toModel(UnitSystem.METRIC).gasBlending

        val defaults = GasBlendingSettings()
        assertEquals(defaults.startGas, restored.startGas)
        assertEquals(defaults.targetGas, restored.targetGas)
        assertEquals(mapOf(Gas.Air to 0.001), restored.gasPrices)
    }
}
