/*
 * Abysner - Dive planner
 * Copyright (C) 2024-2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.data.diveplanning.resources

import kotlinx.serialization.Serializable
import org.neotech.app.abysner.data.SerializableResource

@Serializable
data class DivePlanInputResourceV1(
    val version: Int = 1,
    // Added after 1.0.8-beta
    val diveMode: String = "open-circuit",
    val deeper: Boolean,
    val longer: Boolean,
    // Added after 1.0.8-beta
    val bailout: Boolean = false,
    val cylinders: List<CheckableCylinderResource>,
    val profile: List<ProfileSegmentResource>,
    // Added after 1.0.8-beta
    val surfaceIntervalBeforeMinutes: Int? = null,
): SerializableResource {

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

