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

import android.icu.util.LocaleData
import android.icu.util.ULocale
import android.os.Build
import org.neotech.app.abysner.domain.core.model.UnitSystem
import java.util.Locale

private val IMPERIAL_COUNTRIES = setOf("US", "LR", "MM")

actual fun detectSystemUnitSystem(): UnitSystem {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val measurementSystem = LocaleData.getMeasurementSystem(ULocale.getDefault())
        return if (measurementSystem == LocaleData.MeasurementSystem.US) {
            UnitSystem.IMPERIAL
        } else {
            UnitSystem.METRIC
        }
    }
    val country = Locale.getDefault().country.uppercase()
    return if (country in IMPERIAL_COUNTRIES) {
        UnitSystem.IMPERIAL
    } else {
        UnitSystem.METRIC
    }
}
