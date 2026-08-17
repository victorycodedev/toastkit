package com.victorycodedev.plugins.toastkit.presentation

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.victorycodedev.plugins.toastkit.model.ToastKitPosition
import com.victorycodedev.plugins.toastkit.bridge.ToastKitEventDispatcher
import androidx.fragment.app.FragmentActivity
import java.util.WeakHashMap
import android.os.Handler
import android.os.Looper

internal object ToastKitHostInstaller : Application.ActivityLifecycleCallbacks {
    private val attached = WeakHashMap<Activity, List<ComposeView>>()
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
    override fun onActivityDestroyed(activity: Activity) { attached.remove(activity)?.forEach { (it.parent as? ViewGroup)?.removeView(it) } }

    fun ensureAttached(activity: Activity) {
        if (Looper.myLooper() == Looper.getMainLooper()) attach(activity)
        else Handler(Looper.getMainLooper()).post { attach(activity) }
    }

    private fun attach(activity: Activity) {
        if (attached.containsKey(activity) || activity !is ComponentActivity) return
        val decor = activity.window.decorView as? ViewGroup ?: return
        val views = ToastKitPosition.entries.map { position ->
            ComposeView(activity).apply {
                setViewTreeLifecycleOwner(activity)
                setViewTreeViewModelStoreOwner(activity)
                setViewTreeSavedStateRegistryOwner(activity)
                setContent { ToastKitHost(position) }
                decor.addView(this, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    gravity = when (position) { ToastKitPosition.TOP -> Gravity.TOP; ToastKitPosition.CENTER -> Gravity.CENTER; ToastKitPosition.BOTTOM -> Gravity.BOTTOM }
                })
            }
        }
        attached[activity] = views
    }

    override fun onActivityCreated(a: Activity, b: Bundle?) = Unit
    override fun onActivityStarted(a: Activity) = Unit
    override fun onActivityPaused(a: Activity) = Unit
    override fun onActivityStopped(a: Activity) = Unit
    override fun onActivitySaveInstanceState(a: Activity, b: Bundle) = Unit
}
