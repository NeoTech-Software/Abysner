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

package org.neotech.app.abysner.domain.utilities

import kotlin.test.Test
import kotlin.test.assertEquals

class MapExtensionsTest {

    @Test
    fun updateOrInsert_insertsNewKey() {
        val map = mutableMapOf("a" to 1)
        map.updateOrInsert("b", 2) { current, new -> current + new }
        assertEquals(2, map["b"])
    }

    @Test
    fun updateOrInsert_callsOnConflictForExistingKey() {
        val map = mutableMapOf("a" to 10)
        map.updateOrInsert("a", 5) { current, new -> current + new }
        assertEquals(15, map["a"])
    }

    @Test
    fun mergeInto_addsAllEntriesWhenNoKeysOverlap() {
        val source = mapOf("x" to 1, "y" to 2)
        val destination = mutableMapOf("a" to 10)
        source.mergeInto(destination) { current, _ -> current }
        assertEquals(mapOf("a" to 10, "x" to 1, "y" to 2), destination)
    }

    @Test
    fun mergeInto_resolvesConflictsWithCallback() {
        val source = mapOf("a" to 5)
        val destination = mutableMapOf("a" to 10)
        source.mergeInto(destination) { current, new -> current + new }
        assertEquals(15, destination["a"])
    }

    @Test
    fun merge_returnsNewMapWithoutMutatingOriginal() {
        val original = mapOf("a" to 1, "b" to 2)
        val other = mapOf("b" to 3, "c" to 4)
        val merged = original.merge(other) { current, new -> current + new }
        assertEquals(mapOf("a" to 1, "b" to 5, "c" to 4), merged)
        // Original is not mutated
        assertEquals(mapOf("a" to 1, "b" to 2), original)
    }
}
