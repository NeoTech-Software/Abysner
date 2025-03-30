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

import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope
import org.neotech.app.abysner.data.PersistenceRepositoryImpl
import org.neotech.app.abysner.data.diveplanning.PlanningRepositoryImpl
import org.neotech.app.abysner.data.PlatformFileDataSource
import org.neotech.app.abysner.data.settings.SettingsRepositoryImpl
import org.neotech.app.abysner.domain.diveplanning.PlanningRepository
import org.neotech.app.abysner.domain.persistence.PersistenceRepository
import org.neotech.app.abysner.domain.settings.SettingsRepository
import org.neotech.app.abysner.presentation.MainNavController

@Scope
annotation class AppScope

@AppScope
@Component
abstract class AppComponent(@Component val platformComponent: PlatformComponent) {

    abstract val mainNavController: MainNavController

    @AppScope
    @Provides
    fun providesPlanningRepository(planningRepository: PlanningRepositoryImpl): PlanningRepository = planningRepository

    @AppScope
    @Provides
    fun providesSettingsRepository(settingsRepository: SettingsRepositoryImpl): SettingsRepository = settingsRepository

    @AppScope
    @Provides
    fun providesPersistenceRepository(persistenceRepository: PersistenceRepositoryImpl): PersistenceRepository = persistenceRepository

}

abstract class PlatformComponent {

    abstract val providesPlatformFileDataSource: PlatformFileDataSource
}
