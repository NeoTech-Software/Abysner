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

package org.neotech.app.abysner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import dev.zacsweers.metro.Inject
import org.neotech.app.abysner.domain.settings.SettingsRepository
import org.neotech.app.abysner.domain.settings.model.SettingsModel

@Inject
class MainNavControllerViewModel(
    private val settingsRepository: SettingsRepository
): ViewModel() {

    val settings: StateFlow<SettingsModel> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            // Read on the main thread in a blocking manner to ensure the correct initial value is
            // available immediately, preventing a theme or terms & conditions flicker on startup.
            // This is an acceptable reason to read on the main thread, ideally this would happen
            // during the system splash screen.
            initialValue = runBlocking { settingsRepository.getSettings() }
        )
}
