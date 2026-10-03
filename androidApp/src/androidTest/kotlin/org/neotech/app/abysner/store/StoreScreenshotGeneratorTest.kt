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

package org.neotech.app.abysner.store

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.io.PlatformTestStorageRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okio.Path.Companion.toOkioPath
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.neotech.app.abysner.AbysnerApplication
import org.neotech.app.abysner.data.settings.resources.SettingsResourceV1
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.UnitSystem
import org.neotech.app.abysner.domain.diveplanning.PlanningRepository
import org.neotech.app.abysner.domain.diveplanning.model.MultiDivePlanInputModel
import org.neotech.app.abysner.domain.settings.SettingsRepository
import org.neotech.app.abysner.domain.settings.model.ThemeMode
import org.neotech.app.abysner.extensions.CUTOUT_OVERLAY
import org.neotech.app.abysner.extensions.NAVIGATION_OVERLAY
import org.neotech.app.abysner.extensions.blankStatusBar
import org.neotech.app.abysner.extensions.enableOverlay
import org.neotech.app.abysner.extensions.hasTextStartingWith
import org.neotech.app.abysner.extensions.isStatusBarInsetMatchingCutout
import org.neotech.app.abysner.extensions.nodePositionInRoot
import org.neotech.app.abysner.extensions.scrollToPosition
import org.neotech.app.abysner.extensions.scrollToTop
import org.neotech.app.abysner.presentation.SCREENSHOT_MODE
import org.neotech.app.abysner.presentation.SCREENSHOT_MODE_FAKE_IOS
import org.neotech.app.abysner.presentation.screens.planner.PlanScreenTestTags
import org.neotech.app.abysner.presentation.theme.Platform

/**
 * A special Android test that solely exists to capture on device screenshots of the app, for use in
 * Play Store and App Store artwork. This test not only generates screenshots for Android but also
 * for iOS as it puts the app in a state that makes the app render as if it is running on iOS.
 */
@RunWith(AndroidJUnit4::class)
class StoreScreenshotGeneratorTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val packageName = instrumentation.targetContext.packageName

    private val appComponent
        get() = (instrumentation.targetContext.applicationContext as AbysnerApplication).appComponent()

    private val settingsRepository: SettingsRepository
        get() = appComponent.settingsRepository

    private val planningRepository: PlanningRepository
        get() = appComponent.planningRepository

    @Test
    fun storeScreenshotsAndroidLight() = captureStoreScreenshots(platform = Platform.ANDROID, theme = ThemeMode.LIGHT)

    @Test
    fun storeScreenshotsAndroidDark() = captureStoreScreenshots(platform = Platform.ANDROID, theme = ThemeMode.DARK)

    @Test
    fun storeScreenshotsIosLight() = captureStoreScreenshots(platform = Platform.IOS, theme = ThemeMode.LIGHT)

    @Test
    fun storeScreenshotsIosDark() = captureStoreScreenshots(platform = Platform.IOS, theme = ThemeMode.DARK)

    private fun captureStoreScreenshots(platform: Platform, theme: ThemeMode) {
        val directoryName = "${platform.name.lowercase()}-${theme.name.lowercase()}"

        System.setProperty(SCREENSHOT_MODE_FAKE_IOS, (platform == Platform.IOS).toString())
        configureSystemUi()
        launchApp()
        planningRepository.updateConfiguration { Configuration() }
        setDivePlan(StoreScreenshotFixtures.input)
        setSettings(theme, unitSystem = UnitSystem.METRIC, showBasicDecoTable = false)

        screenshot(directoryName, "screenshot-1.png")

        val firstCardYPosition = compose.nodePositionInRoot(PlanScreenTestTags.CYLINDERS_CARD).y
        compose.onNodeWithText("Show more").performScrollTo().performClick()
        compose.scrollToPosition(PlanScreenTestTags.GAS_PLAN_CARD, firstCardYPosition)
        screenshot(directoryName, "screenshot-3.png")

        compose.scrollToTop()
        compose.onNodeWithText("Dive 2").performClick()
        screenshot(directoryName, "screenshot-5.png")

        compose.onNode(hasTextStartingWith("${StoreScreenshotFixtures.trimixDiluent.gas} - ")).performClick()
        compose.onNodeWithText("Gas & cylinder").assertExists()
        screenshot(directoryName, "screenshot-2.png")

        device.pressBack()
        compose.waitUntil(TIMEOUT_MILLIS) {
            compose.onAllNodesWithText("Gas & cylinder").fetchSemanticsNodes().isEmpty()
        }

        compose.scrollToPosition(PlanScreenTestTags.DECO_PLAN_CARD, firstCardYPosition)
        screenshot(directoryName, "screenshot-4.png")

        compose.onNodeWithText("Dive 1").performClick()
        setDivePlan(StoreScreenshotFixtures.imperialInput)
        setSettings(theme, unitSystem = UnitSystem.IMPERIAL, showBasicDecoTable = true)
        compose.scrollToPosition(PlanScreenTestTags.DECO_PLAN_CARD, firstCardYPosition)
        screenshot(directoryName, "screenshot-6.png")
    }

    private fun setDivePlan(plan: MultiDivePlanInputModel) {
        planningRepository.updateMultiDivePlanInput { plan }
        compose.waitForIdle()
    }

    private fun setSettings(theme: ThemeMode, unitSystem: UnitSystem, showBasicDecoTable: Boolean) {
        settingsRepository.updateSettings {
            it.copy(themeMode = theme, unitSystem = unitSystem, showBasicDecoTable = showBasicDecoTable)
        }
        compose.waitForIdle()
    }

    private fun launchApp() {
        val intent = instrumentation.targetContext.packageManager
            .getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            ?: error("No launch intent for $packageName")
        instrumentation.targetContext.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(packageName).depth(0)), TIMEOUT_MILLIS)
        compose.waitForIdle()
    }

    private fun configureSystemUi() {
        device.enableOverlay(NAVIGATION_OVERLAY)
        device.blankStatusBar()
        device.enableOverlay(CUTOUT_OVERLAY)
        // Wait for the cutout insets to actually arrive at the app.
        compose.waitUntil(TIMEOUT_MILLIS) {
            device.isStatusBarInsetMatchingCutout()
        }
    }

    private fun screenshot(directoryName: String, name: String) {
        compose.waitForIdle()
        val screenshot = instrumentation.uiAutomation.takeScreenshot() ?: error("Unable to take a screenshot")
        PlatformTestStorageRegistry.getInstance().openOutputFile("$directoryName/$name").use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        screenshot.recycle()
    }

    companion object {

        @JvmStatic
        @BeforeClass
        fun setInitialAppState() {
            System.setProperty(SCREENSHOT_MODE, true.toString())
            val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
            val planFilesDirectory = targetContext.filesDir.toOkioPath()

            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            val dataStore = PreferenceDataStoreFactory.createWithPath(scope = scope) {
                planFilesDirectory.resolve("datastore/abysner.preferences_pb")
            }
            val settings = SettingsResourceV1(
                showBasicDecoTable = false,
                termsAndConditionsAccepted = true,
                showDiveEditTooltip = false,
                unitSystem = SettingsResourceV1.UnitSystemResource.METRIC,
                themeMode = SettingsResourceV1.ThemeModeResource.SYSTEM,
            )
            runBlocking {
                dataStore.edit {
                    it[stringPreferencesKey("global.settings")] =
                        Json.encodeToString(SettingsResourceV1.serializer(), settings)
                }
            }
            scope.cancel()
        }
    }
}

private const val TIMEOUT_MILLIS = 30_000L
