# ToastKit for NativePHP Mobile

Rich, customizable native toast notifications for NativePHP Mobile v4.

> Native Compose and SwiftUI renderers are implemented, but the plugin is not yet claimed production-ready: Android/iOS consuming-app compilation and device testing remain required.

## PHP API

```php
use Victorycodedev\ToastKit\Facades\Toast;

$id = Toast::make('Profile updated')
    ->title('Success')->success()->icon('check')->position('top')
    ->duration(3000)->animation('spring')->swipeToDismiss()->show();

Toast::success('Changes saved')->show();
Toast::error('Something went wrong')->show();
Toast::warning('Storage almost full')->show();
Toast::info('Downloading...')->show();
Toast::neutral('Copied')->show();
```

`show()` returns a UUID, or the custom ID supplied with `->id('upload-profile-photo')`. Messages must be non-empty. Defaults are neutral, bottom, 3000ms, spring animation, swipe enabled, no close control, shadow enabled, queue strategy, and a stack limit of three.

`dismissible()` controls the visible close control; `swipeToDismiss()` controls the gesture independently. `persistent()` disables timeout. Calling `duration()` makes a toast non-persistent. On update, `persistent(false)` without a duration tells the native renderer to use the 3000ms default.

```php
$id = Toast::info('Uploading...')->persistent()->show();

Toast::update($id)->message('Upload complete')->success()->duration(2000)->show();
Toast::dismiss($id);
Toast::dismissAll();
```

Updates send only explicitly changed fields. Dismiss operations are idempotent. Stack overflow uses `overflow_behavior: queue`: after `maxVisible()` is reached, later toasts wait in arrival order.

## Actions and styling

```php
Toast::error('Connection lost')
    ->action(label: 'Retry', id: 'retry')
    ->background('#111827')->foreground('#ffffff')
    ->iconColor('#22c55e')->actionColor('#60a5fa')
    ->cornerRadius(18)->padding(16)->shadow()
    ->stack()->maxVisible(3)->show();
```

Colors accept `#RGB`, `#RRGGBB`, or `#AARRGGBB` and are normalized to uppercase. ToastKit passes NativePHP logical icon names through and supports `icon('check', ios: ..., android: ...)`; native rendering uses host icon resolution instead of carrying a duplicate map.

The native renderers reuse NativePHP 4.2's installed icon helpers. Android uses the explicit `android` override or NativePHP's Material icon resolver; iOS uses the explicit `ios` SF Symbol or NativePHP's `getIconForName`. Unknown names therefore follow the host's fallback behavior.

## Native behavior

ToastKit installs lifecycle-aware window overlays through NativePHP's plugin initialization functions. It does not require a Blade/EDGE component and works over native or web content. Queue admission is FIFO. Stack overflow also queues FIFO, and a queue toast and stack group never compete on screen simultaneously.

Timers begin only when a toast becomes visible. Timing updates reset the deadline; other updates preserve it. All dismissals are terminally guarded, so timeout, action, swipe, and bridge calls cannot emit duplicate dismissal events.

## JavaScript

```js
import Toast from './resources/js/index.js';

const id = await Toast.info('Uploading...').persistent().show();
await Toast.update(id).message('Done').success().duration(2000).show();
await Toast.dismiss(id);
```

## Events

- `ToastShown(string $toastId)`
- `ToastDismissed(string $toastId, string $reason)` — `timeout`, `swipe`, `programmatic`, `action`, or `replaced`
- `ToastActionPressed(string $toastId, string $actionId)`

## License

MIT
