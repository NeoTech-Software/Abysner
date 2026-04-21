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

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.testing.Test
import org.gradle.process.CommandLineArgumentProvider

/**
 * Attaches the Kover JVM agent to Android screenshot test tasks and registers the resulting
 * binary coverage reports with Kover's artifact generation tasks. This allows screenshot test
 * coverage to appear in Kover reports alongside regular unit test coverage.
 *
 * The screenshot plugin renders composables inside layoutlib, which uses an isolated classloader
 * that only sees layoutlib's own JARs. Kover's agent instruments classes by rewriting bytecode,
 * and during that process it needs to load .class files as resources to resolve type hierarchies.
 * The isolated classloader can't find the app's .class files, so Kover silently skips those
 * classes, resulting in zero coverage. To fix this, the plugin injects a shadowed copy of the
 * screenshot engine's `com.android.tools.screenshot.renderer.Renderer` class that replaces the
 * classloader with one that falls back to the SystemClassLoader for resource lookups. This shadowed
 * Renderer is compiled as part of buildSrc against the screenshot engine's dependencies, and its
 * .class files are extracted and prepended to the test classpath at runtime.
 *
 * Requires the Kover plugin and the Compose Screenshot plugin to be applied to the same project.
 */
class ScreenshotTestCoveragePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.afterEvaluate {
            configureScreenshotCoverage(target)
        }
    }

    private fun configureScreenshotCoverage(project: Project) {
        val binReportsDirectory = project.layout.buildDirectory.dir("kover/bin-reports")
        val agentConfiguration = project.configurations.findByName("koverJvmAgent")
        if (agentConfiguration == null) {
            project.logger.warn("Warning: screenshot-test-coverage: koverJvmAgent configuration not found. Is the Kover plugin applied?")
            return
        }

        val rendererClassesDir = extractRendererClasses(project)

        // Attach the Kover agent to all screenshot validation tasks.
        project.tasks.withType(Test::class.java)
            .matching { it.name.startsWith("validate") && it.name.endsWith("ScreenshotTest") }
            .configureEach {
                val binReport = binReportsDirectory.map { it.file("${name}.ic") }

                doFirst {
                    verifyScreenshotPluginVersion(project)
                    binReport.get().asFile.delete()
                }

                // Prepend the pre-compiled Renderer classes directory to the classpath so our
                // shadowed Renderer takes precedence over the one in the engine JAR.
                doFirst {
                    classpath = project.files(rendererClassesDir, classpath)
                }

                val argsFile = temporaryDir.resolve("kover-agent.args")
                doFirst {
                    argsFile.parentFile.mkdirs()
                    argsFile.printWriter().use { writer ->
                        writer.append("report.file=").appendLine(binReport.get().asFile.canonicalPath)
                        writer.append("exclude=").appendLine("android.*")
                        writer.append("exclude=").appendLine("com.android.*")
                        writer.append("exclude=").appendLine("jdk.internal.*")
                    }
                }

                jvmArgumentProviders += CommandLineArgumentProvider {
                    val agentJar = agentConfiguration.singleFile
                    if (agentJar.exists()) {
                        mutableListOf("-javaagent:${agentJar.canonicalPath}=file:${argsFile.canonicalPath}")
                    } else {
                        mutableListOf()
                    }
                }
            }

        // Make Kover artifact generation tasks depend on screenshot tests and include their binary reports.
        project.tasks.matching { it.name.startsWith("koverGenerateArtifact") }.configureEach {
            val variantName = name.removePrefix("koverGenerateArtifact")
            // Not every variant has a corresponding screenshot test task so we skip variants that we don't find a task for.
            val screenshotTask = project.tasks.findByName("validate${variantName}ScreenshotTest")
                ?: return@configureEach

            dependsOn(screenshotTask)

            // Kover's ArtifactGenerationTask is internal, so we use reflection to access its report
            // files property. There are two internal task types (one for aggregated reports, one
            // for module-level reports?) with different accessor names.
            val reportFiles = (this::class.java.methods.firstOrNull { it.name == "getReportFiles" }
                ?: this::class.java.methods.first { it.name == "getReports" })
                .invoke(this) as ConfigurableFileCollection
            reportFiles.from(binReportsDirectory.map { it.file("validate${variantName}ScreenshotTest.ic") })
        }
    }

    /**
     * Extracts the pre-compiled Renderer .class files from the plugin's classloader into a build
     * directory.
     */
    private fun extractRendererClasses(project: Project): java.io.File {
        val classesDir = project.layout.buildDirectory
            .dir("generated/screenshotTestCoverage/classes").get().asFile

        val classFiles = listOf(
            "com/android/tools/screenshot/renderer/Renderer.class",
            "com/android/tools/screenshot/renderer/Renderer\$copyObject\$objectIn\$1.class",
            "com/android/tools/screenshot/renderer/RendererKt.class",
            "com/android/tools/screenshot/renderer/RendererKt\$createResourceEnhancedClassLoader\$1.class",
        )

        for (classFile in classFiles) {
            val outputFile = classesDir.resolve(classFile)
            val bytes = ScreenshotTestCoveragePlugin::class.java.classLoader
                .getResourceAsStream(classFile)
                ?.readBytes()
                ?: throw GradleException("Could not find pre-compiled class $classFile in buildSrc, this is a plugin bug and should normally not happen.")
            outputFile.parentFile.mkdirs()
            outputFile.writeBytes(bytes)
        }

        return classesDir
    }

    private fun verifyScreenshotPluginVersion(project: Project) {
        val actualScreenshotVersion = project.configurations
            .findByName("_internal-screenshot-validation-junit-engine")
            ?.resolvedConfiguration
            ?.firstLevelModuleDependencies
            ?.firstOrNull()
            ?.moduleVersion

        if (actualScreenshotVersion == null) {
            throw GradleException("screenshot-test-coverage requires the Compose Screenshot plugin to be applied to the same project.")
        } else if (actualScreenshotVersion != expectedScreenshotPluginVersion) {
            throw GradleException("screenshot-test-coverage plugin requires Compose Screenshot plugin $expectedScreenshotPluginVersion, but found $actualScreenshotVersion.")
        }
    }
}

/**
 * The screenshot plugin version that the pre-compiled Renderer shadow was built against. If the
 * project uses a different version, the plugin fails with a clear error rather than potentially
 * producing mysterious runtime failures from incompatible class files.
 */
private const val expectedScreenshotPluginVersion = "0.0.1-alpha14"
