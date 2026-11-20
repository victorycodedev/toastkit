package com.victorycodedev.plugins.toastkit.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import android.view.accessibility.AccessibilityManager
import android.content.Context
import android.provider.Settings
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.nativephp.mobile.ui.MaterialIcon
import com.nativephp.mobile.ui.NativeUIThemeProvider
import com.victorycodedev.plugins.toastkit.manager.ToastKitManager
import com.victorycodedev.plugins.toastkit.model.*
import kotlin.math.abs
import kotlinx.coroutines.delay

@Composable
internal fun ToastKitHost(
    position: ToastKitPosition,
    includesWindowInsets: Boolean = true,
    animationsReady: Boolean = true,
    onContentVisibilityChanged: ((Boolean) -> Unit)? = null,
    keyboardHeight: Int = 0,
    onCardBounds: (String, Rect?) -> Unit = { _, _ -> },
) {
    val items = ToastKitManager.rendered.filter { effectivePosition(it) == position }
    val addedShadowOutset = with(LocalDensity.current) { 8.dp.toPx() }
    LaunchedEffect(items.isNotEmpty()) {
        onContentVisibilityChanged?.invoke(items.isNotEmpty())
    }
    val vertical = when (position) { ToastKitPosition.TOP -> Arrangement.Top; ToastKitPosition.CENTER -> Arrangement.Center; ToastKitPosition.BOTTOM -> Arrangement.Bottom }
    val safeArea = when (position) {
        ToastKitPosition.TOP -> Modifier.statusBarsPadding()
        ToastKitPosition.BOTTOM -> Modifier.windowInsetsPadding(
            WindowInsets.navigationBars.union(WindowInsets(bottom = keyboardHeight)),
        )
        ToastKitPosition.CENTER -> Modifier
    }
    val hostInsets = if (includesWindowInsets) {
        safeArea
    } else if (position == ToastKitPosition.BOTTOM && keyboardHeight > 0) {
        Modifier.windowInsetsPadding(WindowInsets(bottom = keyboardHeight))
    } else {
        Modifier
    }
    val edgePadding = when (position) {
        ToastKitPosition.TOP -> Modifier
        ToastKitPosition.CENTER -> Modifier.padding(vertical = 12.dp)
        ToastKitPosition.BOTTOM -> Modifier
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 1.dp)
            .then(hostInsets)
            .padding(horizontal = 16.dp)
            .then(edgePadding),
        verticalArrangement = vertical,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items.forEach { toast ->
            key(toast.id) {
                DisposableEffect(toast.id) {
                    onDispose { onCardBounds(toast.id, null) }
                }
                ToastKitCard(
                    toast,
                    animationsReady,
                    Modifier.onGloballyPositioned { coordinates ->
                        val bounds = coordinates.boundsInWindow()
                        onCardBounds(
                            toast.id,
                            Rect(
                                bounds.left,
                                bounds.top + addedShadowOutset,
                                bounds.right,
                                bounds.bottom - addedShadowOutset,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ToastKitCard(
    toast: ToastKitConfiguration,
    animationsReady: Boolean,
    modifier: Modifier = Modifier,
) {
    val position = effectivePosition(toast)
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
    val gesture = if (!toast.swipeToDismiss) Modifier else Modifier.pointerInput(toast.id, position) {
        detectDragGestures(
            onDragStart = { velocityTracker.resetTracking() },
            onDragEnd = {
                val distance = if (position == ToastKitPosition.CENTER) abs(x) else abs(y)
                val velocity = velocityTracker.calculateVelocity()
                val speed = if (position == ToastKitPosition.CENTER) abs(velocity.x) else abs(velocity.y)
                if (distance > 80.dp.toPx() || speed > 900f) ToastKitManager.dismiss(toast.id, "swipe") else { settling = true; x = 0f; y = 0f }
            },
            onDragCancel = { settling = true; x = 0f; y = 0f },
        ) { change, amount ->
            change.consume()
            velocityTracker.addPosition(change.uptimeMillis, change.position)
            settling = false
            when (position) {
                ToastKitPosition.TOP -> y = (y + amount.y).coerceAtMost(0f)
                ToastKitPosition.BOTTOM -> y = (y + amount.y).coerceAtLeast(0f)
                ToastKitPosition.CENTER -> x += amount.x
            }
        }
    }
    val usesNativeStandard = toast.nativeAndroid && toast.presentation == ToastKitPresentation.STANDARD
    // A WindowManager host is initially transparent while its first frame is
    // prepared. Do not consume the enter transition behind that transparent
    // window; start it only after the host is actually visible.
    val shouldBeVisible = animationsReady && toast.id !in ToastKitManager.exiting
    val simplifiedMotion = reduceMotion || usesNativeStandard
    val transition = updateTransition(targetState = shouldBeVisible, label = "toast-visibility")
    val visibilityAlpha by transition.animateFloat(
        transitionSpec = { alphaSpec(toast.animation, targetState, simplifiedMotion) },
        label = "toast-alpha",
    ) { visible -> if (visible) 1f else 0f }
    val motionAmount by transition.animateFloat(
        transitionSpec = { motionSpec(toast.animation, simplifiedMotion) },
        label = "toast-motion",
    ) { visible -> if (visible || !toast.animation.isDirectional() || simplifiedMotion) 0f else 1f }
    val scaleAmount by transition.animateFloat(
        transitionSpec = { scaleSpec(toast.animation, targetState, simplifiedMotion) },
        label = "toast-scale",
    ) { visible -> if (visible || simplifiedMotion) 1f else toast.animation.hiddenScale() }
    val revealAmount by transition.animateFloat(
        transitionSpec = { tween(if (targetState) 240 else 210) },
        label = "toast-reveal",
    ) { visible -> if (visible || toast.animation != ToastKitAnimation.REVEAL || simplifiedMotion) 1f else .01f }
    var cardSize by remember(toast.id) { mutableStateOf(IntSize.Zero) }

    // Keep the full card laid out even while visually hidden. A collapsing
    // AnimatedVisibility changes a WRAP_CONTENT window's height and makes every
    // bottom toast appear to slide, regardless of its selected animation.
    Box(modifier = modifier) {
        // The activity-scoped host is only as tall as its content. Leave enough
        // transparent space for elevation blur so the window edge does not cut
        // the shadow into a hard rectangle.
        val interaction = Modifier
                .padding(vertical = 12.dp)
                .widthIn(max = 560.dp)
                .then(if (toast.presentation == ToastKitPresentation.STANDARD) Modifier.fillMaxWidth() else Modifier)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .then(
                    if (toast.data.isNotEmpty()) Modifier.clickable(onClickLabel = "Open notification") {
                        ToastKitManager.press(toast.id)
                    } else Modifier,
                )
                .onSizeChanged { cardSize = it }
                .graphicsLayer {
                    val direction = resolvedDirection(toast)
                    val width = cardSize.width.toFloat()
                    val height = cardSize.height.toFloat()
                    translationX = displayedX + when (direction) {
                        ToastKitDirection.LEFT -> -width * motionAmount
                        ToastKitDirection.RIGHT -> width * motionAmount
                        else -> 0f
                    }
                    translationY = displayedY + when (direction) {
                        ToastKitDirection.TOP -> -height * motionAmount
                        ToastKitDirection.BOTTOM -> height * motionAmount
                        else -> 0f
                    }
                    scaleX = scaleAmount
                    scaleY = scaleAmount
                    if (toast.animation == ToastKitAnimation.REVEAL && !simplifiedMotion) {
                        clip = true
                        when (direction) {
                            ToastKitDirection.LEFT -> {
                                scaleX = revealAmount
                                transformOrigin = TransformOrigin(0f, .5f)
                            }
                            ToastKitDirection.RIGHT -> {
                                scaleX = revealAmount
                                transformOrigin = TransformOrigin(1f, .5f)
                            }
                            ToastKitDirection.TOP -> {
                                scaleY = revealAmount
                                transformOrigin = TransformOrigin(.5f, 0f)
                            }
                            else -> {
                                scaleY = revealAmount
                                transformOrigin = TransformOrigin(.5f, 1f)
                            }
                        }
                    }
                    alpha = visibilityAlpha *
                        (1f - (abs(displayedX) + abs(displayedY)) / 700f).coerceIn(.35f, 1f)
                }
                .then(toast.opacity?.let { Modifier.alpha(it) } ?: Modifier)
                .then(gesture)
        if (toast.presentation == ToastKitPresentation.DYNAMIC) {
            DynamicToastKitCard(toast, displayedProgress, interaction, reduceMotion, shouldBeVisible)
        } else if (toast.nativeAndroid) {
            NativeToastKitCard(toast, displayedProgress, interaction, reduceMotion)
        } else {
        Column(
            modifier = interaction
                .then(if (toast.style.shadow) Modifier.shadow(8.dp, RoundedCornerShape(toast.style.cornerRadius.dp)) else Modifier)
                .clip(RoundedCornerShape(toast.style.cornerRadius.dp)).background(toast.style.background)
                .padding(toast.style.padding.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (toast.progress == null && toast.loading) {
                CircularProgressIndicator(Modifier.size(22.dp), color = toast.style.iconColor, strokeWidth = 2.5.dp)
            } else toast.icon?.let {
                MaterialIcon(it.android ?: it.name ?: "circle", "Toast icon", size = toastIconSize(toast.iconSize, 24).dp, tint = toast.style.iconColor)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AnimatedContent(
                    targetState = toast.title,
                    transitionSpec = { fadeIn(tween(if (reduceMotion) 1 else 140)) togetherWith fadeOut(tween(if (reduceMotion) 1 else 100)) },
                    label = "toast-title-update",
                ) { displayedTitle ->
                    displayedTitle?.let { Text(
                        text = displayedTitle,
                        color = toast.style.foreground,
                        fontSize = toast.titleText?.size?.let(::toastTextSize) ?: 16.sp,
                        fontWeight = toast.titleText?.weight?.let(::toastTextWeight) ?: FontWeight.SemiBold,
                        fontStyle = toast.titleText?.italic?.let { if (it) FontStyle.Italic else FontStyle.Normal },
                        fontFamily = toast.titleText?.font?.let(::toastFontFamily),
                        textAlign = toast.titleText?.align?.let(::toastTextAlign) ?: TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                    ) }
                }
                AnimatedContent(
                    targetState = toast.message,
                    transitionSpec = { fadeIn(tween(if (reduceMotion) 1 else 140)) togetherWith fadeOut(tween(if (reduceMotion) 1 else 100)) },
                    label = "toast-message-update",
                ) { displayedMessage -> Text(
                    text = displayedMessage,
                    color = toast.style.foreground,
                    fontSize = toast.text?.size?.let(::toastTextSize) ?: toastTextSize(ToastKitTextSize.BASE),
                    fontWeight = toast.text?.weight?.let(::toastTextWeight) ?: FontWeight.Medium,
                    fontStyle = toast.text?.italic?.let { if (it) FontStyle.Italic else FontStyle.Normal },
                    fontFamily = toast.text?.font?.let(::toastFontFamily),
                    textAlign = toast.text?.align?.let(::toastTextAlign) ?: TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                ) }
            }
            toast.actions.forEach { action ->
                TextButton(
                    onClick = { ToastKitManager.action(toast.id, action.id) },
                    colors = ButtonDefaults.textButtonColors(contentColor = action.color ?: toast.style.actionColor),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) { Text(action.label, fontWeight = FontWeight.Bold) }
            }
            if (toast.dismissible) {
                IconButton(onClick = { ToastKitManager.dismiss(toast.id) }) {
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
}

@Composable
private fun DynamicToastKitCard(
    toast: ToastKitConfiguration,
    displayedProgress: Float,
    modifier: Modifier,
    reduceMotion: Boolean,
    visible: Boolean,
) {
    var expanded by remember(toast.id) { mutableStateOf(false) }
    var dropExtended by remember(toast.id) { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            if (reduceMotion) {
                dropExtended = false
                expanded = true
                return@LaunchedEffect
            }
            dropExtended = true
            delay(70)
            expanded = true
            delay(180)
            dropExtended = false
        } else {
            expanded = false
            if (!reduceMotion) delay(45)
            dropExtended = false
        }
    }
    val dropScale by animateFloatAsState(
        targetValue = if (dropExtended) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 1 else 140),
        label = "dynamic-toast-drop",
    )
    val expansion by animateFloatAsState(
        targetValue = if (expanded) 1f else .18f,
        animationSpec = tween(if (reduceMotion) 1 else 220),
        label = "dynamic-toast-expansion",
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 1 else 150),
        label = "dynamic-toast-content",
    )

    Box(
        modifier = modifier.fillMaxWidth().padding(top = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .offset(y = (-20).dp)
                .size(width = 20.dp, height = 26.dp)
                .graphicsLayer {
                    scaleY = dropScale
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
                .background(toast.style.background, DynamicToastDropShape),
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = expansion
                    scaleY = .82f + (.18f * expansion)
                    transformOrigin = TransformOrigin(0.5f, 0f)
                },
            shape = RoundedCornerShape(toast.style.cornerRadius.dp),
            color = toast.style.background,
            contentColor = toast.style.foreground,
            shadowElevation = if (toast.style.shadow) 10.dp else 0.dp,
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(toast.style.padding.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (toast.progress == null && toast.loading) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = toast.style.iconColor, strokeWidth = 2.5.dp)
                    } else {
                        val icon = toast.icon
                        MaterialIcon(
                            icon?.android ?: icon?.name ?: "notifications",
                            "Toast notification",
                            size = toastIconSize(toast.iconSize, 22).dp,
                            tint = toast.style.iconColor,
                        )
                    }

                    Row(
                        modifier = Modifier.weight(1f).alpha(contentAlpha),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            AnimatedContent(
                                targetState = toast.title,
                                transitionSpec = { fadeIn(tween(if (reduceMotion) 1 else 140)) togetherWith fadeOut(tween(if (reduceMotion) 1 else 100)) },
                                label = "dynamic-toast-title-update",
                            ) { title ->
                                title?.let { Text(
                                    title,
                                    color = toast.style.foreground,
                                    fontSize = toast.titleText?.size?.let(::toastTextSize) ?: 16.sp,
                                    fontWeight = toast.titleText?.weight?.let(::toastTextWeight) ?: FontWeight.SemiBold,
                                    fontStyle = toast.titleText?.italic?.let { italic -> if (italic) FontStyle.Italic else FontStyle.Normal },
                                    fontFamily = toast.titleText?.font?.let(::toastFontFamily),
                                    maxLines = 1,
                                ) }
                            }
                            AnimatedContent(
                                targetState = toast.message,
                                transitionSpec = { fadeIn(tween(if (reduceMotion) 1 else 140)) togetherWith fadeOut(tween(if (reduceMotion) 1 else 100)) },
                                label = "dynamic-toast-message-update",
                            ) { message -> Text(
                                message,
                                color = toast.style.foreground,
                                fontSize = toast.text?.size?.let(::toastTextSize) ?: toastTextSize(ToastKitTextSize.BASE),
                                fontWeight = toast.text?.weight?.let(::toastTextWeight) ?: FontWeight.Medium,
                                fontStyle = toast.text?.italic?.let { if (it) FontStyle.Italic else FontStyle.Normal },
                                fontFamily = toast.text?.font?.let(::toastFontFamily),
                                maxLines = 2,
                            ) }
                        }
                        toast.actions.forEach { action ->
                            TextButton(
                                onClick = { ToastKitManager.action(toast.id, action.id) },
                                colors = ButtonDefaults.textButtonColors(contentColor = action.color ?: toast.style.actionColor),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                            ) { Text(action.label, fontWeight = FontWeight.Bold, maxLines = 1) }
                        }
                        if (toast.dismissible) {
                            IconButton(onClick = { ToastKitManager.dismiss(toast.id) }) {
                                MaterialIcon("close", "Dismiss", size = 20.dp, tint = toast.style.foreground)
                            }
                        }
                    }
                }
                if (toast.progress != null) {
                    LinearProgressIndicator(
                        progress = { displayedProgress },
                        modifier = Modifier.fillMaxWidth().height(3.dp).alpha(contentAlpha),
                        color = toast.style.actionColor,
                        trackColor = toast.style.foreground.copy(alpha = .22f),
                    )
                }
            }
        }
    }
}

private val DynamicToastDropShape = GenericShape { size, _ ->
    moveTo(size.width * .36f, 0f)
    cubicTo(size.width * .38f, size.height * .38f, size.width * .30f, size.height * .48f, size.width * .15f, size.height * .66f)
    cubicTo(0f, size.height * .84f, size.width * .18f, size.height, size.width * .5f, size.height)
    cubicTo(size.width * .82f, size.height, size.width, size.height * .84f, size.width * .85f, size.height * .66f)
    cubicTo(size.width * .70f, size.height * .48f, size.width * .62f, size.height * .38f, size.width * .64f, 0f)
    close()
}

@Composable
private fun NativeToastKitCard(toast: ToastKitConfiguration, displayedProgress: Float, modifier: Modifier, reduceMotion: Boolean) {
    Snackbar(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (toast.progress == null && toast.loading) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = LocalContentColor.current, strokeWidth = 2.5.dp)
                } else toast.icon?.let {
                    MaterialIcon(it.android ?: it.name ?: "circle", "Toast icon", size = 24.dp, tint = LocalContentColor.current)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    AnimatedContent(
                        targetState = toast.title,
                        transitionSpec = { fadeIn(tween(if (reduceMotion) 1 else 140)) togetherWith fadeOut(tween(if (reduceMotion) 1 else 100)) },
                        label = "native-toast-title-update",
                    ) { title ->
                        title?.let { Text(it, style = androidx.compose.material3.MaterialTheme.typography.labelLarge) }
                    }
                    AnimatedContent(
                        targetState = toast.message,
                        transitionSpec = { fadeIn(tween(if (reduceMotion) 1 else 140)) togetherWith fadeOut(tween(if (reduceMotion) 1 else 100)) },
                        label = "native-toast-message-update",
                    ) { Text(it, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium) }
                }
                toast.actions.forEach { action ->
                    TextButton(
                        onClick = { ToastKitManager.action(toast.id, action.id) },
                        colors = ButtonDefaults.textButtonColors(contentColor = action.color ?: SnackbarDefaults.actionColor),
                    ) { Text(action.label) }
                }
                if (toast.dismissible) {
                    IconButton(onClick = { ToastKitManager.dismiss(toast.id) }) {
                        MaterialIcon("close", "Dismiss", size = 20.dp, tint = LocalContentColor.current)
                    }
                }
            }
            toast.progress?.let {
                LinearProgressIndicator(progress = { displayedProgress }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun resolvedDirection(toast: ToastKitConfiguration): ToastKitDirection =
    if (toast.presentation == ToastKitPresentation.DYNAMIC) ToastKitDirection.TOP else when (toast.direction) {
        ToastKitDirection.AUTO -> when (effectivePosition(toast)) {
            ToastKitPosition.TOP -> ToastKitDirection.TOP
            ToastKitPosition.BOTTOM -> ToastKitDirection.BOTTOM
            ToastKitPosition.CENTER -> ToastKitDirection.TOP
        }
        else -> toast.direction
    }

internal fun effectivePosition(toast: ToastKitConfiguration): ToastKitPosition =
    if (toast.presentation == ToastKitPresentation.DYNAMIC) ToastKitPosition.TOP else toast.position

private fun ToastKitAnimation.isDirectional(): Boolean =
    this == ToastKitAnimation.SLIDE || this == ToastKitAnimation.SNAP || this == ToastKitAnimation.BOUNCE

private fun ToastKitAnimation.hiddenScale(): Float = when (this) {
    ToastKitAnimation.SCALE -> .9f
    ToastKitAnimation.SPRING -> .86f
    ToastKitAnimation.POP -> .72f
    else -> 1f
}

private fun alphaSpec(
    animation: ToastKitAnimation,
    entering: Boolean,
    simplified: Boolean,
): FiniteAnimationSpec<Float> {
    if (simplified) return tween(if (entering) 120 else 100)
    val duration = when (animation) {
        ToastKitAnimation.FADE -> if (entering) 180 else 160
        ToastKitAnimation.SLIDE -> 220
        ToastKitAnimation.SCALE -> if (entering) 180 else 160
        ToastKitAnimation.SPRING -> 220
        ToastKitAnimation.SNAP -> 90
        ToastKitAnimation.POP -> if (entering) 100 else 120
        ToastKitAnimation.REVEAL -> if (entering) 160 else 150
        ToastKitAnimation.BOUNCE -> if (entering) 120 else 160
    }
    return tween(duration)
}

private fun motionSpec(
    animation: ToastKitAnimation,
    simplified: Boolean,
): FiniteAnimationSpec<Float> {
    if (simplified) return tween(1)
    return when (animation) {
        ToastKitAnimation.SNAP -> spring(dampingRatio = .68f, stiffness = 950f)
        ToastKitAnimation.BOUNCE -> spring(dampingRatio = .45f, stiffness = 360f)
        else -> tween(220)
    }
}

private fun scaleSpec(
    animation: ToastKitAnimation,
    entering: Boolean,
    simplified: Boolean,
): FiniteAnimationSpec<Float> {
    if (simplified) return tween(1)
    return when (animation) {
        ToastKitAnimation.SCALE -> tween(if (entering) 180 else 160)
        ToastKitAnimation.SPRING -> spring()
        ToastKitAnimation.POP -> spring(dampingRatio = .55f, stiffness = 650f)
        else -> tween(1)
    }
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

private fun toastIconSize(size: ToastKitIconSize?, fallback: Int): Int = when (size) {
    ToastKitIconSize.SM -> 18
    ToastKitIconSize.MD -> 24
    ToastKitIconSize.LG -> 32
    null -> fallback
}

private fun toastFontFamily(font: String): FontFamily? =
    NativeUIThemeProvider.resolveChromeFontFamily(font)
