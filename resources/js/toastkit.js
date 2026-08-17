import { bridgeCall } from "./bridge.js";

export const Show = (payload = {}) => bridgeCall("ToastKit.Show", payload);
export const Update = (id, changes = {}) =>
  bridgeCall("ToastKit.Update", { id, changes });
export const Dismiss = (id) => bridgeCall("ToastKit.Dismiss", { id });
export const DismissAll = () => bridgeCall("ToastKit.DismissAll");

export const show = Show;
export const update = Update;
export const dismiss = Dismiss;
export const dismissAll = DismissAll;

export const toastKit = { Show, Update, Dismiss, DismissAll };
export default toastKit;
