/*
 * Abysner - Dive planner
 * Copyright (C) 2024-2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

import com.google.devtools.ksp.gradle.KspAATask
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.neotech.gradle.capitalizeFirstCharacter
import java.io.ByteArrayOutputStream

// DMG distribution does not support "-beta", MSI requires at least MAJOR.MINOR.BUILD
val abysnerVersionBase: String by project.properties
val abysnerVersion: String by project.properties
// iOS supports a String here, but Android only an integer
val abysnerBuildNumber: String by project.properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kover)
}

kover {
    currentProject {
        // composeApp module has no domain code, so we don't include any variants for this coverage report
        createVariant("domain") {}
        createVariant("presentation") {
            add("jvm")
        }
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(project.layout.projectDirectory.file("compose-stability.conf"))
}

kotlin {

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "nl.neotech.app.abysner.composeapp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        androidResources {
            enable = true
        }
    }

    // Do not really support desktop, but this is required to get previews working.
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            binaryOption("bundleId", "nl.neotech.app.abysner")
            binaryOption("bundleShortVersionString", abysnerVersionBase)
            binaryOption("bundleVersion", abysnerBuildNumber)
        }
    }
    
    sourceSets {

        androidMain.dependencies {
            implementation(libs.androidx.startup.runtime)
        }

        val jvmMain by getting
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)

            // For easy access to user data folder
            implementation(libs.appdirs)
        }

        commonMain.configure {
            kotlin.srcDir(project.layout.buildDirectory.file("generated/kotlin/version/"))
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        commonMain.dependencies {
            implementation(project(":domain"))
            implementation(project(":data"))
            implementation(libs.kotlinInject.runtimeKmp)
            implementation(libs.navigation.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel)
            implementation(libs.jetbrains.lifecycle.runtime)
            implementation(libs.jetbrains.kotlinx.datetime)

            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.material)
            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.compose.components.resources)
            implementation(libs.jetbrains.compose.ui.tooling.preview)

            implementation(libs.jetbrains.compose.material3)
            implementation(libs.jetbrains.compose.material.icons)
            implementation(libs.koalaplot.core)

            // Data storage
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)

            // Collections
            implementation(libs.kotlinx.collections.immutable)

            // File IO
            implementation(libs.okio)

            // Markdown parsing & rendering
            implementation(libs.markdown.parser)
            implementation(libs.markdown.render)
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "nl.neotech.app.abysner"
            packageVersion = abysnerVersionBase
        }
    }
}

dependencies {

    // This is the same as repeating:
    //     add(target, libs.kotlinInject.compilerKsp)
    // where `target` is "kspDesktop", "kspAndroid", "kspIosX64" "kspIosArm64" or "kspIosSimulatorArm64"
    val kotlinTargets: Sequence<KotlinTarget> = kotlin.targets.asSequence()
    kotlinTargets.filter {
        // Don't add KSP for common target, only final platforms
        it.platformType != KotlinPlatformType.common
    }.forEach {
        add("ksp${it.targetName.capitalizeFirstCharacter()}", libs.kotlinInject.compilerKsp)
    }

    androidRuntimeClasspath(libs.jetbrains.compose.ui.tooling)
}


abstract class GenerateVersionInfoTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
    abstract val buildNumber: Property<Int>

    @get:Input
    abstract val versionName: Property<String>

    init {
        // Set the default output file path
        outputFile.convention(
            project.layout.buildDirectory.file("generated/kotlin/version/VersionInfo.kt")
        )
    }

    @TaskAction
    fun generate() {
        // Get commit hash
        val commitBuffer = ByteArrayOutputStream()
        execOperations.exec {
            executable = "git"
            args = listOf("rev-parse", "--short", "HEAD")
            standardOutput = commitBuffer
        }
        val commit = commitBuffer.toString(Charsets.UTF_8).trim()

        // Check dirty state
        val dirty = execOperations.exec {
            isIgnoreExitValue = true
            executable = "git"
            args = listOf("diff-index", "--quiet", "HEAD", "--")
        }.exitValue != 0

        // Write file
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package org.neotech.app.abysner.version

            object VersionInfo {
                const val DIRTY: Boolean = $dirty
                const val COMMIT_HASH: String = "$commit"
                const val BUILD: Int = ${buildNumber.get()}
                const val VERSION_NAME: String = "${versionName.get()}"
            }
            """.trimIndent()
        )
    }
}

val versionInfoProvider = tasks.register<GenerateVersionInfoTask>("generateVersionInfo") {
    buildNumber.set(abysnerBuildNumber.toInt())
    versionName.set(abysnerVersion)
}

// Sync version info to iOS xcconfig at configuration time, so it's available before Xcode resolves build settings.
rootProject.file("iosApp/Configuration/Version.xcconfig").writeText(
    """
    MARKETING_VERSION=$abysnerVersion
    CURRENT_PROJECT_VERSION=$abysnerBuildNumber
    """.trimIndent() + "\n"
)

tasks.withType(KspAATask::class.java).configureEach {
    dependsOn(versionInfoProvider)
}
