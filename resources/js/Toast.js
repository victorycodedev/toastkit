import { bridgeCall } from "./bridge.js";
import { PendingToast } from "./PendingToast.js";
import { PendingToastUpdate } from "./PendingToastUpdate.js";
import { nonEmpty } from "./validation.js";
export const Toast = {
  make: (message) => new PendingToast(message),
  success: (message) => new PendingToast(message).success(),
  error: (message) => new PendingToast(message).error(),
  warning: (message) => new PendingToast(message).warning(),
  info: (message) => new PendingToast(message).info(),
  neutral: (message) => new PendingToast(message).neutral(),
  update: (id) => new PendingToastUpdate(id),
  updateUnique: (key) => new PendingToastUpdate(key, { unique: true }),
  dismiss: (id) =>
    bridgeCall("ToastKit.Dismiss", { id: nonEmpty(id, "toast ID") }),
  dismissUnique: (key) =>
    bridgeCall("ToastKit.DismissUnique", {
      unique_key: nonEmpty(key, "unique key"),
    }),
  dismissAll: () => bridgeCall("ToastKit.DismissAll"),
};
