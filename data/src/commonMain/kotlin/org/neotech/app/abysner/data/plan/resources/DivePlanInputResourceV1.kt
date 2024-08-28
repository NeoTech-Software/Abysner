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

package org.neotech.app.abysner.data.plan.resources

import kotlinx.serialization.Serializable

@Serializable
data class DivePlanInputResourceV1(
    val version: Int = 1,
    val deeper: Boolean,
    val longer: Boolean,
    val cylinders: List<CheckableCylinderResource>,
    val profile: List<ProfileSegmentResource>,
) {

    @Serializable
    data class ProfileSegmentResource(
        val duration: Int,
        val depth: Int,
        val cylinderIdentifier: String
    )

    @Serializable
    data class CheckableCylinderResource(
        val cylinder: CylinderResource,
        val checked: Boolean,
    )

    @Serializable
    data class CylinderResource(
        val gas: GasResource,
        val pressure: Double,
        val volume: Double,
        val uniqueIdentifier: String,
    )

    @Serializable
    data class GasResource(
        val oxygenFraction: Double,
        val heliumFraction: Double
    )
}

