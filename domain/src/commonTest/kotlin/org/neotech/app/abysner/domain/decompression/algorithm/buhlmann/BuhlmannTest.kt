package org.neotech.app.abysner.domain.decompression.algorithm.buhlmann

import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.physics.depthInMetersToBar
import org.neotech.app.abysner.domain.diveplanning.DivePlannerTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * This is not a complete standalone test suite to test the Buhlmann algorithm, most of the
 * algorithm will be covered by tests such as [DivePlannerTest], which are a bit more high level and
 * basically integration tests for the dive planning as a whole
 */
class BuhlmannTest {

    @Test
    fun testNdlCalculation() {
        // These settings kinda mick the US Navy/PADI tables. What we use doesn't really mather for
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
        val expectedNdlTimes = listOf(294, 150, 95, 71, 55, 42, 34, 25, 16, 12, 9, 8)
        // Real PADI values:          219, 147, 98, 72, 56, 45, 37, 29, 20, 14, 9, 8
        // Difference:                +75, +3,  -3, -1, -1, -3, -3, -4, -4, -2, 0, 0

        depths.forEachIndexed { index, depth ->
            val ndlTime = model.getNoDecompressionLimit(
                depthInMetersToBar(depth.toDouble(), environment),
                Gas.Air
            )
            assertEquals(expectedNdlTimes[index], ndlTime)
            // Reset model to clear tissues
            model.reset()
        }
    }
}
