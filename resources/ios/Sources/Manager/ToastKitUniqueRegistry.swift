import Foundation

struct ToastKitShowResult {
    let id: String
    let accepted: Bool
}

final class ToastKitUniqueRegistry: @unchecked Sendable {
    static let shared = ToastKitUniqueRegistry()

    private let lock = NSLock()
    private var activeIds: Set<String> = []
    private var uniqueToId: [String: String] = [:]

    func reserve(_ configuration: ToastKitConfiguration) -> ToastKitShowResult {
        lock.lock()
        defer { lock.unlock() }

        if let key = configuration.uniqueKey, let existing = uniqueToId[key] {
            return ToastKitShowResult(id: existing, accepted: false)
        }
        guard activeIds.insert(configuration.id).inserted else {
            return ToastKitShowResult(id: configuration.id, accepted: false)
        }
        if let key = configuration.uniqueKey { uniqueToId[key] = configuration.id }
        return ToastKitShowResult(id: configuration.id, accepted: true)
    }

    func resolve(_ key: String) throws -> String {
        lock.lock()
        defer { lock.unlock() }
        guard let id = uniqueToId[key] else {
            throw ToastKitInputError.invalid("Unique toast [\(key)] is not active.")
        }
        return id
    }

    func release(_ configuration: ToastKitConfiguration) {
        lock.lock()
        defer { lock.unlock() }
        activeIds.remove(configuration.id)
        if let key = configuration.uniqueKey, uniqueToId[key] == configuration.id {
            uniqueToId.removeValue(forKey: key)
        }
    }
}
