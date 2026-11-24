package com.vibe.build.engine.internal

import android.content.Context
import java.io.File

/**
 * Public entry point for first-run build environment preparation.
 *
 * Delegates to [PackagedBinaryResolver] to extract the packaged native build
 * tools (aapt2, zipalign) from the APK into the app's private storage, so the
 * first user-triggered build does not have to pay that cost.
 */
object BuildEnvironmentInstaller {

    fun prepareNativeTools(context: Context) {
        PackagedBinaryResolver.resolveAapt2(context)
    }
}
