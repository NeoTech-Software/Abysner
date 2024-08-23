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

package org.neotech.app.abysner.presentation.utilities

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import org.neotech.app.abysner.applicationContext
import org.neotech.app.abysner.currentActivity
import java.io.File
import java.io.FileOutputStream

actual fun shareImageBitmap(image: ImageBitmap) {
    val bitmap = image.asAndroidBitmap()

    val file = File(applicationContext.cacheDir, "share/shared-image.png")
    file.parentFile?.mkdirs()
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    val uri: Uri = FileProvider.getUriForFile(
        applicationContext,
        "${applicationContext.packageName}.provider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    currentActivity.get()!!.startActivity(Intent.createChooser(intent, "Share Image"))
}
