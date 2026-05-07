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

package org.neotech.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Builds an iOS .xcarchive using xcodebuild. This task must run on macOS with Xcode installed.
 * Use [IosExportTask] to export the archive as an IPA for App Store upload.
 */
abstract class IosArchiveTask : DefaultTask() {

    @get:InputDirectory
    abstract val xcodeProjectDirectory: DirectoryProperty

    @get:Input
    abstract val scheme: Property<String>

    @get:Input
    @get:Optional
    abstract val configuration: Property<String>

    @get:InputDirectory
    @get:Optional
    abstract val xcodeAppPath: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        group = "release"
        description = "Archives the iOS app into an .xcarchive bundle."
    }

    @TaskAction
    fun execute() {
        val projectDirectory = xcodeProjectDirectory.get().asFile
        val schemeName = scheme.get()
        val config = configuration.getOrElse("Release")
        val outputDir = outputDirectory.get().asFile
        val archivePath = outputDir.resolve("$schemeName.xcarchive")

        outputDir.mkdirs()

        logger.lifecycle("Archiving iOS app (scheme=$schemeName, configuration=$config)...")

        val archiveResult = runCommand(
            listOf(
                "xcodebuild",
                "-project", projectDirectory.resolve("iosApp.xcodeproj").absolutePath,
                "-scheme", schemeName,
                "-configuration", config,
                "-archivePath", archivePath.absolutePath,
                "-destination", "generic/platform=iOS",
                "archive",
            ),
            workingDirectory = projectDirectory,
        )

        if (archiveResult != 0) {
            throw GradleException("xcodebuild archive failed with exit code $archiveResult")
        }

        logger.lifecycle("Archive created at: ${archivePath.absolutePath}")
    }

    private fun runCommand(command: List<String>, workingDirectory: java.io.File): Int {
        logger.info("Running: ${command.joinToString(" ")}")
        val process = ProcessBuilder(command)
            .directory(workingDirectory)
            .redirectErrorStream(true)
            .also { builder ->
                if (xcodeAppPath.isPresent) {
                    val developerDir = xcodeAppPath.get().asFile.resolve("Contents/Developer")
                    logger.info("Using Xcode at: ${xcodeAppPath.get().asFile.absolutePath}")
                    builder.environment()["DEVELOPER_DIR"] = developerDir.absolutePath
                }
            }
            .start()

        process.inputStream.bufferedReader().forEachLine { line ->
            logger.info("  $line")
        }

        return process.waitFor()
    }
}

