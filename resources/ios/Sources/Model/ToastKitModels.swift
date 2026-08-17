import SwiftUI

enum ToastPosition: String { case top, center, bottom }
enum ToastAnimationKind: String { case fade, slide, scale, spring }
enum ToastStrategy: String { case queue, stack }

struct ToastIconConfiguration {
    let name: String?
    let ios: String?
}

struct ToastActionConfiguration {
    let id: String
    let label: String
}

struct ToastStyleConfiguration {
    var background: Color
    var foreground: Color
    var iconColor: Color
    var actionColor: Color
    var cornerRadius: CGFloat
    var padding: CGFloat
    var shadow: Bool
}

struct ToastConfiguration: Identifiable {
    let id: String
    var message: String
    var title: String?
    var variant: String
    var icon: ToastIconConfiguration?
    var position: ToastPosition
    var durationMilliseconds: Int?
    var animation: ToastAnimationKind
    var swipeToDismiss: Bool
    var dismissible: Bool
    var action: ToastActionConfiguration?
    var style: ToastStyleConfiguration
    var strategy: ToastStrategy
    var maxVisible: Int
}

struct ToastState {
    var configuration: ToastConfiguration
    var visible = false
    var terminated = false
}
