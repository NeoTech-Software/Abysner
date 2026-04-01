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

package org.neotech.app.abysner.presentation.utilities

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorRenderingIntent
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGDataProviderCreateWithData
import platform.CoreGraphics.CGDataProviderRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageCreate
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGBitmapByteOrderDefault
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageOrientation
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIScreen
import platform.posix.free
import platform.posix.malloc

/**
 * Taken from: https://github.com/adessoTurkey/compose-multiplatform-sampleapp/blob/2dbbd48654ee482258c37f0e51e52eb6af15ec3c/shared/src/iosMain/kotlin/com/example/moveeapp_compose_kmm/utils/Image.ios.kt#L42
 * Which is licensed under Apache 2.0
 */
@OptIn(ExperimentalForeignApi::class)
fun ImageBitmap.toUiImage(scale: Double = 1.0): UIImage {
    val pixels = asSkiaBitmap().readPixels(
        ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.PREMUL, ColorSpace.sRGB)
    )!!

    return createWithCGImage(pixels.refTo(0), width.toULong(), height.toULong(), scale)
}

/**
 * Taken from: https://github.com/adessoTurkey/compose-multiplatform-sampleapp/blob/2dbbd48654ee482258c37f0e51e52eb6af15ec3c/shared/src/iosMain/kotlin/com/example/moveeapp_compose_kmm/utils/Image.ios.kt#L42
 * Which is licensed under Apache 2.0
 */
@OptIn(ExperimentalForeignApi::class)
private fun createWithCGImage(buffer: CValuesRef<ByteVar>, width: ULong, height: ULong, scale: Double): UIImage {
    val bufferLength = width * height * 4u
    val bitsPerComponent = 8uL
    val bitsPerPixel = 32uL
    val bytesPerRow = 4u * width

    val colorSpaceRef = CGColorSpaceCreateDeviceRGB()
    val bitmapInfo =
        kCGBitmapByteOrderDefault or CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
    val srcProvider = CGDataProviderCreateWithData(null, buffer, bufferLength, null)

    val srcImage = CGImageCreate(
        width,
        height,
        bitsPerComponent,
        bitsPerPixel,
        bytesPerRow,
        colorSpaceRef,
        bitmapInfo,
        srcProvider,
        null,
        true,
        CGColorRenderingIntent.kCGRenderingIntentDefault
    )

    val pixels = malloc(bufferLength)

    val context = CGBitmapContextCreate(
        pixels,
        width,
        height,
        bitsPerComponent,
        bytesPerRow,
        colorSpaceRef,
        bitmapInfo
    )!!

    CGContextDrawImage(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()), srcImage)

    // Release the source objects now that the draw is done.
    CGImageRelease(srcImage)
    CGContextRelease(context)
    CGDataProviderRelease(srcProvider)

    // Create the final image with a release callback so Core Graphics frees the buffer when it is
    // truly done with the pixel data.
    val ownedProvider = CGDataProviderCreateWithData(
        info = null,
        data = pixels,
        size = bufferLength,
        releaseData = staticCFunction { _, data, _ -> free(data) }
    )
    val imageRef = CGImageCreate(
        width,
        height,
        bitsPerComponent,
        bitsPerPixel,
        bytesPerRow,
        colorSpaceRef,
        bitmapInfo,
        ownedProvider,
        null,
        true,
        CGColorRenderingIntent.kCGRenderingIntentDefault
    )
    val image = UIImage.imageWithCGImage(imageRef, scale, UIImageOrientation.UIImageOrientationUp)

    CGImageRelease(imageRef)
    CGDataProviderRelease(ownedProvider)
    CGColorSpaceRelease(colorSpaceRef)
    return image
}

actual suspend fun shareImageBitmap(image: ImageBitmap) {
    // UIScreen.mainScreen must be read on the main thread.
    val scale = UIScreen.mainScreen.scale

    // Offload pixel conversion, PNG encoding and file writing to a background dispatcher.
    // UIActivityViewController must be created and presented on the main thread.
    val filePath: String? = withContext(Dispatchers.Default) {
        val uiImage: UIImage = image.toUiImage(scale)

        val pngData: NSData? = UIImagePNGRepresentation(uiImage)

        val cachesDirectory = NSSearchPathForDirectoriesInDomains(
            NSCachesDirectory,
            NSUserDomainMask,
            true
        ).first() as String

        val filePath = "$cachesDirectory/diveplan.png"

        if (pngData?.writeToFile(filePath, true) == true) {
            filePath
        } else {
            println("Error: Could not share image file because file could not be written or conversion to PNG failed.")
            null
        }
    }

    if (filePath == null) {
        return
    }

    val shareViewController = UIActivityViewController(
        activityItems = listOf(NSURL.fileURLWithPath(filePath)),
        applicationActivities = null,
    )

    UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
        shareViewController,
        true,
        null,
    )
}
