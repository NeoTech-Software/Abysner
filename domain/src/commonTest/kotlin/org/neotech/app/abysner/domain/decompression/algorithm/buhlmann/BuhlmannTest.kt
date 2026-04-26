package org.neotech.app.abysner.domain.decompression.algorithm.buhlmann

import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.physics.metersToAmbientPressure
import org.neotech.app.abysner.domain.diveplanning.DivePlannerTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * This is not a complete standalone test suite to test the Buhlmann algorithm, most of the
 * algorithm will be covered by tests such as [DivePlannerTest], which are a bit more high level and
 * basically integration tests for the dive planning as a whole
 */
class BuhlmannTest {

    @Test
    fun ndlCalculation_matchesExpectedValues() {
        // These settings kinda mimic the US Navy/PADI tables. What we use doesn't really matter for
        // this test, but this gives us something to compare against that is relatively well known.
        // There is no real comparing between Buhlmann and Navy tables, but this will make it close.
        val environment = Environment.SeaLevelSalt
        val model = Buhlmann(
            version = Buhlmann.Version.ZH16C,
            environment = environment,
            gfLow = 0.85,
            gfHigh = 1.0
        )

        // Depths from PADI table (in meters)
        val depths = listOf(10, 12, 14, 16, 18, 20, 22, 25, 30, 35, 40, 42)

        // Expected NDL times
        val expectedNdlTimes = listOf(409, 176, 107, 77, 59, 45, 36, 26, 17, 12, 9, 8)
        // Real PADI values:          219, 147, 98, 72, 56, 45, 37, 29, 20, 14, 9, 8
        // Difference:                +190, +29, +9, +5, +3,  0, -1, -3, -3, -2, 0, 0

        depths.forEachIndexed { index, depth ->
            val ndlTime = model.getNoDecompressionLimit(
                metersToAmbientPressure(depth.toDouble(), environment),
                Gas.Air
            )
            assertEquals(expectedNdlTimes[index], ndlTime)
            // Reset model to clear tissues
            model.reset()
        }
    }

    @Test
    fun getCeiling_isIdempotent() {
        // getCeiling() internally ratchets up the `lowestCeiling` field used for gradient-factor
        // interpolation. Because the ratchet is monotonic and is applied before the return value
        // is computed within the same call, the result must be identical on every subsequent call
        // as long as no tissue loading has changed.
        val environment = Environment.SeaLevelSalt
        val model = Buhlmann(
            version = Buhlmann.Version.ZH16C,
            environment = environment,
            gfLow = 0.3,
            gfHigh = 0.7,
        )
        model.addFlat(metersToAmbientPressure(30.0, environment), Gas.Air, 30)

        val first  = model.getCeiling()
        val second = model.getCeiling()
        val third  = model.getCeiling()

        assertEquals(first,  second, "getCeiling() must be idempotent: 1st vs 2nd call differ")
        assertEquals(second, third,  "getCeiling() must be idempotent: 2nd vs 3rd call differ")
    }

    @Test
    fun reset_snapshotIsNotMutatedByLaterTissueLoading() {
        val environment = Environment.SeaLevelSalt
        val model = Buhlmann(
            version = Buhlmann.Version.ZH16C,
            environment = environment,
            gfLow = 0.3,
            gfHigh = 0.7,
        )
        val depth = metersToAmbientPressure(30.0, environment)

        model.addFlat(depth, Gas.Air, 20)
        val snapshot = model.snapshot()

        val pNitrogen = snapshot.tissues.first().partialNitrogenPressure
        val pHelium = snapshot.tissues.first().partialHeliumPressure

        model.reset(snapshot)
        model.addFlat(depth, Gas.Air, 20)

        assertEquals(pNitrogen, snapshot.tissues.first().partialNitrogenPressure)
        assertEquals(pHelium, snapshot.tissues.first().partialHeliumPressure)
    }

    @Test
    fun addPressureChange_throwsForZeroDuration() {
        val environment = Environment.SeaLevelSalt
        val model = Buhlmann(
            version = Buhlmann.Version.ZH16C,
            environment = environment,
            gfLow = 0.3,
            gfHigh = 0.7,
        )
        val depth = metersToAmbientPressure(21.0, environment)
        assertFailsWith<IllegalArgumentException>(
            message = "Expected IllegalArgumentException for timeInMinutes=0"
        ) {
            model.addPressureChange(depth, depth, Gas.Nitrox50, timeInMinutes = 0)
        }
    }
}
