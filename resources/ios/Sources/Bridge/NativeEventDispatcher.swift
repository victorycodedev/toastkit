import Foundation

enum NativeEventDispatcher {
    private static let shownEvent = "Victorycodedev\\ToastKit\\Events\\ToastShown"
    private static let dismissedEvent = "Victorycodedev\\ToastKit\\Events\\ToastDismissed"
    private static let actionEvent = "Victorycodedev\\ToastKit\\Events\\ToastActionPressed"

    static func shown(_ id: String) { send(shownEvent, ["toastId": id]) }
    static func dismissed(_ id: String, reason: String) { send(dismissedEvent, ["toastId": id, "reason": reason]) }
    static func action(_ id: String, actionId: String) { send(actionEvent, ["toastId": id, "actionId": actionId]) }

    private static func send(_ event: String, _ payload: [String: Any?]) {
        LaravelBridge.shared.send?(event, payload)
    }
}
