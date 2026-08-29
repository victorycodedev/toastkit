import assert from "node:assert/strict";
import test from "node:test";

import {
  Toast,
  PendingToast,
  PendingToastUpdate,
  Show,
  Update,
  UpdateUnique,
  Dismiss,
  DismissUnique,
  DismissAll,
  show,
  update,
  updateUnique,
  dismiss,
  dismissUnique,
  dismissAll,
} from "../index.js";

function fakeBridge(calls) {
  globalThis.fetch = async (_url, options) => {
    calls.push(JSON.parse(options.body));
    return { ok: true, json: async () => ({ status: "success", data: {} }) };
  };
}

function resetFetch() {
  delete globalThis.fetch;
}

test("Toast.make returns a PendingToast builder", () => {
  const pending = Toast.make("Hello");
  assert.ok(pending instanceof PendingToast);
  assert.equal(pending.payload().message, "Hello");
});

test("shortcut variants set the correct variant and icon", () => {
  assert.equal(Toast.success("A").payload().variant, "success");
  assert.equal(Toast.error("A").payload().variant, "error");
  assert.equal(Toast.warning("A").payload().variant, "warning");
  assert.equal(Toast.info("A").payload().variant, "info");
  assert.equal(Toast.neutral("A").payload().variant, "neutral");
  assert.deepEqual(Toast.success("A").payload().icon, { name: "check" });
});

test("generated IDs are stable strings and custom IDs override", () => {
  const pending = Toast.info("A");
  assert.equal(typeof pending.payload().id, "string");
  assert.ok(pending.payload().id.length > 0);

  const custom = Toast.info("A").id("upload");
  assert.equal(custom.payload().id, "upload");
});

test("title and message flow through the payload", () => {
  const payload = Toast.make("Body").title("Heading").payload();
  assert.equal(payload.message, "Body");
  assert.equal(payload.title, "Heading");
});

test("icon supports logical names and platform overrides", () => {
  assert.deepEqual(Toast.make("x").icon("check").payload().icon, {
    name: "check",
  });
  assert.deepEqual(
    Toast.make("x")
      .icon("check", { ios: "checkmark.circle.fill", android: "done" })
      .payload().icon,
    { name: "check", ios: "checkmark.circle.fill", android: "done" },
  );
});

test("position, duration, persistent and animation are set", () => {
  const payload = Toast.make("x")
    .position("top")
    .duration(1200)
    .animation("slide")
    .payload();
  assert.equal(payload.position, "top");
  assert.equal(payload.duration, 1200);
  assert.equal(payload.animation, "slide");
  assert.equal(Toast.make("x").persistent().payload().persistent, true);
  assert.equal(Toast.make("x").persistent().payload().duration, null);
});

test("native appearance is custom by default and selectable per platform", () => {
  assert.deepEqual(Toast.success("Default").payload().native, { ios: false, android: false });
  assert.deepEqual(Toast.success("Both").native().payload().native, { ios: true, android: true });
  assert.deepEqual(Toast.success("iOS").native({ ios: true, android: false }).payload().native, { ios: true, android: false });
  assert.deepEqual(Toast.success("Android").native({ ios: false, android: true }).payload().native, { ios: false, android: true });
  assert.deepEqual(Toast.success("Neither").native({ ios: false, android: false }).payload().native, { ios: false, android: false });
});

test("native appearance preserves custom styles and is chain-order independent", () => {
  const before = Toast.make("Before").background("#000000").native({ ios: true, android: false }).payload();
  const after = Toast.make("After").native({ ios: true, android: false }).background("#000000").payload();
  assert.deepEqual(before.native, after.native);
  assert.deepEqual(before.style, after.style);
  assert.equal(before.style.background, "#000000");
});

test("native appearance can be changed through a sparse update", () => {
  const payload = Toast.update("toast").native({ ios: false, android: true }).payload();
  assert.deepEqual(payload.changes, { native: { ios: false, android: true } });
});

test("new animations, direction, progress and loading are set", () => {
  for (const animation of ["snap", "pop", "reveal", "bounce"])
    assert.equal(Toast.make("x").animation(animation).payload().animation, animation);
  assert.equal(Toast.make("x").direction("left").payload().direction, "left");
  assert.equal(Toast.make("x").progress(-5).payload().progress, 0);
  assert.equal(Toast.make("x").progress(42.5).payload().progress, 42.5);
  assert.equal(Toast.make("x").progress(105).payload().progress, 100);
  assert.equal(Toast.make("x").loading().payload().loading, true);
  assert.equal(Toast.make("x").loading(false).payload().loading, false);
});

test("progress and loading updates remain sparse", async () => {
  const calls = [];
  fakeBridge(calls);
  await Toast.update("upload").progress(40).show();
  await Toast.update("upload").loading(false).show();
  assert.deepEqual(calls[0].params.changes, { progress: 40 });
  assert.deepEqual(calls[1].params.changes, { loading: false });
  resetFetch();
});

test("swipe, dismissible and action are set", () => {
  const payload = Toast.make("x")
    .swipeToDismiss(false)
    .dismissible()
    .action("Retry", "retry")
    .payload();
  assert.equal(payload.swipe_to_dismiss, false);
  assert.equal(payload.dismissible, true);
  assert.deepEqual(payload.action, { id: "retry", label: "Retry" });
});

test("custom styling is normalized", () => {
  const payload = Toast.make("x")
    .background("#abc")
    .foreground("#ffffff")
    .iconColor("#22c55e")
    .actionColor("#60a5fa")
    .cornerRadius(18)
    .padding(12)
    .shadow(false)
    .payload();
  assert.equal(payload.style.background, "#ABC");
  assert.equal(payload.style.foreground, "#FFFFFF");
  assert.equal(payload.style.icon_color, "#22C55E");
  assert.equal(payload.style.action_color, "#60A5FA");
  assert.equal(payload.style.corner_radius, 18);
  assert.equal(payload.style.padding, 12);
  assert.equal(payload.style.shadow, false);
});

test("queue, stack and maxVisible are set", () => {
  assert.equal(Toast.make("x").queue().payload().strategy, "queue");
  assert.equal(Toast.make("x").stack().payload().strategy, "stack");
  assert.equal(Toast.make("x").stack().maxVisible(4).payload().max_visible, 4);
});

test("show() sends the full payload to the bridge", async () => {
  const calls = [];
  fakeBridge(calls);
  const id = await Toast.success("Saved").position("top").duration(1200).show();
  assert.equal(calls[0].method, "ToastKit.Show");
  assert.equal(calls[0].params.id, id);
  assert.equal(calls[0].params.variant, "success");
  assert.equal(calls[0].params.contract_version, 1);
  assert.equal(calls[0].params.position, "top");
  assert.equal(calls[0].params.duration, 1200);
  resetFetch();
});

test("unique() sends explicit identity and returns the native resolved UUID", async () => {
  const calls = [];
  globalThis.fetch = async (_url, options) => {
    calls.push(JSON.parse(options.body));
    return {
      ok: true,
      json: async () => ({ status: "success", data: { id: "existing-id", accepted: false } }),
    };
  };
  const id = await Toast.error("Offline").unique("network-status").show();
  assert.equal(id, "existing-id");
  assert.equal(calls[0].params.unique_key, "network-status");
  resetFetch();
});

test("update is sparse and retains the ID", async () => {
  const calls = [];
  fakeBridge(calls);
  await Toast.update("upload").message("Done").success().show();
  assert.deepEqual(calls[0], {
    method: "ToastKit.Update",
    params: { id: "upload", changes: { message: "Done", variant: "success" } },
  });
  resetFetch();
});

test("dismiss and dismissAll call the native bridge", async () => {
  const calls = [];
  fakeBridge(calls);
  await Toast.dismiss("one");
  await Toast.dismissAll();
  assert.deepEqual(
    calls.map((x) => x.method),
    ["ToastKit.Dismiss", "ToastKit.DismissAll"],
  );
  assert.deepEqual(calls[0].params, { id: "one" });
  resetFetch();
});

test("semantic update and dismiss use the unique bridge contracts", async () => {
  const calls = [];
  fakeBridge(calls);
  await Toast.updateUnique("network-status").message("Online").show();
  await Toast.dismissUnique("network-status");
  assert.deepEqual(calls, [
    {
      method: "ToastKit.UpdateUnique",
      params: { unique_key: "network-status", changes: { message: "Online" } },
    },
    {
      method: "ToastKit.DismissUnique",
      params: { unique_key: "network-status" },
    },
  ]);
  resetFetch();
});

test("builders isolate their state", () => {
  const a = Toast.success("A").position("top");
  const b = Toast.error("B");
  assert.equal(a.payload().position, "top");
  assert.equal(b.payload().position, "bottom");
  assert.equal(b.payload().variant, "error");
});

test("every bridge function has a named export", async () => {
  const calls = [];
  fakeBridge(calls);
  await Show({ id: "one", message: "Hi" });
  await Update("two", { message: "Bye" });
  await UpdateUnique("status", { message: "Ready" });
  await Dismiss("three");
  await DismissUnique("status");
  await DismissAll();
  assert.deepEqual(
    calls.map((x) => x.method),
    [
      "ToastKit.Show",
      "ToastKit.Update",
      "ToastKit.UpdateUnique",
      "ToastKit.Dismiss",
      "ToastKit.DismissUnique",
      "ToastKit.DismissAll",
    ],
  );
  assert.deepEqual(calls[0].params, { id: "one", message: "Hi" });
  assert.deepEqual(calls[1].params, { id: "two", changes: { message: "Bye" } });
  assert.deepEqual(calls[2].params, {
    unique_key: "status",
    changes: { message: "Ready" },
  });
  assert.deepEqual(calls[3].params, { id: "three" });
  assert.deepEqual(calls[4].params, { unique_key: "status" });
  resetFetch();
});

test("lowercase aliases match the PascalCase exports", () => {
  assert.equal(show, Show);
  assert.equal(update, Update);
  assert.equal(updateUnique, UpdateUnique);
  assert.equal(dismiss, Dismiss);
  assert.equal(dismissUnique, DismissUnique);
  assert.equal(dismissAll, DismissAll);
});

test("validation rejects bad input", () => {
  assert.throws(() => Toast.make(""), /message/);
  assert.throws(() => Toast.make("x").background("red"), /Colors/);
  assert.throws(() => Toast.make("x").position("left"), /position/);
  assert.throws(() => Toast.make("x").animation("spin"), /animation/);
  assert.throws(() => Toast.make("x").direction("diagonal"), /direction/);
  assert.throws(() => Toast.make("x").progress(Infinity), /progress/);
  assert.throws(() => Toast.make("x").variant("danger"), /variant/);
  assert.throws(() => Toast.make("x").strategy("grid"), /strategy/);
  assert.throws(() => Toast.make("x").maxVisible(0), /maxVisible/);
  assert.throws(() => Toast.make("x").padding(-1), /padding/);
  assert.throws(() => Toast.make("x").cornerRadius(-1), /corner radius/);
  assert.throws(() => Toast.make("x").duration(0), /duration/);
  assert.throws(() => Toast.make("x").action("", "retry"), /action label/);
  assert.throws(() => Toast.make("x").icon(), /icon name or platform override/);
  assert.throws(() => Toast.make("x").id(""), /toast ID/);
  assert.throws(() => Toast.make("x").unique(""), /unique key/);
  assert.throws(() => Toast.update(""), /toast ID/);
  assert.throws(() => Toast.updateUnique(""), /unique key/);
  assert.throws(() => new PendingToastUpdate("one").payload(), /change/);
});

test("text() sends the full typography payload", () => {
  const payload = Toast.make("Body")
    .text({
      font: "Inter",
      size: "sm",
      weight: "medium",
      align: "center",
      italic: false,
    })
    .payload();
  assert.deepEqual(payload.text, {
    font: "Inter",
    size: "sm",
    weight: "medium",
    align: "center",
    italic: false,
  });
});

test("titleText() sends the title typography payload", () => {
  const payload = Toast.make("Body")
    .title("Heading")
    .titleText({ size: "lg", weight: "bold" })
    .payload();
  assert.deepEqual(payload.title_text, { size: "lg", weight: "bold" });
});

test("text() only sends supplied options", () => {
  assert.deepEqual(Toast.make("x").text({ weight: "semibold" }).payload().text, {
    weight: "semibold",
  });
  assert.deepEqual(Toast.make("x").text({ size: "sm", align: "center" }).payload().text, {
    size: "sm",
    align: "center",
  });
});

test("message and title typography are independent", () => {
  const payload = Toast.make("Body")
    .title("Heading")
    .text({ size: "sm" })
    .titleText({ size: "lg", weight: "bold" })
    .payload();
  assert.deepEqual(payload.text, { size: "sm" });
  assert.deepEqual(payload.title_text, { size: "lg", weight: "bold" });
});

test("text() merges repeated calls", () => {
  const payload = Toast.make("x").text({ font: "Inter" }).text({ weight: "bold" }).payload();
  assert.deepEqual(payload.text, { font: "Inter", weight: "bold" });
});

test("toast without typography omits text keys", () => {
  const payload = Toast.success("Saved").payload();
  assert.ok(!("text" in payload));
  assert.ok(!("title_text" in payload));
});

test("update() sends sparse typography changes", async () => {
  const calls = [];
  fakeBridge(calls);
  await Toast.update("upload").text({ weight: "bold" }).show();
  assert.deepEqual(calls[0], {
    method: "ToastKit.Update",
    params: { id: "upload", changes: { text: { weight: "bold" } } },
  });
  resetFetch();
});

test("typography validation rejects bad input", () => {
  assert.throws(() => Toast.make("x").text({ size: "huge" }), /size/);
  assert.throws(() => Toast.make("x").text({ weight: "heavy" }), /weight/);
  assert.throws(() => Toast.make("x").text({ align: "justify" }), /align/);
  assert.throws(() => Toast.make("x").text({ font: "" }), /font/);
  assert.throws(() => Toast.make("x").text({ italic: "yes" }), /italic/);
  assert.throws(() => Toast.make("x").titleText({ size: "xxl" }), /size/);
});
