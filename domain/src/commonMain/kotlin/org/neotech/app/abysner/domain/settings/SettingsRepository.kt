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

package org.neotech.app.abysner.domain.settings

import kotlinx.coroutines.flow.StateFlow
import org.neotech.app.abysner.domain.settings.model.SettingsModel

interface SettingsRepository {

    val settings: StateFlow<SettingsModel>

    suspend fun setTermsAndConditionsAccepted(accepted: Boolean)

    fun updateSettings(updateBlock: (SettingsModel) -> SettingsModel)

    suspend fun getSettings(): SettingsModel
}
