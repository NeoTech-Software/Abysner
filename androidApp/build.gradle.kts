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

import com.android.compose.screenshot.tasks.PreviewScreenshotValidationTask
import java.util.Properties

val abysnerVersion: String = providers.gradleProperty("abysnerVersion").get()
val abysnerBuildNumber: String = providers.gradleProperty("abysnerBuildNumber").get()

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.metro)
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

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Used by StoreScreenshotGeneratorTest, so screenshots can be read from the AVD
        testInstrumentationRunnerArguments["useTestStorageService"] = "true"
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
        // Checked into the repository on purpose. Without a fixed key every machine (and every
        // fresh checkout) signs with its own automatically generated debug key, and Android then
        // refuses to update an already installed build with
        // INSTALL_FAILED_UPDATE_INCOMPATIBLE, forcing an uninstall that takes the app's data with
        // it. The credentials are the well-known Android debug ones and protect nothing: this key
        // can only sign debug and development builds, never a release.
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storeType = "PKCS12"
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }

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
            // initWith copies every property from the debug build type, including its
            // applicationIdSuffix, so the suffix has to be set afterwards or development builds end
            // up sharing the ".debug" application id and replacing the debug app on install.
            initWith(getByName("debug"))
            applicationIdSuffix = ".development"
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

    testOptions {
        animationsDisabled = true
        @Suppress("UnstableApiUsage")
        managedDevices {
            localDevices {
                create("storeDevice") {
                    device = "Pixel 10"
                    apiLevel = 36
                    systemImageSource = "aosp"
                }
            }
        }
    }
}

tasks.withType<PreviewScreenshotValidationTask>().configureEach {
    testEngineInput.threshold = 0.001f // 0.1%
}

dependencies {
    implementation(project(":composeApp"))
    implementation(project(":data"))
    implementation(libs.androidx.activity.compose)

    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.ui.tooling)
    screenshotTestImplementation(libs.jetbrains.compose.material3)
    screenshotTestImplementation(project(":domain"))

    androidTestImplementation(project(":composeApp"))
    androidTestImplementation(project(":data"))
    androidTestImplementation(project(":domain"))
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.datastore.preferences)
    androidTestImplementation(libs.okio)
    androidTestImplementation(libs.kotlinx.serialization)
    androidTestImplementation(libs.kotlinx.coroutines.core)
    androidTestUtil(libs.androidx.test.services)
}

