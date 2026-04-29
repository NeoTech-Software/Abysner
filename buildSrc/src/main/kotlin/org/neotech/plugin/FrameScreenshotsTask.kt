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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.io.File
import javax.imageio.ImageIO

/**
 * Frames screenshots into a device bezel, using a mask to cut the screenshot so it matches the
 * shape of the device screen and any camera holes or rounded corners.
 *
 * The [bezelFile] filename encodes the screenshot offset (x, y) and the size (width, height) the
 * screenshot must be scaled to, following the pattern `bezel-{x}-{y}-{width}-{height}.png`. The
 * [maskFile] is a grayscale image at the same resolution as the bezel where white pixels represent
 * the screen area and black pixels represent areas to cut (corners, camera hole).
 *
 * Each screenshot listed in [screenshotFiles] produces a `bezel-<original-name>.png` output next
 * to the original. If `oxipng` is on the PATH, outputs are losslessly optimized automatically.
 */
abstract class FrameScreenshotsTask : DefaultTask() {

    @get:InputFile
    abstract val bezelFile: RegularFileProperty

    @get:InputFile
    abstract val maskFile: RegularFileProperty

    @get:InputFiles
    abstract val screenshotFiles: ListProperty<File>

    @get:OutputFiles
    val outputFiles: List<File>
        get() = screenshotFiles.get().map { File(it.parentFile, "bezel-${it.name}") }

    @TaskAction
    fun execute() {
        val bezel = bezelFile.get().asFile
        val (offsetX, offsetY, targetWidth, targetHeight) = bezel.nameWithoutExtension
            .removePrefix("bezel-")
            .split("-")
            .map { it.toInt() }

        logger.lifecycle("Bezel: ${bezel.name} (offset=$offsetX,$offsetY size=${targetWidth}x${targetHeight})")

        val bezelImage = ImageIO.read(bezel)
        val maskAlpha = readMaskAlpha(ImageIO.read(maskFile.get().asFile))

        val canOptimize = isOxipngAvailable()
        if (!canOptimize) {
            logger.warn("oxipng not found on PATH, skipping PNG optimization.")
        }

        for (screenshotFile in screenshotFiles.get()) {
            if (!screenshotFile.exists()) {
                throw GradleException("Screenshot not found: $screenshotFile")
            }

            logger.lifecycle("Processing: ${screenshotFile.name}")

            val screenshot = ImageIO.read(screenshotFile)
            val scaled = scaleImage(screenshot, targetWidth, targetHeight)
            val result = composite(scaled, bezelImage, maskAlpha, offsetX, offsetY)

            val outputFile = File(screenshotFile.parentFile, "bezel-${screenshotFile.name}")
            ImageIO.write(result, "png", outputFile)
            if (canOptimize) {
                optimizePng(outputFile)
            }
            val size = outputFile.length() / (1024.0 * 1024.0)
            logger.lifecycle("  Output: ${outputFile.name} (${"%.2f".format(size)} MB)")
        }
    }

    private fun scaleImage(image: BufferedImage, width: Int, height: Int): BufferedImage {
        val scaled = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        scaled.createGraphics().apply {
            setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
            )
            setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            drawImage(image, 0, 0, width, height, null)
            dispose()
        }
        return scaled
    }

    private fun readMaskAlpha(mask: BufferedImage): ByteArray {
        val grayscale = if (mask.type == BufferedImage.TYPE_BYTE_GRAY) {
            mask
        } else {
            val converted = BufferedImage(mask.width, mask.height, BufferedImage.TYPE_BYTE_GRAY).also {
                it.createGraphics().apply {
                    drawImage(mask, 0, 0, null)
                    dispose()
                }
            }
            val suggestion = File(maskFile.get().asFile.parentFile, "mask-grayscale.png")
            ImageIO.write(converted, "png", suggestion)
            logger.warn("Mask is not a grayscale PNG. A converted copy has been saved to ${suggestion.name}.")
            converted
        }
        return (grayscale.raster.dataBuffer as DataBufferByte).data
    }

    private fun applyMask(image: BufferedImage, maskAlpha: ByteArray): BufferedImage {
        val result = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
        val pixels = IntArray(image.width * image.height)
        image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)
        for (index in pixels.indices) {
            val maskValue = maskAlpha[index].toInt() and 0xFF
            val alpha = (pixels[index] ushr 24) * maskValue / 255
            pixels[index] = (alpha shl 24) or (pixels[index] and 0x00FFFFFF)
        }
        result.setRGB(0, 0, image.width, image.height, pixels, 0, image.width)
        return result
    }

    private fun composite(
        screenshot: BufferedImage,
        bezel: BufferedImage,
        maskAlpha: ByteArray,
        offsetX: Int,
        offsetY: Int
    ): BufferedImage {
        // Place screenshot on a bezel-sized canvas and mask it (cut corners and camera hole).
        val canvas = BufferedImage(bezel.width, bezel.height, BufferedImage.TYPE_INT_ARGB)
        canvas.createGraphics().apply {
            drawImage(screenshot, offsetX, offsetY, null)
            dispose()
        }
        val maskedScreenshot = applyMask(canvas, maskAlpha)

        // Create output image, place the masked screenshot first, masked bezel on top.
        val output = BufferedImage(bezel.width, bezel.height, BufferedImage.TYPE_INT_ARGB)
        output.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            drawImage(maskedScreenshot, 0, 0, null)
            drawImage(bezel, 0, 0, null)
            dispose()
        }
        return output
    }

    private fun optimizePng(file: File) {
        val process = ProcessBuilder("oxipng", "-o", "4", "--strip", "safe", file.absolutePath)
            .redirectErrorStream(true)
            .start()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val output = process.inputStream.bufferedReader().readText()
            logger.warn("  oxipng failed (exit $exitCode): $output")
        }
    }

    private fun isOxipngAvailable(): Boolean = try {
        ProcessBuilder("which", "oxipng")
            .redirectErrorStream(true)
            .start()
            .waitFor() == 0
    } catch (_: Exception) {
        false
    }
}
