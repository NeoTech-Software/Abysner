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

import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import org.neotech.app.abysner.App
import org.neotech.app.abysner.di.AppComponent

// Unfortunately it seems non-trivial (impossible) to use KSP on the iosMain sourceSet, since
// iosMain is not a target but rather common code (the actual targets are X64, Arm64 and SimulatorArm64.
// See: https://github.com/google/ksp/issues/567.
//
// Therefore create() functions are not generated within this common sourceSet, but rather in the
// target source sets, so these create functions are not accessible here (undefined).
//
// A workaround would be to create an expected function here and implement them in the
// targets, to makes this less cumbersome using @KmpComponentCreate would seem like a good
// solution as it automatically generates the actual implementations. However that also does
// not seem to work (error: '@KmpComponentCreate() actual fun foo(): Bar' has no corresponding
// expected declaration) because the generated actual functions do not seem to correspond to
// a expect function?
//
// TLDR: We have to manually implement a actual and expected method in each iOS target to provide
// us the create methods.
expect fun createAppComponent(): AppComponent

private val appComponent = createAppComponent()

@Suppress("unused", "FunctionName")
fun MainViewController() = ComposeUIViewController(
    configure = {
        onFocusBehavior = OnFocusBehavior.FocusableAboveKeyboard
    }
) {
    App(appComponent)
}
