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

package org.neotech.app.abysner.domain.gasplanning

import org.neotech.app.abysner.domain.core.model.BreathingMode
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.physics.depthInMetersToBar
import org.neotech.app.abysner.domain.decompression.model.DiveSegment
import kotlin.test.Test
import kotlin.test.assertEquals

class OxygenToxicityCalculatorTest {

    private val environment = Environment.Default

    /**
     * A flat 30 meters deep and 30 minute long CCR dive at setpoint 1.3 bar, with Trimix 21/35 as
     * diluent should produce the same CNS as an open-circuit dive breathing a gas that gives 1.3
     * bar ppO2 at 30 meters (ignoring ascent and descent).
     */
    @Test
    fun calculateCns_ccrMatchesOcAtEquivalentPpO2() {
        val setpoint = 1.3
        val depth = 30.0
        val duration = 30
        val ambientPressure = depthInMetersToBar(depth, environment).value

        val ccrSegments = flatSegment(depth, duration, Gas.Trimix2135, BreathingMode.ccr(setpoint))

        val equivalentOxygenFraction = setpoint / ambientPressure
        val equivalentGas = Gas(oxygenFraction = equivalentOxygenFraction, heliumFraction = 0.0)
        val ocSegments = flatSegment(depth, duration, equivalentGas, BreathingMode.oc())

        val ccrCns = OxygenToxicityCalculator.calculateCns(ccrSegments, environment)
        val ocCns = OxygenToxicityCalculator.calculateCns(ocSegments, environment)

        assertEquals(ocCns, ccrCns, 1e-6)
    }

    /**
     * At 0 meters with a 1.3 setpoint, the ppO2 is capped at ambient because it is physically
     * impossible for the loop to exceed ambient pressure. At most, it should match open circuit
     * breathing pure oxygen.
     */
    @Test
    fun calculateCns_ccrShallowCapsAtAmbientPpO2() {
        val ccrSegments = flatSegment(0.0, 30, Gas.Air, BreathingMode.ccr(1.3))
        val ocSegments = flatSegment(0.0, 30, Gas.Oxygen, BreathingMode.oc())

        val ccrCns = OxygenToxicityCalculator.calculateCns(ccrSegments, environment)
        val ocCns = OxygenToxicityCalculator.calculateCns(ocSegments, environment)

        assertEquals(ocCns, ccrCns, 1e-6)
    }

    /**
     * The ppO2 of air diluent at 60 meters with a setpoint of 1.3 exceeds the setpoint, so the CNS
     * should match open-circuit CNS on the same gas. Abysner assumes the loop ppO2 does not
     * equalize down to the setpoint in these cases, matching other planning software.
     */
    @Test
    fun calculateCns_ccrUsesTrueDiluentPpO2WhenDiluentExceedsSetpoint() {
        val ccrSegments = flatSegment(60.0, 10, Gas.Air, BreathingMode.ccr(1.3))
        val ocSegments = flatSegment(60.0, 10, Gas.Air, BreathingMode.oc())

        val ccrCns = OxygenToxicityCalculator.calculateCns(ccrSegments, environment)
        val ocCns = OxygenToxicityCalculator.calculateCns(ocSegments, environment)
        assertEquals(ocCns, ccrCns, 1e-6)
    }

    /**
     * A flat 30 meters deep and 30 minute long CCR dive at setpoint 1.3 bar, with Trimix 21/35 as
     * diluent should produce the same OTU as an open-circuit dive breathing a gas that gives 1.3
     * bar ppO2 at 30 meters (ignoring ascent and descent).
     */
    @Test
    fun calculateOtu_ccrMatchesOcAtEquivalentPpO2() {
        val setpoint = 1.3
        val depth = 30.0
        val duration = 30
        val ambientPressure = depthInMetersToBar(depth, environment).value

        val ccrSegments = flatSegment(depth, duration, Gas.Trimix2135, BreathingMode.ccr(setpoint))

        val equivalentOxygenFraction = setpoint / ambientPressure
        val equivalentGas = Gas(oxygenFraction = equivalentOxygenFraction, heliumFraction = 0.0)
        val ocSegments = flatSegment(depth, duration, equivalentGas, BreathingMode.oc())

        val ccrOtu = OxygenToxicityCalculator.calculateOtu(ccrSegments, environment)
        val ocOtu = OxygenToxicityCalculator.calculateOtu(ocSegments, environment)

        assertEquals(ocOtu, ccrOtu, 1e-6)
    }

    private fun flatSegment(depth: Double, duration: Int, gas: Gas, breathingMode: BreathingMode) =
        listOf(
            DiveSegment(
                start = 0,
                duration = duration,
                startDepth = depth,
                endDepth = depth,
                cylinder = Cylinder.steel12Liter(gas),
                gfCeilingAtEnd = 0.0,
                type = DiveSegment.Type.FLAT,
                breathingMode = breathingMode,
            )
        )
}
