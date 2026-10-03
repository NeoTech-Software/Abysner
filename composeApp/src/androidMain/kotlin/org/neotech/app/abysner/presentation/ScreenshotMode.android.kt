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

package org.neotech.app.abysner.presentation

/**
 * Set by StoreScreenshotGeneratorTest, allows the app to know if it is being used to capture
 * screenshots, and can adjust some behavior accordingly to aid in that process.
 */
const val SCREENSHOT_MODE = "abysner.screenshotMode"

/**
 * Set by StoreScreenshotGeneratorTest, allows the app to present itself as if running on iOS even
 * though it is running on Android.
 */
const val SCREENSHOT_MODE_FAKE_IOS = "abysner.screenshotMode.fakeiOS"

internal actual fun isScreenshotMode(): Boolean = System.getProperty(SCREENSHOT_MODE).toBoolean()
