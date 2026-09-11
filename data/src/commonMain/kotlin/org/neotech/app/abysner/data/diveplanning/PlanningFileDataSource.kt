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

import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import org.neotech.app.abysner.data.diveplanning.resources.ConfigurationResourceV1
import org.neotech.app.abysner.data.diveplanning.resources.MultiDivePlanInputResourceV1

class PlanningFileDataSource(
    baseDirectory: Path,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : PlanningDataSource {

    private val rootDirectory = baseDirectory.resolve("planning")
    private val planFile = rootDirectory.resolve("plan.json")
    private val configurationFile = rootDirectory.resolve("configuration.json")

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    override suspend fun loadConfiguration(): ConfigurationResourceV1? {
        return readJsonFile<ConfigurationResourceV1>(configurationFile)
    }

    override suspend fun saveConfiguration(configuration: ConfigurationResourceV1) {
        writeJsonFile(configurationFile, configuration)
    }

    override suspend fun loadPlan(): MultiDivePlanInputResourceV1? {
        return readJsonFile<MultiDivePlanInputResourceV1>(planFile)
    }

    override suspend fun savePlan(plan: MultiDivePlanInputResourceV1) {
        writeJsonFile(planFile, plan)
    }

    private inline fun <reified T> readJsonFile(path: Path): T? {
        return try {
            val content = fileSystem.read(path) { readUtf8() }
            json.decodeFromString<T>(content)
        } catch (exception: Exception) {
            exception.printStackTrace()
            null
        }
    }

    private inline fun <reified T> writeJsonFile(path: Path, value: T) {
        val content = json.encodeToString(value)
        updateFile(path, content)
    }

    private fun updateFile(path: Path, content: String) {
        val parent = path.parent ?: error("Path '$path' has no parent directory!")
        fileSystem.createDirectories(parent)
        val temporaryPath = parent.resolve("${path.name}.tmp")
        fileSystem.write(temporaryPath) {
            writeUtf8(content)
        }
        fileSystem.atomicMove(temporaryPath, path)
    }
}
