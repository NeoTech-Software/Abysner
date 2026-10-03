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

package org.neotech.plugin.store

enum class ScreenshotVariant(val platform: String, val isDarkTheme: Boolean) {
    ANDROID_LIGHT("Android", isDarkTheme = false),
    ANDROID_DARK("Android", isDarkTheme = true),
    IOS_LIGHT("iOS", isDarkTheme = false),
    IOS_DARK("iOS", isDarkTheme = true);

    val spec: DeviceSpec
        get() = when (this) {
            ANDROID_LIGHT, ANDROID_DARK -> androidDeviceSpec
            IOS_LIGHT, IOS_DARK -> iosDeviceSpec
        }
}
