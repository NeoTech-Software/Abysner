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

package org.neotech.app.abysner.presentation.formatting

import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.UnitSystem
import org.neotech.app.abysner.domain.core.physics.asLitersToCubicFeet
import org.neotech.app.abysner.presentation.utilities.formatVolume
import org.neotech.app.abysner.presentation.utilities.volumeUnitLabel
import kotlin.math.roundToInt

/**
 * Formats how big this cylinder is: metric by water volume ("12.0 L"), imperial by rated capacity
 * ("80 ft³").
 */
fun Cylinder.formatCapacity(unitSystem: UnitSystem): String = when (unitSystem) {
    UnitSystem.METRIC -> waterVolume.formatVolume(unitSystem, decimals = 1)
    UnitSystem.IMPERIAL -> "${size.ratedCapacity().asLitersToCubicFeet().roundToInt()} ${unitSystem.volumeUnitLabel}"
}
