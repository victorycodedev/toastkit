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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
    val displayedProgress by animateFloatAsState(
        targetValue = (toast.progress ?: 0f) / 100f,
        animationSpec = if (reduceMotion) tween(100) else tween(220),
        label = "toast-progress",
    )
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
        ToastKitAnimation.SLIDE -> directionalEnter(toast, tween(220)) + fadeIn()
        ToastKitAnimation.SCALE -> scaleIn(tween(180), initialScale = .9f) + fadeIn()
        ToastKitAnimation.SPRING -> scaleIn(spring(), initialScale = .86f) + fadeIn()
        ToastKitAnimation.SNAP -> directionalEnter(toast, spring(dampingRatio = .68f, stiffness = 950f)) + fadeIn(tween(90))
        ToastKitAnimation.POP -> scaleIn(spring(dampingRatio = .55f, stiffness = 650f), initialScale = .72f) + fadeIn(tween(100))
        ToastKitAnimation.REVEAL -> expandHorizontally(tween(240), expandFrom = revealAlignment(toast)) + fadeIn(tween(160))
        ToastKitAnimation.BOUNCE -> directionalEnter(toast, spring(dampingRatio = .45f, stiffness = 360f)) + fadeIn(tween(120))
    }
    val exit = if (reduceMotion) fadeOut(tween(100)) else when (toast.animation) {
        ToastKitAnimation.FADE -> fadeOut(tween(160))
        ToastKitAnimation.SLIDE -> directionalExit(toast, tween(220)) + fadeOut()
        ToastKitAnimation.SCALE -> scaleOut(tween(160), targetScale = .9f) + fadeOut()
        ToastKitAnimation.SPRING -> scaleOut(spring(), targetScale = .86f) + fadeOut(tween(220))
        ToastKitAnimation.SNAP -> directionalExit(toast, tween(110)) + fadeOut(tween(90))
        ToastKitAnimation.POP -> scaleOut(tween(150), targetScale = .72f) + fadeOut(tween(120))
        ToastKitAnimation.REVEAL -> shrinkHorizontally(tween(210), shrinkTowards = revealAlignment(toast)) + fadeOut(tween(150))
        ToastKitAnimation.BOUNCE -> directionalExit(toast, tween(220)) + fadeOut(tween(160))
    }
    val visibility = remember(toast.id) { MutableTransitionState(false) }
    val shouldBeVisible = toast.id !in ToastKitManager.exiting
    LaunchedEffect(shouldBeVisible) { visibility.targetState = shouldBeVisible }
    AnimatedVisibility(visibleState = visibility, enter = enter, exit = exit) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp).widthIn(max = 560.dp).fillMaxWidth()
                .graphicsLayer { translationX = displayedX; translationY = displayedY; alpha = (1f - (abs(displayedX) + abs(displayedY)) / 700f).coerceIn(.35f, 1f) }
                .then(if (toast.style.shadow) Modifier.shadow(8.dp, RoundedCornerShape(toast.style.cornerRadius.dp)) else Modifier)
                .clip(RoundedCornerShape(toast.style.cornerRadius.dp)).background(toast.style.background)
                .padding(toast.style.padding.dp).then(gesture),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (toast.progress == null && toast.loading) {
                CircularProgressIndicator(Modifier.size(22.dp), color = toast.style.iconColor, strokeWidth = 2.5.dp)
            } else toast.icon?.let {
                MaterialIcon(it.android ?: it.name ?: "circle", "Toast icon", size = 24.dp, tint = toast.style.iconColor)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                toast.title?.let { title ->
                    Text(
                        text = title,
                        color = toast.style.foreground,
                        fontSize = toast.titleText?.size?.let(::toastTextSize) ?: 16.sp,
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
          toast.progress?.let {
              Spacer(Modifier.height(10.dp))
              LinearProgressIndicator(
                  progress = { displayedProgress },
                  modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                  color = toast.style.actionColor,
                  trackColor = toast.style.foreground.copy(alpha = .22f),
              )
          }
        }
    }
}

private fun resolvedDirection(toast: ToastKitConfiguration): ToastKitDirection = when (toast.direction) {
    ToastKitDirection.AUTO -> when (toast.position) {
        ToastKitPosition.TOP -> ToastKitDirection.TOP
        ToastKitPosition.BOTTOM -> ToastKitDirection.BOTTOM
        ToastKitPosition.CENTER -> ToastKitDirection.TOP
    }
    else -> toast.direction
}

private fun directionalEnter(toast: ToastKitConfiguration, spec: androidx.compose.animation.core.FiniteAnimationSpec<androidx.compose.ui.unit.IntOffset>): EnterTransition =
    when (resolvedDirection(toast)) {
        ToastKitDirection.LEFT -> slideInHorizontally(spec) { -it }
        ToastKitDirection.RIGHT -> slideInHorizontally(spec) { it }
        ToastKitDirection.TOP -> slideInVertically(spec) { -it }
        else -> slideInVertically(spec) { it }
    }

private fun directionalExit(toast: ToastKitConfiguration, spec: androidx.compose.animation.core.FiniteAnimationSpec<androidx.compose.ui.unit.IntOffset>): ExitTransition =
    when (resolvedDirection(toast)) {
        ToastKitDirection.LEFT -> slideOutHorizontally(spec) { -it }
        ToastKitDirection.RIGHT -> slideOutHorizontally(spec) { it }
        ToastKitDirection.TOP -> slideOutVertically(spec) { -it }
        else -> slideOutVertically(spec) { it }
    }

private fun revealAlignment(toast: ToastKitConfiguration): Alignment.Horizontal = when (resolvedDirection(toast)) {
    ToastKitDirection.RIGHT -> Alignment.End
    else -> Alignment.Start
}

private fun toastTextSize(size: ToastKitTextSize) = when (size) {
    ToastKitTextSize.XS -> 12.sp
    ToastKitTextSize.SM -> 14.sp
    ToastKitTextSize.BASE -> 15.sp
    ToastKitTextSize.LG -> 17.sp
    ToastKitTextSize.XL -> 19.sp
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
