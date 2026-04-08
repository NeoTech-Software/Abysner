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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.neotech.app.abysner.data.diveplanning.resources.DivePlanInputResourceV1
import org.neotech.app.abysner.data.diveplanning.resources.MultiDivePlanInputResourceV1
import org.neotech.app.abysner.data.getJson
import org.neotech.app.abysner.data.setJson
import org.neotech.app.abysner.domain.persistence.PersistenceRepository
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PlanningRepositoryImplTest {

    @Test
    fun init_migratesOldDivePlanToMultiDivePlan() = runBlocking {
        val inMemoryPersistenceRepository = InMemoryPersistenceRepository()

        // Prepare in-memory old data state
        val singleDive = DivePlanInputResourceV1(deeper = false, longer = false, cylinders = emptyList(), profile = emptyList())
        inMemoryPersistenceRepository.updatePreferences { it.setJson(KEY_OLD, singleDive) }

        // Create the planning repository, this will trigger migration
        PlanningRepositoryImpl(inMemoryPersistenceRepository)

        // Wait for migration to finish (at most 1 second, there is no real IO)
        val updatedPrefs = withTimeout(1_000) {
            inMemoryPersistenceRepository.prefsFlow.first { !it.contains(KEY_OLD) }
        }

        // Check if new key exists, and if old key got removed
        assertNotNull(updatedPrefs.getJson<MultiDivePlanInputResourceV1>(KEY_NEW))
        assertFalse(updatedPrefs.contains(KEY_OLD))
    }

    @Test
    fun init_withCorruptOldDivePlan_removesOldKeyWithoutMigrating() = runBlocking {
        val inMemoryPersistenceRepository = InMemoryPersistenceRepository()

        // Prepare in-memory old data state (with corrupt JSON value)
        inMemoryPersistenceRepository.updatePreferences { it[KEY_OLD] = "NOT_VALID_JSON" }

        // Create the planning repository, this will trigger migration
        PlanningRepositoryImpl(inMemoryPersistenceRepository)

        // Wait for migration to finish (at most 1 second, there is no real IO)
        val updatedPrefs = withTimeout(1_000) {
            inMemoryPersistenceRepository.prefsFlow.first { !it.contains(KEY_OLD) }
        }

        // Check that old key is removed and no new key was written
        assertFalse(updatedPrefs.contains(KEY_OLD))
        assertNull(updatedPrefs.getJson<MultiDivePlanInputResourceV1>(KEY_NEW))
    }
}

private val KEY_OLD = stringPreferencesKey("input.diveplan")
private val KEY_NEW  = stringPreferencesKey("input.diveplan.multi.v1")

private class InMemoryPersistenceRepository : PersistenceRepository {

    val prefsFlow = MutableStateFlow(emptyPreferences())

    override fun getPreferences(): Flow<Preferences> = prefsFlow

    override suspend fun updatePreferences(update: (MutablePreferences) -> Unit) {
        val mutable = prefsFlow.value.toMutablePreferences()
        update(mutable)
        prefsFlow.value = mutable
    }
}

