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

package org.neotech.app.abysner.domain.persistence

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow

/**
 * Repository for persisting key-value pairs.
 */
interface PersistenceRepository {

    suspend fun updatePreferences(update: (MutablePreferences) -> Unit)

    fun getPreferences(): Flow<Preferences>
}

fun MutablePreferences.set(key: String, value: String) {
    set(stringPreferencesKey(key), value)
}

fun MutablePreferences.set(key: String, value: Int) {
    set(intPreferencesKey(key), value)
}

fun MutablePreferences.set(key: String, value: Double) {
    set(doublePreferencesKey(key), value)
}

fun MutablePreferences.set(key: String, value: Boolean) {
    set(booleanPreferencesKey(key), value)
}

fun Preferences.get(key: Preferences.Key<String>, default: String): String {
    return this[key] ?: default
}

fun Preferences.get(key: Preferences.Key<Int>, default: Int): Int {
    return this[key] ?: default
}

fun Preferences.get(key: Preferences.Key<Double>, default: Double): Double {
    return this[key] ?: default
}

fun Preferences.get(key: Preferences.Key<Boolean>, default: Boolean): Boolean {
    return this[key] ?: default
}

inline operator fun <reified T> MutablePreferences.set(
    key: Preferences.Key<String>,
    default: T
) where T : Enum<T>, T : EnumPreference {
    this[key] = default.preferenceValue
}

inline fun <reified T> Preferences.get(
    key: Preferences.Key<String>,
    default: T
): T where T : Enum<T>, T : EnumPreference {
    val value = this[key] ?: default.preferenceValue
    return enumValues<T>().find { it.preferenceValue == value } ?: default
}

interface EnumPreference {
    val preferenceValue: String
}