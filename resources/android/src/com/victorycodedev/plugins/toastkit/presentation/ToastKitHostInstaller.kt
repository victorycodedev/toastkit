package com.victorycodedev.plugins.toastkit.presentation

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.nativephp.mobile.ui.NativeUIThemeProvider
import com.victorycodedev.plugins.toastkit.bridge.ToastKitEventDispatcher
import com.victorycodedev.plugins.toastkit.model.ToastKitPosition
import java.util.WeakHashMap

internal object ToastKitHostInstaller : Application.ActivityLifecycleCallbacks {
    // Compose Popup uses an activity sub-window. When LayoutKit's optional
    // overlay contract is present, a
    // transparent, non-focusable activity Dialog so the toast has its own
    // application window above activity sub-windows without taking focus or
    // intercepting touches outside the toast bounds.
    private val attached = WeakHashMap<Activity, List<HostAttachment>>()
    private var installed = false

    @Synchronized fun initialize(context: Context) {
        if (installed) return
        (context.applicationContext as? Application)?.registerActivityLifecycleCallbacks(this)
        installed = true
    }

    override fun onActivityResumed(activity: Activity) {
        (activity as? FragmentActivity)?.let(ToastKitEventDispatcher::bind)
        ensureAttached(activity)
    }
    override fun onActivityDestroyed(activity: Activity) {
        attached.remove(activity)?.forEach(HostAttachment::dispose)
    }

    fun ensureAttached(activity: Activity) {
        if (Looper.myLooper() == Looper.getMainLooper()) attach(activity)
        else Handler(Looper.getMainLooper()).post { attach(activity) }
    }

    private fun attach(activity: Activity) {
        if (attached.containsKey(activity) || activity !is ComponentActivity) return
        val decor = activity.window.decorView as? ViewGroup ?: return
        val hosts = if (LayoutKitOverlaySupport.isAvailable(activity)) {
            attachAbovePopups(activity, decor) ?: attachToDecor(activity, decor)
        } else {
            attachToDecor(activity, decor)
        }
        attached[activity] = hosts
    }

    private fun attachToDecor(activity: ComponentActivity, decor: ViewGroup): List<HostAttachment> =
        ToastKitPosition.entries.map { position ->
            val view = createComposeView(activity, position)
            decor.addView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { gravity = position.gravity },
            )
            HostAttachment(view) { (view.parent as? ViewGroup)?.removeView(view) }
        }

    private fun attachAbovePopups(
        activity: ComponentActivity,
        decor: ViewGroup,
    ): List<HostAttachment>? {
        if (decor.windowToken == null) return null
        val hosts = mutableListOf<HostAttachment>()

        return try {
            ToastKitPosition.entries.forEach { position ->
                var updateHostVisibility: ((Boolean) -> Unit)? = null
                val view = createComposeView(
                    activity,
                    position,
                    includesWindowInsets = false,
                    onContentVisibilityChanged = { visible ->
                        updateHostVisibility?.invoke(visible)
                    },
                )
                val dialog = Dialog(activity).apply {
                    setContentView(view)
                    setCancelable(false)
                    setCanceledOnTouchOutside(false)
                }
                val window = checkNotNull(dialog.window) { "ToastKit dialog has no window" }
                window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                )
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                window.attributes = window.attributes.apply {
                    width = hostWidth(decor)
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                    gravity = position.windowGravity
                    y = 0
                    title = "ToastKit ${position.name.lowercase()} host"
                    dimAmount = 0f
                    windowAnimations = 0
                    alpha = 0f
                }
                updateHostVisibility = { visible ->
                    window.attributes = window.attributes.apply {
                        alpha = if (visible) 1f else 0f
                    }
                }

                val relayout = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                    if (dialog.isShowing) {
                        window.attributes = window.attributes.apply {
                            width = hostWidth(decor)
                            y = 0
                        }
                    }
                }
                decor.addOnLayoutChangeListener(relayout)

                try {
                    dialog.show()
                } catch (throwable: Throwable) {
                    decor.removeOnLayoutChangeListener(relayout)
                    throw throwable
                }
                hosts += HostAttachment(view) {
                    decor.removeOnLayoutChangeListener(relayout)
                    runCatching { dialog.dismiss() }
                }
            }
            hosts
        } catch (exception: RuntimeException) {
            Log.w("ToastKit", "Unable to attach the above-popup host; using the activity decor instead.", exception)
            hosts.forEach(HostAttachment::dispose)
            null
        }
    }

    private fun createComposeView(
        activity: ComponentActivity,
        position: ToastKitPosition,
        includesWindowInsets: Boolean = true,
        onContentVisibilityChanged: ((Boolean) -> Unit)? = null,
    ): ComposeView = ComposeView(activity).apply {
        setViewTreeLifecycleOwner(activity)
        setViewTreeViewModelStoreOwner(activity)
        setViewTreeSavedStateRegistryOwner(activity)
        setContent {
            val isDark = isSystemInDarkTheme()
            MaterialTheme(
                colorScheme = NativeUIThemeProvider.resolve(isDark),
                typography = NativeUIThemeProvider.resolveTypography(),
            ) {
                ToastKitHost(position, includesWindowInsets, onContentVisibilityChanged)
            }
        }
    }

    private val ToastKitPosition.gravity: Int
        get() = when (this) {
            ToastKitPosition.TOP -> Gravity.TOP
            ToastKitPosition.CENTER -> Gravity.CENTER
            ToastKitPosition.BOTTOM -> Gravity.BOTTOM
        }

    private val ToastKitPosition.windowGravity: Int
        get() = gravity or Gravity.CENTER_HORIZONTAL

    private fun hostWidth(decor: View): Int {
        val margin = (16 * decor.resources.displayMetrics.density).toInt()
        val width = decor.width.takeIf { it > 0 } ?: decor.resources.displayMetrics.widthPixels

        return (width - margin * 2).coerceAtLeast(1)
    }

    private class HostAttachment(
        private val view: View,
        private val remove: () -> Unit,
    ) {
        fun dispose() {
            view.disposeCompositionIfPossible()
            remove()
        }
    }

    private fun View.disposeCompositionIfPossible() {
        (this as? ComposeView)?.disposeComposition()
    }

    override fun onActivityCreated(a: Activity, b: Bundle?) = Unit
    override fun onActivityStarted(a: Activity) = Unit
    override fun onActivityPaused(a: Activity) = Unit
    override fun onActivityStopped(a: Activity) = Unit
    override fun onActivitySaveInstanceState(a: Activity, b: Bundle) = Unit
}
