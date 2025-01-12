/*
 * Abysner - Dive planner
 * Copyright (C) 2024 Neotech
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

data class GasUsage(
    val gas: Cylinder,
    /**
     * Amount of gas used in liters at 1 ATA.
     */
    val amount: Double,
    /**
     * Amount of gas extra used in liters at 1 ATA.
     */
    val amountEmergencyExtra: Double
) {
    /**
     * Amount of gas used in total (normal usage + emergency extra) in liters at 1 ATA.
     */
    val amountTotal = amount + amountEmergencyExtra
}
