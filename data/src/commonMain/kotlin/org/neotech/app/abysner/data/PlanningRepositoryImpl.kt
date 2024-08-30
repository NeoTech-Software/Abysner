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

package org.neotech.app.abysner.data

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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject
import org.neotech.app.abysner.data.plan.resources.DivePlanInputResourceV1
import org.neotech.app.abysner.data.plan.toModel
import org.neotech.app.abysner.data.plan.toResource
import org.neotech.app.abysner.domain.diveplanning.PlanningRepository
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Salinity
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanInputModel
import org.neotech.app.abysner.domain.persistence.PersistenceRepository
import org.neotech.app.abysner.domain.persistence.get
import org.neotech.app.abysner.domain.persistence.set

/**
 * TODO:
 *   May need to consider updating to JSON or something for serialization and deserialization of the
 *   configuration object here, so manual mapping is not required.
 */
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
                configuration.update {
                    it.copy(
                        algorithm = preferences.get(PREFERENCE_KEY_ALGORITHM_TYPE, Configuration.Algorithm.BUHLMANN_ZH16C),
                        gfLow = preferences.get(PREFERENCE_KEY_ALGORITHM_GF_LOW, 0.3),
                        gfHigh = preferences.get(PREFERENCE_KEY_ALGORITHM_GF_HIGH, 0.7),
                        salinity = preferences.get(PREFERENCE_KEY_ENVIRONMENT_SALINITY, Salinity.WATER_FRESH),
                        maxAscentRate = preferences.get(PREFERENCE_KEY_DIVER_SPEED_ASCENT, 5.0),
                        maxDescentRate = preferences.get(PREFERENCE_KEY_DIVER_SPEED_DESCENT, 20.0),
                        sacRate = preferences.get(PREFERENCE_KEY_DIVER_NORMAL_SAC, 20.0),
                        sacRateOutOfAir = preferences.get(PREFERENCE_KEY_DIVER_OUT_OF_AIR_SAC, 40.0),
                        forceMinimalDecoStopTime = preferences.get(PREFERENCE_KEY_FORCE_MINIMAL_STOP_TIME, true),
                        decoStepSize = preferences.get(PREFERENCE_KEY_DECO_STEP_SIZE, 3),
                        lastDecoStopDepth = preferences.get(PREFERENCE_KEY_LAST_DECO_STOP, 3),
                        maxPPO2Deco = preferences.get(PREFERENCE_KEY_MAX_PPO2_DECO, 1.6),
                        maxPPO2 = preferences.get(PREFERENCE_KEY_MAX_PPO2, 1.4),
                        useDecoGasBetweenSections = preferences.get(PREFERENCE_KEY_USE_DECO_GAS_BETWEEN_SECTIONS, false),
                        contingencyDeeper = preferences.get(PREFERENCE_KEY_CONTINGENCY_DEEPER, 3),
                        contingencyLonger = preferences.get(PREFERENCE_KEY_CONTINGENCY_LONGER, 3)
                    )
                }
            }
        }
    }

    override fun updateConfiguration(updateBlock: (Configuration) -> Configuration) {
        val newConfiguration = updateBlock(configuration.value)
        configuration.update {
            newConfiguration
        }
        scope.launch {
            persistenceRepository.updatePreferences {

                it[PREFERENCE_KEY_ALGORITHM_TYPE] = newConfiguration.algorithm
                it[PREFERENCE_KEY_ALGORITHM_GF_LOW] = newConfiguration.gfLow
                it[PREFERENCE_KEY_ALGORITHM_GF_HIGH] = newConfiguration.gfHigh

                it[PREFERENCE_KEY_ENVIRONMENT_SALINITY] = newConfiguration.salinity

                it[PREFERENCE_KEY_DIVER_SPEED_ASCENT] = newConfiguration.maxAscentRate
                it[PREFERENCE_KEY_DIVER_SPEED_DESCENT] = newConfiguration.maxDescentRate
                it[PREFERENCE_KEY_DIVER_NORMAL_SAC] = newConfiguration.sacRate
                it[PREFERENCE_KEY_DIVER_OUT_OF_AIR_SAC] = newConfiguration.sacRateOutOfAir

                it[PREFERENCE_KEY_FORCE_MINIMAL_STOP_TIME] = newConfiguration.forceMinimalDecoStopTime
                it[PREFERENCE_KEY_DECO_STEP_SIZE] = newConfiguration.decoStepSize
                it[PREFERENCE_KEY_LAST_DECO_STOP] = newConfiguration.lastDecoStopDepth
                it[PREFERENCE_KEY_MAX_PPO2] = newConfiguration.maxPPO2
                it[PREFERENCE_KEY_MAX_PPO2_DECO] = newConfiguration.maxPPO2Deco

                it[PREFERENCE_KEY_USE_DECO_GAS_BETWEEN_SECTIONS] = newConfiguration.useDecoGasBetweenSections

                it[PREFERENCE_KEY_CONTINGENCY_DEEPER] = newConfiguration.contingencyDeeper
                it[PREFERENCE_KEY_CONTINGENCY_LONGER] = newConfiguration.contingencyLonger
            }
        }
    }

    override fun setDivePlanInput(divePlanInputModel: DivePlanInputModel) {
        scope.launch {
            persistenceRepository.updatePreferences {
                it[PREFERENCE_KEY_INPUT_DIVE_PLAN] = Json.encodeToString(divePlanInputModel.toResource())
            }
        }
    }

    override suspend fun getDivePlanInput(): DivePlanInputModel? {
        val json = persistenceRepository.getPreferences().first()[PREFERENCE_KEY_INPUT_DIVE_PLAN] ?: return null
        return Json.decodeFromString<DivePlanInputResourceV1>(json).toModel()
    }
}

private val PREFERENCE_KEY_INPUT_DIVE_PLAN = stringPreferencesKey("input.diveplan")

private val PREFERENCE_KEY_ALGORITHM_TYPE = stringPreferencesKey("algorithm.type")
private val PREFERENCE_KEY_ALGORITHM_GF_LOW = doublePreferencesKey("algorithm.buhlmann.gradientFactor.low")
private val PREFERENCE_KEY_ALGORITHM_GF_HIGH = doublePreferencesKey("algorithm.buhlmann.gradientFactor.high")

private val PREFERENCE_KEY_ENVIRONMENT_SALINITY = stringPreferencesKey("environment.salinity")

private val PREFERENCE_KEY_DIVER_SPEED_ASCENT = doublePreferencesKey("diver.speed.ascent")
private val PREFERENCE_KEY_DIVER_SPEED_DESCENT = doublePreferencesKey("diver.speed.descent")
private val PREFERENCE_KEY_DIVER_NORMAL_SAC = doublePreferencesKey("diver.sac.normal")
private val PREFERENCE_KEY_DIVER_OUT_OF_AIR_SAC = doublePreferencesKey("diver.sac.outOfAir")

private val PREFERENCE_KEY_FORCE_MINIMAL_STOP_TIME = booleanPreferencesKey("deco.minimalStopTime")
private val PREFERENCE_KEY_LAST_DECO_STOP = intPreferencesKey("deco.lastStop")
private val PREFERENCE_KEY_DECO_STEP_SIZE = intPreferencesKey("deco.stepSize")
private val PREFERENCE_KEY_MAX_PPO2 = doublePreferencesKey("deco.ppo2.normal")
private val PREFERENCE_KEY_MAX_PPO2_DECO = doublePreferencesKey("deco.ppo2.deco")

private val PREFERENCE_KEY_CONTINGENCY_DEEPER = intPreferencesKey("contingency.deeper")
private val PREFERENCE_KEY_CONTINGENCY_LONGER = intPreferencesKey("contingency.longer")

private val PREFERENCE_KEY_USE_DECO_GAS_BETWEEN_SECTIONS = booleanPreferencesKey("multiLevel.useDecoGasBetweenSections")
