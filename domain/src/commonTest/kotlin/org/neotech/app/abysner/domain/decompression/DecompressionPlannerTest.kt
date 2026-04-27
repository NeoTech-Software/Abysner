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

package org.neotech.app.abysner.domain.decompression

import org.neotech.app.abysner.domain.core.model.BreathingMode
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.physics.ambientPressureToFeet
import org.neotech.app.abysner.domain.core.physics.ambientPressureToMeters
import org.neotech.app.abysner.domain.core.physics.feetToAmbientPressure
import org.neotech.app.abysner.domain.core.physics.feetToHydrostaticPressure
import org.neotech.app.abysner.domain.core.physics.metersToAmbientPressure
import org.neotech.app.abysner.domain.core.physics.metersToHydrostaticPressure
import org.neotech.app.abysner.domain.decompression.algorithm.buhlmann.Buhlmann
import org.neotech.app.abysner.domain.decompression.model.compactSimilarSegments
import org.neotech.app.abysner.domain.utilities.removeFloatingPointNoise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DecompressionPlannerTest {

    private val environment = Environment.SeaLevelFresh

    @Test
    fun init_rejectsDecoStepNotMultipleOfDisplayUnit() {
        assertFailsWith<IllegalArgumentException> {
            buildPlanner(
                decoStepSizePressureDelta = metersToHydrostaticPressure(2.0, environment).value,
                displayUnitPressureDelta = metersToHydrostaticPressure(3.0, environment).value,
                lastDecoStopAmbientPressure = metersToAmbientPressure(2.0, environment).value,
                pressureToDepth = { ambientPressureToMeters(it, environment) }
            )
        }
    }

    @Test
    fun init_acceptsDecoStepThatIsMultipleOfDisplayUnit() {
        buildPlanner(
            decoStepSizePressureDelta = metersToHydrostaticPressure(3.0, environment).value,
            displayUnitPressureDelta = metersToHydrostaticPressure(1.0, environment).value,
            lastDecoStopAmbientPressure = metersToAmbientPressure(3.0, environment).value,
            pressureToDepth = { ambientPressureToMeters(it, environment) }
        )
    }

    @Test
    fun init_rejectsLastDecoStopNotOnDisplayUnitGrid() {
        assertFailsWith<IllegalArgumentException> {
            buildPlanner(
                decoStepSizePressureDelta = metersToHydrostaticPressure(3.0, environment).value,
                displayUnitPressureDelta = metersToHydrostaticPressure(1.0, environment).value,
                lastDecoStopAmbientPressure = metersToAmbientPressure(2.5, environment).value,
                pressureToDepth = { ambientPressureToMeters(it, environment) }
            )
        }
    }

    @Test
    fun init_acceptsLastDecoStopOnDisplayUnitGrid() {
        buildPlanner(
            decoStepSizePressureDelta = metersToHydrostaticPressure(3.0, environment).value,
            displayUnitPressureDelta = metersToHydrostaticPressure(1.0, environment).value,
            lastDecoStopAmbientPressure = metersToAmbientPressure(6.0, environment).value,
            pressureToDepth = { ambientPressureToMeters(it, environment) }
        )
    }

    @Test
    fun init_acceptsImperialAlignedStepSize() {
        buildPlanner(
            decoStepSizePressureDelta = feetToHydrostaticPressure(10.0, environment).value,
            displayUnitPressureDelta = feetToHydrostaticPressure(1.0, environment).value,
            lastDecoStopAmbientPressure = feetToAmbientPressure(6.0, environment).value,
            pressureToDepth = { ambientPressureToFeet(it, environment) }
        )
    }

    @Test
    fun imperialDecoSteps_allSegmentDepthsAlignToTenFootGrid() {
        val planner = buildPlanner(
            decoStepSizePressureDelta = feetToHydrostaticPressure(10.0, environment).value,
            lastDecoStopAmbientPressure = feetToAmbientPressure(10.0, environment).value,
            displayUnitPressureDelta = feetToHydrostaticPressure(1.0, environment).value,
            pressureToDepth = { removeFloatingPointNoise(ambientPressureToFeet(it, environment)) },
        )

        val bottomGas = Cylinder.steel12Liter(Gas.Air)

        // Descend to 100 feet
        val depthPressure = feetToAmbientPressure(100.0, environment).value
        planner.addDepthChange(
            startAmbientPressure = environment.atmosphericPressure,
            endAmbientPressure = depthPressure,
            gas = bottomGas,
            timeInMinutes = 6,
            breathingMode = BreathingMode.OpenCircuit,
        )

        // Bottom time
        planner.addFlat(depthPressure, bottomGas, 30, BreathingMode.OpenCircuit)

        // Ascent and decompress to the surface
        planner.calculateDecompression(
            toAmbientPressure = environment.atmosphericPressure,
            breathingMode = BreathingMode.OpenCircuit,
        )

        val rawSegments = planner.getSegments()

        // Compacted segments start and end points are expected to be exactly on the display-unit
        // grid, because the input is aligned for each profile section, and the decompression
        // planner aligns ascents and stops to the display-unit grid as well.
        val segments = rawSegments.toMutableList().compactSimilarSegments()
        segments.forEach {
            // Due to removeFloatingPointNoise we should be getting exact multiples of 10.0 here.
            assertEquals(0.0, it.startDepth % 10.0)
        }
    }

    private fun buildPlanner(
        decoStepSizePressureDelta: Double,
        lastDecoStopAmbientPressure: Double,
        displayUnitPressureDelta: Double,
        pressureToDepth: (Double) -> Double,
    ) = DecompressionPlanner(
        model = Buhlmann(
            version = Buhlmann.Version.ZH16C,
            environment = environment,
            gfLow = 0.3,
            gfHigh = 0.7,
        ),
        surfacePressure = environment.atmosphericPressure,
        maxPpO2 = 1.6,
        maxEquivalentNarcoticAmbientPressure = metersToAmbientPressure(30.0, environment).value,
        ascentRatePressureDelta = metersToHydrostaticPressure(5.0, environment).value,
        decoStepSizePressureDelta = decoStepSizePressureDelta,
        lastDecoStopAmbientPressure = lastDecoStopAmbientPressure,
        displayUnitPressureDelta = displayUnitPressureDelta,
        forceMinimalDecoStopTime = false,
        gasSwitchTime = 1,
        pressureToDepth = pressureToDepth,
    )
}
