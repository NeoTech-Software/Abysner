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

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.neotech.app.abysner.App
import org.neotech.app.abysner.di.AppComponent
import org.neotech.app.abysner.di.PlatformComponentImpl
import org.neotech.app.abysner.di.create

private val appComponent = AppComponent::class.create(PlatformComponentImpl::class.create())

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Abysner - The open-source dive planner",
    ) {
        App(appComponent)
    }
}
