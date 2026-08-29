package com.victorycodedev.plugins.toastkit.bridge

import androidx.compose.ui.graphics.Color
import com.victorycodedev.plugins.toastkit.model.*
import org.json.JSONObject

internal class ToastKitInputException(message: String) : IllegalArgumentException(message)

internal object ToastKitBridgeNormalizer {
    fun show(parameters: Map<String, Any>): ToastKitConfiguration {
        val version = (parameters["contract_version"] as? Number)?.toInt() ?: 1
        requireInput(version == 1, "Unsupported contract_version: $version")
        return configuration(parameters)
    }

    fun update(parameters: Map<String, Any>): Pair<String, Map<String, Any>> {
        val id = requiredString(parameters, "id")
        val changes = stringMap(parameters["changes"]) ?: throw ToastKitInputException("changes must be an object")
        requireInput(changes.isNotEmpty(), "changes must not be empty")
        return id to changes
    }

    fun updateUnique(parameters: Map<String, Any>): Pair<String, Map<String, Any>> {
        val key = requiredString(parameters, "unique_key")
        val changes = stringMap(parameters["changes"]) ?: throw ToastKitInputException("changes must be an object")
        requireInput(changes.isNotEmpty(), "changes must not be empty")
        return key to changes
    }

    fun id(parameters: Map<String, Any>): String = requiredString(parameters, "id")
    fun uniqueKey(parameters: Map<String, Any>): String = requiredString(parameters, "unique_key")

    fun applyChanges(current: ToastKitConfiguration, changes: Map<String, Any>): ToastKitConfiguration {
        var next = current
        changes["message"]?.let { next = next.copy(message = nonEmptyString(it, "message")) }
        if (changes.containsKey("title")) next = next.copy(title = nullableString(changes["title"], "title"))
        changes["variant"]?.let {
            val name = variant(it)
            next = next.copy(variant = name, style = variantStyle(name), icon = defaultIcon(name))
        }
        if (changes.containsKey("icon")) next = next.copy(icon = optionalValue(changes["icon"])?.let(::icon))
        changes["position"]?.let { next = next.copy(position = position(it)) }
        changes["animation"]?.let { next = next.copy(animation = animation(it)) }
        changes["direction"]?.let { next = next.copy(direction = direction(it)) }
        changes["native"]?.let {
            val mode = nativeMode(it)
            next = next.copy(nativeIos = mode.first, nativeAndroid = mode.second)
        }
        if (changes.containsKey("progress")) next = next.copy(progress = optionalValue(changes["progress"])?.let(::progress))
        changes["loading"]?.let { next = next.copy(loading = boolean(it, "loading")) }
        changes["swipe_to_dismiss"]?.let { next = next.copy(swipeToDismiss = boolean(it, "swipe_to_dismiss")) }
        changes["dismissible"]?.let { next = next.copy(dismissible = boolean(it, "dismissible")) }
        if (changes.containsKey("action")) next = next.copy(action = optionalValue(changes["action"])?.let(::action))
        changes["strategy"]?.let { next = next.copy(strategy = strategy(it)) }
        changes["max_visible"]?.let { next = next.copy(maxVisible = positiveInt(it, "max_visible")) }
        changes["style"]?.let { next = next.copy(style = style(it, next.style)) }
        if (changes.containsKey("text")) next = next.copy(text = mergeTypography(next.text, changes["text"], "text"))
        if (changes.containsKey("title_text")) next = next.copy(titleText = mergeTypography(next.titleText, changes["title_text"], "title_text"))

        val persistentChanged = changes.containsKey("persistent")
        val durationChanged = changes.containsKey("duration")
        if (persistentChanged || durationChanged) {
            val persistent = changes["persistent"]?.let { boolean(it, "persistent") } ?: false
            val duration = optionalValue(changes["duration"])
            next = next.copy(durationMs = if (persistent) null else duration?.let { positiveLong(it, "duration") } ?: 3000L)
        }
        return next
    }

    private fun configuration(values: Map<String, Any>): ToastKitConfiguration {
        val persistent = values["persistent"]?.let { boolean(it, "persistent") } ?: false
        return ToastKitConfiguration(
            id = requiredString(values, "id"),
            uniqueKey = nullableString(values["unique_key"], "unique_key"),
            nativeIos = nativeMode(values["native"] ?: emptyMap<String, Any>()).first,
            nativeAndroid = nativeMode(values["native"] ?: emptyMap<String, Any>()).second,
            message = requiredString(values, "message"),
            title = nullableString(values["title"], "title"),
            variant = variant(values["variant"] ?: "neutral"),
            icon = optionalValue(values["icon"])?.let(::icon),
            position = position(values["position"] ?: "bottom"),
            durationMs = if (persistent) null else positiveLong(values["duration"] ?: 3000, "duration"),
            animation = animation(values["animation"] ?: "scale"),
            direction = direction(values["direction"] ?: "auto"),
            progress = optionalValue(values["progress"])?.let(::progress),
            loading = values["loading"]?.let { boolean(it, "loading") } ?: false,
            swipeToDismiss = values["swipe_to_dismiss"]?.let { boolean(it, "swipe_to_dismiss") } ?: true,
            dismissible = values["dismissible"]?.let { boolean(it, "dismissible") } ?: false,
            action = optionalValue(values["action"])?.let(::action),
            style = style(values["style"] ?: emptyMap<String, Any>(), variantStyle(values["variant"] as? String ?: "neutral")),
            strategy = strategy(values["strategy"] ?: "queue"),
            maxVisible = positiveInt(values["max_visible"] ?: 3, "max_visible"),
            text = optionalValue(values["text"])?.let(::typography),
            titleText = optionalValue(values["title_text"])?.let(::typography),
        )
    }

    private fun icon(value: Any): ToastKitIcon {
        val map = stringMap(value) ?: throw ToastKitInputException("icon must be an object")
        val name = nullableString(map["name"], "icon.name")
        val android = nullableString(map["android"], "icon.android")
        requireInput(name != null || android != null, "icon requires name or android")
        return ToastKitIcon(name, android)
    }

    private fun action(value: Any): ToastKitAction {
        val map = stringMap(value) ?: throw ToastKitInputException("action must be an object")
        return ToastKitAction(requiredString(map, "id"), requiredString(map, "label"))
    }

    private fun nativeMode(value: Any): Pair<Boolean, Boolean> {
        val map = stringMap(value) ?: throw ToastKitInputException("native must be an object")
        return (map["ios"]?.let { boolean(it, "native.ios") } ?: false) to
            (map["android"]?.let { boolean(it, "native.android") } ?: false)
    }

    private fun style(value: Any, fallback: ToastKitStyle): ToastKitStyle {
        val map = stringMap(value) ?: throw ToastKitInputException("style must be an object")
        return fallback.copy(
            background = map["background"]?.let { color(it, "background") } ?: fallback.background,
            foreground = map["foreground"]?.let { color(it, "foreground") } ?: fallback.foreground,
            iconColor = map["icon_color"]?.let { color(it, "icon_color") } ?: fallback.iconColor,
            actionColor = map["action_color"]?.let { color(it, "action_color") } ?: fallback.actionColor,
            cornerRadius = map["corner_radius"]?.let { nonNegativeFloat(it, "corner_radius") } ?: fallback.cornerRadius,
            padding = map["padding"]?.let { nonNegativeFloat(it, "padding") } ?: fallback.padding,
            shadow = map["shadow"]?.let { boolean(it, "shadow") } ?: fallback.shadow,
        )
    }

    private fun variantStyle(variant: String): ToastKitStyle {
        val colors = when (variant) {
            "success" -> listOf("#166534", "#FFFFFF", "#86EFAC", "#BBF7D0")
            "error" -> listOf("#991B1B", "#FFFFFF", "#FCA5A5", "#FECACA")
            "warning" -> listOf("#92400E", "#FFFFFF", "#FDE68A", "#FEF3C7")
            "info" -> listOf("#1E40AF", "#FFFFFF", "#93C5FD", "#BFDBFE")
            else -> listOf("#1F2937", "#FFFFFF", "#D1D5DB", "#E5E7EB")
        }
        return ToastKitStyle(parseColor(colors[0]), parseColor(colors[1]), parseColor(colors[2]), parseColor(colors[3]), 16f, 16f, true)
    }

    private fun defaultIcon(variant: String): ToastKitIcon? = when (variant) {
        "success" -> ToastKitIcon("check", null)
        "error" -> ToastKitIcon("error", null)
        "warning" -> ToastKitIcon("warning", null)
        "info" -> ToastKitIcon("info", null)
        else -> null
    }

    private fun typography(value: Any): ToastKitTypography {
        val map = stringMap(value) ?: throw ToastKitInputException("typography must be an object")
        if (map.isEmpty()) throw ToastKitInputException("typography must not be empty")
        return ToastKitTypography(
            font = map["font"]?.let { nonEmptyString(it, "font") },
            size = map["size"]?.let { textSize(it) },
            weight = map["weight"]?.let { textWeight(it) },
            align = map["align"]?.let { textAlign(it) },
            italic = map["italic"]?.let { boolean(it, "italic") },
        )
    }

    private fun mergeTypography(current: ToastKitTypography?, value: Any?, name: String): ToastKitTypography? {
        if (isNull(value)) return current
        val map = stringMap(value) ?: throw ToastKitInputException("$name must be an object")
        if (map.isEmpty()) return current
        val base = current ?: ToastKitTypography()
        return base.copy(
            font = if (map.containsKey("font")) map["font"]?.let { nonEmptyString(it, "font") } else base.font,
            size = if (map.containsKey("size")) map["size"]?.let { textSize(it) } else base.size,
            weight = if (map.containsKey("weight")) map["weight"]?.let { textWeight(it) } else base.weight,
            align = if (map.containsKey("align")) map["align"]?.let { textAlign(it) } else base.align,
            italic = if (map.containsKey("italic")) map["italic"]?.let { boolean(it, "italic") } else base.italic,
        )
    }

    private fun color(value: Any, name: String): Color {
        val text = value as? String ?: throw ToastKitInputException("$name must be a color string")
        return try { parseColor(text) } catch (_: IllegalArgumentException) { throw ToastKitInputException("Invalid $name color") }
    }

    private fun parseColor(value: String): Color {
        val hex = value.removePrefix("#")
        require(hex.length == 3 || hex.length == 6 || hex.length == 8)
        val expanded = if (hex.length == 3) hex.map { "$it$it" }.joinToString("") else hex
        val argb = if (expanded.length == 6) "FF$expanded" else expanded
        return Color(argb.toLong(16))
    }

    private fun variant(value: Any): String = enumString(value, "variant", setOf("neutral", "success", "error", "warning", "info"))
    private fun position(value: Any) = ToastKitPosition.valueOf(enumString(value, "position", setOf("top", "center", "bottom")).uppercase())
    private fun animation(value: Any) = ToastKitAnimation.valueOf(enumString(value, "animation", setOf("fade", "slide", "scale", "spring", "snap", "pop", "reveal", "bounce")).uppercase())
    private fun direction(value: Any) = ToastKitDirection.valueOf(enumString(value, "direction", setOf("auto", "left", "right", "top", "bottom")).uppercase())
    private fun strategy(value: Any) = ToastKitStrategy.valueOf(enumString(value, "strategy", setOf("queue", "stack")).uppercase())
    private fun textSize(value: Any) = ToastKitTextSize.valueOf(enumString(value, "text size", setOf("xs", "sm", "base", "lg", "xl")).uppercase())
    private fun textWeight(value: Any) = ToastKitTextWeight.valueOf(enumString(value, "text weight", setOf("normal", "medium", "semibold", "bold")).uppercase())
    private fun textAlign(value: Any) = ToastKitTextAlign.valueOf(enumString(value, "text align", setOf("left", "center", "right")).uppercase())
    private fun enumString(value: Any, name: String, allowed: Set<String>): String {
        val text = nonEmptyString(value, name).lowercase()
        requireInput(text in allowed, "Invalid $name: $text")
        return text
    }
    private fun requiredString(map: Map<String, Any>, key: String) = nonEmptyString(map[key], key)
    private fun nonEmptyString(value: Any?, name: String): String {
        val text = value as? String ?: throw ToastKitInputException("$name must be a string")
        requireInput(text.isNotBlank(), "$name must not be empty")
        return text
    }
    private fun nullableString(value: Any?, name: String): String? {
        if (isNull(value)) return null
        return nonEmptyString(value, name)
    }
    private fun optionalValue(value: Any?): Any? = if (isNull(value)) null else value
    private fun isNull(value: Any?): Boolean = value == null || value === JSONObject.NULL
    private fun boolean(value: Any, name: String) = value as? Boolean ?: throw ToastKitInputException("$name must be boolean")
    private fun positiveLong(value: Any, name: String): Long {
        val result = (value as? Number)?.toLong() ?: throw ToastKitInputException("$name must be numeric")
        requireInput(result > 0, "$name must be greater than zero")
        return result
    }
    private fun positiveInt(value: Any, name: String) = positiveLong(value, name).toInt()
    private fun nonNegativeFloat(value: Any, name: String): Float {
        val result = (value as? Number)?.toFloat() ?: throw ToastKitInputException("$name must be numeric")
        requireInput(result >= 0, "$name must not be negative")
        return result
    }
    private fun progress(value: Any): Float {
        val result = (value as? Number)?.toFloat() ?: throw ToastKitInputException("progress must be numeric")
        requireInput(result.isFinite(), "progress must be finite")
        return result.coerceIn(0f, 100f)
    }
    private fun stringMap(value: Any?): Map<String, Any>? = when (value) {
        is JSONObject -> value.keys().asSequence().associate { key -> key to value.get(key) }
        is Map<*, *> -> value.entries.associate { (key, v) -> key.toString() to (v ?: JSONObject.NULL) }
        else -> null
    }
    private fun requireInput(condition: Boolean, message: String) { if (!condition) throw ToastKitInputException(message) }
}
