import { bridgeCall } from "./bridge.js";
import { PendingToastBase } from "./PendingToastBase.js";
import { nonEmpty } from "./validation.js";
export class PendingToastUpdate extends PendingToastBase {
  constructor(id, { unique = false } = {}) {
    super({});
    this.target = nonEmpty(id, unique ? "unique key" : "toast ID");
    this.usesUniqueKey = unique;
  }
  payload() {
    if (!Object.keys(this.values).length)
      throw new TypeError(
        "At least one toast change is required before show()",
      );
    return {
      [this.usesUniqueKey ? "unique_key" : "id"]: this.target,
      changes: structuredClone(this.values),
    };
  }
  async show() {
    const result = await bridgeCall(
      this.usesUniqueKey ? "ToastKit.UpdateUnique" : "ToastKit.Update",
      this.payload(),
    );
    return result?.id ?? this.target;
  }
}
