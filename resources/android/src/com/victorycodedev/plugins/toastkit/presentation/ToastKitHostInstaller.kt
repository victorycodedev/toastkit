package com.victorycodedev.plugins.toastkit.presentation

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Region
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Choreographer
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inspector.WindowInspector
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.nativephp.mobile.ui.NativeUIThemeProvider
import com.victorycodedev.plugins.toastkit.bridge.ToastKitEventDispatcher
import com.victorycodedev.plugins.toastkit.manager.ToastKitManager
import com.victorycodedev.plugins.toastkit.model.ToastKitPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.WeakHashMap
import kotlin.math.roundToInt

/**
 * Draws toasts in their own application windows instead of the activity's decor view.
 *
 * Dialogs, including Material3's ModalBottomSheet, are separate windows stacked above the
 * activity window, so anything inside the activity's decor view ends up underneath them.
 * Each toast position gets a window of its own, added through the activity's WindowManager
 * while that position has toasts and removed when it is empty. Windows that share the
 * activity's token are z-ordered by when they were added, so while toasts are on screen the
 * overlay checks every frame whether another window of the activity was added after it and,
 * if so, re-adds itself to get back on top.
 *
 * The windows are not focusable, so they never take focus or the keyboard away from a text
 * field, and taps outside the window go to whatever is underneath. On API 34+ the touchable
 * region is narrowed to the toast cards so the padding around them passes taps through too.
 * The overlay draws above the keyboard, so bottom toasts are lifted clear of it.
 */
internal object ToastKitHostInstaller : Application.ActivityLifecycleCallbacks {
    private val overlays = WeakHashMap<Activity, ToastKitOverlay>()
    private var installed = false

    @Synchronized fun initialize(context: Context) {
        if (installed) return
        (context.applicationContext as? Application)?.registerActivityLifecycleCallbacks(this)
        ToastKitManager.onRenderedChanged = ::syncAll
        installed = true
    }

    override fun onActivityResumed(activity: Activity) {
        (activity as? FragmentActivity)?.let(ToastKitEventDispatcher::bind)
        ensureAttached(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        overlays.remove(activity)?.destroy()
    }

    fun ensureAttached(activity: Activity) {
        if (Looper.myLooper() == Looper.getMainLooper()) attach(activity)
        else Handler(Looper.getMainLooper()).post { attach(activity) }
    }

    private fun attach(activity: Activity) {
        if (ToastKitManager.onRenderedChanged == null) ToastKitManager.onRenderedChanged = ::syncAll
        if (overlays.containsKey(activity) || activity !is ComponentActivity) return
        if (activity.isFinishing || activity.isDestroyed) return
        overlays[activity] = ToastKitOverlay(activity).also { it.sync() }
    }

    private fun syncAll() {
        overlays.values.toList().forEach { it.sync() }
    }

    override fun onActivityCreated(a: Activity, b: Bundle?) = Unit
    override fun onActivityStarted(a: Activity) = Unit
    override fun onActivityPaused(a: Activity) = Unit
    override fun onActivityStopped(a: Activity) = Unit
    override fun onActivitySaveInstanceState(a: Activity, b: Bundle) = Unit
}

private class ToastKitOverlay(private val activity: ComponentActivity) : Choreographer.FrameCallback {
    private val windowManager = activity.windowManager

    // The hosts are detached and re-attached to change their z-order. A window's default
    // recomposer is cancelled when its root view detaches, so the overlay owns one that lives
    // as long as the activity. That keeps each toast's animation and swipe state across a re-add.
    private val scope = CoroutineScope(AndroidUiDispatcher.Main + SupervisorJob())
    private val recomposer = Recomposer(scope.coroutineContext).also { recomposer ->
        scope.launch { recomposer.runRecomposeAndApplyChanges() }
    }

    private val hosts = ToastKitPosition.entries.associateWith(::Host)
    private var watching = false
    private var destroyed = false

    fun sync() {
        if (destroyed) return
        if (activity.isFinishing || activity.isDestroyed) return destroy()
        val needed = ToastKitManager.rendered.map(::effectivePosition).toSet()
        hosts.forEach { (position, host) ->
            if (position in needed) host.show() else host.hide()
        }
        val anyShown = hosts.values.any { it.shown }
        if (anyShown) followActivityWindows()
        if (anyShown && !watching) {
            watching = true
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (destroyed || hosts.values.none { it.shown }) {
            watching = false
            return
        }
        followActivityWindows()
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        Choreographer.getInstance().removeFrameCallback(this)
        watching = false
        hosts.values.forEach { it.destroy() }
        recomposer.cancel()
        scope.cancel()
    }

    /**
     * Re-adds any shown host that has another window of this activity stacked above it, and
     * passes the keyboard's height to the hosts.
     */
    private fun followActivityWindows() {
        val shown = hosts.values.filter { it.shown }
        if (shown.isEmpty()) return
        val roots = try { WindowInspector.getGlobalWindowViews() } catch (_: Throwable) { return }
        val ownRoots = hosts.values.map { it.view }.toSet()
        val appToken = activity.window.attributes.token ?: return

        // Windows that belong to this activity: anything added with its token (its own window,
        // dialogs, sheets) plus sub-windows such as popups and menus attached to one of those.
        // Windows of other activities are skipped so a toast never fights an activity above it.
        val activityWindows = HashSet<IBinder>()
        var lastForeign = -1
        var keyboard = 0
        roots.forEachIndexed { index, root ->
            val params = root.layoutParams as? WindowManager.LayoutParams ?: return@forEachIndexed
            val belongs = params.token != null && (params.token == appToken || params.token in activityWindows)
            if (!belongs) return@forEachIndexed
            root.windowToken?.let(activityWindows::add)
            if (root in ownRoots || !root.isAttachedToWindow || root.windowVisibility != View.VISIBLE) return@forEachIndexed
            lastForeign = index
            // WindowManager only reports the keyboard to windows beneath it, and the overlay is
            // above it, so its height is read from the window that is being typed in.
            root.rootWindowInsets?.let { insets ->
                keyboard = maxOf(keyboard, WindowInsetsCompat.toWindowInsetsCompat(insets, root).getInsets(WindowInsetsCompat.Type.ime()).bottom)
            }
        }
        hosts.values.forEach { it.keyboardHeight.intValue = keyboard }
        if (lastForeign < 0) return
        shown.filter { roots.indexOf(it.view) < lastForeign }.forEach { it.readd() }
    }

    private inner class Host(private val position: ToastKitPosition) {
        private val cardBounds = mutableMapOf<String, Rect>()
        val keyboardHeight = mutableIntStateOf(0)
        var shown = false
            private set

        val view: ComposeView = ComposeView(activity).apply {
            setViewTreeLifecycleOwner(activity)
            setViewTreeViewModelStoreOwner(activity)
            setViewTreeSavedStateRegistryOwner(activity)
            setParentCompositionContext(recomposer)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(activity))
            // The root surface only exists once the view is attached, and each re-add is a new one.
            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) = updateTouchableRegion()
                override fun onViewDetachedFromWindow(v: View) = Unit
            })
            setContent {
                val isDark = isSystemInDarkTheme()
                MaterialTheme(
                    colorScheme = NativeUIThemeProvider.resolve(isDark),
                    typography = NativeUIThemeProvider.resolveTypography(),
                ) { ToastKitHost(position, keyboardHeight.intValue, ::onCardBounds) }
            }
        }

        fun show() {
            if (shown) return
            try {
                windowManager.addView(view, layoutParams())
                shown = true
            } catch (_: WindowManager.BadTokenException) {
            } catch (_: IllegalStateException) {
            }
        }

        fun hide() {
            if (!shown) return
            shown = false
            try { windowManager.removeViewImmediate(view) } catch (_: IllegalArgumentException) {}
        }

        fun readd() {
            hide()
            show()
        }

        fun destroy() {
            hide()
            view.disposeComposition()
        }

        private fun onCardBounds(id: String, bounds: Rect?) {
            if (bounds == null) cardBounds.remove(id) else cardBounds[id] = bounds
            updateTouchableRegion()
        }

        // Without this the whole band (the safe-area and the padding around the cards) would
        // swallow taps. Before API 34 there is no public way to narrow a window's touch region.
        private fun updateTouchableRegion() {
            if (!shown || Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
            val region = Region()
            cardBounds.values.forEach { bounds ->
                region.op(
                    bounds.left.roundToInt(), bounds.top.roundToInt(),
                    bounds.right.roundToInt(), bounds.bottom.roundToInt(),
                    Region.Op.UNION,
                )
            }
            view.rootSurfaceControl?.setTouchableRegion(region)
        }

        private fun layoutParams() = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = when (position) {
                ToastKitPosition.TOP -> Gravity.TOP
                ToastKitPosition.CENTER -> Gravity.CENTER
                ToastKitPosition.BOTTOM -> Gravity.BOTTOM
            }
            title = "ToastKit:${position.name}"
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            // Lay out under the system bars like the decor view did; ToastKitHost pads for them.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                fitInsetsTypes = 0
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }
}
