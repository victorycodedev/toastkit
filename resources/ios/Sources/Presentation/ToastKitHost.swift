import SwiftUI

struct ToastKitHost: View {
    @ObservedObject private var manager = ToastKitManager.shared
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                host(for: .top, alignment: .top)
                host(for: .center, alignment: .center)
                host(for: .bottom, alignment: .bottom)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .frame(width: proxy.size.width, height: proxy.size.height)
        }
        .onPreferenceChange(ToastKitFramePreferenceKey.self) { frames in
            manager.updateInteractiveFrames(frames)
        }
        .allowsHitTesting(!manager.visible.isEmpty)
        .ignoresSafeArea(.keyboard)
    }

    @ViewBuilder
    private func host(for position: ToastKitPosition, alignment: Alignment) -> some View {
        VStack(spacing: 8) {
            ForEach(manager.visible.filter { $0.position == position }) { toast in
                ToastKitView(toast: toast, reduceMotion: reduceMotion)
                    .transition(transition(for: toast))
            }
        }
        .frame(maxWidth: 560)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: alignment)
    }

    private func transition(for toast: ToastKitConfiguration) -> AnyTransition {
        if reduceMotion { return .opacity }
        switch toast.animation {
        case .fade: return .opacity
        case .slide: return .move(edge: toast.position == .top ? .top : .bottom).combined(with: .opacity)
        case .scale: return .scale(scale: 0.9).combined(with: .opacity)
        case .spring: return .scale(scale: 0.86).combined(with: .opacity)
        }
    }
}

private struct ToastKitView: View {
    let toast: ToastKitConfiguration
    let reduceMotion: Bool
    @State private var drag: CGSize = .zero

    var body: some View {
        HStack(spacing: 12) {
            if let icon = toast.icon {
                Image(systemName: icon.ios ?? getIconForName(icon.name ?? "circle"))
                    .font(.system(size: 21, weight: .semibold)).foregroundStyle(toast.style.iconColor)
                    .accessibilityHidden(true)
            }
            VStack(alignment: .leading, spacing: 2) {
                if let title = toast.title { Text(title).font(.subheadline.weight(.semibold)) }
                Text(toast.message).font(.subheadline).fixedSize(horizontal: false, vertical: true)
            }
            .foregroundStyle(toast.style.foreground).frame(maxWidth: .infinity, alignment: .leading)
            if let action = toast.action {
                Button(action.label) { ToastKitManager.shared.pressAction(toastId: toast.id, actionId: action.id) }
                    .font(.subheadline.weight(.bold)).foregroundStyle(toast.style.actionColor)
            }
            if toast.dismissible {
                Button { ToastKitManager.shared.dismiss(toast.id) } label: {
                    Image(systemName: "xmark").font(.system(size: 13, weight: .bold)).frame(width: 28, height: 28)
                }
                .foregroundStyle(toast.style.foreground).accessibilityLabel("Dismiss")
            }
        }
        .padding(toast.style.padding)
        .background(toast.style.background, in: RoundedRectangle(cornerRadius: toast.style.cornerRadius, style: .continuous))
        .shadow(color: toast.style.shadow ? .black.opacity(0.22) : .clear, radius: 10, y: 4)
        .offset(drag)
        .opacity(Double(max(CGFloat(0.35), CGFloat(1) - (abs(drag.width) + abs(drag.height)) / CGFloat(700))))
        .contentShape(Rectangle())
        .background {
            GeometryReader { proxy in
                Color.clear.preference(
                    key: ToastKitFramePreferenceKey.self,
                    value: [toast.id: proxy.frame(in: .global)]
                )
            }
        }
        .toastKitConditional(toast.swipeToDismiss) { $0.gesture(dragGesture) }
        .animation(reduceMotion ? .linear(duration: 0.1) : .spring(response: 0.3, dampingFraction: 0.78), value: drag)
    }

    private var dragGesture: some Gesture {
        DragGesture(minimumDistance: 8)
            .onChanged { value in
                switch toast.position {
                case .top: drag.height = min(0, value.translation.height)
                case .bottom: drag.height = max(0, value.translation.height)
                case .center: drag.width = value.translation.width
                }
            }
            .onEnded { value in
                let projected = toast.position == .center ? value.predictedEndTranslation.width : value.predictedEndTranslation.height
                let distance = toast.position == .center ? abs(value.translation.width) : abs(value.translation.height)
                if distance > 80 || abs(projected) > 180 { ToastKitManager.shared.dismiss(toast.id, reason: "swipe") }
                else { drag = .zero }
            }
    }
}

private struct ToastKitFramePreferenceKey: PreferenceKey {
    static var defaultValue: [String: CGRect] = [:]

    static func reduce(value: inout [String: CGRect], nextValue: () -> [String: CGRect]) {
        value.merge(nextValue(), uniquingKeysWith: { _, latest in latest })
    }
}

private extension View {
    @ViewBuilder
    func toastKitConditional<Content: View>(_ condition: Bool, transform: (Self) -> Content) -> some View {
        if condition { transform(self) } else { self }
    }
}
