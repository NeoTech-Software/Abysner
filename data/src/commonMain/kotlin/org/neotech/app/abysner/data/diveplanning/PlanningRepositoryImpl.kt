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

package org.neotech.app.abysner.data.diveplanning

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
import org.neotech.app.abysner.data.diveplanning.resources.ConfigurationResourceV1
import org.neotech.app.abysner.data.diveplanning.resources.DivePlanInputResourceV1
import org.neotech.app.abysner.data.getJson
import org.neotech.app.abysner.data.setJson
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Salinity
import org.neotech.app.abysner.domain.diveplanning.PlanningRepository
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanInputModel
import org.neotech.app.abysner.domain.persistence.PersistenceRepository
import org.neotech.app.abysner.domain.persistence.get

@Inject
class PlanningRepositoryImpl(
    private val persistenceRepository: PersistenceRepository,
) : PlanningRepository {

    override val configuration: MutableStateFlow<Configuration> =
        MutableStateFlow(Configuration())

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        scope.launch {
            persistenceRepository.getPreferences().collect { preferences ->
                if(preferences.contains(PREFERENCE_KEY_ALGORITHM_TYPE)) {
                   preferences.migrateConfiguration()
                }
                configuration.emit(preferences.getJson<ConfigurationResourceV1>(PREFERENCE_KEY_GLOBAL_CONFIGURATION)?.toModel() ?: Configuration())
            }
        }
    }

    private suspend fun Preferences.migrateConfiguration() {
        val oldConfiguration = ConfigurationResourceV1(
            sacRate = get(PREFERENCE_KEY_DIVER_NORMAL_SAC, 20.0),
            sacRateOutOfAir = get(PREFERENCE_KEY_DIVER_OUT_OF_AIR_SAC, 40.0),
            maxPPO2Deco = get(PREFERENCE_KEY_MAX_PPO2_DECO, 1.6),
            maxPPO2 = get(PREFERENCE_KEY_MAX_PPO2, 1.4),
            maxAscentRate = get(PREFERENCE_KEY_DIVER_SPEED_ASCENT, 5.0),
            maxDescentRate = get(PREFERENCE_KEY_DIVER_SPEED_DESCENT, 20.0),
            gfLow = get(PREFERENCE_KEY_ALGORITHM_GF_LOW, 0.6),
            gfHigh = get(PREFERENCE_KEY_ALGORITHM_GF_HIGH, 0.7),
            forceMinimalDecoStopTime = get(PREFERENCE_KEY_FORCE_MINIMAL_STOP_TIME, true),
            useDecoGasBetweenSections = get(PREFERENCE_KEY_USE_DECO_GAS_BETWEEN_SECTIONS, false),
            decoStepSize = get(PREFERENCE_KEY_DECO_STEP_SIZE, 3),
            lastDecoStopDepth = get(PREFERENCE_KEY_LAST_DECO_STOP, 3),
            salinity = get(PREFERENCE_KEY_ENVIRONMENT_SALINITY, Salinity.WATER_FRESH).preferenceValue,
            algorithm = get(PREFERENCE_KEY_ALGORITHM_TYPE, Configuration.Algorithm.BUHLMANN_ZH16C).preferenceValue,
            contingencyDeeper = get(PREFERENCE_KEY_CONTINGENCY_DEEPER, 3),
            contingencyLonger = get(PREFERENCE_KEY_CONTINGENCY_LONGER, 3),
            maxEND = 30.0,
            altitude = 0.0
        )

        persistenceRepository.updatePreferences {
            it.setJson(PREFERENCE_KEY_GLOBAL_CONFIGURATION, oldConfiguration)

            // Remove old keys
            it.remove(PREFERENCE_KEY_ALGORITHM_TYPE)
            it.remove(PREFERENCE_KEY_ALGORITHM_GF_LOW)
            it.remove(PREFERENCE_KEY_ALGORITHM_GF_HIGH)
            it.remove(PREFERENCE_KEY_ENVIRONMENT_SALINITY)
            it.remove(PREFERENCE_KEY_DIVER_SPEED_ASCENT)
            it.remove(PREFERENCE_KEY_DIVER_SPEED_DESCENT)
            it.remove(PREFERENCE_KEY_DIVER_NORMAL_SAC)
            it.remove(PREFERENCE_KEY_DIVER_OUT_OF_AIR_SAC)
            it.remove(PREFERENCE_KEY_FORCE_MINIMAL_STOP_TIME)
            it.remove(PREFERENCE_KEY_DECO_STEP_SIZE)
            it.remove(PREFERENCE_KEY_LAST_DECO_STOP)
            it.remove(PREFERENCE_KEY_MAX_PPO2_DECO)
            it.remove(PREFERENCE_KEY_MAX_PPO2)
            it.remove(PREFERENCE_KEY_USE_DECO_GAS_BETWEEN_SECTIONS)
            it.remove(PREFERENCE_KEY_CONTINGENCY_DEEPER)
            it.remove(PREFERENCE_KEY_CONTINGENCY_LONGER)
        }

    }

    override fun updateConfiguration(updateBlock: (Configuration) -> Configuration) {
        val newConfiguration = updateBlock(configuration.value)
        configuration.update {
            newConfiguration
        }
        scope.launch {
            persistenceRepository.updatePreferences {
                it.setJson(PREFERENCE_KEY_GLOBAL_CONFIGURATION, newConfiguration.toResource())
            }
        }
    }

    override fun setDivePlanInput(divePlanInputModel: DivePlanInputModel) {
        scope.launch {
            persistenceRepository.updatePreferences {
                it.setJson(PREFERENCE_KEY_INPUT_DIVE_PLAN, divePlanInputModel.toResource())
            }
        }
    }

    override suspend fun getDivePlanInput(): DivePlanInputModel? {
        return persistenceRepository.getPreferences().first().getJson<DivePlanInputResourceV1>(PREFERENCE_KEY_INPUT_DIVE_PLAN)?.toModel()
    }
}

private val PREFERENCE_KEY_GLOBAL_CONFIGURATION = stringPreferencesKey("global.configuration")
private val PREFERENCE_KEY_INPUT_DIVE_PLAN = stringPreferencesKey("input.diveplan")

@Deprecated(PREFERENCE_DEPRECATION_WARNING)
private val PREFERENCE_KEY_ALGORITHM_TYPE = stringPreferencesKey("algorithm.type")
@Deprecated(PREFERENCE_DEPRECATION_WARNING)
private val PREFERENCE_KEY_ALGORITHM_GF_LOW = doublePreferencesKey("algorithm.buhlmann.gradientFactor.low")
@Deprecated(PREFERENCE_DEPRECATION_WARNING)
private val PREFERENCE_KEY_ALGORITHM_GF_HIGH = doublePreferencesKey("algorithm.buhlmann.gradientFactor.high")

@Deprecated(PREFERENCE_DEPRECATION_WARNING)
private val PREFERENCE_KEY_ENVIRONMENT_SALINITY = stringPreferencesKey("environment.salinity")

@Deprecated(PREFERENCE_DEPRECATION_WARNING)
private val PREFERENCE_KEY_DIVER_SPEED_ASCENT = doublePreferencesKey("diver.speed.ascent")
@Deprecated(PREFERENCE_DEPRECATION_WARNING)
private val PREFERENCE_KEY_DIVER_SPEED_DESCENT = doublePreferencesKey("diver.speed.descent")
@Deprecated(PREFERENCE_DEPRECATION_WARNING)
private val PREFERENCE_KEY_DIVER_NORMAL_SAC = doublePreferencesKey("diver.sac.normal")
@Deprecated(PREFERENCE_DEPRECATION_WARNING)
private val PREFERENCE_KEY_DIVER_OUT_OF_AIR_SAC = doublePreferencesKey("diver.sac.outOfAir")

@Deprecated(PREFERENCE_DEPRECATION_WARNING)
private val PREFERENCE_KEY_FORCE_MINIMAL_STOP_TIME = booleanPreferencesKey("deco.minimalStopTime")
@Deprecated(PREFERENCE_DEPRECATION_WARNING)
private val PREFERENCE_KEY_LAST_DECO_STOP = intPreferencesKey("deco.lastStop")
@Deprecated(PREFERENCE_DEPRECATION_WARNING)
private val PREFERENCE_KEY_DECO_STEP_SIZE = intPreferencesKey("deco.stepSize")
@Deprecated(PREFERENCE_DEPRECATION_WARNING)
private val PREFERENCE_KEY_MAX_PPO2 = doublePreferencesKey("deco.ppo2.normal")
@Deprecated(PREFERENCE_DEPRECATION_WARNING)
private val PREFERENCE_KEY_MAX_PPO2_DECO = doublePreferencesKey("deco.ppo2.deco")

@Deprecated(PREFERENCE_DEPRECATION_WARNING)
private val PREFERENCE_KEY_CONTINGENCY_DEEPER = intPreferencesKey("contingency.deeper")

@Deprecated(PREFERENCE_DEPRECATION_WARNING)
private val PREFERENCE_KEY_CONTINGENCY_LONGER = intPreferencesKey("contingency.longer")

@Deprecated(PREFERENCE_DEPRECATION_WARNING)
private val PREFERENCE_KEY_USE_DECO_GAS_BETWEEN_SECTIONS = booleanPreferencesKey("multiLevel.useDecoGasBetweenSections")

private const val PREFERENCE_DEPRECATION_WARNING = "Will be removed in future version when most users have migrated to the JSON format stored configuration."
