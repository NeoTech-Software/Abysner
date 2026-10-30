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

package org.neotech.app.abysner.domain.settings.model

import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas

/**
 * Everything the gas blending calculator remembers between visits: the inputs last used, and what
 * the gases cost. Kept in settings rather than in a view model so a blender filling several
 * cylinders does not have to type it all again after the app is closed.
 */
data class GasBlendingSettings(
    /** True when blending with three top-up gases, which is what balancing helium needs. */
    val trimixMode: Boolean = false,
    val cylinderSize: Cylinder.Size = Cylinder.STEEL_12L,
    val startGas: Gas = Gas.Air,
    /** In bar. */
    val startPressure: Double = 50.0,
    val targetGas: Gas = Gas.Nitrox32,
    /** In bar. */
    val targetPressure: Double = 200.0,
    val firstTopUpGas: Gas = Gas.Oxygen,
    /** Only used in [trimixMode], but kept either way so switching mode loses nothing. */
    val secondTopUpGas: Gas = Gas.Helium,
    val finalTopUpGas: Gas = Gas.Air,
    /**
     * What a liter of gas at 1 bar costs, per gas. Stored per liter regardless of the unit system,
     * the UI converts to whatever denominator it shows.
     */
    val gasPrices: Map<Gas, Double> = emptyMap(),
) {

    /** The top-up gases in fill order: two for nitrox, three for trimix. */
    val topUpGases: List<Gas>
        get() = if (trimixMode) {
            listOf(firstTopUpGas, secondTopUpGas, finalTopUpGas)
        } else {
            // Nitrox skips the helium slot, keeping the first and final gas. That matches how a
            // blender works: enrich first, top up with air last.
            listOf(firstTopUpGas, finalTopUpGas)
        }
}
