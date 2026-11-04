import {
  color,
  nonEmpty,
  nonNegative,
  oneOf,
  positiveInteger,
} from "./validation.js";
export class PendingToastBase {
  constructor(values) {
    this.values = values;
  }
  set(key, value) {
    this.values[key] = value;
    return this;
  }
  unset(key) {
    delete this.values[key];
    return this;
  }
  message(value) {
    return this.set("message", nonEmpty(value, "message"));
  }
  title(value) {
    return this.set("title", value === null ? null : nonEmpty(value, "title"));
  }
  text(options = {}) {
    return this.typography("text", options);
  }
  titleText(options = {}) {
    return this.typography("title_text", options);
  }
  variant(value) {
    return this.set("variant", oneOf(value, "variant"));
  }
  success() {
    return this.variant("success");
  }
  error() {
    return this.variant("error");
  }
  warning() {
    return this.variant("warning");
  }
  info() {
    return this.variant("info");
  }
  neutral() {
    return this.variant("neutral");
  }
  icon(name = null, { ios = null, android = null } = {}) {
    const icon = {};
    if (name !== null) icon.name = customIconName(name, "icon name");
    if (ios !== null) icon.ios = customIconName(ios, "iOS icon");
    if (android !== null)
      icon.android = customIconName(android, "Android icon");
    if (!Object.keys(icon).length)
      throw new TypeError("An icon name or platform override is required");
    return this.set("icon", icon);
  }
  iconSize(value) {
    return this.set("icon_size", oneOf(value, "icon size"));
  }
  position(value) {
    return this.set("position", oneOf(value, "position"));
  }
  presentation(value) {
    return this.set("presentation", oneOf(value, "presentation"));
  }
  dynamic() {
    return this.presentation("dynamic");
  }
  native({ ios = true, android = true } = {}) {
    return this.set("native", { ios: Boolean(ios), android: Boolean(android) });
  }
  opacity(value = 0.8) {
    if (!Number.isFinite(value))
      throw new TypeError("opacity must be a finite number");
    return this.set("opacity", Math.max(0, Math.min(1, value)));
  }
  duration(value) {
    this.set("persistent", false);
    return this.set("duration", positiveInteger(value, "duration"));
  }
  persistent(enabled = true) {
    this.set("persistent", Boolean(enabled));
    if (enabled) this.set("duration", null);
    return this;
  }
  animation(value) {
    return this.set("animation", oneOf(value, "animation"));
  }
  direction(value) {
    return this.set("direction", oneOf(value, "direction"));
  }
  progress(value) {
    if (!Number.isFinite(value))
      throw new TypeError("progress must be a finite number");
    return this.set("progress", Math.max(0, Math.min(100, value)));
  }
  loading(enabled = true) {
    return this.set("loading", Boolean(enabled));
  }
  swipeToDismiss(enabled = true) {
    return this.set("swipe_to_dismiss", Boolean(enabled));
  }
  dismissible(enabled = true) {
    return this.set("dismissible", Boolean(enabled));
  }
  action(label, id) {
    this.unset("actions");
    return this.set("action", {
      id: nonEmpty(id, "action ID"),
      label: nonEmpty(label, "action label"),
    });
  }
  actions(actions) {
    if (!Array.isArray(actions) || actions.length < 1 || actions.length > 2)
      throw new TypeError("toast actions must contain one or two actions");
    const normalized = actions.map((action) => {
      if (action === null || typeof action !== "object" || Array.isArray(action))
        throw new TypeError("each toast action must specify a label and ID");
      const normalized = {
        id: nonEmpty(action.id, "action ID"),
        label: nonEmpty(action.label, "action label"),
      };
      if (action.color !== undefined)
        normalized.color = color(action.color, "action color");
      return normalized;
    });
    if (new Set(normalized.map(({ id }) => id)).size !== normalized.length)
      throw new TypeError("toast action IDs must be unique");
    this.unset("action");
    return this.set("actions", normalized);
  }
  data(value) {
    if (value === null || typeof value !== "object" || Array.isArray(value))
      throw new TypeError("toast data must be an object");
    try {
      const encoded = JSON.stringify(value, (_key, item) => {
        if (["bigint", "function", "symbol"].includes(typeof item))
          throw new TypeError("unsupported toast data value");
        return item;
      });
      return this.set("data", JSON.parse(encoded));
    } catch {
      throw new TypeError("toast data must be serializable");
    }
  }
  typography(group, options) {
    const values = {};
    if (options.font !== undefined)
      values.font = nonEmpty(options.font, "text font");
    if (options.size !== undefined) values.size = oneOf(options.size, "size");
    if (options.weight !== undefined)
      values.weight = oneOf(options.weight, "weight");
    if (options.align !== undefined)
      values.align = oneOf(options.align, "align");
    if (options.italic !== undefined) {
      if (typeof options.italic !== "boolean")
        throw new TypeError("italic must be boolean");
      values.italic = options.italic;
    }
    if (!Object.keys(values).length) return this;
    this.values[group] = { ...(this.values[group] ?? {}), ...values };
    return this;
  }
  style(key, value) {
    this.values.style ??= {};
    this.values.style[key] = value;
    return this;
  }
  background(value) {
    return this.style("background", color(value));
  }
  foreground(value) {
    return this.style("foreground", color(value));
  }
  iconColor(value) {
    return this.style("icon_color", color(value));
  }
  actionColor(value) {
    return this.style("action_color", color(value));
  }
  cornerRadius(value) {
    return this.style("corner_radius", nonNegative(value, "corner radius"));
  }
  padding(value) {
    return this.style("padding", nonNegative(value, "padding"));
  }
  shadow(enabled = true) {
    return this.style("shadow", Boolean(enabled));
  }
  strategy(value) {
    return this.set("strategy", oneOf(value, "strategy"));
  }
  queue() {
    return this.strategy("queue");
  }
  stack() {
    return this.strategy("stack");
  }
  maxVisible(value) {
    return this.set("max_visible", positiveInteger(value, "maxVisible"));
  }
}

function customIconName(value, label) {
  const name = nonEmpty(value, label);
  if (name.startsWith("custom:") && !/^custom:[a-z0-9]+(?:-[a-z0-9]+)*$/.test(name))
    throw new TypeError(`${label} custom icon must use custom: followed by a lowercase kebab-case name`);
  return name;
}
