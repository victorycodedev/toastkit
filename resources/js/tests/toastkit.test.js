import assert from "node:assert/strict";
import test from "node:test";
import { Toast } from "../Toast.js";
function fakeBridge(calls) {
  globalThis.fetch = async (_url, options) => {
    calls.push(JSON.parse(options.body));
    return { ok: true, json: async () => ({ status: "success", data: {} }) };
  };
}
test("show uses V1 contract and returns stable ID", async () => {
  const calls = [];
  fakeBridge(calls);
  const pending = Toast.success("Saved").position("top").duration(1200);
  const id = await pending.show();
  assert.equal(calls[0].method, "ToastKit.Show");
  assert.equal(calls[0].params.id, id);
  assert.equal(calls[0].params.variant, "success");
});
test("update is sparse", async () => {
  const calls = [];
  fakeBridge(calls);
  await Toast.update("upload").message("Done").success().show();
  assert.deepEqual(calls[0], {
    method: "ToastKit.Update",
    params: { id: "upload", changes: { message: "Done", variant: "success" } },
  });
});
test("builders isolate and validate", () => {
  assert.equal(Toast.success("A").payload().variant, "success");
  assert.equal(Toast.error("B").payload().variant, "error");
  assert.throws(() => Toast.make(""), /message/);
  assert.throws(() => Toast.make("x").background("red"), /Colors/);
});
test("dismiss uses native bridge", async () => {
  const calls = [];
  fakeBridge(calls);
  await Toast.dismiss("one");
  await Toast.dismissAll();
  assert.deepEqual(
    calls.map((x) => x.method),
    ["ToastKit.Dismiss", "ToastKit.DismissAll"],
  );
});
