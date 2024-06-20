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

package org.neotech.app.abysner.domain.decompression.algorithm

interface Snapshotable {

    /**
     * Reset the model (e.g. in case of Buhlmann this effectively resets all the tissue compartments).
     */
    fun reset()

    /**
     * Reset the model to a given snapshot
     */
    fun reset(snapshot: Snapshot)

    /**
     * Creates a new snapshot that contains the models state, can be used to restore a model to a
     * certain state.
     */
    fun snapshot(): Snapshot

    /**
     * Execute the given block, on the current model state, then reset the state to the state before
     * executing the block.
     */
    fun <T> resetAfter(block: SnapshotScope.() -> T): T {
        val snapshot = snapshot()
        val scope = SnapshotScopeImpl()
        val result = scope.block()
        if(!scope.keepChanges) {
            reset(snapshot)
        }
        return result
    }
}

interface Snapshot

interface SnapshotScope {
    fun keepChanges()
    fun rejectChanges()
}

data class SnapshotScopeImpl(var keepChanges: Boolean = false): SnapshotScope {
    override fun keepChanges() {
        keepChanges = true
    }

    override fun rejectChanges() {
        keepChanges = false
    }
}
