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
        case .slide: return .move(edge: edge(for: toast)).combined(with: .opacity)
        case .scale: return .scale(scale: 0.9).combined(with: .opacity)
        case .spring: return .scale(scale: 0.86).combined(with: .opacity)
        case .snap: return .move(edge: edge(for: toast)).combined(with: .opacity)
        case .pop: return .scale(scale: 0.72).combined(with: .opacity)
        case .reveal:
            return .modifier(
                active: ToastKitRevealModifier(amount: 0.01, anchor: anchor(for: toast)),
                identity: ToastKitRevealModifier(amount: 1, anchor: anchor(for: toast))
            ).combined(with: .opacity)
        case .bounce: return .move(edge: edge(for: toast)).combined(with: .opacity)
        }
    }

    private func edge(for toast: ToastKitConfiguration) -> Edge {
        switch resolvedDirection(for: toast) {
        case .left: return .leading
        case .right: return .trailing
        case .top: return .top
        case .bottom, .auto: return .bottom
        }
    }

    private func anchor(for toast: ToastKitConfiguration) -> UnitPoint {
        switch resolvedDirection(for: toast) {
        case .right: return .trailing
        case .top: return .top
        case .bottom: return .bottom
        default: return .leading
        }
    }

    private func resolvedDirection(for toast: ToastKitConfiguration) -> ToastKitDirection {
        guard toast.direction == .auto else { return toast.direction }
        switch toast.position {
        case .top: return .top
        case .bottom, .center: return .bottom
        }
    }
}

private struct ToastKitRevealModifier: ViewModifier {
    let amount: CGFloat
    let anchor: UnitPoint

    func body(content: Content) -> some View {
        content.scaleEffect(x: amount, y: 1, anchor: anchor).clipped()
    }
}

private struct ToastKitView: View {
    let toast: ToastKitConfiguration
    let reduceMotion: Bool
    @State private var drag: CGSize = .zero

    var body: some View {
        HStack(spacing: 12) {
            if toast.progress == nil && toast.loading {
                ProgressView().tint(toast.style.iconColor).controlSize(.small)
            } else if let icon = toast.icon {
                Image(systemName: icon.ios ?? getIconForName(icon.name ?? "circle"))
                    .font(.system(size: 21, weight: .semibold)).foregroundStyle(toast.style.iconColor)
                    .accessibilityHidden(true)
            }
        VStack(alignment: .leading, spacing: 2) {
            if let title = toast.title {
                Text(title)
                    .font(toastKitFont(toast.titleText, defaultSize: 16, defaultWeight: .semibold))
                    .applyToastKitTextAlignment(toast.titleText?.align)
            }
            Text(toast.message)
                .font(toastKitFont(toast.text, defaultSize: toastKitFontSize(.base), defaultWeight: .medium))
                .applyToastKitTextAlignment(toast.text?.align)
                .fixedSize(horizontal: false, vertical: true)
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
        .overlay(alignment: .bottom) {
            if let progress = toast.progress {
                ProgressView(value: progress, total: 100)
                    .tint(toast.style.actionColor)
                    .padding(.horizontal, toast.style.padding)
                    .padding(.bottom, 4)
                    .animation(reduceMotion ? .linear(duration: 0.1) : .easeInOut(duration: 0.2), value: progress)
            }
        }
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

    @ViewBuilder
    func applyToastKitTextAlignment(_ align: ToastKitTextAlign?) -> some View {
        if let align {
            self
                .multilineTextAlignment(toastKitTextAlignment(align))
                .frame(maxWidth: .infinity, alignment: toastKitFrameAlignment(align))
        } else {
            self
        }
    }
}

private func toastKitFont(_ typography: ToastKitTypographyConfiguration?, defaultSize: CGFloat, defaultWeight: Font.Weight) -> Font {
    let size = typography?.size.map { toastKitFontSize($0) } ?? defaultSize
    let weight = typography?.weight.map { toastKitFontWeight($0) } ?? defaultWeight
    var font: Font
    if let name = typography?.font, !name.isEmpty {
        if let postScriptName = NativeChromeFontResolver.postScriptName(for: name) {
            font = .custom(postScriptName, size: size)
        } else {
            font = .system(size: size, weight: weight)
        }
    } else {
        font = .system(size: size, weight: weight)
    }
    if typography?.italic == true { font = font.italic() }
    return font
}

private func toastKitFontSize(_ size: ToastKitTextSize) -> CGFloat {
    switch size {
    case .xs: return 12
    case .sm: return 14
    case .base: return 15
    case .lg: return 17
    case .xl: return 19
    }
}

private func toastKitFontWeight(_ weight: ToastKitTextWeight) -> Font.Weight {
    switch weight {
    case .normal: return .regular
    case .medium: return .medium
    case .semibold: return .semibold
    case .bold: return .bold
    }
}

private func toastKitTextAlignment(_ align: ToastKitTextAlign?) -> TextAlignment {
    switch align {
    case .left: return .leading
    case .center: return .center
    case .right: return .trailing
    case nil: return .leading
    }
}

private func toastKitFrameAlignment(_ align: ToastKitTextAlign?) -> Alignment {
    switch align {
    case .left: return .leading
    case .center: return .center
    case .right: return .trailing
    case nil: return .leading
    }
}
