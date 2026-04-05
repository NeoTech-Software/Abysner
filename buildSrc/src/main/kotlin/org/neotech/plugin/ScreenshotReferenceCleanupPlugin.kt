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

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

/**
 * Simple plugin that clears all screenshot reference images before any `update*ScreenshotTest` task
 * runs, this effectively cleans any stale reference images whenever a update runs.
 */
class ScreenshotReferenceCleanupPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.plugins.withId("com.android.compose.screenshot") {
            target.tasks.configureEach {
                if (!isScreenshotUpdateTask(name)) {
                    return@configureEach
                }
                val variant = screenshotVariant(name)
                if (variant.isEmpty()) {
                    return@configureEach
                }

                // Follows the Android screenshot plugin convention:
                // task: update{Variant}ScreenshotTest
                // path: src/screenshotTest{Variant}/reference
                val referenceDir = target.file("src/screenshotTest$variant/reference")

                // Mutable state shared between doFirst and doLast. Not compatible with
                // Gradle's configuration cache, need to revisit this if that gets enabled.
                var existingImages = emptySet<String>()

                doFirst {
                    if (referenceDir.exists()) {
                        existingImages = referencePngPaths(referenceDir)
                        existingImages.forEach { referenceDir.resolve(it).delete() }
                    }
                }

                doLast {
                    val regenerated = referencePngPaths(referenceDir)
                    val removed = existingImages - regenerated
                    val added = regenerated - existingImages

                    if (removed.isNotEmpty()) {
                        println("Removed ${removed.size} stale screenshot reference image(s):")
                        removed.sorted().forEach { println("  - $it") }
                    }
                    if (added.isNotEmpty()) {
                        println("Added ${added.size} new screenshot reference image(s):")
                        added.sorted().forEach { println("  + $it") }
                    }
                }
            }
        }
    }

    private fun isScreenshotUpdateTask(taskName: String): Boolean =
        taskName.startsWith("update") && taskName.endsWith("ScreenshotTest")

    private fun screenshotVariant(taskName: String): String =
        taskName.removePrefix("update").removeSuffix("ScreenshotTest")

    private fun referencePngPaths(referenceDir: File): Set<String> =
        if (!referenceDir.exists()) {
            emptySet()
        } else {
            referenceDir.walk()
                .filter { it.isFile && it.extension == "png" }
                .map { it.relativeTo(referenceDir).path }
                .toSet()
        }
}
