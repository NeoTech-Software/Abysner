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
