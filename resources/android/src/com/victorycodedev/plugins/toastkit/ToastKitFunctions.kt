package com.victorycodedev.plugins.toastkit

import com.nativephp.mobile.bridge.BridgeFunction
import com.nativephp.mobile.bridge.BridgeResponse

/** Contract placeholders. Native rendering will be implemented in a later phase. */
object ToastKitFunctions {
    class Show : BridgeFunction { override fun execute(parameters: Map<String, Any>) = BridgeResponse.success(mapOf("accepted" to false)) }
    class Update : BridgeFunction { override fun execute(parameters: Map<String, Any>) = BridgeResponse.success(mapOf("accepted" to false)) }
    class Dismiss : BridgeFunction { override fun execute(parameters: Map<String, Any>) = BridgeResponse.success(mapOf("accepted" to false)) }
    class DismissAll : BridgeFunction { override fun execute(parameters: Map<String, Any>) = BridgeResponse.success(mapOf("accepted" to false)) }
}
