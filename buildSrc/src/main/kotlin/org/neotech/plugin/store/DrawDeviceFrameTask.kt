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
import com.github.weisj.jsvg.view.ViewBox
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.neotech.plugin.isOxipngAvailable
import org.neotech.plugin.optimizePng
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Draws a device frame around screenshots based on the [DeviceSpec] of the [variant] platform. This
 * includes drawing a status-bar in either iOS or Android style.
 */
abstract class DrawDeviceFrameTask : DefaultTask() {

    @get:Input
    abstract val variant: Property<ScreenshotVariant>

    @get:InputFiles
    abstract val screenshotFiles: ListProperty<File>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun execute() {
        check(screenshotFiles.get().isNotEmpty()) {
            "No screenshots to frame for ${variant.get()}, have they been captured?"
        }

        val spec = variant.get().spec

        val canOptimize = isOxipngAvailable()
        if (!canOptimize) {
            logger.warn("oxipng not found on PATH, skipping PNG optimization.")
        }

        val destination = outputDirectory.get().asFile

        for (screenshotFile in screenshotFiles.get()) {
            val screenshot = ImageIO.read(screenshotFile)

            val screenshotWithStatusBar = screenshot.drawStatusBar(spec)
            screenshotWithStatusBar.write(destination.resolve(screenshotFile.name), canOptimize)

            val screenshotWithFrame = screenshotWithStatusBar.drawFrame(spec)
            screenshotWithFrame.write(destination.resolve("framed-${screenshotFile.name}"), canOptimize)

            logger.lifecycle("Created ${screenshotFile.name} and framed-${screenshotFile.name}")
        }
    }

    private fun BufferedImage.drawStatusBar(spec: DeviceSpec): BufferedImage {
        val output = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = output.createGraphics()
        graphics.applyQualityHints()
        graphics.drawImage(this, 0, 0, null)

        val appearance = spec.statusBar.appearance
        graphics.color = if (variant.get().isDarkTheme) {
            appearance.colorOnDark
        } else {
            appearance.colorOnLight
        }
        graphics.drawStatusBar(spec, width)

        graphics.dispose()
        return output
    }

    private fun BufferedImage.drawFrame(spec: DeviceSpec): BufferedImage {
        val bezel = spec.bezel.width.toPixels()
        val extraImageSize = (2 * bezel).toInt()
        val output = BufferedImage(width + extraImageSize, height + extraImageSize, BufferedImage.TYPE_INT_ARGB)

        val body = RoundRectangle2D.Double(
            0.0,
            0.0,
            output.width.toDouble(),
            output.height.toDouble(),
            spec.bezel.outerRadius.toPixels() * 2,
            spec.bezel.outerRadius.toPixels() * 2,
        )

        val screen = RoundRectangle2D.Double(
            bezel,
            bezel,
            width.toDouble(),
            height.toDouble(),
            spec.bezel.innerRadius.toPixels() * 2,
            spec.bezel.innerRadius.toPixels() * 2,
        )

        val graphics = output.createGraphics()
        graphics.applyQualityHints()

        // Shrink the body by half the bezel width, so anti-aliased edges are under the solid bezel.
        graphics.color = Color.BLACK
        graphics.fill(body.shrink(bezel / 2))

        graphics.composite = AlphaComposite.SrcIn
        graphics.drawImage(this, bezel.toInt(), bezel.toInt(), null)

        graphics.composite = AlphaComposite.SrcOver
        graphics.color = BEZEL_COLOR
        val bezelShape = Path2D.Double(Path2D.WIND_EVEN_ODD).apply {
            append(body, false)
            append(screen, false)
        }
        graphics.fill(bezelShape)

        graphics.dispose()

        return output
    }

    private fun Graphics2D.drawStatusBar(
        spec: DeviceSpec,
        screenshotWidth: Int,
    ) {
        val appearance = spec.statusBar.appearance
        val centerY = spec.statusBar.height.toPixels() / 2

        font = if (appearance.clockIsBold) {
            appearance.clockFont.deriveFont(Font.BOLD, appearance.clockSize.toPixels().toFloat())
        } else {
            appearance.clockFont.deriveFont(appearance.clockSize.toPixels().toFloat())
        }
        val metrics = fontMetrics
        drawString(
            spec.statusBar.time,
            appearance.clockInset.toPixels().toFloat(),
            (centerY + (metrics.ascent - metrics.descent) / 2).toFloat(),
        )

        var right = screenshotWidth - appearance.iconInset.toPixels()
        appearance.icons.forEachIndexed { index, icon ->
            right -= drawIcon(icon, appearance.iconHeight, right, centerY)
            if (index != appearance.icons.lastIndex) {
                right -= appearance.iconGap.toPixels()
            }
        }
    }

    private fun Graphics2D.drawIcon(
        icon: SVGDocument,
        iconHeight: Double,
        right: Double,
        centerY: Double,
    ): Double {
        val size = icon.size()
        val height = iconHeight.toPixels().toInt()
        val width = (height * size.width / size.height).toInt()

        val bitmap = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val bitmapGraphics = bitmap.createGraphics()
        bitmapGraphics.applyQualityHints()
        icon.render(null, bitmapGraphics, ViewBox(0f, 0f, width.toFloat(), height.toFloat()))
        bitmapGraphics.composite = AlphaComposite.getInstance(AlphaComposite.SRC_IN)
        bitmapGraphics.color = color
        bitmapGraphics.fillRect(0, 0, width, height)
        bitmapGraphics.dispose()

        drawImage(bitmap, (right - width).toInt(), (centerY - height / 2.0).toInt(), null)
        return width.toDouble()
    }

    private fun Graphics2D.applyQualityHints() {
        setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    }

    private fun BufferedImage.write(file: File, optimize: Boolean) {
        ImageIO.write(this, "png", file)
        if (optimize) {
            optimizePng(file, logger)
        }
    }

    private fun RoundRectangle2D.shrink(amount: Double): RoundRectangle2D = RoundRectangle2D.Double(
        x + amount,
        y + amount,
        width - amount * 2,
        height - amount * 2,
        arcWidth - amount * 2,
        arcHeight - amount * 2,
    )
}

private const val SCREENSHOT_DPI = 420

private const val BASELINE_DPI = 160

private fun Double.toPixels(): Double = this * SCREENSHOT_DPI / BASELINE_DPI

private val BEZEL_COLOR = Color(0x1B, 0x1B, 0x1F)
