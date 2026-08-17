package com.victorycodedev.plugins.toastkit.bridge

import androidx.fragment.app.FragmentActivity
import com.nativephp.mobile.utils.NativeActionCoordinator
import org.json.JSONObject
import java.lang.ref.WeakReference

internal object ToastKitEventDispatcher {
    private const val SHOWN = "Victorycodedev\\ToastKit\\Events\\ToastShown"
    private const val DISMISSED = "Victorycodedev\\ToastKit\\Events\\ToastDismissed"
    private const val ACTION = "Victorycodedev\\ToastKit\\Events\\ToastActionPressed"
    private var activity = WeakReference<FragmentActivity>(null)

    fun bind(value: FragmentActivity) { activity = WeakReference(value) }
    fun shown(id: String) = send(SHOWN, JSONObject().put("toastId", id))
    fun dismissed(id: String, reason: String) = send(DISMISSED, JSONObject().put("toastId", id).put("reason", reason))
    fun action(id: String, actionId: String) = send(ACTION, JSONObject().put("toastId", id).put("actionId", actionId))

    private fun send(event: String, payload: JSONObject) {
        activity.get()?.let { NativeActionCoordinator.dispatchEvent(it, event, payload.toString()) }
    }
}
