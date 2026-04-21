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
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("screenshotReferenceCleanup") {
            id = "screenshot-reference-cleanup"
            implementationClass = "org.neotech.plugin.ScreenshotReferenceCleanupPlugin"
        }
        register("screenshotTestCoverage") {
            id = "screenshot-test-coverage"
            implementationClass = "org.neotech.plugin.ScreenshotTestCoveragePlugin"
        }
    }
}

dependencies {
    // Required to compile the shadowed Renderer class.
    compileOnly(libs.screenshot.validation.junit.engine)
    compileOnly(libs.compose.preview.renderer)
}
