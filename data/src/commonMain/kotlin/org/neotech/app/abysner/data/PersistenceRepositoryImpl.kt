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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject
import okio.Path
import org.neotech.app.abysner.domain.persistence.PersistenceRepository

/**
 * Gets the singleton DataStore instance, creating it if necessary.
 */
private fun createDataStore(producePath: () -> Path): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath() })

internal const val dataStoreFileName = "abysner.preferences_pb"

@Inject
class PersistenceRepositoryImpl(
    private val platformFileDataSource: PlatformFileDataSource
): PersistenceRepository {

    private val dataStore = createDataStore { platformFileDataSource.getPrivateFileStoragePath().resolve("datastore/$dataStoreFileName") }

    override fun getPreferences(): Flow<Preferences> = dataStore.data

    override suspend fun updatePreferences(update: (MutablePreferences) -> Unit) {
        dataStore.edit {
            update(it)
        }
    }
}

inline fun <reified T: SerializableResource> MutablePreferences.setJson(key: Preferences.Key<String>, value: T) {
    set(key, Json.encodeToString(value))
}

inline fun <reified T: SerializableResource> Preferences.getJson(key: Preferences.Key<String>): T? {
    val rawValue = get(key)
    return if(rawValue != null) {
        Json.decodeFromString<T>(rawValue)
    } else {
        null
    }
}

internal const val PREFERENCE_DEPRECATION_WARNING = "Will be removed in future version when most users have migrated to the JSON format stored configuration."
