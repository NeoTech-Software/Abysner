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
import org.neotech.plugin.agent.RenderClassLoaderPatchAgent
import java.io.File
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

/**
 * Attaches the Kover JVM agent to Android screenshot test tasks, plus a second agent
 * ([RenderClassLoaderPatchAgent]) that extends the render sandbox's classloader allowlist so Kover
 * can run inside it. Registers the resulting binary coverage reports with Kover's artifact
 * generation tasks, so screenshot test coverage appears in Kover reports alongside regular unit
 * test coverage.
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

        val patchAgentJar = buildPatchAgentJar(project)

        // Attach the Kover agent to all screenshot validation tasks.
        project.tasks.withType(Test::class.java)
            .matching { it.name.startsWith("validate") && it.name.endsWith("ScreenshotTest") }
            .configureEach {
                val binReport = binReportsDirectory.map { it.file("${name}.ic") }

                doFirst {
                    verifyScreenshotPluginVersion(project)
                    binReport.get().asFile.delete()
                }

                val argsFile = temporaryDir.resolve("kover-agent.args")
                doFirst {
                    argsFile.parentFile.mkdirs()
                    argsFile.printWriter().use { writer ->
                        writer.append("report.file=").appendLine(binReport.get().asFile.canonicalPath)
                        writer.append("exclude=").appendLine("android.*")
                        writer.append("exclude=").appendLine("com.android.*")
                        writer.append("exclude=").appendLine("jdk.internal.*")
                        // Layoutlib renames its bundled Kotlin runtime into this package as it
                        // loads it. Instrumenting those copies breaks the rename.
                        writer.append("exclude=").appendLine($$"_layoutlib_._internal_.*")
                    }
                }

                jvmArgumentProviders += CommandLineArgumentProvider {
                    val agentJar = agentConfiguration.singleFile
                    if (agentJar.exists()) {
                        mutableListOf(
                            "-javaagent:${agentJar.canonicalPath}=file:${argsFile.canonicalPath}",
                            "-javaagent:${patchAgentJar.canonicalPath}",
                            // The patch agent swaps the backing array of a java.util list.
                            "--add-opens=java.base/java.util=ALL-UNNAMED",
                        )
                    } else {
                        mutableListOf()
                    }
                }
            }

        // Make Kover artifact generation tasks depend on screenshot tests and include their binary reports.
        project.tasks.matching { it.name.startsWith("koverGenerateArtifact") }.configureEach {
            val variantName = name.removePrefix("koverGenerateArtifact")
            // Skip variants without a matching screenshot test task.
            val screenshotTask = project.tasks.findByName("validate${variantName}ScreenshotTest")
                ?: return@configureEach

            dependsOn(screenshotTask)

            // Kover's ArtifactGenerationTask is internal, so reach its report files via reflection.
            // Two internal task types exist, with different accessor names.
            val reportFiles = (this::class.java.methods.firstOrNull { it.name == "getReportFiles" }
                ?: this::class.java.methods.first { it.name == "getReports" })
                .invoke(this) as ConfigurableFileCollection
            reportFiles.from(binReportsDirectory.map { it.file("validate${variantName}ScreenshotTest.ic") })
        }
    }

    /**
     * Packages the pre-compiled [RenderClassLoaderPatchAgent] from this plugin's own classloader
     * into a JAR with a `Premain-Class` manifest entry, so it can be attached with `-javaagent`.
     */
    private fun buildPatchAgentJar(project: Project): File {
        val agentClassName = RenderClassLoaderPatchAgent::class.java.name
        val agentClassResource = "${agentClassName.replace('.', '/')}.class"

        val bytes = ScreenshotTestCoveragePlugin::class.java.classLoader
            .getResourceAsStream(agentClassResource)
            ?.readBytes()
            ?: throw GradleException("Could not find pre-compiled class $agentClassResource in buildSrc, this is a plugin bug and should normally not happen.")

        val manifest = Manifest().apply {
            mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
            mainAttributes[Attributes.Name("Premain-Class")] = agentClassName
        }

        val jarFile = project.layout.buildDirectory
            .file("generated/screenshotTestCoverage/render-classloader-patch-agent.jar").get().asFile
        jarFile.parentFile.mkdirs()
        JarOutputStream(jarFile.outputStream().buffered(), manifest).use { jar ->
            jar.putNextEntry(JarEntry(agentClassResource))
            jar.write(bytes)
            jar.closeEntry()
        }

        return jarFile
    }

    private fun verifyScreenshotPluginVersion(project: Project) {
        val actualScreenshotVersion = project.configurations
            .findByName("_internal-screenshot-validation-junit-engine")
            ?.resolvedConfiguration
            ?.firstLevelModuleDependencies
            ?.firstOrNull { it.moduleName == "screenshot-validation-junit-engine" }
            ?.moduleVersion

        if (actualScreenshotVersion == null) {
            throw GradleException("screenshot-test-coverage requires the Compose Screenshot plugin to be applied to the same project.")
        } else if (actualScreenshotVersion != expectedScreenshotPluginVersion) {
            project.logger.warn(
                "Warning: screenshot-test-coverage was verified against Compose Screenshot plugin $expectedScreenshotPluginVersion, but found $actualScreenshotVersion."
            )
        }
    }
}

/**
 * The screenshot plugin version this plugin's classloader patch was verified against.
 */
private const val expectedScreenshotPluginVersion = "0.0.1-alpha16"
