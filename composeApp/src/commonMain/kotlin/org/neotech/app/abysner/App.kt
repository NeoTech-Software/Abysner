package org.neotech.app.abysner/*
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

import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.neotech.app.abysner.di.AppComponent

@Composable
@Preview
fun App(appComponent: AppComponent) {
    appComponent.mainNavController()
}
