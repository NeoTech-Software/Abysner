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

package org.neotech.app.abysner.extensions

import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

internal fun UiDevice.enableOverlay(overlay: String) {
    if (!executeShellCommand("cmd overlay list").contains("[x] $overlay")) {
        executeShellCommand("cmd overlay enable-exclusive --category $overlay")
    }
}

@RequiresApi(Build.VERSION_CODES.R)
internal fun UiDevice.isStatusBarInsetMatchingCutout(): Boolean {
    // UiDevice is not actually used here, but the extension makes it pair nicely with enableOverlay
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val insets = context.getSystemService(WindowManager::class.java).currentWindowMetrics.windowInsets
    val cutoutTop = insets.displayCutout?.safeInsetTop ?: 0
    return cutoutTop > 0 && cutoutTop == insets.getInsets(WindowInsets.Type.statusBars()).top
}

internal fun UiDevice.blankStatusBar() {
    executeShellCommand("cmd statusbar send-disable-flag clock system-icons notification-icons")
}

const val NAVIGATION_OVERLAY = "com.android.internal.systemui.navbar.gestural"
const val CUTOUT_OVERLAY = "com.android.internal.display.cutout.emulation.tall"
