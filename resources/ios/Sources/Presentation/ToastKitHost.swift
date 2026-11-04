import SwiftUI

private struct ToastKitIconView: View {
    let name: String
    let pointSize: CGFloat

    var body: some View {
        if let asset = NativeIcon.customAssetName(name) {
            Image(asset)
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .frame(width: pointSize, height: pointSize)
        } else {
            Image(systemName: getIconForName(name))
                .font(.system(size: pointSize, weight: .semibold))
        }
    }
}

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
            ForEach(manager.visible.filter { effectivePosition(for: $0) == position }) { toast in
                ToastKitView(toast: toast, reduceMotion: reduceMotion)
                    .transition(transition(for: toast))
            }
        }
        .frame(maxWidth: 560)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: alignment)
    }

    private func transition(for toast: ToastKitConfiguration) -> AnyTransition {
        if reduceMotion || (toast.nativeIos && toast.presentation == .standard) { return .opacity }
        let transition: AnyTransition
        switch toast.animation {
        case .fade: transition = .opacity
        case .slide: transition = .move(edge: edge(for: toast)).combined(with: .opacity)
        case .scale: transition = .scale(scale: 0.9).combined(with: .opacity)
        case .spring: transition = .scale(scale: 0.86).combined(with: .opacity)
        case .snap: transition = .move(edge: edge(for: toast)).combined(with: .opacity)
        case .pop: transition = .scale(scale: 0.72).combined(with: .opacity)
        case .reveal:
            transition = .modifier(
                active: ToastKitRevealModifier(amount: 0.01, anchor: anchor(for: toast)),
                identity: ToastKitRevealModifier(amount: 1, anchor: anchor(for: toast))
            ).combined(with: .opacity)
        case .bounce: transition = .move(edge: edge(for: toast)).combined(with: .opacity)
        }
        guard toast.presentation == .dynamic else { return transition }
        return transition.combined(with: .modifier(
            active: ToastKitDynamicPillModifier(amount: 0.18),
            identity: ToastKitDynamicPillModifier(amount: 1)
        ))
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
        if toast.presentation == .dynamic { return .top }
        guard toast.direction == .auto else { return toast.direction }
        switch effectivePosition(for: toast) {
        case .top: return .top
        case .bottom, .center: return .bottom
        }
    }
}

private struct ToastKitDynamicPillModifier: ViewModifier {
    let amount: CGFloat

    func body(content: Content) -> some View {
        content.scaleEffect(x: amount, y: 1)
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
    @State private var dynamicExpanded = false
    @State private var dynamicDropExtended = false

    var body: some View {
        Group {
            if toast.presentation == .dynamic {
                dynamicContent
            } else if toast.nativeIos {
                nativeContent
            } else {
                customContent
            }
        }
        .offset(drag)
        .opacity(Double(max(CGFloat(0.35), CGFloat(1) - (abs(drag.width) + abs(drag.height)) / CGFloat(700))))
        .toastKitConfiguredOpacity(toast.opacity)
        .contentShape(Rectangle())
        .toastKitConditional(!toast.data.isEmpty) {
            $0.onTapGesture { ToastKitManager.shared.press(toast.id) }
                .accessibilityAction(named: Text("Open notification")) {
                    ToastKitManager.shared.press(toast.id)
                }
        }
        .background {
            GeometryReader { proxy in
                Color.clear.preference(
                    key: ToastKitFramePreferenceKey.self,
                    value: [toast.id: proxy.frame(in: .global)]
                )
                .allowsHitTesting(false)
            }
        }
        .toastKitConditional(toast.swipeToDismiss) { $0.gesture(dragGesture) }
        .accessibilityElement(children: .contain)
        .animation(reduceMotion ? .linear(duration: 0.1) : .spring(response: 0.3, dampingFraction: 0.78), value: drag)
        .task(id: toast.presentation) {
            guard toast.presentation == .dynamic else {
                dynamicExpanded = false
                dynamicDropExtended = false
                return
            }
            withAnimation(dynamicAnimation) { dynamicDropExtended = true }
            if !reduceMotion { try? await Task.sleep(for: .milliseconds(70)) }
            withAnimation(dynamicAnimation) { dynamicExpanded = true }
            if !reduceMotion { try? await Task.sleep(for: .milliseconds(180)) }
            withAnimation(dynamicAnimation) { dynamicDropExtended = false }
        }
    }

    private var dynamicContent: some View {
        VStack(spacing: 0) {
            HStack(spacing: 10) {
                if toast.progress == nil && toast.loading {
                    ProgressView()
                        .tint(toast.style.iconColor)
                        .controlSize(.small)
                        .frame(width: 24, height: 24)
                } else {
                    let icon = toast.icon
                    ToastKitIconView(
                        name: icon?.ios ?? icon?.name ?? "bell.fill",
                        pointSize: toastKitIconSize(toast.iconSize, fallback: 20)
                    )
                        .foregroundStyle(toast.style.iconColor)
                        .frame(width: 24, height: 24)
                        .accessibilityHidden(true)
                }

                if dynamicExpanded {
                    HStack(spacing: 6) {
                        VStack(alignment: .leading, spacing: 2) {
                            if let title = toast.title {
                                Text(title)
                                    .font(toastKitFont(toast.titleText, defaultSize: 15, defaultWeight: .semibold))
                                    .lineLimit(1)
                                    .contentTransition(.opacity)
                                    .animation(contentUpdateAnimation, value: title)
                            }
                            Text(toast.message)
                                .font(toastKitFont(toast.text, defaultSize: 14, defaultWeight: .medium))
                                .lineLimit(2)
                                .contentTransition(.opacity)
                                .animation(contentUpdateAnimation, value: toast.message)
                        }
                        .foregroundStyle(toast.style.foreground)
                        .frame(minWidth: 120, maxWidth: 300, alignment: .leading)

                        ForEach(toast.actions, id: \.id) { action in
                            Button(action.label) {
                                ToastKitManager.shared.pressAction(toastId: toast.id, actionId: action.id)
                            }
                            .buttonStyle(.plain)
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(action.color ?? toast.style.actionColor)
                        }
                        if toast.dismissible {
                            Button { ToastKitManager.shared.dismiss(toast.id) } label: {
                                Image(systemName: "xmark")
                                    .font(.system(size: 12, weight: .bold))
                                    .frame(minWidth: 44, minHeight: 44)
                            }
                            .buttonStyle(.plain)
                            .foregroundStyle(toast.style.foreground)
                            .accessibilityLabel("Dismiss")
                        }
                    }
                    .transition(.move(edge: .leading).combined(with: .opacity))
                }
            }
            .padding(toast.style.padding)

            if dynamicExpanded, let progress = toast.progress {
                ProgressView(value: progress, total: 100)
                    .tint(toast.style.actionColor)
                    .animation(reduceMotion ? .linear(duration: 0.1) : .easeInOut(duration: 0.2), value: progress)
            }
        }
        .background(
            toast.style.background,
            in: RoundedRectangle(cornerRadius: toast.style.cornerRadius, style: .continuous)
        )
        .clipShape(RoundedRectangle(cornerRadius: toast.style.cornerRadius, style: .continuous))
        .shadow(color: toast.style.shadow ? .black.opacity(0.28) : .clear, radius: 12, y: 5)
        .overlay(alignment: .top) {
            ToastKitDropShape()
                .fill(toast.style.background)
                .frame(width: 20, height: 26)
                .scaleEffect(x: 1, y: dynamicDropExtended ? 1 : 0, anchor: .top)
                .offset(y: -20)
                .allowsHitTesting(false)
        }
        .animation(dynamicAnimation, value: dynamicExpanded)
    }

    private var customContent: some View {
        HStack(spacing: 12) {
            if toast.progress == nil && toast.loading {
                ProgressView().tint(toast.style.iconColor).controlSize(.small)
            } else if let icon = toast.icon {
                ToastKitIconView(
                    name: icon.ios ?? icon.name ?? "circle",
                    pointSize: toastKitIconSize(toast.iconSize, fallback: 21)
                )
                    .foregroundStyle(toast.style.iconColor)
                    .accessibilityHidden(true)
            }
        VStack(alignment: .leading, spacing: 2) {
            Group {
                if let title = toast.title {
                    Text(title)
                        .font(toastKitFont(toast.titleText, defaultSize: 16, defaultWeight: .semibold))
                        .applyToastKitTextAlignment(toast.titleText?.align)
                        .contentTransition(.opacity)
                }
            }
            .transition(.opacity)
            .animation(contentUpdateAnimation, value: toast.title)
            Text(toast.message)
                .font(toastKitFont(toast.text, defaultSize: toastKitFontSize(.base), defaultWeight: .medium))
                .applyToastKitTextAlignment(toast.text?.align)
                .fixedSize(horizontal: false, vertical: true)
                .contentTransition(.opacity)
                .animation(contentUpdateAnimation, value: toast.message)
        }
        .foregroundStyle(toast.style.foreground).frame(maxWidth: .infinity, alignment: .leading)
            ForEach(toast.actions, id: \.id) { action in
                Button(action.label) { ToastKitManager.shared.pressAction(toastId: toast.id, actionId: action.id) }
                    .font(.subheadline.weight(.bold)).foregroundStyle(action.color ?? toast.style.actionColor)
            }
            if toast.dismissible {
                Button { ToastKitManager.shared.dismiss(toast.id) } label: {
                    Image(systemName: "xmark").font(.system(size: 13, weight: .bold)).frame(minWidth: 44, minHeight: 44)
                }
                .foregroundStyle(toast.style.foreground).accessibilityLabel("Dismiss")
            }
        }
        .padding(toast.style.padding)
        .background(toast.style.background, in: RoundedRectangle(cornerRadius: toast.style.cornerRadius, style: .continuous))
        .shadow(color: toast.style.shadow ? .black.opacity(0.22) : .clear, radius: 10, y: 4)
        .overlay(alignment: .bottom) {
            if let progress = toast.progress {
                ProgressView(value: progress, total: 100)
                    .tint(toast.style.actionColor)
                    .padding(.horizontal, toast.style.padding)
                    .padding(.bottom, 4)
                    .animation(reduceMotion ? .linear(duration: 0.1) : .easeInOut(duration: 0.2), value: progress)
            }
        }
    }

    private var nativeContent: some View {
        HStack(spacing: 12) {
            if toast.progress == nil && toast.loading {
                ProgressView().controlSize(.small)
            } else if let icon = toast.icon {
                ToastKitIconView(name: icon.ios ?? icon.name ?? "circle", pointSize: 20)
                    .accessibilityHidden(true)
            }
            VStack(alignment: .leading, spacing: 2) {
                Group {
                    if let title = toast.title {
                        Text(title)
                            .font(.headline)
                            .contentTransition(.opacity)
                    }
                }
                .transition(.opacity)
                .animation(contentUpdateAnimation, value: toast.title)
                Text(toast.message)
                    .font(.subheadline)
                    .fixedSize(horizontal: false, vertical: true)
                    .contentTransition(.opacity)
                    .animation(contentUpdateAnimation, value: toast.message)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            ForEach(toast.actions, id: \.id) { action in
                Button(action.label) { ToastKitManager.shared.pressAction(toastId: toast.id, actionId: action.id) }
                    .buttonStyle(.borderless)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(action.color ?? toast.style.actionColor)
            }
            if toast.dismissible {
                Button { ToastKitManager.shared.dismiss(toast.id) } label: {
                    Image(systemName: "xmark").frame(minWidth: 44, minHeight: 44)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Dismiss")
            }
        }
        .padding()
        .toastKitNativeAppearance()
        .overlay(alignment: .bottom) {
            if let progress = toast.progress {
                ProgressView(value: progress, total: 100)
                    .padding(.horizontal)
                    .padding(.bottom, 4)
                    .animation(reduceMotion ? .linear(duration: 0.1) : .easeInOut(duration: 0.2), value: progress)
            }
        }
    }

    private var dragGesture: some Gesture {
        DragGesture(minimumDistance: 8)
            .onChanged { value in
                switch effectivePosition(for: toast) {
                case .top: drag.height = min(0, value.translation.height)
                case .bottom: drag.height = max(0, value.translation.height)
                case .center: drag.width = value.translation.width
                }
            }
            .onEnded { value in
                let projected = effectivePosition(for: toast) == .center ? value.predictedEndTranslation.width : value.predictedEndTranslation.height
                let distance = effectivePosition(for: toast) == .center ? abs(value.translation.width) : abs(value.translation.height)
                if distance > 80 || abs(projected) > 180 { ToastKitManager.shared.dismiss(toast.id, reason: "swipe") }
                else { drag = .zero }
            }
    }

    private var contentUpdateAnimation: Animation {
        reduceMotion ? .linear(duration: 0.01) : .easeInOut(duration: 0.16)
    }

    private var dynamicAnimation: Animation {
        reduceMotion ? .linear(duration: 0.01) : .spring(response: 0.34, dampingFraction: 0.8)
    }
}

private struct ToastKitDropShape: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.width * 0.36, y: 0))
        path.addCurve(
            to: CGPoint(x: rect.width * 0.15, y: rect.height * 0.66),
            control1: CGPoint(x: rect.width * 0.38, y: rect.height * 0.38),
            control2: CGPoint(x: rect.width * 0.30, y: rect.height * 0.48)
        )
        path.addCurve(
            to: CGPoint(x: rect.width * 0.50, y: rect.height),
            control1: CGPoint(x: 0, y: rect.height * 0.84),
            control2: CGPoint(x: rect.width * 0.18, y: rect.height)
        )
        path.addCurve(
            to: CGPoint(x: rect.width * 0.85, y: rect.height * 0.66),
            control1: CGPoint(x: rect.width * 0.82, y: rect.height),
            control2: CGPoint(x: rect.width, y: rect.height * 0.84)
        )
        path.addCurve(
            to: CGPoint(x: rect.width * 0.64, y: 0),
            control1: CGPoint(x: rect.width * 0.70, y: rect.height * 0.48),
            control2: CGPoint(x: rect.width * 0.62, y: rect.height * 0.38)
        )
        path.closeSubpath()
        return path
    }
}

private struct ToastKitFramePreferenceKey: PreferenceKey {
    static var defaultValue: [String: CGRect] = [:]

    static func reduce(value: inout [String: CGRect], nextValue: () -> [String: CGRect]) {
        value.merge(nextValue(), uniquingKeysWith: { _, latest in latest })
    }
}

private func effectivePosition(for toast: ToastKitConfiguration) -> ToastKitPosition {
    toast.presentation == .dynamic ? .top : toast.position
}

private extension View {
    @ViewBuilder
    func toastKitConfiguredOpacity(_ opacity: Double?) -> some View {
        if let opacity { self.opacity(opacity) } else { self }
    }

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

    @ViewBuilder
    func toastKitNativeAppearance() -> some View {
        if #available(iOS 26.0, *) {
            self.glassEffect(.regular, in: .rect(cornerRadius: 16))
        } else {
            self.background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
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

private func toastKitIconSize(_ size: ToastKitIconSize?, fallback: CGFloat) -> CGFloat {
    switch size {
    case .sm: return 18
    case .md: return 24
    case .lg: return 32
    case nil: return fallback
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
