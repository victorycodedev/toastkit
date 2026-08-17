import Foundation

/// Contract placeholders. Native rendering will be implemented in a later phase.
enum ToastKitFunctions {
    class Show: BridgeFunction { func execute(parameters: [String: Any]) throws -> [String: Any] { BridgeResponse.success(data: ["accepted": false]) } }
    class Update: BridgeFunction { func execute(parameters: [String: Any]) throws -> [String: Any] { BridgeResponse.success(data: ["accepted": false]) } }
    class Dismiss: BridgeFunction { func execute(parameters: [String: Any]) throws -> [String: Any] { BridgeResponse.success(data: ["accepted": false]) } }
    class DismissAll: BridgeFunction { func execute(parameters: [String: Any]) throws -> [String: Any] { BridgeResponse.success(data: ["accepted": false]) } }
}
