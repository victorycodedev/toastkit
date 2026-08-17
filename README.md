# ToastKit for NativePHP Mobile

Rich, customizable native toast notifications for NativePHP Mobile v4.

> Native rendering is work in progress. This phase defines and tests the PHP, manifest, event, and bridge contracts; it has not been device-tested and does not display UI yet.

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

Colors accept `#RGB`, `#RRGGBB`, or `#AARRGGBB` and are normalized to uppercase. ToastKit passes NativePHP logical icon names through and supports `icon('check', ios: ..., android: ...)`; the future native renderer will use the host icon resolution instead of carrying a duplicate map.

## Events

- `ToastShown(string $toastId)`
- `ToastDismissed(string $toastId, string $reason)` — `timeout`, `swipe`, `programmatic`, `action`, or `replaced`
- `ToastActionPressed(string $toastId, string $actionId)`

## License

MIT
