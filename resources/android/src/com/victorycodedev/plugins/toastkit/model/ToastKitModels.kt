package com.victorycodedev.plugins.toastkit.model

import androidx.compose.ui.graphics.Color

internal enum class ToastKitPosition { TOP, CENTER, BOTTOM }
internal enum class ToastKitAnimation { FADE, SLIDE, SCALE, SPRING, SNAP, POP, REVEAL, BOUNCE }
internal enum class ToastKitDirection { AUTO, LEFT, RIGHT, TOP, BOTTOM }
internal enum class ToastKitStrategy { QUEUE, STACK }
internal enum class ToastKitTextSize { XS, SM, BASE, LG, XL }
internal enum class ToastKitTextWeight { NORMAL, MEDIUM, SEMIBOLD, BOLD }
internal enum class ToastKitTextAlign { LEFT, CENTER, RIGHT }

internal data class ToastKitIcon(val name: String?, val android: String?)
internal data class ToastKitAction(val id: String, val label: String)
internal data class ToastKitTypography(
    val font: String? = null,
    val size: ToastKitTextSize? = null,
    val weight: ToastKitTextWeight? = null,
    val align: ToastKitTextAlign? = null,
    val italic: Boolean? = null,
)
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
    val uniqueKey: String?,
    val message: String,
    val title: String?,
    val variant: String,
    val icon: ToastKitIcon?,
    val position: ToastKitPosition,
    val durationMs: Long?,
    val animation: ToastKitAnimation,
    val direction: ToastKitDirection,
    val progress: Float?,
    val loading: Boolean,
    val swipeToDismiss: Boolean,
    val dismissible: Boolean,
    val action: ToastKitAction?,
    val style: ToastKitStyle,
    val strategy: ToastKitStrategy,
    val maxVisible: Int,
    val text: ToastKitTypography? = null,
    val titleText: ToastKitTypography? = null,
)

internal data class ToastKitState(
    val configuration: ToastKitConfiguration,
    val visible: Boolean = false,
    val terminated: Boolean = false,
)
