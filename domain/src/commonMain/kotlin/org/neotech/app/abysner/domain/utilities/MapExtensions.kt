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

package org.neotech.app.abysner.domain.utilities

/**
 * Insert an element into the map, if an element is already available for the given [key] then the
 * element will not be overwritten but the conflict will be solved by calling the [onConflict] block.
 */
inline fun <T, V> MutableMap<T, V>.updateOrInsert(key: T, value: V, onConflict: (current: V, new: V) -> V) {
    val currentValue = this[key]
    this[key] = if (currentValue == null) {
        value
    } else {
        onConflict(currentValue, value)
    }
}

/**
 * Merge this map into the given [destination] map, if keys exist in both maps, then conflicts will be solved by
 * calling the [onConflict] block.
 */
fun <K, V> Map<K, V>.mergeInto(destination: MutableMap<K, V>, onConflict: (current: V, new: V) -> V) {
    forEach {
        destination.updateOrInsert(it.key, it.value, onConflict)
    }
}

/**
 * Merge the given map into this map, if keys exist in both maps, then conflicts will be solved by
 * calling the [onConflict] block.
 */
fun <K, V> Map<K, V>.merge(other: Map<K, V>, onConflict: (current: V, new: V) -> V): Map<K, V> {
    return toMutableMap().apply {
        other.mergeInto(this, onConflict)
    }
}
