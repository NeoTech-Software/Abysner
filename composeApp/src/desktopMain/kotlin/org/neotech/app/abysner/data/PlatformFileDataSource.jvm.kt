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

package org.neotech.app.abysner.data

import ca.gosyer.appdirs.AppDirs
import me.tatarka.inject.annotations.Inject
import okio.Path
import okio.Path.Companion.toPath
import org.neotech.app.abysner.di.AppScope

@AppScope
@Inject
class PlatformFileDataSourceImpl: PlatformFileDataSource {

    private val appDirs = AppDirs("Abysner", "Neotech")
    private val userData = appDirs.getUserDataDir()

    override fun getPrivateFileStoragePath(): Path {
        return userData.toPath()
    }
}
