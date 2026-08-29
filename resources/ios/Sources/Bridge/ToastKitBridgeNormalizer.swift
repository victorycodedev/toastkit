import SwiftUI

enum ToastKitInputError: LocalizedError {
    case invalid(String)
    var errorDescription: String? { if case .invalid(let message) = self { message } else { "Invalid ToastKit payload" } }
}

enum ToastKitBridgeNormalizer {
    static func show(_ values: [String: Any]) throws -> ToastKitConfiguration {
        let version = (values["contract_version"] as? NSNumber)?.intValue ?? 1
        guard version == 1 else { throw ToastKitInputError.invalid("Unsupported contract_version: \(version)") }
        return try configuration(values)
    }

    static func update(_ values: [String: Any]) throws -> (String, [String: Any]) {
        let id = try requiredString(values, "id")
        guard let changes = values["changes"] as? [String: Any], !changes.isEmpty else {
            throw ToastKitInputError.invalid("changes must be a non-empty object")
        }
        return (id, changes)
    }

    static func updateUnique(_ values: [String: Any]) throws -> (String, [String: Any]) {
        let key = try requiredString(values, "unique_key")
        guard let changes = values["changes"] as? [String: Any], !changes.isEmpty else {
            throw ToastKitInputError.invalid("changes must be a non-empty object")
        }
        return (key, changes)
    }

    static func id(_ values: [String: Any]) throws -> String { try requiredString(values, "id") }
    static func uniqueKey(_ values: [String: Any]) throws -> String { try requiredString(values, "unique_key") }

    static func applying(_ changes: [String: Any], to current: ToastKitConfiguration) throws -> ToastKitConfiguration {
        var next = current
        if let value = changes["message"] { next.message = try nonEmptyString(value, "message") }
        if changes.keys.contains("title") { next.title = try nullableString(changes["title"], "title") }
        if let value = changes["variant"] {
            let name = try variant(value)
            next.variant = name
            next.style = variantStyle(name)
            next.icon = defaultIcon(name)
        }
        if let value = changes["icon"] { next.icon = try icon(value) }
        if let value = changes["position"] { next.position = try enumValue(value, "position") }
        if let value = changes["animation"] { next.animation = try enumValue(value, "animation") }
        if let value = changes["direction"] { next.direction = try enumValue(value, "direction") }
        if changes.keys.contains("progress") { next.progress = try changes["progress"].map(progress) }
        if let value = changes["loading"] { next.loading = try boolean(value, "loading") }
        if let value = changes["swipe_to_dismiss"] { next.swipeToDismiss = try boolean(value, "swipe_to_dismiss") }
        if let value = changes["dismissible"] { next.dismissible = try boolean(value, "dismissible") }
        if changes.keys.contains("action") { next.action = try changes["action"].map(action) }
        if let value = changes["strategy"] { next.strategy = try enumValue(value, "strategy") }
        if let value = changes["max_visible"] { next.maxVisible = try positiveInt(value, "max_visible") }
        if let value = changes["style"] { next.style = try style(value, fallback: next.style) }
        if changes.keys.contains("text") { next.text = try mergeTypography(next.text, changes["text"], "text") }
        if changes.keys.contains("title_text") { next.titleText = try mergeTypography(next.titleText, changes["title_text"], "title_text") }

        if changes.keys.contains("duration") || changes.keys.contains("persistent") {
            let persistent = try changes["persistent"].map { try boolean($0, "persistent") } ?? false
            next.durationMilliseconds = persistent ? nil : try changes["duration"].map { try positiveInt($0, "duration") } ?? 3000
        }
        return next
    }

    private static func configuration(_ values: [String: Any]) throws -> ToastKitConfiguration {
        let variantName = try variant(values["variant"] ?? "neutral")
        let persistent = try values["persistent"].map { try boolean($0, "persistent") } ?? false
        return ToastKitConfiguration(
            id: try requiredString(values, "id"),
            uniqueKey: try nullableString(values["unique_key"], "unique_key"),
            message: try requiredString(values, "message"),
            title: try nullableString(values["title"], "title"),
            variant: variantName,
            icon: try values["icon"].map(icon),
            position: try enumValue(values["position"] ?? "bottom", "position"),
            durationMilliseconds: persistent ? nil : try positiveInt(values["duration"] ?? 3000, "duration"),
            animation: try enumValue(values["animation"] ?? "scale", "animation"),
            direction: try enumValue(values["direction"] ?? "auto", "direction"),
            progress: try values["progress"].map(progress),
            loading: try values["loading"].map { try boolean($0, "loading") } ?? false,
            swipeToDismiss: try values["swipe_to_dismiss"].map { try boolean($0, "swipe_to_dismiss") } ?? true,
            dismissible: try values["dismissible"].map { try boolean($0, "dismissible") } ?? false,
            action: try values["action"].map(action),
            style: try style(values["style"] ?? [String: Any](), fallback: variantStyle(variantName)),
            strategy: try enumValue(values["strategy"] ?? "queue", "strategy"),
            maxVisible: try positiveInt(values["max_visible"] ?? 3, "max_visible"),
            text: try values["text"].map(typography),
            titleText: try values["title_text"].map(typography)
        )
    }

    private static func icon(_ value: Any) throws -> ToastKitIconConfiguration {
        guard let map = value as? [String: Any] else { throw ToastKitInputError.invalid("icon must be an object") }
        let name = try nullableString(map["name"], "icon.name")
        let ios = try nullableString(map["ios"], "icon.ios")
        guard name != nil || ios != nil else { throw ToastKitInputError.invalid("icon requires name or ios") }
        return ToastKitIconConfiguration(name: name, ios: ios)
    }

    private static func action(_ value: Any) throws -> ToastKitActionConfiguration {
        guard let map = value as? [String: Any] else { throw ToastKitInputError.invalid("action must be an object") }
        return ToastKitActionConfiguration(id: try requiredString(map, "id"), label: try requiredString(map, "label"))
    }

    private static func typography(_ value: Any) throws -> ToastKitTypographyConfiguration {
        guard let map = value as? [String: Any] else { throw ToastKitInputError.invalid("typography must be an object") }
        guard !map.isEmpty else { throw ToastKitInputError.invalid("typography must not be empty") }
        var result = ToastKitTypographyConfiguration(font: nil, size: nil, weight: nil, align: nil, italic: nil)
        if let v = map["font"] { result.font = try nonEmptyString(v, "font") }
        if let v = map["size"] { result.size = try enumValue(v, "text size") }
        if let v = map["weight"] { result.weight = try enumValue(v, "text weight") }
        if let v = map["align"] { result.align = try enumValue(v, "text align") }
        if let v = map["italic"] { result.italic = try boolean(v, "italic") }
        return result
    }

    private static func mergeTypography(_ current: ToastKitTypographyConfiguration?, _ value: Any?, _ name: String) throws -> ToastKitTypographyConfiguration? {
        guard value != nil, !(value is NSNull) else { return current }
        guard let map = value as? [String: Any] else { throw ToastKitInputError.invalid("\(name) must be an object") }
        guard !map.isEmpty else { return current }
        var next = current ?? ToastKitTypographyConfiguration(font: nil, size: nil, weight: nil, align: nil, italic: nil)
        if let value = map["font"] { next.font = try nonEmptyString(value, "font") }
        if let value = map["size"] { next.size = try enumValue(value, "text size") }
        if let value = map["weight"] { next.weight = try enumValue(value, "text weight") }
        if let value = map["align"] { next.align = try enumValue(value, "text align") }
        if let value = map["italic"] { next.italic = try boolean(value, "italic") }
        return next
    }

    private static func style(_ value: Any, fallback: ToastKitStyleConfiguration) throws -> ToastKitStyleConfiguration {
        guard let map = value as? [String: Any] else { throw ToastKitInputError.invalid("style must be an object") }
        var result = fallback
        if let value = map["background"] { result.background = try color(value, "background") }
        if let value = map["foreground"] { result.foreground = try color(value, "foreground") }
        if let value = map["icon_color"] { result.iconColor = try color(value, "icon_color") }
        if let value = map["action_color"] { result.actionColor = try color(value, "action_color") }
        if let value = map["corner_radius"] { result.cornerRadius = try nonNegative(value, "corner_radius") }
        if let value = map["padding"] { result.padding = try nonNegative(value, "padding") }
        if let value = map["shadow"] { result.shadow = try boolean(value, "shadow") }
        return result
    }

    private static func variantStyle(_ variant: String) -> ToastKitStyleConfiguration {
        let colors: [String] = switch variant {
        case "success": ["#166534", "#FFFFFF", "#86EFAC", "#BBF7D0"]
        case "error": ["#991B1B", "#FFFFFF", "#FCA5A5", "#FECACA"]
        case "warning": ["#92400E", "#FFFFFF", "#FDE68A", "#FEF3C7"]
        case "info": ["#1E40AF", "#FFFFFF", "#93C5FD", "#BFDBFE"]
        default: ["#1F2937", "#FFFFFF", "#D1D5DB", "#E5E7EB"]
        }
        return ToastKitStyleConfiguration(background: safeColor(colors[0]), foreground: safeColor(colors[1]), iconColor: safeColor(colors[2]), actionColor: safeColor(colors[3]), cornerRadius: 16, padding: 16, shadow: true)
    }

    private static func defaultIcon(_ variant: String) -> ToastKitIconConfiguration? {
        switch variant {
        case "success": ToastKitIconConfiguration(name: "check", ios: nil)
        case "error": ToastKitIconConfiguration(name: "error", ios: nil)
        case "warning": ToastKitIconConfiguration(name: "warning", ios: nil)
        case "info": ToastKitIconConfiguration(name: "info", ios: nil)
        default: nil
        }
    }

    private static func color(_ value: Any, _ name: String) throws -> Color {
        guard let text = value as? String else { throw ToastKitInputError.invalid("\(name) must be a color string") }
        do { return try parseColor(text) } catch { throw ToastKitInputError.invalid("Invalid \(name) color") }
    }

    private static func parseColor(_ value: String) throws -> Color {
        var hex = value.hasPrefix("#") ? String(value.dropFirst()) : value
        guard [3, 6, 8].contains(hex.count), hex.allSatisfy({ $0.isHexDigit }) else { throw ToastKitInputError.invalid("Invalid color") }
        if hex.count == 3 { hex = hex.map { "\($0)\($0)" }.joined() }
        guard let number = UInt64(hex, radix: 16) else { throw ToastKitInputError.invalid("Invalid color") }
        let a, r, g, b: Double
        if hex.count == 8 { a = Double((number >> 24) & 255); r = Double((number >> 16) & 255); g = Double((number >> 8) & 255); b = Double(number & 255) }
        else { a = 255; r = Double((number >> 16) & 255); g = Double((number >> 8) & 255); b = Double(number & 255) }
        return Color(red: r / 255, green: g / 255, blue: b / 255, opacity: a / 255)
    }

    private static func safeColor(_ value: String) -> Color { (try? parseColor(value)) ?? .black }

    private static func variant(_ value: Any) throws -> String {
        let text = try nonEmptyString(value, "variant").lowercased()
        guard ["neutral", "success", "error", "warning", "info"].contains(text) else { throw ToastKitInputError.invalid("Invalid variant: \(text)") }
        return text
    }
    private static func enumValue<T: RawRepresentable>(_ value: Any, _ name: String) throws -> T where T.RawValue == String {
        let text = try nonEmptyString(value, name).lowercased()
        guard let result = T(rawValue: text) else { throw ToastKitInputError.invalid("Invalid \(name): \(text)") }
        return result
    }
    private static func requiredString(_ map: [String: Any], _ key: String) throws -> String { try nonEmptyString(map[key], key) }
    private static func nonEmptyString(_ value: Any?, _ name: String) throws -> String {
        guard let text = value as? String, !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { throw ToastKitInputError.invalid("\(name) must be a non-empty string") }
        return text
    }
    private static func nullableString(_ value: Any?, _ name: String) throws -> String? { value == nil || value is NSNull ? nil : try nonEmptyString(value, name) }
    private static func boolean(_ value: Any, _ name: String) throws -> Bool {
        guard let result = value as? Bool else { throw ToastKitInputError.invalid("\(name) must be boolean") }; return result
    }
    private static func positiveInt(_ value: Any, _ name: String) throws -> Int {
        guard let result = (value as? NSNumber)?.intValue, result > 0 else { throw ToastKitInputError.invalid("\(name) must be greater than zero") }; return result
    }
    private static func nonNegative(_ value: Any, _ name: String) throws -> CGFloat {
        guard let result = (value as? NSNumber)?.doubleValue, result >= 0 else { throw ToastKitInputError.invalid("\(name) must not be negative") }; return CGFloat(result)
    }
    private static func progress(_ value: Any) throws -> Double {
        guard let result = (value as? NSNumber)?.doubleValue, result.isFinite else {
            throw ToastKitInputError.invalid("progress must be a finite number")
        }
        return min(100, max(0, result))
    }
}
