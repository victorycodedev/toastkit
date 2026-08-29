import Foundation
import SwiftUI

@MainActor
final class ToastKitManager: ObservableObject {
    static let shared = ToastKitManager()

    @Published private(set) var visible: [ToastKitConfiguration] = []
    private var interactiveFrames: [String: CGRect] = [:]
    private var states: [String: ToastKitState] = [:]
    private var waiting: [String] = []
    private var timers: [String: Task<Void, Never>] = [:]

    func show(_ configuration: ToastKitConfiguration) {
        guard states[configuration.id] == nil else { return }
        states[configuration.id] = ToastKitState(configuration: configuration)
        if canAdmit(configuration) { admit(configuration.id) } else { waiting.append(configuration.id) }
    }

    func update(_ id: String, changes: [String: Any]) throws {
        guard let state = states[id], !state.terminated else { return }
        let updated = try ToastKitBridgeNormalizer.applying(changes, to: state.configuration)
        var next = state
        next.configuration = updated
        states[id] = next
        if state.visible {
            if let index = visible.firstIndex(where: { $0.id == id }) { visible[index] = updated }
            if changes.keys.contains("duration") || changes.keys.contains("persistent") { schedule(updated) }
        }
    }

    func dismiss(_ id: String, reason: String = "programmatic") { terminate(id, reason: reason) }

    func dismissAll() {
        for id in Array(states.keys) { terminate(id, reason: "programmatic", shouldPromote: false) }
        waiting.removeAll()
        promote()
    }

    func pressAction(toastId: String, actionId: String) {
        guard let state = states[toastId], !state.terminated else { return }
        ToastKitEventDispatcher.action(toastId, actionId: actionId)
        terminate(toastId, reason: "action")
    }

    func updateInteractiveFrames(_ frames: [String: CGRect]) {
        interactiveFrames = frames
    }

    func containsInteractivePoint(_ point: CGPoint) -> Bool {
        interactiveFrames.values.contains { $0.insetBy(dx: -8, dy: -8).contains(point) }
    }

    private func canAdmit(_ configuration: ToastKitConfiguration) -> Bool {
        switch configuration.strategy {
        case .queue: return visible.isEmpty
        case .stack:
            return !visible.contains(where: { $0.strategy == .queue }) &&
                visible.filter { $0.strategy == .stack }.count < configuration.maxVisible
        }
    }

    private func admit(_ id: String) {
        guard var state = states[id], !state.terminated, !state.visible else { return }
        state.visible = true
        states[id] = state
        withAnimation(animation(for: state.configuration)) {
            visible.append(state.configuration)
        }
        schedule(state.configuration)
        ToastKitEventDispatcher.shown(id)
    }

    private func schedule(_ configuration: ToastKitConfiguration) {
        timers.removeValue(forKey: configuration.id)?.cancel()
        guard let milliseconds = configuration.durationMilliseconds else { return }
        timers[configuration.id] = Task { [weak self] in
            try? await Task.sleep(for: .milliseconds(milliseconds))
            guard !Task.isCancelled else { return }
            self?.dismiss(configuration.id, reason: "timeout")
        }
    }

    private func terminate(_ id: String, reason: String, shouldPromote: Bool = true) {
        guard var state = states[id], !state.terminated else { return }
        state.terminated = true
        states[id] = state
        timers.removeValue(forKey: id)?.cancel()
        waiting.removeAll { $0 == id }
        withAnimation(animation(for: state.configuration)) { visible.removeAll { $0.id == id } }
        states.removeValue(forKey: id)
        ToastKitEventDispatcher.dismissed(id, reason: reason)
        releaseIdentity(for: state)
        if shouldPromote { promote() }
    }

    private func releaseIdentity(for state: ToastKitState) {
        guard state.visible else {
            ToastKitUniqueRegistry.shared.release(state.configuration)
            return
        }
        let milliseconds = switch state.configuration.animation {
        case .spring, .bounce: 450
        case .reveal: 280
        default: 260
        }
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(milliseconds))
            ToastKitUniqueRegistry.shared.release(state.configuration)
        }
    }

    private func promote() {
        while let id = waiting.first {
            guard let state = states[id], !state.terminated else { waiting.removeFirst(); continue }
            guard canAdmit(state.configuration) else { break }
            waiting.removeFirst()
            admit(id)
        }
    }

    private func animation(for toast: ToastKitConfiguration) -> Animation {
        switch toast.animation {
        case .fade, .slide, .scale, .reveal: return .easeInOut(duration: 0.2)
        case .snap: return .spring(response: 0.22, dampingFraction: 0.62)
        case .spring: return .spring(response: 0.35, dampingFraction: 0.78)
        case .pop: return .spring(response: 0.28, dampingFraction: 0.58)
        case .bounce: return .spring(response: 0.48, dampingFraction: 0.55)
        }
    }
}
