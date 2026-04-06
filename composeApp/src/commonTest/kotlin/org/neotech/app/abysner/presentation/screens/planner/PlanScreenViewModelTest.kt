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

package org.neotech.app.abysner.presentation.screens.planner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Salinity
import org.neotech.app.abysner.domain.diveplanning.PlanningRepository
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanInputModel
import org.neotech.app.abysner.domain.settings.SettingsRepository
import org.neotech.app.abysner.domain.settings.model.SettingsModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PlanScreenViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_configurationReflectsPlanningRepositoryConfiguration() = runTest {
        val saltWaterConfig = Configuration(salinity = Salinity.WATER_SALT)

        val viewModel = PlanScreenViewModel(
            planningRepository = FakePlanningRepository(saltWaterConfig),
            settingsRepository = FakeSettingsRepository(),
        )

        val state = viewModel.uiState.drop(1).first()
        assertEquals(saltWaterConfig, state.configuration)
    }

    @Test
    fun uiState_configurationUpdatesWhenRepositoryConfigurationChanges() = runTest {
        val initialConfig = Configuration(salinity = Salinity.WATER_SALT)
        val updatedConfig = Configuration(salinity = Salinity.WATER_FRESH)

        val planningRepo = FakePlanningRepository(initialConfig)
        val viewModel = PlanScreenViewModel(
            planningRepository = planningRepo,
            settingsRepository = FakeSettingsRepository(),
        )

        assertEquals(initialConfig, viewModel.uiState.drop(1).first().configuration)

        val nextState = async { viewModel.uiState.drop(1).first() }
        planningRepo.configuration.value = updatedConfig

        assertEquals(updatedConfig, nextState.await().configuration)
    }
}

private class FakePlanningRepository(
    initialConfig: Configuration = Configuration(),
) : PlanningRepository {
    override val configuration = MutableStateFlow(initialConfig)

    override fun updateConfiguration(updateBlock: (Configuration) -> Configuration) {
        configuration.value = updateBlock(configuration.value)
    }

    override fun setDivePlanInput(divePlanInputModel: DivePlanInputModel) {}

    override suspend fun getDivePlanInput(): DivePlanInputModel? = null
}

private class FakeSettingsRepository : SettingsRepository {
    override val settings: StateFlow<SettingsModel> = MutableStateFlow(SettingsModel())

    override suspend fun setTermsAndConditionsAccepted(accepted: Boolean) {}

    override fun updateSettings(updateBlock: (SettingsModel) -> SettingsModel) {}

    override suspend fun getSettings(): SettingsModel = SettingsModel()
}
