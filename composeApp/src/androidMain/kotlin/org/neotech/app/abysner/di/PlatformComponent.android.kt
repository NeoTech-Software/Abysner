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

package org.neotech.app.abysner.di

import android.content.Context
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import org.neotech.app.abysner.data.PlatformFileDataSource
import org.neotech.app.abysner.data.PlatformFileDataSourceImpl

@AppScope
@Component
abstract class PlatformComponentImpl(private val applicationContext: Context): PlatformComponent() {

    val providesApplicationContext: Context
       @AppScope @Provides get() = applicationContext

    @AppScope
    @Provides
    fun providesPlatformFileDataSource(applicationContext: Context): PlatformFileDataSource = PlatformFileDataSourceImpl(applicationContext)
}
