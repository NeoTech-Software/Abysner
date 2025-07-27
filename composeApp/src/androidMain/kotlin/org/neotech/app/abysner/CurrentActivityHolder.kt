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

import android.app.Activity
import java.lang.ref.WeakReference

// This is required for closeApp and shareImage functionality.
// Set by MainActivity in the androidApp module.
var currentActivity: WeakReference<Activity> = WeakReference(null)

