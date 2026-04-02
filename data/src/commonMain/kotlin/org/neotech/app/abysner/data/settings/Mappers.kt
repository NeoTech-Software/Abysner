/*
 * Abysner - Dive planner
 * Copyright (C) 2025-2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.data.settings

import org.neotech.app.abysner.data.settings.resources.SettingsResourceV1
import org.neotech.app.abysner.data.settings.resources.SettingsResourceV1.ThemeModeResource
import org.neotech.app.abysner.domain.settings.model.SettingsModel
import org.neotech.app.abysner.domain.settings.model.ThemeMode

fun SettingsModel.toResource() = SettingsResourceV1(
    showBasicDecoTable = showBasicDecoTable,
    termsAndConditionsAccepted = termsAndConditionsAccepted,
    themeMode = themeMode.toResource()
)

fun SettingsResourceV1.toModel() = SettingsModel(
    showBasicDecoTable = showBasicDecoTable,
    termsAndConditionsAccepted = termsAndConditionsAccepted,
    themeMode = themeMode.toModel()
)

private fun ThemeMode.toResource() = when (this) {
    ThemeMode.SYSTEM -> ThemeModeResource.SYSTEM
    ThemeMode.LIGHT -> ThemeModeResource.LIGHT
    ThemeMode.DARK -> ThemeModeResource.DARK
}

private fun ThemeModeResource.toModel() = when (this) {
    ThemeModeResource.SYSTEM -> ThemeMode.SYSTEM
    ThemeModeResource.LIGHT -> ThemeMode.LIGHT
    ThemeModeResource.DARK -> ThemeMode.DARK
}

