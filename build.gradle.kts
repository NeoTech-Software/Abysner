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

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.screenshot) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kover)
}

dependencies {
    kover(project(":domain"))
    kover(project(":data"))
    kover(project(":composeApp"))
    kover(project(":androidApp"))
}

kover {
    currentProject {
        createVariant("domain") {
            add("jvm", optional = true)
        }
        createVariant("presentation") {
            add("jvm", optional = true)
            add("debug", optional = true)
        }
    }

    reports {
        filters {
            excludes {
                // Compose compiler-generated singleton holders — present in every file with
                // @Preview or default-parameter composables.
                classes("org.neotech.app.abysner.presentation.**ComposableSingletons*")
                classes("androidx.compose.material3.ComposableSingletons*")

                // kotlin-inject KSP-generated component implementations
                // (InjectAppComponent, InjectPlatformComponentImpl, ...)
                classes("org.neotech.app.abysner.di.Inject*")

                // kotlinx.serialization compiler-generated $serializer objects
                // Only the data module uses @Serializable (resources packages)
                classes("org.neotech.app.abysner.data.**\$serializer")
            }
        }
    }
}

val archiveIosApp = tasks.register<org.neotech.plugin.IosArchiveTask>("archiveIosApp") {
    xcodeProjectDirectory = layout.projectDirectory.dir("iosApp")
    scheme = "iosApp"
    configuration = "Release"
    outputDirectory = layout.projectDirectory.dir("iosApp/build")
}

tasks.register<org.neotech.plugin.IosExportTask>("exportIosApp") {
    dependsOn(archiveIosApp)
    archivePath = layout.projectDirectory.dir("iosApp/build/iosApp.xcarchive")
    val localProperties = java.util.Properties()
    try {
        localProperties.load(rootProject.file("local.properties").inputStream())
    } catch (_: Exception) {
        logger.warn("w: Unable to load local.properties file!")
    }
    teamId = localProperties.getProperty("apple.teamId") ?: ""
    outputDirectory = layout.projectDirectory.dir("iosApp/build/export")
}

tasks.register<org.neotech.plugin.FrameScreenshotsTask>("frameAndroidScreenshots") {
    group = "store"
    description = "Frames Nothing Phone screenshots into device bezels."

    val nothingPhone = layout.projectDirectory.dir("store-art/Nothing Phone 1")

    bezelFile = nothingPhone.file("bezel-58-59-1524-3386.png")
    maskFile = nothingPhone.file("mask.png")
    screenshotFiles = listOf(
        "screenshot-1.png",
        "screenshot-2.png",
        "screenshot-3.png",
        "screenshot-4.png",
    ).map { nothingPhone.file(it).asFile }
}

tasks.register<org.neotech.plugin.FrameScreenshotsTask>("frameIosSmallScreenshots") {
    group = "store"
    description = "Frames iPhone 6s screenshots into device bezels."

    val nothingPhone = layout.projectDirectory.dir("store-art/iPhone 6s Plus (5.5)")

    bezelFile = nothingPhone.file("bezel-102-379-1242-2210.png")
    maskFile = nothingPhone.file("mask.png")
    screenshotFiles = listOf(
        "screenshot-1.png",
        "screenshot-2.png",
        "screenshot-3.png",
        "screenshot-4.png",
    ).map { nothingPhone.file(it).asFile }
}

tasks.register<org.neotech.plugin.FrameScreenshotsTask>("frameIosLargeScreenshots") {
    group = "store"
    description = "Frames iPhone 15 screenshots into device bezels."

    val nothingPhone = layout.projectDirectory.dir("store-art/iPhone 15 Pro Max (6.7)")

    bezelFile = nothingPhone.file("bezel-120-120-1290-2796.png")
    maskFile = nothingPhone.file("mask.png")
    screenshotFiles = listOf(
        "screenshot-1.png",
        "screenshot-2.png",
        "screenshot-3.png",
        "screenshot-4.png",
    ).map { nothingPhone.file(it).asFile }
}
