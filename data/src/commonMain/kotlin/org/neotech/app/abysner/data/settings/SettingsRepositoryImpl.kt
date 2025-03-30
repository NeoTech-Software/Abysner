/*
 * Abysner - Dive planner
 * Copyright (C) 2025 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.data.settings

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import org.neotech.app.abysner.data.PREFERENCE_DEPRECATION_WARNING
import org.neotech.app.abysner.data.getJson
import org.neotech.app.abysner.data.setJson
import org.neotech.app.abysner.data.settings.resources.SettingsResourceV1
import org.neotech.app.abysner.domain.persistence.PersistenceRepository
import org.neotech.app.abysner.domain.persistence.get
import org.neotech.app.abysner.domain.settings.SettingsRepository
import org.neotech.app.abysner.domain.settings.model.SettingsModel

@Inject
class SettingsRepositoryImpl(
    private val persistenceRepository: PersistenceRepository,
) : SettingsRepository {

    override val settings: MutableStateFlow<SettingsModel> = MutableStateFlow(SettingsModel())
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        scope.launch {
            persistenceRepository.getPreferences().collect { preferences ->
                if(preferences.contains(PREFERENCE_KEY_TERMS_AND_CONDITIONS_ACCEPTED)) {
                    preferences.migrateSettings()
                }

                settings.emit(preferences.getJson<SettingsResourceV1>(PREFERENCE_KEY_GLOBAL_SETTINGS)?.toModel() ?: SettingsModel())
            }
        }
    }

    private suspend fun Preferences.migrateSettings() {
        val oldSettings = SettingsResourceV1(
            showBasicDecoTable = get(PREFERENCE_KEY_BASIC_DECO_TABLE, false),
            termsAndConditionsAccepted = get(PREFERENCE_KEY_TERMS_AND_CONDITIONS_ACCEPTED, false)
        )

        persistenceRepository.updatePreferences {
            it.setJson(PREFERENCE_KEY_GLOBAL_SETTINGS, oldSettings)

            // Remove old keys
            it.remove(PREFERENCE_KEY_BASIC_DECO_TABLE)
            it.remove(PREFERENCE_KEY_TERMS_AND_CONDITIONS_ACCEPTED)
        }
    }

    override fun updateSettings(updateBlock: (SettingsModel) -> SettingsModel) {
        val newSettings = updateBlock(settings.value)
        settings.update {
            newSettings
        }
        scope.launch {
            persistenceRepository.updatePreferences {
                it.setJson(PREFERENCE_KEY_GLOBAL_SETTINGS, newSettings.toResource())
            }
        }
    }

    override suspend fun setTermsAndConditionsAccepted(accepted: Boolean) {
        updateSettings {
            it.copy(termsAndConditionsAccepted = accepted)
        }
    }

    override suspend fun getSettings(): SettingsModel {
        return persistenceRepository.getPreferences().first().getJson<SettingsResourceV1>(PREFERENCE_KEY_GLOBAL_SETTINGS)?.toModel() ?: SettingsModel()
    }
}

private val PREFERENCE_KEY_GLOBAL_SETTINGS = stringPreferencesKey("global.settings")

@Deprecated(PREFERENCE_DEPRECATION_WARNING)
private val PREFERENCE_KEY_BASIC_DECO_TABLE = booleanPreferencesKey("settings.basicDecoTable")

@Deprecated(PREFERENCE_DEPRECATION_WARNING)
private val PREFERENCE_KEY_TERMS_AND_CONDITIONS_ACCEPTED = booleanPreferencesKey("settings.termsAndConditions.accepted")
