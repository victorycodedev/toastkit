import SwiftUI

enum ToastKitPosition: String { case top, center, bottom }
enum ToastKitAnimationKind: String { case fade, slide, scale, spring }
enum ToastKitStrategy: String { case queue, stack }

struct ToastKitIconConfiguration {
    let name: String?
    let ios: String?
}

struct ToastKitActionConfiguration {
    let id: String
    let label: String
}

struct ToastKitStyleConfiguration {
    var background: Color
    var foreground: Color
    var iconColor: Color
    var actionColor: Color
    var cornerRadius: CGFloat
    var padding: CGFloat
    var shadow: Bool
}

struct ToastKitConfiguration: Identifiable {
    let id: String
    var message: String
    var title: String?
    var variant: String
    var icon: ToastKitIconConfiguration?
    var position: ToastKitPosition
    var durationMilliseconds: Int?
    var animation: ToastKitAnimationKind
    var swipeToDismiss: Bool
    var dismissible: Bool
    var action: ToastKitActionConfiguration?
    var style: ToastKitStyleConfiguration
    var strategy: ToastKitStrategy
    var maxVisible: Int
}

struct ToastKitState {
    var configuration: ToastKitConfiguration
    var visible = false
    var terminated = false
}
