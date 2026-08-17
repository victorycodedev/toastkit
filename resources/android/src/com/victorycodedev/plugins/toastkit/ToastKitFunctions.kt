package com.victorycodedev.plugins.toastkit

import androidx.fragment.app.FragmentActivity
import com.nativephp.mobile.bridge.BridgeFunction
import com.nativephp.mobile.bridge.BridgeResponse
import com.victorycodedev.plugins.toastkit.bridge.ToastKitBridgeNormalizer
import com.victorycodedev.plugins.toastkit.bridge.ToastKitEventDispatcher
import com.victorycodedev.plugins.toastkit.manager.ToastKitManager

object ToastKitFunctions {
    class Show(private val activity: FragmentActivity) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>): Map<String, Any> = respond {
            ToastKitEventDispatcher.bind(activity)
            val toast = ToastKitBridgeNormalizer.show(parameters)
            ToastKitManager.show(toast)
            mapOf("id" to toast.id, "accepted" to true)
        }
    }

    class Update(private val activity: FragmentActivity) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>): Map<String, Any> = respond {
            ToastKitEventDispatcher.bind(activity)
            val (id, changes) = ToastKitBridgeNormalizer.update(parameters)
            ToastKitManager.update(id, changes)
            mapOf("id" to id, "accepted" to true)
        }
    }

    class Dismiss(private val activity: FragmentActivity) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>): Map<String, Any> = respond {
            ToastKitEventDispatcher.bind(activity)
            val id = ToastKitBridgeNormalizer.id(parameters)
            ToastKitManager.dismiss(id)
            mapOf("id" to id, "accepted" to true)
        }
    }

    class DismissAll(private val activity: FragmentActivity) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>): Map<String, Any> = respond {
            ToastKitEventDispatcher.bind(activity)
            ToastKitManager.dismissAll()
            mapOf("accepted" to true)
        }
    }

    private inline fun respond(operation: () -> Map<String, Any>): Map<String, Any> =
        try { BridgeResponse.success(operation()) } catch (error: IllegalArgumentException) {
            BridgeResponse.error("TOASTKIT_INVALID_ARGUMENT", error.message ?: "Invalid ToastKit payload")
        }
}
