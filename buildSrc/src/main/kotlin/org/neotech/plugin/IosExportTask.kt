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
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Exports an existing .xcarchive as an IPA and uploads it to App Store Connect using
 * xcodebuild. Generates the ExportOptions.plist at build time from the provided [teamId].
 * Typically depends on [IosArchiveTask] to produce the archive first.
 */
abstract class IosExportTask : DefaultTask() {

    @get:InputDirectory
    abstract val archivePath: DirectoryProperty

    @get:Input
    abstract val teamId: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        group = "release"
        description = "Exports an .xcarchive as an IPA and uploads it to App Store Connect."
    }

    @TaskAction
    fun execute() {
        val archive = archivePath.get().asFile
        val outputDir = outputDirectory.get().asFile
        val exportOptionsFile = outputDir.resolve("ExportOptions.plist")

        outputDir.mkdirs()

        exportOptionsFile.writeText(
            """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            |<plist version="1.0">
            |<dict>
            |    <key>method</key>
            |    <string>app-store-connect</string>
            |    <key>teamID</key>
            |    <string>${teamId.get()}</string>
            |    <key>uploadSymbols</key>
            |    <true/>
            |    <key>destination</key>
            |    <string>upload</string>
            |</dict>
            |</plist>
            """.trimMargin()
        )

        logger.lifecycle("Exporting and uploading IPA from ${archive.name} to App Store Connect...")

        val process = ProcessBuilder(
            listOf(
                "xcodebuild",
                "-exportArchive",
                "-archivePath", archive.absolutePath,
                "-exportOptionsPlist", exportOptionsFile.absolutePath,
                "-exportPath", outputDir.absolutePath,
            ),
        )
            .redirectErrorStream(true)
            .start()

        process.inputStream.bufferedReader().forEachLine { line ->
            logger.info("  $line")
        }

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw GradleException("xcodebuild -exportArchive failed with exit code $exitCode")
        }

        logger.lifecycle("IPA exported and uploaded to App Store Connect.")
    }
}
