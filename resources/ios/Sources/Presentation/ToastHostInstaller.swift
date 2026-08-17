import SwiftUI
import UIKit

@MainActor
final class ToastHostInstaller {
    static let shared = ToastHostInstaller()
    private var windows: [ObjectIdentifier: UIWindow] = [:]
    private var observers: [NSObjectProtocol] = []
    private var installed = false

    func install() {
        guard !installed else { return }
        installed = true
        attachAvailableScenes()
        observers.append(NotificationCenter.default.addObserver(forName: UIScene.didActivateNotification, object: nil, queue: .main) { [weak self] _ in
            Task { @MainActor in self?.attachAvailableScenes() }
        })
        observers.append(NotificationCenter.default.addObserver(forName: UIScene.didDisconnectNotification, object: nil, queue: .main) { [weak self] notification in
            guard let scene = notification.object as? UIWindowScene else { return }
            Task { @MainActor in self?.windows.removeValue(forKey: ObjectIdentifier(scene))?.isHidden = true }
        })
    }

    private func attachAvailableScenes() {
        for case let scene as UIWindowScene in UIApplication.shared.connectedScenes where scene.activationState != .unattached {
            let key = ObjectIdentifier(scene)
            guard windows[key] == nil else { continue }
            let window = PassthroughWindow(windowScene: scene)
            window.windowLevel = .alert - 1
            window.backgroundColor = .clear
            window.rootViewController = UIHostingController(rootView: ToastKitHost())
            window.rootViewController?.view.backgroundColor = .clear
            window.isHidden = false
            windows[key] = window
        }
    }
}

private final class PassthroughWindow: UIWindow {
    override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        let result = super.hitTest(point, with: event)
        return result === rootViewController?.view ? nil : result
    }
}

func initializeToastKit() {
    Task { @MainActor in ToastHostInstaller.shared.install() }
}
