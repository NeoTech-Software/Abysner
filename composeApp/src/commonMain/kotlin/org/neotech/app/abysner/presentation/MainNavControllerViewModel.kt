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

package org.neotech.app.abysner.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.runBlocking
import me.tatarka.inject.annotations.Inject
import org.neotech.app.abysner.domain.settings.SettingsRepository

@Inject
class MainNavControllerViewModel(
    settingsRepository: SettingsRepository
): ViewModel() {

    // TODO: This is a bit dirty, but I want to avoid a flashing screen. A better approach would
    //       be to load this information during the splash screen.
    val areTermsAndConditionsAccepted: Boolean = runBlocking {
        settingsRepository.getSettings().termsAndConditionsAccepted
    }
}
