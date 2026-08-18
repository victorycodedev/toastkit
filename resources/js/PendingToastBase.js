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
    if (name !== null) icon.name = nonEmpty(name, "icon name");
    if (ios !== null) icon.ios = nonEmpty(ios, "iOS icon");
    if (android !== null) icon.android = nonEmpty(android, "Android icon");
    if (!Object.keys(icon).length)
      throw new TypeError("An icon name or platform override is required");
    return this.set("icon", icon);
  }
  position(value) {
    return this.set("position", oneOf(value, "position"));
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
  swipeToDismiss(enabled = true) {
    return this.set("swipe_to_dismiss", Boolean(enabled));
  }
  dismissible(enabled = true) {
    return this.set("dismissible", Boolean(enabled));
  }
  action(label, id) {
    return this.set("action", {
      id: nonEmpty(id, "action ID"),
      label: nonEmpty(label, "action label"),
    });
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
