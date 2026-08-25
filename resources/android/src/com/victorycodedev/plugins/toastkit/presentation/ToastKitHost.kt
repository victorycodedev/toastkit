package com.victorycodedev.plugins.toastkit.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import android.view.accessibility.AccessibilityManager
import android.content.Context
import android.provider.Settings
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nativephp.mobile.ui.MaterialIcon
import com.nativephp.mobile.ui.NativeUIThemeProvider
import com.victorycodedev.plugins.toastkit.manager.ToastKitManager
import com.victorycodedev.plugins.toastkit.model.*
import kotlin.math.abs

@Composable
internal fun ToastKitHost(position: ToastKitPosition) {
    val items = ToastKitManager.rendered.filter { it.position == position }
    val vertical = when (position) { ToastKitPosition.TOP -> Arrangement.Top; ToastKitPosition.CENTER -> Arrangement.Center; ToastKitPosition.BOTTOM -> Arrangement.Bottom }
    val safeArea = when (position) {
        ToastKitPosition.TOP -> Modifier.statusBarsPadding()
        ToastKitPosition.BOTTOM -> Modifier.navigationBarsPadding()
        ToastKitPosition.CENTER -> Modifier
    }
    Column(
        modifier = Modifier.fillMaxWidth().then(safeArea).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = vertical,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items.forEach { toast ->
            key(toast.id) { ToastKitCard(toast) }
        }
    }
}

@Composable
private fun ToastKitCard(toast: ToastKitConfiguration) {
    var x by remember(toast.id) { mutableFloatStateOf(0f) }
    var y by remember(toast.id) { mutableFloatStateOf(0f) }
    var settling by remember(toast.id) { mutableStateOf(false) }
    val velocityTracker = remember(toast.id) { VelocityTracker() }
    val displayedX by animateFloatAsState(x, if (settling) spring() else tween(0), label = "toast-x")
    val displayedY by animateFloatAsState(y, if (settling) spring() else tween(0), label = "toast-y")
    val accessibility = LocalContext.current.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    val animationsDisabled = Settings.Global.getFloat(LocalContext.current.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    val reduceMotion = animationsDisabled || (accessibility?.isEnabled == true && accessibility.isTouchExplorationEnabled)
    val gesture = if (!toast.swipeToDismiss) Modifier else Modifier.pointerInput(toast.id) {
        detectDragGestures(
            onDragStart = { velocityTracker.resetTracking() },
            onDragEnd = {
                val distance = if (toast.position == ToastKitPosition.CENTER) abs(x) else abs(y)
                val velocity = velocityTracker.calculateVelocity()
                val speed = if (toast.position == ToastKitPosition.CENTER) abs(velocity.x) else abs(velocity.y)
                if (distance > 80.dp.toPx() || speed > 900f) ToastKitManager.dismiss(toast.id, "swipe") else { settling = true; x = 0f; y = 0f }
            },
            onDragCancel = { settling = true; x = 0f; y = 0f },
        ) { change, amount ->
            change.consume()
            velocityTracker.addPosition(change.uptimeMillis, change.position)
            settling = false
            when (toast.position) {
                ToastKitPosition.TOP -> y = (y + amount.y).coerceAtMost(0f)
                ToastKitPosition.BOTTOM -> y = (y + amount.y).coerceAtLeast(0f)
                ToastKitPosition.CENTER -> x += amount.x
            }
        }
    }
    val enter = if (reduceMotion) fadeIn(tween(120)) else when (toast.animation) {
        ToastKitAnimation.FADE -> fadeIn(tween(180))
        ToastKitAnimation.SLIDE -> slideInVertically(tween(220)) { if (toast.position == ToastKitPosition.BOTTOM) it else -it } + fadeIn()
        ToastKitAnimation.SCALE -> scaleIn(tween(180), initialScale = .9f) + fadeIn()
        ToastKitAnimation.SPRING -> scaleIn(spring(), initialScale = .86f) + fadeIn()
    }
    val exit = if (reduceMotion) fadeOut(tween(100)) else when (toast.animation) {
        ToastKitAnimation.FADE -> fadeOut(tween(160))
        ToastKitAnimation.SLIDE -> slideOutVertically(tween(220)) { if (toast.position == ToastKitPosition.BOTTOM) it else -it } + fadeOut()
        ToastKitAnimation.SCALE -> scaleOut(tween(160), targetScale = .9f) + fadeOut()
        ToastKitAnimation.SPRING -> scaleOut(spring(), targetScale = .86f) + fadeOut(tween(220))
    }
    val visibility = remember(toast.id) { MutableTransitionState(false) }
    val shouldBeVisible = toast.id !in ToastKitManager.exiting
    LaunchedEffect(shouldBeVisible) { visibility.targetState = shouldBeVisible }
    AnimatedVisibility(visibleState = visibility, enter = enter, exit = exit) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp).widthIn(max = 560.dp).fillMaxWidth()
                .graphicsLayer { translationX = displayedX; translationY = displayedY; alpha = (1f - (abs(displayedX) + abs(displayedY)) / 700f).coerceIn(.35f, 1f) }
                .then(if (toast.style.shadow) Modifier.shadow(8.dp, RoundedCornerShape(toast.style.cornerRadius.dp)) else Modifier)
                .clip(RoundedCornerShape(toast.style.cornerRadius.dp)).background(toast.style.background)
                .padding(toast.style.padding.dp).then(gesture),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            toast.icon?.let {
                MaterialIcon(it.android ?: it.name ?: "circle", "Toast icon", size = 24.dp, tint = toast.style.iconColor)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                toast.title?.let { title ->
                    Text(
                        text = title,
                        color = toast.style.foreground,
                        fontSize = toast.titleText?.size?.let(::toastTextSize) ?: 15.sp,
                        fontWeight = toast.titleText?.weight?.let(::toastTextWeight) ?: FontWeight.SemiBold,
                        fontStyle = toast.titleText?.italic?.let { if (it) FontStyle.Italic else FontStyle.Normal },
                        fontFamily = toast.titleText?.font?.let(::toastFontFamily),
                        textAlign = toast.titleText?.align?.let(::toastTextAlign) ?: TextAlign.Left,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    text = toast.message,
                    color = toast.style.foreground,
                    fontSize = toast.text?.size?.let(::toastTextSize) ?: toastTextSize(ToastKitTextSize.BASE),
                    fontWeight = toast.text?.weight?.let(::toastTextWeight) ?: FontWeight.Medium,
                    fontStyle = toast.text?.italic?.let { if (it) FontStyle.Italic else FontStyle.Normal },
                    fontFamily = toast.text?.font?.let(::toastFontFamily),
                    textAlign = toast.text?.align?.let(::toastTextAlign) ?: TextAlign.Left,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            toast.action?.let { action ->
                Text(action.label, color = toast.style.actionColor, fontWeight = FontWeight.Bold,
                    modifier = Modifier.pointerInput(action.id) { detectTapGestures { ToastKitManager.action(toast.id, action.id) } })
            }
            if (toast.dismissible) {
                Box(Modifier.size(32.dp).pointerInput(toast.id) { detectTapGestures { ToastKitManager.dismiss(toast.id) } }, contentAlignment = Alignment.Center) {
                    MaterialIcon("close", "Dismiss", size = 20.dp, tint = toast.style.foreground)
                }
            }
        }
    }
}

private fun toastTextSize(size: ToastKitTextSize) = when (size) {
    ToastKitTextSize.XS -> 11.sp
    ToastKitTextSize.SM -> 13.sp
    ToastKitTextSize.BASE -> 14.sp
    ToastKitTextSize.LG -> 16.sp
    ToastKitTextSize.XL -> 18.sp
}

private fun toastTextWeight(weight: ToastKitTextWeight) = when (weight) {
    ToastKitTextWeight.NORMAL -> FontWeight.Normal
    ToastKitTextWeight.MEDIUM -> FontWeight.Medium
    ToastKitTextWeight.SEMIBOLD -> FontWeight.SemiBold
    ToastKitTextWeight.BOLD -> FontWeight.Bold
}

private fun toastTextAlign(align: ToastKitTextAlign) = when (align) {
    ToastKitTextAlign.LEFT -> TextAlign.Left
    ToastKitTextAlign.CENTER -> TextAlign.Center
    ToastKitTextAlign.RIGHT -> TextAlign.Right
}

private fun toastFontFamily(font: String): FontFamily? =
    NativeUIThemeProvider.resolveChromeFontFamily(font)
