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

import java.util.Properties

val abysnerVersion: String by project.properties
val abysnerBuildNumber: String by project.properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.screenshot)
    alias(libs.plugins.kover)
    id("screenshot-reference-cleanup")
    id("screenshot-test-coverage")
}

kover {
    currentProject {
        // androidApp module has no domain code, so we don't include any variants for this coverage report
        createVariant("domain") {}
        createVariant("presentation") {
            add("debug")
        }
    }
}

android {
    namespace = "nl.neotech.app.abysner"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "nl.neotech.app.abysner"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = abysnerBuildNumber.toInt()
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
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        create("development") {
            applicationIdSuffix = ".development"
            initWith(getByName("debug"))
            matchingFallbacks += listOf("release")
            isMinifyEnabled = true
            isShrinkResources = true
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

    @Suppress("UnstableApiUsage")
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

screenshotTests {
    imageDifferenceThreshold = 0.001f // 0.1%
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)

    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.ui.tooling)
    screenshotTestImplementation(libs.jetbrains.compose.material3)
    screenshotTestImplementation(project(":domain"))
}

