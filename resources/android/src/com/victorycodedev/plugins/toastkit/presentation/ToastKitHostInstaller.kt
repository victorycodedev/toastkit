package com.victorycodedev.plugins.toastkit.presentation

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Color
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
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inspector.WindowInspector
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Recomposer
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

/** Hosts Android toasts above activity-owned dialogs, sheets, popups, and detached bars. */
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
    private val scope = CoroutineScope(AndroidUiDispatcher.Main + SupervisorJob())
    private val recomposer = Recomposer(scope.coroutineContext).also { recomposer ->
        scope.launch { recomposer.runRecomposeAndApplyChanges() }
    }
    private val hosts = ToastKitPosition.entries.associateWith(::Host)
    private val handler = Handler(Looper.getMainLooper())
    private var reorderPending = false
    private var lastReorderedAbove: View? = null
    private var watching = false
    private var destroyed = false

    fun sync() {
        if (destroyed) return
        if (activity.isFinishing || activity.isDestroyed) return destroy()
        val needed = ToastKitManager.rendered.map(::effectivePosition).toSet()
        hosts.forEach { (position, host) -> if (position in needed) host.show() else host.hide() }
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
        handler.removeCallbacksAndMessages(this)
        reorderPending = false
        lastReorderedAbove = null
        recomposer.cancel()
        scope.cancel()
    }

    private fun followActivityWindows() {
        val shown = hosts.values.filter { it.shown }
        if (shown.isEmpty()) return
        val roots = try { WindowInspector.getGlobalWindowViews() } catch (_: Throwable) { return }
        val ownRoots = hosts.values.map { it.view }.toSet()
        val appToken = activity.window.attributes.token ?: return
        val activityWindows = HashSet<IBinder>()
        val activeForeignRoots = HashSet<View>()
        var lastForeign = -1
        var keyboardHeight = 0

        roots.forEachIndexed { index, root ->
            val params = root.layoutParams as? WindowManager.LayoutParams ?: return@forEachIndexed
            val belongs = params.token != null && (params.token == appToken || params.token in activityWindows)
            if (!belongs) return@forEachIndexed
            root.windowToken?.let(activityWindows::add)
            if (root in ownRoots || !root.isAttachedToWindow || root.windowVisibility != View.VISIBLE) return@forEachIndexed
            activeForeignRoots.add(root)
            lastForeign = index
            root.rootWindowInsets?.let { insets ->
                keyboardHeight = maxOf(
                    keyboardHeight,
                    WindowInsetsCompat.toWindowInsetsCompat(insets, root)
                        .getInsets(WindowInsetsCompat.Type.ime()).bottom,
                )
            }
        }

        hosts.values.forEach { it.updateKeyboardHeight(keyboardHeight) }
        lastReorderedAbove?.let { root ->
            if (root !in activeForeignRoots) lastReorderedAbove = null
        }
        if (lastForeign < 0) {
            return
        }
        val foreignRoot = roots[lastForeign]
        if (shown.none { roots.indexOf(it.view) < lastForeign }) {
            return
        }
        if (reorderPending || foreignRoot === lastReorderedAbove) return

        // A dialog or sheet gets its own window while its opening animation is
        // still being prepared. Reordering synchronously at that point can make
        // the toast window briefly own the system-bar surface, producing a black
        // strip and delaying the new overlay. Let the new window finish its
        // first layout, then move the toast once.
        reorderPending = true
        lastReorderedAbove = foreignRoot
        handler.postAtTime({
            reorderPending = false
            if (!destroyed && !activity.isFinishing && !activity.isDestroyed) {
                hosts.values.filter { it.shown }.forEach { it.readd() }
            }
        }, this, android.os.SystemClock.uptimeMillis() + 180L)
    }

    private inner class Host(private val position: ToastKitPosition) {
        private val cardBounds = mutableMapOf<String, Rect>()
        private var requested = false
        private var keyboardOffset = 0
        private var attachedParams: WindowManager.LayoutParams? = null
        private val showRunnable = Runnable {
            if (!requested || destroyed || activity.isFinishing || activity.isDestroyed) {
                return@Runnable
            }
            attachWindow()
            this@ToastKitOverlay.sync()
        }
        var shown = false
            private set

        val view: ComposeView = ComposeView(activity).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setViewTreeLifecycleOwner(activity)
            setViewTreeViewModelStoreOwner(activity)
            setViewTreeSavedStateRegistryOwner(activity)
            setParentCompositionContext(recomposer)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(activity))
            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) = updateTouchableRegion()
                override fun onViewDetachedFromWindow(v: View) = Unit
            })
            setContent {
                val isDark = isSystemInDarkTheme()
                MaterialTheme(
                    colorScheme = NativeUIThemeProvider.resolve(isDark),
                    typography = NativeUIThemeProvider.resolveTypography(),
                ) {
                    ToastKitHost(
                        position = position,
                        includesWindowInsets = false,
                        onCardBounds = ::onCardBounds,
                    )
                }
            }
        }

        fun show() {
            requested = true
            if (shown || handler.hasCallbacks(showRunnable)) return

            // Toast calls and a modal visibility update can be produced by the
            // same PHP action. Give that UI transaction time to attach the modal
            // first, so this window is added above it once instead of being
            // immediately torn down and re-added.
            handler.postDelayed(showRunnable, 80L)
        }

        private fun attachWindow() {
            if (shown) return
            val params = layoutParams().apply { alpha = 0f }
            val firstFrameListener = object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (view.viewTreeObserver.isAlive) {
                        view.viewTreeObserver.removeOnPreDrawListener(this)
                    }
                    view.post {
                        if (shown && view.isAttachedToWindow) {
                            params.alpha = 1f
                            try {
                                windowManager.updateViewLayout(view, params)
                            } catch (_: IllegalArgumentException) {
                            }
                        }
                    }
                    return true
                }
            }
            view.viewTreeObserver.addOnPreDrawListener(firstFrameListener)
            try {
                windowManager.addView(view, params)
                attachedParams = params
                shown = true
            } catch (_: WindowManager.BadTokenException) {
                if (view.viewTreeObserver.isAlive) {
                    view.viewTreeObserver.removeOnPreDrawListener(firstFrameListener)
                }
            } catch (_: IllegalStateException) {
                if (view.viewTreeObserver.isAlive) {
                    view.viewTreeObserver.removeOnPreDrawListener(firstFrameListener)
                }
            }
        }

        fun hide() {
            requested = false
            handler.removeCallbacks(showRunnable)
            if (!shown) return
            detachWindow()
        }

        fun readd() {
            if (!shown) return
            detachWindow()
            if (requested) attachWindow()
        }

        fun destroy() {
            hide()
            view.disposeComposition()
        }

        private fun detachWindow() {
            shown = false
            attachedParams = null
            try { windowManager.removeViewImmediate(view) } catch (_: IllegalArgumentException) {}
        }

        fun updateKeyboardHeight(height: Int) {
            if (position != ToastKitPosition.BOTTOM || keyboardOffset == height) return
            keyboardOffset = height
            val params = attachedParams ?: return
            params.y = keyboardOffset
            try {
                windowManager.updateViewLayout(view, params)
            } catch (_: IllegalArgumentException) {
            }
        }

        private fun onCardBounds(id: String, bounds: Rect?) {
            if (bounds == null) cardBounds.remove(id) else cardBounds[id] = bounds
            updateTouchableRegion()
        }

        private fun updateTouchableRegion() {
            if (!shown || Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
            val region = Region()
            cardBounds.values.forEach { bounds ->
                region.op(
                    bounds.left.roundToInt(), bounds.top.roundToInt(),
                    bounds.right.roundToInt(), bounds.bottom.roundToInt(), Region.Op.UNION,
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
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = when (position) {
                ToastKitPosition.TOP -> Gravity.TOP
                ToastKitPosition.CENTER -> Gravity.CENTER
                ToastKitPosition.BOTTOM -> Gravity.BOTTOM
            }
            title = "ToastKit:${position.name}"
            if (position == ToastKitPosition.BOTTOM) y = keyboardOffset
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        }
    }
}
