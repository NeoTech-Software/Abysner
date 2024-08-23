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
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
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
fun ImageBitmap.toUiImage(): UIImage {
    val pixels = asSkiaBitmap().readPixels(
        ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.PREMUL, ColorSpace.sRGB)
    )!!

    return createWithCGImage(pixels.refTo(0), width.toULong(), height.toULong())
}

/**
 * Taken from: https://github.com/adessoTurkey/compose-multiplatform-sampleapp/blob/2dbbd48654ee482258c37f0e51e52eb6af15ec3c/shared/src/iosMain/kotlin/com/example/moveeapp_compose_kmm/utils/Image.ios.kt#L42
 * Which is licensed under Apache 2.0
 */
@OptIn(ExperimentalForeignApi::class)
private fun createWithCGImage(buffer: CValuesRef<ByteVar>, width: ULong, height: ULong): UIImage {
    val bufferLength = width * height * 4u
    val bitsPerComponent = 8uL
    val bitsPerPixel = 32uL
    val bytesPerRow = 4u * width

    val colorSpaceRef = CGColorSpaceCreateDeviceRGB()
    val bitmapInfo =
        kCGBitmapByteOrderDefault or CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
    val provider = CGDataProviderCreateWithData(null, buffer, bufferLength, null)

    val iref = CGImageCreate(
        width,
        height,
        bitsPerComponent,
        bitsPerPixel,
        bytesPerRow,
        colorSpaceRef,
        bitmapInfo,
        provider,
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

    CGContextDrawImage(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()), iref)

    val imageRef = CGBitmapContextCreateImage(context)

    val scale = UIScreen.mainScreen.scale
    val image = UIImage.imageWithCGImage(imageRef, scale, UIImageOrientation.UIImageOrientationUp)

    CGImageRelease(imageRef)
    CGContextRelease(context)
    CGColorSpaceRelease(colorSpaceRef)
    CGImageRelease(iref)
    CGDataProviderRelease(provider)
    free(pixels)
    return image
}

actual fun shareImageBitmap(image: ImageBitmap) {
    val uiImage: UIImage = image.toUiImage()

    val pngData: NSData? = UIImagePNGRepresentation(uiImage)

    val cachesDirectory = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).first() as String
    val filePath = "$cachesDirectory/temp.png"

    if(pngData?.writeToFile(filePath, true) == true) {

        val fileURL = NSURL.fileURLWithPath(filePath)

        val activityViewController = UIActivityViewController(listOf(fileURL), null)

        val application = UIApplication.sharedApplication
        application.keyWindow?.rootViewController?.presentViewController(
            activityViewController,
            true,
            null
        )
    } else {
        println("Error: Could not share image file because file could not be written.")
    }
}
