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

import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.SYSTEM
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanInputModel
import org.neotech.app.abysner.domain.diveplanning.model.MultiDivePlanInputModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlanningFileDataSourceTest {

    private val fileSystem = FileSystem.SYSTEM
    private val testDirectory = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "planning_test"
    private val planningDirectory = testDirectory / "planning"
    private lateinit var dataSource: PlanningFileDataSource

    @BeforeTest
    fun setUp() {
        fileSystem.deleteRecursively(testDirectory)
        fileSystem.createDirectories(testDirectory)
        dataSource = PlanningFileDataSource(baseDirectory = testDirectory, fileSystem = fileSystem)
    }

    @AfterTest
    fun tearDown() {
        fileSystem.deleteRecursively(testDirectory)
    }

    @Test
    fun loadPlan_returnsNullWhenFileDoesNotExist() = runBlocking {
        assertNull(dataSource.loadPlan())
    }

    @Test
    fun loadPlan_returnsNullWhenFileContainsInvalidJson() = runBlocking {
        fileSystem.createDirectories(planningDirectory)
        fileSystem.write(planningDirectory / "plan.json") {
            writeUtf8("{ invalid json content }")
        }

        assertNull(dataSource.loadPlan())
    }

    @Test
    fun savePlan_persistsPlan() = runBlocking {
        val initialPlan = createPlan(deeper = false)
        val updatedPlan = createPlan(deeper = true)

        dataSource.savePlan(initialPlan)
        dataSource.savePlan(updatedPlan)

        val loaded = dataSource.loadPlan()
        assertEquals(updatedPlan, loaded)
    }

    @Test
    fun loadConfiguration_returnsNullWhenFileDoesNotExist() = runBlocking {
        assertNull(dataSource.loadConfiguration())
    }

    @Test
    fun loadConfiguration_returnsNullWhenFileContainsInvalidJson() = runBlocking {
        fileSystem.createDirectories(planningDirectory)
        fileSystem.write(planningDirectory / "configuration.json") {
            writeUtf8("{ invalid json content }")
        }

        assertNull(dataSource.loadConfiguration())
    }

    @Test
    fun saveConfiguration_persistsConfiguration() = runBlocking {
        val configuration = createConfiguration()

        dataSource.saveConfiguration(configuration)
        val loaded = dataSource.loadConfiguration()

        assertEquals(configuration, loaded)
    }

    private fun createPlan(deeper: Boolean = false) = MultiDivePlanInputModel.Default.copy(
        dives = listOf(DivePlanInputModel.Default.copy(deeper = deeper))
    ).toResource()

    private fun createConfiguration() = Configuration().toResource()
}
