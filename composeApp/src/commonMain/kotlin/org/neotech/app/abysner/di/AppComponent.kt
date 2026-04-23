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

package org.neotech.app.abysner.di

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.neotech.app.abysner.data.PersistenceRepositoryImpl
import org.neotech.app.abysner.data.diveplanning.PlanningRepositoryImpl
import org.neotech.app.abysner.data.PlatformFileDataSource
import org.neotech.app.abysner.data.settings.SettingsRepositoryImpl
import org.neotech.app.abysner.domain.diveplanning.PlanningRepository
import org.neotech.app.abysner.domain.persistence.PersistenceRepository
import org.neotech.app.abysner.domain.settings.SettingsRepository
import org.neotech.app.abysner.presentation.MainNavController

abstract class AppScope

@SingleIn(AppScope::class)
@DependencyGraph
abstract class AppComponent {

    abstract val mainNavController: MainNavController

    @SingleIn(AppScope::class)
    @Provides
    fun providesPlanningRepository(planningRepository: PlanningRepositoryImpl): PlanningRepository = planningRepository

    @SingleIn(AppScope::class)
    @Provides
    fun providesSettingsRepository(settingsRepository: SettingsRepositoryImpl): SettingsRepository = settingsRepository

    @SingleIn(AppScope::class)
    @Provides
    fun providesPersistenceRepository(persistenceRepository: PersistenceRepositoryImpl): PersistenceRepository = persistenceRepository

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides platformFileDataSource: PlatformFileDataSource): AppComponent
    }
}
