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

package org.neotech.app.abysner.presentation.screens.terms_and_conditions

import abysner.composeapp.generated.resources.Res
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.neotech.app.abysner.domain.settings.SettingsRepository
import org.neotech.app.abysner.presentation.utilities.StateEvent
import org.neotech.app.abysner.presentation.utilities.consumed
import org.neotech.app.abysner.presentation.utilities.event

@Inject
class TermsAndConditionsViewModel(
    private val settingsRepository: SettingsRepository,
): ViewModel() {

    val viewState = MutableStateFlow<ViewState>(ViewState.Loading)

    init {
        loadTermsAndConditions()
    }

    @OptIn(ExperimentalResourceApi::class)
    private fun loadTermsAndConditions() {
        // TODO: In the future these should be loaded from a webserver and only fall-back to a
        //       hardcoded copy if loading fails. This makes sure these are as up-to-date as possible
        //       without requiring a network connection on first start.
        viewModelScope.launch {
            // This should not fail, yes it is IO, but the file should always be available, don't catch any error?
            viewState.value = ViewState.Content(
                accepted = settingsRepository.getSettings().termsAndConditionsAccepted,
                termsAndConditionsText = Res.readBytes("files/terms-and-conditions.md").decodeToString(),
                acceptAndNavigate = consumed<Boolean>()
            )
        }
    }

    fun acceptTermsAndConditions(accepted: Boolean) {
        viewModelScope.launch {
            settingsRepository.setTermsAndConditionsAccepted(accepted)
            viewState.updateType<ViewState, ViewState.Content> {
                it.copy(acceptAndNavigate = event(accepted, ::onHandledAcceptOrDeclineNavigation))
            }
        }
    }

    private fun onHandledAcceptOrDeclineNavigation() {
        viewState.updateType<ViewState, ViewState.Content> {
            it.copy(acceptAndNavigate = consumed<Boolean>())
        }
    }

    inline fun <K, reified T: K> MutableStateFlow<K>.updateType(block: (T) -> T) {
        update {
            if(it is T) {
                block(it)
            } else {
                it
            }
        }
    }

    sealed class ViewState {
        data object Loading: ViewState()

        data class Content(
            val accepted: Boolean,
            val termsAndConditionsText: String,
            val acceptAndNavigate: StateEvent<Boolean>,
        ): ViewState()
    }
}
