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

package org.neotech.app.abysner.data

import org.neotech.app.abysner.domain.core.model.UnitSystem

/**
 * Detects the unit system preferred by the device locale. Falls back to [UnitSystem.METRIC] if
 * detection is unavailable.
 */
expect fun detectSystemUnitSystem(): UnitSystem
