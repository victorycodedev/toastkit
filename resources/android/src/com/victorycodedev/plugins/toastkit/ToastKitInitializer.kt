package com.victorycodedev.plugins.toastkit

import android.content.Context
import com.victorycodedev.plugins.toastkit.presentation.ToastHostInstaller

fun initializeToastKit(context: Context) {
    ToastHostInstaller.initialize(context)
}
