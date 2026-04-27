package org.neotech.app.abysner.presentation

import androidx.compose.ui.tooling.preview.Preview

@Preview(fontScale = 1.0f, device = DEVICE_SCREENSHOT_TESTS_MAX, locale = "en")
annotation class PreviewForScreenshotTestsMaxHeight

@Preview(fontScale = 1.0f, device = DEVICE_SCREENSHOT_TESTS_DEFAULT_HEIGHT, locale = "en")
annotation class PreviewForScreenshotTestsDefaultHeight

@Preview(fontScale = 1.0f, device = DEVICE_SCREENSHOT_TESTS_DIALOG, locale = "en")
annotation class PreviewForScreenshotTestsDialog

@Preview(fontScale = 1.0f, device = DEVICE_FOLDABLE_SCREENSHOT_TESTS_DEFAULT_HEIGHT, locale = "en")
annotation class PreviewForScreenshotTestsFoldableDefaultHeight

/**
 * Same as [DEVICE_SCREENSHOT_TESTS_DEFAULT_HEIGHT] but with the height of the device set to a
 * rather big number so more fits in the screenshot test, dpi remains the same lower number.
 */
const val DEVICE_SCREENSHOT_TESTS_MAX = "spec:width=411dp,height=2350dp,dpi=240"

/**
 * Same as [androidx.compose.ui.tooling.preview.Devices.PHONE] but with dpi set lower to prevent
 * reference images from becoming too big.
 */
const val DEVICE_SCREENSHOT_TESTS_DEFAULT_HEIGHT = "spec:width=411dp,height=891dp,dpi=240"

/**
 * Narrower device for dialog screenshot tests. Dialogs don't fill the screen width, so a smaller
 * specification makes for more realistic screenshots.
 */
const val DEVICE_SCREENSHOT_TESTS_DIALOG = "spec:width=340dp,height=891dp,dpi=240"

/**
 * Same as [androidx.compose.ui.tooling.preview.Devices.FOLDABLE] but with dpi set lower to prevent
 * reference images from becoming too big.
 */
const val DEVICE_FOLDABLE_SCREENSHOT_TESTS_DEFAULT_HEIGHT = "spec:width=841dp,height=673dp,dpi=240"
