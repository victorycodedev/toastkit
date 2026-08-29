import SwiftUI

enum ToastKitPosition: String { case top, center, bottom }
enum ToastKitAnimationKind: String { case fade, slide, scale, spring, snap, pop, reveal, bounce }
enum ToastKitDirection: String { case auto, left, right, top, bottom }
enum ToastKitStrategy: String { case queue, stack }
enum ToastKitTextSize: String { case xs, sm, base, lg, xl }
enum ToastKitTextWeight: String { case normal, medium, semibold, bold }
enum ToastKitTextAlign: String { case left, center, right }

struct ToastKitIconConfiguration {
    let name: String?
    let ios: String?
}

struct ToastKitActionConfiguration {
    let id: String
    let label: String
}

struct ToastKitTypographyConfiguration {
    var font: String?
    var size: ToastKitTextSize?
    var weight: ToastKitTextWeight?
    var align: ToastKitTextAlign?
    var italic: Bool?
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
    let uniqueKey: String?
    var nativeIos: Bool
    var nativeAndroid: Bool
    var message: String
    var title: String?
    var variant: String
    var icon: ToastKitIconConfiguration?
    var position: ToastKitPosition
    var durationMilliseconds: Int?
    var animation: ToastKitAnimationKind
    var direction: ToastKitDirection
    var progress: Double?
    var loading: Bool
    var swipeToDismiss: Bool
    var dismissible: Bool
    var action: ToastKitActionConfiguration?
    var style: ToastKitStyleConfiguration
    var strategy: ToastKitStrategy
    var maxVisible: Int
    var text: ToastKitTypographyConfiguration?
    var titleText: ToastKitTypographyConfiguration?
}

struct ToastKitState {
    var configuration: ToastKitConfiguration
    var visible = false
    var terminated = false
}
