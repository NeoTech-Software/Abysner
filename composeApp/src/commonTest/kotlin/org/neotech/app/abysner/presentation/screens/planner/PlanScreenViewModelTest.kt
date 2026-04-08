/*
 * Abysner - Dive planner
 * Copyright (C) 2026 Neotech
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Salinity
import org.neotech.app.abysner.domain.diveplanning.PlanningRepository
import org.neotech.app.abysner.domain.diveplanning.model.MultiDivePlanInputModel
import org.neotech.app.abysner.domain.settings.SettingsRepository
import org.neotech.app.abysner.domain.settings.model.SettingsModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.minutes

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

        val planningRepo = FakePlanningRepository(saltWaterConfig)
        val viewModel = createViewModel(planningRepository = planningRepo)

        collectForTest(viewModel.uiState)

        assertEquals(saltWaterConfig, viewModel.uiState.value.configuration)
    }

    @Test
    fun uiState_configurationUpdatesWhenRepositoryConfigurationChanges() = runTest {
        val initialConfig = Configuration(salinity = Salinity.WATER_SALT)
        val updatedConfig = Configuration(salinity = Salinity.WATER_FRESH)

        val planningRepo = FakePlanningRepository(initialConfig)
        val viewModel = createViewModel(planningRepository = planningRepo)

        collectForTest(viewModel.uiState)

        assertEquals(initialConfig, viewModel.uiState.value.configuration)

        planningRepo.configuration.value = updatedConfig
        assertEquals(updatedConfig, viewModel.uiState.value.configuration)
    }

    @Test
    fun addDive_newDiveHasSurfaceIntervalAndSelectedIndexIsLast() = runTest {
        val viewModel = createViewModel()

        collectForTest(viewModel.uiState)

        viewModel.addDive(60.minutes)

        assertEquals(2, viewModel.uiState.value.diveCount)
        assertEquals(1, viewModel.uiState.value.selectedDiveIndex)
        assertEquals(1, viewModel.uiState.value.surfaceIntervals.size)
        assertEquals(60.minutes, viewModel.uiState.value.surfaceIntervals[0])
    }

    @Test
    fun removeDive_atLastIndexShiftsSelectedDiveIndex() = runTest {
        val viewModel = createViewModel()

        collectForTest(viewModel.uiState)

        viewModel.addDive(60.minutes)
        viewModel.removeDive(1)

        assertEquals(1, viewModel.uiState.value.diveCount)
        assertEquals(0, viewModel.uiState.value.surfaceIntervals.size)
        assertEquals(0, viewModel.uiState.value.selectedDiveIndex)
    }

    @Test
    fun removeDive_atFirstIndexChangesSecondDiveToNullSurfaceInterval() = runTest {
        val viewModel = createViewModel()

        collectForTest(viewModel.uiState)

        viewModel.addDive(60.minutes)
        viewModel.removeDive(0)

        assertEquals(1, viewModel.uiState.value.diveCount)
        assertEquals(0, viewModel.uiState.value.surfaceIntervals.size)
    }

    @Test
    fun removeDive_whenOnlyOneDiveIsNoOp() = runTest {
        val viewModel = createViewModel()

        collectForTest(viewModel.uiState)

        assertEquals(1, viewModel.uiState.value.diveCount)

        viewModel.removeDive(0)

        assertEquals(1, viewModel.uiState.value.diveCount)
    }

    @Test
    fun updateSurfaceInterval_onFirstDiveThrows() = runTest {
        val viewModel = createViewModel()

        collectForTest(viewModel.uiState)

        assertFailsWith<IllegalArgumentException> {
            viewModel.updateSurfaceInterval(0, 60.minutes)
        }
    }

    @Test
    fun selectDive_doesNotRetriggerCalculation() = runTest {
        val viewModel = createViewModel()

        var calculationTriggered: Boolean
        collectForTest(viewModel.uiState.map { it.isCalculatingDivePlan }) {
            if (it) calculationTriggered = true
        }

        viewModel.addDive(60.minutes)
        calculationTriggered = false

        viewModel.selectDive(0)

        assertFalse(calculationTriggered)
        assertEquals(0, viewModel.uiState.value.selectedDiveIndex)
    }
}

private class FakePlanningRepository(
    initialConfig: Configuration = Configuration(),
) : PlanningRepository {
    override val configuration = MutableStateFlow(initialConfig)

    override fun updateConfiguration(updateBlock: (Configuration) -> Configuration) {
        configuration.value = updateBlock(configuration.value)
    }

    override fun setMultiDivePlanInput(model: MultiDivePlanInputModel) {}

    override suspend fun getMultiDivePlanInput(): MultiDivePlanInputModel? = null
}

private class FakeSettingsRepository : SettingsRepository {
    override val settings: StateFlow<SettingsModel> = MutableStateFlow(SettingsModel())

    override suspend fun setTermsAndConditionsAccepted(accepted: Boolean) {}

    override fun updateSettings(updateBlock: (SettingsModel) -> SettingsModel) {}

    override suspend fun getSettings(): SettingsModel = SettingsModel()
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun TestScope.createViewModel(
    planningRepository: PlanningRepository = FakePlanningRepository(),
    settingsRepository: SettingsRepository = FakeSettingsRepository(),
) = PlanScreenViewModel(
    planningRepository = planningRepository,
    settingsRepository = settingsRepository,
    ioDispatcher = UnconfinedTestDispatcher(testScheduler),
    calculationDispatcher = UnconfinedTestDispatcher(testScheduler),
)

/**
 * Launches a no-op collector for [flow] in
 * [backgroundScope][kotlinx.coroutines.test.TestScope.backgroundScope] using an
 * [UnconfinedTestDispatcher], keeping any `SharingStarted.WhileSubscribed` upstream
 * active for the lifetime of the test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private fun <T> TestScope.collectForTest(flow: Flow<T>, block: suspend (T) -> Unit = {}) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        flow.collect(block)
    }
}
