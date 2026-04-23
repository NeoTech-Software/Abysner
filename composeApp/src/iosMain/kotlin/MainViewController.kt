/*
 * Abysner - Dive planner
 * Copyright (C) 2024-2026 Neotech
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
import dev.zacsweers.metro.createGraphFactory
import org.neotech.app.abysner.App
import org.neotech.app.abysner.data.PlatformFileDataSourceImpl
import org.neotech.app.abysner.di.AppComponent

// Metro graphs cannot have constructor parameters, and platform source sets cannot extend the
// shared graph with additional bindings (only the reverse direction is supported via
// @GraphExtension it seems). So platform dependencies must be constructed manually and passed
// through a @DependencyGraph.Factory. Currently, there is only one (PlatformFileDataSource), but
// if more platform-specific bindings are added this might get messy? Each one needs a factory
// parameter, a @Provides function in AppComponent, and manual construction at every call site
// (Android, iOS, JVM). kotlin-inject avoided this through component inheritance.
private val appComponent = createGraphFactory<AppComponent.Factory>().create(PlatformFileDataSourceImpl())

@Suppress("unused", "FunctionName")
fun MainViewController() = ComposeUIViewController(
    configure = {
        onFocusBehavior = OnFocusBehavior.FocusableAboveKeyboard
    }
) {
    App(appComponent)
}
