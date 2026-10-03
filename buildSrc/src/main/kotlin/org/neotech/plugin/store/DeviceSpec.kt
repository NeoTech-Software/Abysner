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

import com.github.weisj.jsvg.SVGDocument
import com.github.weisj.jsvg.parser.SVGLoader
import org.gradle.api.GradleException
import java.awt.Color
import java.awt.Font
import java.io.File

data class DeviceSpec(
    val bezel: Bezel,
    val statusBar: StatusBar,
) {

    data class Bezel(
        val width: Double,
        val outerRadius: Double,
    ) {
        val innerRadius: Double get() = outerRadius - width
    }

    data class StatusBar(
        val height: Double,
        val time: String,
        val appearance: StatusBarAppearance,
    )

    class StatusBarAppearance(
        val clockFont: Font,
        val clockIsBold: Boolean,
        val clockSize: Double,
        val clockInset: Double,
        val iconHeight: Double,
        val iconInset: Double,
        val iconGap: Double,
        val icons: List<SVGDocument>,
        val colorOnLight: Color,
        val colorOnDark: Color,
    )
}

internal val androidDeviceSpec: DeviceSpec by lazy {
    DeviceSpec(
        bezel = DeviceSpec.Bezel(width = 10.0, outerRadius = 48.0),
        statusBar = DeviceSpec.StatusBar(
            height = 48.0,
            time = "13:37",
            appearance = DeviceSpec.StatusBarAppearance(
                clockFont = Font.createFont(Font.TRUETYPE_FONT, resourceStream("fonts/Roboto-Medium.ttf")),
                clockIsBold = false,
                clockSize = 14.0,
                clockInset = 24.0,
                iconHeight = 11.3,
                iconInset = 28.6,
                iconGap = 6.0,
                icons = listOf("android/battery.svg", "android/wifi.svg", "android/signal.svg")
                    .map { loadStatusBarSvg(it) },
                colorOnLight = Color(98, 100, 101),
                colorOnDark = Color(220, 222, 224),
            ),
        ),
    )
}

internal val iosDeviceSpec: DeviceSpec by lazy {
    DeviceSpec(
        bezel = DeviceSpec.Bezel(width = 10.0, outerRadius = 44.0),
        statusBar = DeviceSpec.StatusBar(
            height = 54.0,
            time = "9:41",
            appearance = DeviceSpec.StatusBarAppearance(
                // TODO we cannot include the San Francisco itself in this repository, so we get it from
                //  the system instead, however this only works on macOS machines. Perhaps we could
                //  find a close enough font instead?
                clockFont = Font.createFont(Font.TRUETYPE_FONT, File("/System/Library/Fonts/SFNS.ttf")),
                clockIsBold = true,
                clockSize = 17.0,
                clockInset = 53.0,
                iconHeight = 11.6,
                iconInset = 42.3,
                iconGap = 7.5,
                icons = listOf("ios/battery.svg", "ios/wifi.svg", "ios/signal.svg").map { loadStatusBarSvg(it) },
                colorOnLight = Color.BLACK,
                colorOnDark = Color.WHITE,
            ),
        ),
    )
}

private fun loadStatusBarSvg(fileName: String): SVGDocument {
    val resource = DeviceSpec::class.java.getResource("/statusbar-icons/$fileName")
        ?: throw GradleException("Missing resource: statusbar-icons/$fileName")
    return SVGLoader().load(resource)!!
}

private fun resourceStream(path: String) =
    DeviceSpec::class.java.getResourceAsStream("/$path")
        ?: throw GradleException("Missing resource: $path")
