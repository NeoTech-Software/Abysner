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

import App
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import org.neotech.app.abysner.di.AppComponent
import org.neotech.app.abysner.di.PlatformComponentImpl
import org.neotech.app.abysner.di.create
import java.lang.ref.WeakReference

class MainActivity : ComponentActivity() {

    init {
        currentActivity = WeakReference(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Code below for easier debugging of the splash screen
        /*
        var showSplashScreen = true
        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    return if (!showSplashScreen) {
                        // Hide splash screen, start drawing.
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else {
                        false
                    }
                }
            }
        )
        content.postDelayed({ showSplashScreen = false }, 1000)
         */

        setContent {
            App((application as AbysnerApplication).appComponent())
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(AppComponent::class.create(PlatformComponentImpl::class.create(LocalContext.current)))
}
