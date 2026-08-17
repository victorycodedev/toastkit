package com.victorycodedev.plugins.toastkit

import android.content.Context
import com.victorycodedev.plugins.toastkit.presentation.ToastKitHostInstaller

fun initializeToastKit(context: Context) {
    ToastKitHostInstaller.initialize(context)
}
