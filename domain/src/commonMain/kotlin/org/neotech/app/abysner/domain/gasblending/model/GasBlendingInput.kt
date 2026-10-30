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

package org.neotech.app.abysner.domain.gasblending.model

import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas

/**
 * Everything needed to compute a blending procedure: what is in the cylinder now, what should be
 * in it afterwards, and which gases are available to top up with.
 */
data class GasBlendingInput(
    val cylinderSize: Cylinder.Size,
    val startGas: Gas,
    val startPressure: Double,
    val targetGas: Gas,
    val targetPressure: Double,
    /**
     * The gases available to top up with, in the order they are added. Two gases can balance total
     * volume and oxygen (nitrox), three can additionally balance helium (trimix).
     *
     * Order matters: the volumes required of each gas do not depend on it, but the intermediate
     * pressures a blender has to fill to obviously do.
     */
    val topUpGases: List<Gas>,
) {

    init {
        require(topUpGases.size in 2..3) {
            "Blending requires 2 top-up gases (nitrox) or 3 (trimix), but got ${topUpGases.size}."
        }
    }

    companion object {
        val Default = GasBlendingInput(
            cylinderSize = Cylinder.STEEL_12L,
            startGas = Gas.Air,
            startPressure = 0.0,
            targetGas = Gas.Nitrox32,
            targetPressure = 200.0,
            topUpGases = listOf(Gas.Oxygen, Gas.Air),
        )
    }
}
