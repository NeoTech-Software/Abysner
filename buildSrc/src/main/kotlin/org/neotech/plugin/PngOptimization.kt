/*
 * Abysner - Dive planner
 * Copyright (C) 2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.plugin

import org.gradle.api.logging.Logger
import java.io.File

fun isOxipngAvailable(): Boolean = try {
    ProcessBuilder("oxipng", "--version")
        .redirectErrorStream(true)
        .start()
        .waitFor() == 0
} catch (_: Exception) {
    false
}

fun optimizePng(file: File, logger: Logger) {
    val process = ProcessBuilder("oxipng", "-o", "4", "--strip", "safe", file.absolutePath)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().readText()
    val exitCode = process.waitFor()
    if (exitCode != 0) {
        logger.warn("oxipng failed (exit $exitCode): $output")
    }
}
