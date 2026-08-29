const baseUrl = "/_native/api/call";
export async function bridgeCall(method, params = {}) {
  const response = await fetch(baseUrl, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-TOKEN":
        globalThis.document?.querySelector('meta[name="csrf-token"]')
          ?.content || "",
    },
    body: JSON.stringify({ method, params }),
  });
  const result = await response.json();
  if (!response.ok)
    throw new Error(result.message || `Native bridge HTTP ${response.status}`);
  if (result.status === "error")
    throw new Error(result.message || "Native call failed");
  return result.data?.data ?? result.data;
}
