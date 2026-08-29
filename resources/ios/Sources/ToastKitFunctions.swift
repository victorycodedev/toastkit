import Foundation

enum ToastKitFunctions {
    final class Show: BridgeFunction {
        func execute(parameters: [String: Any]) throws -> [String: Any] {
            do {
                let toast = try ToastKitBridgeNormalizer.show(parameters)
                let result = ToastKitUniqueRegistry.shared.reserve(toast)
                if result.accepted {
                    Task { @MainActor in ToastKitManager.shared.show(toast) }
                }
                return BridgeResponse.success(data: ["id": result.id, "accepted": result.accepted])
            } catch { return BridgeResponse.error(code: "TOASTKIT_INVALID_ARGUMENT", message: ToastKitFunctions.message(for: error)) }
        }
    }

    final class UpdateUnique: BridgeFunction {
        func execute(parameters: [String: Any]) throws -> [String: Any] {
            do {
                let (key, changes) = try ToastKitBridgeNormalizer.updateUnique(parameters)
                let id = try ToastKitUniqueRegistry.shared.resolve(key)
                Task { @MainActor in try? ToastKitManager.shared.update(id, changes: changes) }
                return BridgeResponse.success(data: ["id": id, "unique_key": key, "accepted": true])
            } catch { return BridgeResponse.error(code: "TOASTKIT_INVALID_ARGUMENT", message: ToastKitFunctions.message(for: error)) }
        }
    }

    final class Update: BridgeFunction {
        func execute(parameters: [String: Any]) throws -> [String: Any] {
            do {
                let (id, changes) = try ToastKitBridgeNormalizer.update(parameters)
                Task { @MainActor in try? ToastKitManager.shared.update(id, changes: changes) }
                return BridgeResponse.success(data: ["id": id, "accepted": true])
            } catch { return BridgeResponse.error(code: "TOASTKIT_INVALID_ARGUMENT", message: ToastKitFunctions.message(for: error)) }
        }
    }

    final class DismissUnique: BridgeFunction {
        func execute(parameters: [String: Any]) throws -> [String: Any] {
            do {
                let key = try ToastKitBridgeNormalizer.uniqueKey(parameters)
                let id = try ToastKitUniqueRegistry.shared.resolve(key)
                Task { @MainActor in ToastKitManager.shared.dismiss(id) }
                return BridgeResponse.success(data: ["id": id, "unique_key": key, "accepted": true])
            } catch { return BridgeResponse.error(code: "TOASTKIT_INVALID_ARGUMENT", message: ToastKitFunctions.message(for: error)) }
        }
    }

    final class Dismiss: BridgeFunction {
        func execute(parameters: [String: Any]) throws -> [String: Any] {
            do {
                let id = try ToastKitBridgeNormalizer.id(parameters)
                Task { @MainActor in ToastKitManager.shared.dismiss(id) }
                return BridgeResponse.success(data: ["id": id, "accepted": true])
            } catch { return BridgeResponse.error(code: "TOASTKIT_INVALID_ARGUMENT", message: ToastKitFunctions.message(for: error)) }
        }
    }

    final class DismissAll: BridgeFunction {
        func execute(parameters: [String: Any]) throws -> [String: Any] {
            Task { @MainActor in ToastKitManager.shared.dismissAll() }
            return BridgeResponse.success(data: ["accepted": true])
        }
    }

    private static func message(for error: Error) -> String {
        (error as? LocalizedError)?.errorDescription ?? "Invalid ToastKit payload"
    }
}
