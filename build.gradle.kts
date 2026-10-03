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

import org.neotech.plugin.store.DrawDeviceFrameTask
import org.neotech.plugin.store.ScreenshotVariant

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

val createScreenshotsTask = tasks.register("createStoreScreenshots") {
    group = "store"
    description = "Captures the generic store screenshots for every platform and theme, and frames them in a device bezel."
}

ScreenshotVariant.entries.forEach { variant ->

    val theme = if (variant.isDarkTheme) {
        "dark"
    } else {
        "light"
    }
    val taskNameSuffix = variant.platform.lowercase().replaceFirstChar(Char::uppercase) + theme.replaceFirstChar(Char::uppercase)
    val directoryName = "${variant.platform.lowercase()}-$theme"

    val task = tasks.register<DrawDeviceFrameTask>("createStore${taskNameSuffix}Screenshots") {
        group = "store"
        description = "Captures ${variant.platform} screenshots in $theme-mode, and frames them in a device bezel."

        val captures = project(":androidApp").layout.buildDirectory
            .dir("outputs/managed_device_android_test_additional_output/debug/storeDevice/${directoryName}")

        dependsOn(":androidApp:storeDeviceDebugAndroidTest")

        outputDirectory = layout.projectDirectory.dir("store-art/${directoryName}")
        this.variant = variant
        screenshotFiles = provider {
            captures.get().asFile.listFiles { file -> file.extension == "png" }
                ?.sortedBy { it.name }
                .orEmpty()
        }
    }
    createScreenshotsTask.configure { dependsOn(task) }
}
