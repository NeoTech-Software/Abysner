/*
 * Abysner - Dive planner
 * Copyright (C) 2025 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

import org.gradle.kotlin.dsl.jacoco

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

plugins {
    jacoco
}

jacoco {
    toolVersion = "0.8.12"
    reportsDirectory = layout.buildDirectory.dir("reports/jacoco")
}

tasks.register("jacocoTestReport", JacocoReport::class) {
    group = "coverage"
    dependsOn(tasks.withType(Test::class))
    val coverageSourceDirs = arrayOf(
        "src/commonMain/kotlin"
    )
    val buildDir = layout.buildDirectory

    // Include all compiled classes.
    val classFiles = buildDir.dir("classes/kotlin/jvm").get().asFile
        .walkBottomUp()
        .toSet()

    // This helps with test coverage accuracy.
    classDirectories.setFrom(classFiles)
    sourceDirectories.setFrom(files(coverageSourceDirs))

    // The resulting test report in binary format.
    // It serves as the basis for human-readable reports.
    buildDir.files("jacoco/jvmTest.exec").let {
        executionData.setFrom(it)
    }
    reports {
        xml.required = false
        html.required = true
    }
}

tasks.withType<Test> {
    finalizedBy(tasks.withType(JacocoReport::class))
}
