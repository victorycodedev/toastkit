import { bridgeCall } from "./bridge.js";
import { PendingToastBase } from "./PendingToastBase.js";
import { nonEmpty } from "./validation.js";
const profiles = {
  neutral: { style: { background: "#1F2937", foreground: "#FFFFFF" } },
  success: {
    icon: { name: "check" },
    style: {
      background: "#166534",
      foreground: "#FFFFFF",
      icon_color: "#86EFAC",
    },
  },
  error: {
    icon: { name: "error" },
    style: {
      background: "#991B1B",
      foreground: "#FFFFFF",
      icon_color: "#FCA5A5",
    },
  },
  warning: {
    icon: { name: "warning" },
    style: {
      background: "#92400E",
      foreground: "#FFFFFF",
      icon_color: "#FDE68A",
    },
  },
  info: {
    icon: { name: "info" },
    style: {
      background: "#1E40AF",
      foreground: "#FFFFFF",
      icon_color: "#93C5FD",
    },
  },
};
export class PendingToast extends PendingToastBase {
  constructor(message = null) {
    super({});
    this.toastId =
      globalThis.crypto?.randomUUID?.() ??
      `toast-${Date.now()}-${Math.random().toString(16).slice(2)}`;
    if (message !== null) this.message(message);
  }
  id(value) {
    this.toastId = nonEmpty(value, "toast ID");
    return this;
  }
  unique(key) {
    this.values.unique_key = nonEmpty(key, "unique key");
    return this;
  }
  payload() {
    if (!this.values.message)
      throw new TypeError("A toast message is required before show()");
    const variant = this.values.variant ?? "neutral";
    const defaults = {
      contract_version: 1,
      id: this.toastId,
      title: null,
      variant: "neutral",
      position: "bottom",
      duration: 3000,
      persistent: false,
      animation: "scale",
      direction: "auto",
      loading: false,
      swipe_to_dismiss: true,
      dismissible: false,
      style: { corner_radius: 16, padding: 16, shadow: true },
      strategy: "queue",
      max_visible: 3,
      overflow_behavior: "queue",
    };
    return {
      ...defaults,
      ...profiles[variant],
      ...this.values,
      id: this.toastId,
      style: {
        ...defaults.style,
        ...profiles[variant].style,
        ...this.values.style,
      },
    };
  }
  async show() {
    const result = await bridgeCall("ToastKit.Show", this.payload());
    return result?.id ?? this.toastId;
  }
}
