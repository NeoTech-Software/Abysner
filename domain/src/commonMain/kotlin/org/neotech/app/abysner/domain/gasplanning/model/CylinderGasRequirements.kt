/*
 * Abysner - Dive planner
 * Copyright (C) 2025 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.domain.gasplanning.model

import org.neotech.app.abysner.domain.core.model.Cylinder

data class CylinderGasRequirements(
    val cylinder: Cylinder,
    /**
     * Amount of gas required in liters at 1 ATA under normal diving conditions.
     */
    val normalRequirement: Double,
    /**
     * Amount of gas extra required to handle an emergency, in liters at 1 ATA.
     */
    val extraEmergencyRequirement: Double
) {
    /**
     * Amount of gas used in total (normal usage + emergency extra) in liters at 1 ATA.
     */
    val totalGasRequirement = normalRequirement + extraEmergencyRequirement

    /**
     * Pressure left in the cylinder (in bars) when finishing the dive abnormally (with an emergency)
     */
    val pressureLeftWithEmergency: Double? = cylinder.pressureAfter(volumeUsage = totalGasRequirement)

    /**
     * Pressure left in the cylinder (in bars) when finishing the dive under normal conditions.
     */
    val pressureLeft: Double? = cylinder.pressureAfter(volumeUsage = normalRequirement)
}
