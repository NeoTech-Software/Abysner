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

package org.neotech.app.abysner.data.diveplanning

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.neotech.app.abysner.data.diveplanning.resources.ConfigurationResourceV1
import org.neotech.app.abysner.data.diveplanning.resources.MultiDivePlanInputResourceV1
import org.neotech.app.abysner.data.getJson
import org.neotech.app.abysner.data.setJson
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.DiveMode
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanInputModel
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.diveplanning.model.MultiDivePlanInputModel
import org.neotech.app.abysner.domain.diveplanning.model.PlannedCylinderModel
import org.neotech.app.abysner.domain.persistence.PersistenceRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class PlanningRepositoryImplTest {

    @Test
    fun init_loadsMultiDivePlanInputFromStorage() = runTest {
        val dataSource = InMemoryPlanningDataSource()
        // Create a non-default dive plan to later verify against.
        val storedPlan = MultiDivePlanInputModel.Default.copy(
            dives = listOf(DivePlanInputModel.Default, DivePlanInputModel.Default)
        ).toResource()
        dataSource.savePlan(storedPlan)

        val repository = PlanningRepositoryImpl(dataSource, InMemoryPersistenceRepository(), StandardTestDispatcher(testScheduler))

        val loaded = withTimeout(1.seconds) { repository.multiDivePlanInput.filterNotNull().first() }

        assertEquals(storedPlan, loaded.toResource())
    }

    @Test
    fun init_loadsConfigurationFromStorage() = runTest {
        val dataSource = InMemoryPlanningDataSource()
        // Create a non-default configuration to later verify against.
        val storedConfiguration = Configuration(altitude = 500.0).toResource()
        dataSource.saveConfiguration(storedConfiguration)

        val repository = PlanningRepositoryImpl(dataSource, InMemoryPersistenceRepository(), StandardTestDispatcher(testScheduler))

        withTimeout(1.seconds) { repository.multiDivePlanInput.filterNotNull().first() }

        assertEquals(storedConfiguration.toModel(), repository.configuration.value)
    }

    @Test
    fun init_loadsDefaultPlanWhenNoStoredInput() = runTest {
        val dataSource = InMemoryPlanningDataSource()
        val repository = PlanningRepositoryImpl(dataSource, InMemoryPersistenceRepository(), StandardTestDispatcher(testScheduler))

        val loaded = withTimeout(1.seconds) { repository.multiDivePlanInput.filterNotNull().first() }

        assertEquals(MultiDivePlanInputModel.Default, loaded)
    }

    @Test
    fun init_migratesPlanFromPreferencesWhenNoStoredInput() = runTest {
        val dataSource = InMemoryPlanningDataSource()
        val persistenceRepository = InMemoryPersistenceRepository()
        val storedPlan = MultiDivePlanInputModel.Default.copy(
            dives = listOf(DivePlanInputModel.Default, DivePlanInputModel.Default)
        ).toResource()
        persistenceRepository.updatePreferences {
            it.setJson(PREFERENCE_KEY_LEGACY_PLAN, storedPlan)
        }

        val repository = PlanningRepositoryImpl(dataSource, persistenceRepository, StandardTestDispatcher(testScheduler))

        val loaded = withTimeout(1.seconds) { repository.multiDivePlanInput.filterNotNull().first() }

        assertEquals(storedPlan, loaded.toResource())
        assertEquals(storedPlan, dataSource.loadPlan())
        assertNull(persistenceRepository.preferencesFlow.value.getJson<MultiDivePlanInputResourceV1>(PREFERENCE_KEY_LEGACY_PLAN))
    }

    @Test
    fun init_migratesConfigurationFromPreferences() = runTest {
        val dataSource = InMemoryPlanningDataSource()
        val persistenceRepository = InMemoryPersistenceRepository()
        val legacyConfiguration = Configuration(altitude = 1000.0).toResource()
        persistenceRepository.updatePreferences {
            it.setJson<ConfigurationResourceV1>(PREFERENCE_KEY_LEGACY_CONFIGURATION, legacyConfiguration)
        }

        val repository = PlanningRepositoryImpl(dataSource, persistenceRepository, StandardTestDispatcher(testScheduler))

        withTimeout(1.seconds) { repository.multiDivePlanInput.filterNotNull().first() }

        assertEquals(legacyConfiguration.toModel(), repository.configuration.value)
        assertEquals(legacyConfiguration, dataSource.loadConfiguration())
        assertNull(persistenceRepository.preferencesFlow.value.getJson<ConfigurationResourceV1>(PREFERENCE_KEY_LEGACY_CONFIGURATION))
    }

    @Test
    fun init_recomputesCylinderStateOnLoadedInput() = runTest {
        val dataSource = InMemoryPlanningDataSource()
        val cylinder = Cylinder.steel12Liter(gas = Gas.Air)
        val storedPlan = MultiDivePlanInputModel(
            dives = listOf(
                DivePlanInputModel(
                    diveMode = DiveMode.OPEN_CIRCUIT,
                    deeper = false,
                    longer = false,
                    bailout = false,
                    plannedProfile = listOf(DiveProfileSection(duration = 10, depthInMeters = 20.0, cylinder = cylinder)),
                    // This is the only cylinder in the plan, so it should be marked as checked and locked when active in use.
                    cylinders = listOf(PlannedCylinderModel(cylinder = cylinder, isChecked = false, isLocked = false)),
                    surfaceIntervalBefore = null,
                )
            )
        ).toResource()
        dataSource.savePlan(storedPlan)

        val repository = PlanningRepositoryImpl(dataSource, InMemoryPersistenceRepository(), StandardTestDispatcher(testScheduler))

        val loaded = withTimeout(1.seconds) { repository.multiDivePlanInput.filterNotNull().first() }

        val loadedCylinder = loaded.dives.first().cylinders.first()
        assertTrue(loadedCylinder.isChecked)
        assertTrue(loadedCylinder.isLocked)
    }

    @Test
    fun updateMultiDivePlanInput_persistsToStorage() = runTest {
        val dataSource = InMemoryPlanningDataSource()
        val repository = PlanningRepositoryImpl(dataSource, InMemoryPersistenceRepository(), StandardTestDispatcher(testScheduler))
        withTimeout(1.seconds) { repository.multiDivePlanInput.filterNotNull().first() }

        repository.updateMultiDivePlanInput { it.updateDive(0) { copy(deeper = true) } }

        advanceTimeBy(1001.milliseconds)
        testScheduler.runCurrent()

        assertEquals(true, dataSource.planFlow.value?.dives?.first()?.deeper)
    }

    @Test
    fun updateConfiguration_persistsToStorage() = runTest {
        val dataSource = InMemoryPlanningDataSource()
        val repository = PlanningRepositoryImpl(dataSource, InMemoryPersistenceRepository(), StandardTestDispatcher(testScheduler))
        withTimeout(1.seconds) { repository.multiDivePlanInput.filterNotNull().first() }

        repository.updateConfiguration { it.copy(altitude = 500.0) }

        advanceTimeBy(1001.milliseconds)
        testScheduler.runCurrent()

        assertEquals(500.0, dataSource.configurationFlow.value?.altitude)
    }
}

private val PREFERENCE_KEY_LEGACY_CONFIGURATION = stringPreferencesKey("global.configuration")
private val PREFERENCE_KEY_LEGACY_PLAN = stringPreferencesKey("input.diveplan.multi.v1")


private class InMemoryPlanningDataSource : PlanningDataSource {

    val configurationFlow = MutableStateFlow<ConfigurationResourceV1?>(null)
    val planFlow = MutableStateFlow<MultiDivePlanInputResourceV1?>(null)

    override suspend fun loadConfiguration(): ConfigurationResourceV1? = configurationFlow.value

    override suspend fun saveConfiguration(configuration: ConfigurationResourceV1) {
        configurationFlow.value = configuration
    }

    override suspend fun loadPlan(): MultiDivePlanInputResourceV1? = planFlow.value

    override suspend fun savePlan(plan: MultiDivePlanInputResourceV1) {
        planFlow.value = plan
    }
}

private class InMemoryPersistenceRepository : PersistenceRepository {

    val preferencesFlow = MutableStateFlow(emptyPreferences())

    override fun getPreferences(): Flow<Preferences> = preferencesFlow

    override suspend fun updatePreferences(update: (MutablePreferences) -> Unit) {
        val mutable = preferencesFlow.value.toMutablePreferences()
        update(mutable)
        preferencesFlow.value = mutable
    }
}
