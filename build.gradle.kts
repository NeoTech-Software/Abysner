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
    // Applied to the root to produce an aggregated coverage report across all modules.
    alias(libs.plugins.kover)
}

dependencies {
    kover(project(":domain"))
    kover(project(":composeApp"))
}

kover {
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
        total {
            xml { onCheck = false }
            html { onCheck = false }
        }
    }
}
