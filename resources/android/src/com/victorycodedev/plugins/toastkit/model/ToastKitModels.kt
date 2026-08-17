package com.victorycodedev.plugins.toastkit.model

import androidx.compose.ui.graphics.Color

internal enum class ToastKitPosition { TOP, CENTER, BOTTOM }
internal enum class ToastKitAnimation { FADE, SLIDE, SCALE, SPRING }
internal enum class ToastKitStrategy { QUEUE, STACK }

internal data class ToastKitIcon(val name: String?, val android: String?)
internal data class ToastKitAction(val id: String, val label: String)
internal data class ToastKitStyle(
    val background: Color,
    val foreground: Color,
    val iconColor: Color,
    val actionColor: Color,
    val cornerRadius: Float,
    val padding: Float,
    val shadow: Boolean,
)

internal data class ToastKitConfiguration(
    val id: String,
    val message: String,
    val title: String?,
    val variant: String,
    val icon: ToastKitIcon?,
    val position: ToastKitPosition,
    val durationMs: Long?,
    val animation: ToastKitAnimation,
    val swipeToDismiss: Boolean,
    val dismissible: Boolean,
    val action: ToastKitAction?,
    val style: ToastKitStyle,
    val strategy: ToastKitStrategy,
    val maxVisible: Int,
)

internal data class ToastKitState(
    val configuration: ToastKitConfiguration,
    val visible: Boolean = false,
    val terminated: Boolean = false,
)
