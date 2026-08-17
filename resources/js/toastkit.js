import { bridgeCall } from './bridge.js';
export const show = payload => bridgeCall('ToastKit.Show', payload); export const update = (id, changes) => bridgeCall('ToastKit.Update', { id, changes });
export const dismiss = id => bridgeCall('ToastKit.Dismiss', { id }); export const dismissAll = () => bridgeCall('ToastKit.DismissAll');
export const toastKit = { show, update, dismiss, dismissAll }; export default toastKit;
