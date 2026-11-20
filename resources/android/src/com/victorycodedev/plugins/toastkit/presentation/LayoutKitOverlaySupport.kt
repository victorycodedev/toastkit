package com.victorycodedev.plugins.toastkit.presentation

import android.app.Activity

/** Keeps LayoutKit integration optional so existing ToastKit apps retain their original host. */
internal object LayoutKitOverlaySupport {
    private const val REGISTRY = "com.victorycodedev.plugins.layoutkit.ui.LayoutKitOverlayRegistry"

    fun isAvailable(activity: Activity): Boolean = runCatching {
        Class.forName(REGISTRY, false, activity.classLoader)
    }.isSuccess
}
