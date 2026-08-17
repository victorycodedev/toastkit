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
import com.victorycodedev.plugins.toastkit.model.ToastPosition
import com.victorycodedev.plugins.toastkit.bridge.NativeEventDispatcher
import androidx.fragment.app.FragmentActivity
import java.util.WeakHashMap

internal object ToastHostInstaller : Application.ActivityLifecycleCallbacks {
    private val attached = WeakHashMap<Activity, List<ComposeView>>()
    private var installed = false

    @Synchronized fun initialize(context: Context) {
        if (installed) return
        (context.applicationContext as? Application)?.registerActivityLifecycleCallbacks(this)
        installed = true
    }

    override fun onActivityResumed(activity: Activity) {
        (activity as? FragmentActivity)?.let(NativeEventDispatcher::bind)
        attach(activity)
    }
    override fun onActivityDestroyed(activity: Activity) { attached.remove(activity)?.forEach { (it.parent as? ViewGroup)?.removeView(it) } }

    private fun attach(activity: Activity) {
        if (attached.containsKey(activity) || activity !is ComponentActivity) return
        val decor = activity.window.decorView as? ViewGroup ?: return
        val views = ToastPosition.entries.map { position ->
            ComposeView(activity).apply {
                setViewTreeLifecycleOwner(activity)
                setViewTreeViewModelStoreOwner(activity)
                setViewTreeSavedStateRegistryOwner(activity)
                setContent { ToastHost(position) }
                decor.addView(this, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    gravity = when (position) { ToastPosition.TOP -> Gravity.TOP; ToastPosition.CENTER -> Gravity.CENTER; ToastPosition.BOTTOM -> Gravity.BOTTOM }
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
