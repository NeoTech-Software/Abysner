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

package org.neotech.app.abysner.data.settings.resources

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.neotech.app.abysner.data.SerializableResource

@Serializable
data class SettingsResourceV1(
    val showBasicDecoTable: Boolean,
    val termsAndConditionsAccepted: Boolean,
    val themeMode: ThemeModeResource = ThemeModeResource.SYSTEM,
): SerializableResource {

    @Serializable
    enum class ThemeModeResource {
        @SerialName("system") SYSTEM,
        @SerialName("light") LIGHT,
        @SerialName("dark") DARK,
    }
}

