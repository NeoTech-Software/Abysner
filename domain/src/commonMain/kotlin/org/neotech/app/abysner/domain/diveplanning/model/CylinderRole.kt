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

package org.neotech.app.abysner.domain.diveplanning.model

import org.neotech.app.abysner.domain.persistence.EnumPreference

/**
 * Defines the role a cylinder plays in a dive. Currently, the roles are limited to what the app
 * actually needs to know, and exclusively related to closed-circuit planning. Perhaps we should
 * keep it this way, not too much bloat?
 */
enum class CylinderRole : EnumPreference {

    CCR_OXYGEN {
        override val preferenceValue: String = "ccr-oxygen"
    },

    CCR_DILUENT {
        override val preferenceValue: String = "ccr-diluent"
    },

    /**
     * In many rebreather setups the diluent cylinder is often also a bailout, this seems to be
     * common for chest mounted setups for example.
     */
    CCR_DILUENT_AND_BAILOUT {
        override val preferenceValue: String = "ccr-diluent-and-bailout"
    },
}

val CylinderRole?.isCcrOxygen: Boolean get() = this == CylinderRole.CCR_OXYGEN
val CylinderRole?.isCcrDiluent: Boolean get() = this == CylinderRole.CCR_DILUENT || this == CylinderRole.CCR_DILUENT_AND_BAILOUT

/**
 * True if the cylinder is available for bailout. A cylinder without role is assumed to be
 * available as well.
 */
val CylinderRole?.isAvailableForBailout: Boolean get() = this == null || this == CylinderRole.CCR_DILUENT_AND_BAILOUT

fun CylinderRole?.toggleAvailableForBailout(availableForBailout: Boolean): CylinderRole? =
    if (this.isCcrDiluent) {
        if (availableForBailout) {
            CylinderRole.CCR_DILUENT_AND_BAILOUT
        } else {
            CylinderRole.CCR_DILUENT
        }
    } else {
        this
    }
