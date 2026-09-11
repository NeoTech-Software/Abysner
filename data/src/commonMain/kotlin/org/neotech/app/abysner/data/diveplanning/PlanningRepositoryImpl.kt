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

package org.neotech.app.abysner.data.diveplanning

import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import dev.zacsweers.metro.Inject
import org.neotech.app.abysner.data.diveplanning.resources.ConfigurationResourceV1
import org.neotech.app.abysner.data.diveplanning.resources.MultiDivePlanInputResourceV1
import org.neotech.app.abysner.data.getJson
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.diveplanning.PlanningRepository
import org.neotech.app.abysner.domain.diveplanning.model.MultiDivePlanInputModel
import org.neotech.app.abysner.domain.persistence.PersistenceRepository
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
class PlanningRepositoryImpl @Inject constructor(
    private val dataSource: PlanningDataSource,
    private val persistenceRepository: PersistenceRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PlanningRepository {

    override val configuration: MutableStateFlow<Configuration> = MutableStateFlow(Configuration())

    override val multiDivePlanInput = MutableStateFlow<MultiDivePlanInputModel?>(null)

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    init {
        scope.launch {
            loadInitialState()
        }

        scope.launch {
            configuration
                .debounce(PERSIST_DEBOUNCE_MILLIS.milliseconds)
                .collectLatest { config ->
                    runCatching {
                        dataSource.saveConfiguration(config.toResource())
                    }.onFailure { it.printStackTrace() }
                }
        }

        scope.launch {
            multiDivePlanInput.filterNotNull()
                .distinctUntilChanged()
                .debounce(PERSIST_DEBOUNCE_MILLIS.milliseconds)
                .collectLatest { model ->
                    runCatching {
                        dataSource.savePlan(model.toResource())
                    }.onFailure { it.printStackTrace() }
                }
        }
    }

    private suspend fun loadInitialState() {
        val loadedConfiguration = dataSource.loadConfiguration() ?: migrateConfigurationFromPreferences()
        configuration.value = loadedConfiguration?.toModel() ?: Configuration()

        val loadedPlan = dataSource.loadPlan() ?: migrateActivePlanFromPreferences()
        val model = loadedPlan?.toModel() ?: MultiDivePlanInputModel.Default
        multiDivePlanInput.value = model.copy(dives = model.dives.map { it.recomputeCylinderState() })
    }

    private suspend fun migrateConfigurationFromPreferences(): ConfigurationResourceV1? {
        val preferences = persistenceRepository.getPreferences().firstOrNull() ?: return null
        val legacy = preferences.getJson<ConfigurationResourceV1>(PREFERENCE_KEY_GLOBAL_CONFIGURATION) ?: return null
        dataSource.saveConfiguration(legacy)
        persistenceRepository.updatePreferences { it.remove(PREFERENCE_KEY_GLOBAL_CONFIGURATION) }
        return legacy
    }

    private suspend fun migrateActivePlanFromPreferences(): MultiDivePlanInputResourceV1? {
        val preferences = persistenceRepository.getPreferences().firstOrNull() ?: return null
        val legacy = preferences.getJson<MultiDivePlanInputResourceV1>(PREFERENCE_KEY_INPUT_MULTI_DIVE_PLAN) ?: return null
        dataSource.savePlan(legacy)
        persistenceRepository.updatePreferences { it.remove(PREFERENCE_KEY_INPUT_MULTI_DIVE_PLAN) }
        return legacy
    }

    override fun updateConfiguration(updateBlock: (Configuration) -> Configuration): Configuration {
        val newConfiguration = updateBlock(configuration.value)
        configuration.value = newConfiguration
        return newConfiguration
    }

    override fun updateMultiDivePlanInput(updateBlock: (MultiDivePlanInputModel) -> MultiDivePlanInputModel): MultiDivePlanInputModel? {
        val current = multiDivePlanInput.value ?: return null
        val updated = updateBlock(current)
        multiDivePlanInput.value = updated
        return updated
    }
}

private val PREFERENCE_KEY_GLOBAL_CONFIGURATION = stringPreferencesKey("global.configuration")
private val PREFERENCE_KEY_INPUT_MULTI_DIVE_PLAN = stringPreferencesKey("input.diveplan.multi.v1")

private const val PERSIST_DEBOUNCE_MILLIS = 1000L
