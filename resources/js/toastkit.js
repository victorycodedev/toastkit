import { bridgeCall } from "./bridge.js";

export const Show = (payload = {}) => bridgeCall("ToastKit.Show", payload);
export const Update = (id, changes = {}) =>
  bridgeCall("ToastKit.Update", { id, changes });
export const UpdateUnique = (key, changes = {}) =>
  bridgeCall("ToastKit.UpdateUnique", { unique_key: key, changes });
export const Dismiss = (id) => bridgeCall("ToastKit.Dismiss", { id });
export const DismissUnique = (key) =>
  bridgeCall("ToastKit.DismissUnique", { unique_key: key });
export const DismissAll = () => bridgeCall("ToastKit.DismissAll");

export const show = Show;
export const update = Update;
export const updateUnique = UpdateUnique;
export const dismiss = Dismiss;
export const dismissUnique = DismissUnique;
export const dismissAll = DismissAll;

export const toastKit = {
  Show,
  Update,
  UpdateUnique,
  Dismiss,
  DismissUnique,
  DismissAll,
};
export default toastKit;
