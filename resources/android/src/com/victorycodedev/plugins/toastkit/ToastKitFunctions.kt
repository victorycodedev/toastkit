package com.victorycodedev.plugins.toastkit

import androidx.fragment.app.FragmentActivity
import com.nativephp.mobile.bridge.BridgeFunction
import com.nativephp.mobile.bridge.BridgeResponse
import com.victorycodedev.plugins.toastkit.bridge.BridgeNormalizer
import com.victorycodedev.plugins.toastkit.bridge.NativeEventDispatcher
import com.victorycodedev.plugins.toastkit.manager.ToastManager

object ToastKitFunctions {
    class Show(private val activity: FragmentActivity) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>): Map<String, Any> = respond {
            NativeEventDispatcher.bind(activity)
            val toast = BridgeNormalizer.show(parameters)
            ToastManager.show(toast)
            mapOf("id" to toast.id, "accepted" to true)
        }
    }

    class Update(private val activity: FragmentActivity) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>): Map<String, Any> = respond {
            NativeEventDispatcher.bind(activity)
            val (id, changes) = BridgeNormalizer.update(parameters)
            ToastManager.update(id, changes)
            mapOf("id" to id, "accepted" to true)
        }
    }

    class Dismiss(private val activity: FragmentActivity) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>): Map<String, Any> = respond {
            NativeEventDispatcher.bind(activity)
            val id = BridgeNormalizer.id(parameters)
            ToastManager.dismiss(id)
            mapOf("id" to id, "accepted" to true)
        }
    }

    class DismissAll(private val activity: FragmentActivity) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>): Map<String, Any> = respond {
            NativeEventDispatcher.bind(activity)
            ToastManager.dismissAll()
            mapOf("accepted" to true)
        }
    }

    private inline fun respond(operation: () -> Map<String, Any>): Map<String, Any> =
        try { BridgeResponse.success(operation()) } catch (error: IllegalArgumentException) {
            BridgeResponse.error("TOASTKIT_INVALID_ARGUMENT", error.message ?: "Invalid ToastKit payload")
        }
}
