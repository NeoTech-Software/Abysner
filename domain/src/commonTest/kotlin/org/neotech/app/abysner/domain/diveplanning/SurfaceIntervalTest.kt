package org.neotech.app.abysner.domain.diveplanning

import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Configuration.Algorithm
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.model.Salinity
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SurfaceIntervalTest {

    @Test
    fun surfaceIntervalTest() {
        val bottomGas = Cylinder.steel12Liter(Gas.Air)
        val divePlanner = DivePlanner()
        divePlanner.configuration = Configuration(
            maxAscentRate = 5.0,
            maxDescentRate = 5.0,
            gfLow = 0.85, gfHigh = 0.85,
            salinity = Salinity.WATER_FRESH,
            algorithm = Algorithm.BUHLMANN_ZH16C,
            altitude = 0.0,
            decoStepSize = 3,
            lastDecoStopDepth = 3
        )

        val plannedDive = listOf(DiveProfileSection(duration = 30, 30, bottomGas))

        val divePlan1 = divePlanner.addDive(plannedDive, emptyList())
        divePlanner.addSurfaceInterval(30.toDuration(DurationUnit.MINUTES))
        val divePlan2 = divePlanner.addDive(plannedDive, emptyList())


        assertEquals(45, divePlan1.runtime)

        // Dive plan 2 is the same as dive plan 1, except that it has to take into account tissues
        // from the previous dive (dive plan 1), as the surface interval is short.
        assertEquals(64, divePlan2.runtime)
    }
}
