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

package org.neotech.app.abysner.domain.diveplanning.model

import kotlin.time.Duration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.DiveMode

data class DivePlanInputModel(
    val diveMode: DiveMode = DiveMode.OPEN_CIRCUIT,
    val deeper: Boolean,
    val longer: Boolean,
    val plannedProfile: List<DiveProfileSection>,
    val cylinders: List<PlannedCylinderModel>,
    /**
     * The surface interval before this dive, null if there was no preceding dive.
     */
    val surfaceIntervalBefore: Duration?,
)

data class PlannedCylinderModel(
    val cylinder: Cylinder,
    val isChecked: Boolean,
    /**
     * True only when this is the last checked cylinder of a gas mix that is referenced in a
     * planned segment. When true the checkbox and delete controls are disabled/hidden, preventing
     * the planner from being left without any cylinder of that gas.
     */
    val isLocked: Boolean,
)
