const baseUrl = '/_native/api/call';

async function bridgeCall(method, params = {}) {
    const response = await fetch(baseUrl, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': document.querySelector('meta[name="csrf-token"]')?.content || ''
        },
        body: JSON.stringify({ method, params })
    });
    const result = await response.json();
    if (result.status === 'error') throw new Error(result.message || 'Native call failed');
    return result.data?.data ?? result.data;
}

export const show = payload => bridgeCall('ToastKit.Show', payload);
export const update = (id, changes) => bridgeCall('ToastKit.Update', { id, changes });
export const dismiss = id => bridgeCall('ToastKit.Dismiss', { id });
export const dismissAll = () => bridgeCall('ToastKit.DismissAll');

export const toastKit = { show, update, dismiss, dismissAll };
export default toastKit;
