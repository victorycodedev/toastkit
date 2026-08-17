import { bridgeCall } from "./bridge.js";
import { PendingToastBase } from "./PendingToastBase.js";
import { nonEmpty } from "./validation.js";
export class PendingToastUpdate extends PendingToastBase {
  constructor(id) {
    super({});
    this.toastId = nonEmpty(id, "toast ID");
  }
  payload() {
    if (!Object.keys(this.values).length)
      throw new TypeError(
        "At least one toast change is required before show()",
      );
    return { id: this.toastId, changes: structuredClone(this.values) };
  }
  async show() {
    await bridgeCall("ToastKit.Update", this.payload());
    return this.toastId;
  }
}
