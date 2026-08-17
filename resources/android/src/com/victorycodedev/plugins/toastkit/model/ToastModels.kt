package com.victorycodedev.plugins.toastkit.model

import androidx.compose.ui.graphics.Color

internal enum class ToastPosition { TOP, CENTER, BOTTOM }
internal enum class ToastAnimation { FADE, SLIDE, SCALE, SPRING }
internal enum class ToastStrategy { QUEUE, STACK }

internal data class ToastIcon(val name: String?, val android: String?)
internal data class ToastAction(val id: String, val label: String)
internal data class ToastStyle(
    val background: Color,
    val foreground: Color,
    val iconColor: Color,
    val actionColor: Color,
    val cornerRadius: Float,
    val padding: Float,
    val shadow: Boolean,
)

internal data class ToastConfiguration(
    val id: String,
    val message: String,
    val title: String?,
    val variant: String,
    val icon: ToastIcon?,
    val position: ToastPosition,
    val durationMs: Long?,
    val animation: ToastAnimation,
    val swipeToDismiss: Boolean,
    val dismissible: Boolean,
    val action: ToastAction?,
    val style: ToastStyle,
    val strategy: ToastStrategy,
    val maxVisible: Int,
)

internal data class ToastState(
    val configuration: ToastConfiguration,
    val visible: Boolean = false,
    val terminated: Boolean = false,
)
