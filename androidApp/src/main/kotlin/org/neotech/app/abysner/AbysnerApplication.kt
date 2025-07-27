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

package org.neotech.app.abysner

import android.app.Application
import org.neotech.app.abysner.di.AppComponent
import org.neotech.app.abysner.di.PlatformComponentImpl
import org.neotech.app.abysner.di.create

class AbysnerApplication: Application() {

    private lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        val platformComponent = PlatformComponentImpl::class.create(this.applicationContext)
        appComponent = AppComponent::class.create(platformComponent)
    }

    fun appComponent(): AppComponent = appComponent
}

