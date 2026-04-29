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
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.usesMetricSystem

actual fun detectSystemUnitSystem(): UnitSystem {
    return if (NSLocale.currentLocale.usesMetricSystem) {
        UnitSystem.METRIC
    } else {
        UnitSystem.IMPERIAL
    }
}
