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

import com.google.devtools.ksp.gradle.KspAATask
import com.google.devtools.ksp.gradle.KspTask
import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.neotech.gradle.capitalizeFirstCharacter
import java.io.ByteArrayOutputStream
import java.util.Properties

// DMG distribution does not support "-beta", MSI requires at least MAJOR.MINOR.BUILD
val abysnerVersionBase = "1.0.8"
val abysnerVersion = "$abysnerVersionBase-beta"
// iOS supports a String here, but Android only an integer
val abysnerBuildNumber = 10

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

kotlin {

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // Do not really support desktop, but this is required to get previews working.
    jvm("desktop") {
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
            binaryOption("bundleVersion", abysnerBuildNumber.toString())
        }
    }
    
    sourceSets {

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.startup.runtime)
            implementation(libs.androidx.ui.tooling)
        }

        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)

            // For easy access to user data folder
            implementation(libs.appdirs)
        }

        commonMain.configure {
            kotlin.srcDir(project.layout.buildDirectory.file("generated/kotlin/version/"))
        }

        commonMain.dependencies {
            implementation(project(":domain"))
            implementation(project(":data"))
            implementation(libs.kotlinInject.runtimeKmp)
            implementation(libs.navigation.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel)
            implementation(libs.jetbrains.lifecycle.runtime)
            implementation(libs.jetbrains.kotlinx.datetime)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(compose.material3)
            implementation(libs.jetbrains.compose.material.icons)
            implementation(libs.koalaplot.core)

            // Data storage
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)

            // File IO
            implementation(libs.okio)

            // Markdown parsing & rendering
            implementation(libs.markdown.parser)
            implementation(libs.markdown.render)
        }
    }
}

android {
    namespace = "nl.neotech.app.abysner"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    // Line below seems not to be required and causes a duplicate root warning.
    // sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "nl.neotech.app.abysner"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = abysnerBuildNumber
        versionName = abysnerVersion
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    try {
        keystoreProperties.load(keystorePropertiesFile.inputStream())
    } catch (_: Exception) {
        logger.warn("w: Unable to load keystore.properties file!")
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
            keystoreProperties.getProperty("storeFile")?.let {
                storeFile = rootProject.file(it)
            }
            storePassword = keystoreProperties.getProperty("storePassword")
            storeType = keystoreProperties.getProperty("storeType") ?: "JKS"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        create("development") {
            initWith(getByName("debug"))
            matchingFallbacks += listOf("release")
            isMinifyEnabled = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    dependencies {
        debugImplementation(compose.uiTooling)
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
    buildNumber.set(abysnerBuildNumber)
    versionName.set(abysnerVersion)
}

tasks.withType(KspTask::class.java).configureEach {
    dependsOn(versionInfoProvider)
}
tasks.withType(KspAATask::class.java).configureEach {
    dependsOn(versionInfoProvider)
}
