## ToastKit

ToastKit adds native toast notifications to NativePHP Mobile apps. It is controlled from PHP/JS; the native overlay
renders independently. There is no Blade/EDGE component to add.

### Installation assumption

ToastKit is already installed and registered in this app (`composer require victorycodedev/toastkit`, then `php artisan
native:plugin:register victorycodedev/toastkit`). Native code changes require `php artisan native:run` (or `php artisan
native:install --force`) before toasts render.

### Facade

```php
use Victorycodedev\ToastKit\Facades\Toast;
```

### Public methods

- `Toast::make(?string $message = null)` — start a builder.
- `Toast::success(string $message)` / `error` / `warning` / `info` / `neutral` — variant shortcuts returning a
`PendingToast`.
- `Toast::update(string $id)` — start a `PendingToastUpdate` for an existing toast.
- `Toast::dismiss(string $id)` — dismiss a toast.
- `Toast::dismissAll()` — dismiss everything.

Every builder requires a final `->show()` call. `show()` returns the toast ID (a UUID, or the custom ID from `->id()`).

### PendingToast / PendingToastUpdate methods

`message()`, `title(?string)`, `success()`, `error()`, `warning()`, `info()`, `neutral()`, `variant()`, `icon(name,
ios:, android:)`, `position()`, `duration(int)`, `persistent(bool = true)`, `animation()`, `swipeToDismiss(bool =
true)`, `dismissible(bool = true)`, `action(label, id)`, `background()`, `foreground()`, `iconColor()`, `actionColor()`,
`cornerRadius()`, `padding()`, `shadow(bool = true)`, `queue()`, `stack()`, `strategy()`, `maxVisible(int)`, `show()`.

`PendingToast` also has `id(string)`. `PendingToastUpdate` does not — its ID is fixed at `Toast::update($id)`.

### Variants

`success`, `error`, `warning`, `info`, `neutral`. Each applies native color/icon defaults; explicit style options
override them.

### IDs

`show()` returns a UUID by default. Supply `->id('my-id')` for a stable reference. Use that ID for `Toast::update($id)`
and `Toast::dismiss($id)`.

### Updates are sparse

Only properties explicitly set on `Toast::update($id)` change. Unspecified values are preserved. Timer semantics: no
timing change preserves the deadline; `duration()` starts a new timer; `duration()` → `persistent()` cancels the timer;
`persistent()` → `duration()` starts a new timer.

### Queue and stack

Default strategy is `queue` (FIFO, one at a time). `stack()` shows up to `maxVisible` (default 3) at once; overflow
waits FIFO. Queue waiting time does not consume the duration — timers start only when a toast becomes visible.

### Styling

Colors accept `#RGB`, `#RRGGBB`, `#AARRGGBB` (alpha first) and are uppercased. `->dismissible()` shows a close control;
`->swipeToDismiss()` controls the gesture (on by default).

### Actions

```php
Toast::error('Connection lost')->action(label: 'Retry', id: 'retry')->show();
```

Actions are native buttons — never pass PHP closures to `action()`. Pressing an action dismisses the toast.

### Events

Listen with NativePHP's `#[OnNative]` attribute. Payloads arrive as named method parameters.

- `Victorycodedev\ToastKit\Events\ToastShown` — `toastId`
- `Victorycodedev\ToastKit\Events\ToastDismissed` — `toastId`, `reason` (`timeout`, `swipe`, `programmatic`, `action`,
`replaced`)
- `Victorycodedev\ToastKit\Events\ToastActionPressed` — `toastId`, `actionId`

```php
use Native\Mobile\Attributes\OnNative;
use Victorycodedev\ToastKit\Events\ToastActionPressed;

#[OnNative(ToastActionPressed::class)]
public function handleToastAction(string $toastId, string $actionId): void
{
// ...
}
```

### FakeBridge test macros

In your own component tests (no device needed):

```php
Native::test(ProfileScreen::class)
->call('save')
->assertToastShownWithMessage('Profile updated');
```

Macros: `assertToastShown()`, `assertToastShownWithMessage($message)`, `assertToastShownWithId($id)`,
`assertToastUpdated($id, $changesFilter = null)`, `assertToastDismissed($id)`, `assertAllToastsDismissed()`.

### JavaScript API

`import { Toast, Show, Update, Dismiss, DismissAll } from 'victorycodedev/toastkit';`

`Toast.make/success/error/warning/info/neutral` build toasts; `.show()` sends them.
`Toast.update(id).message(...).show()`, `Toast.dismiss(id)`, `Toast.dismissAll()`. Raw bridge functions
`Show/Update/Dismiss/DismissAll` are also exported. npm publishing is future work; the source lives in `resources/js/`.

### Common mistakes

- Forgetting `->show()` — builders do nothing until shown.
- Expecting `Toast::update()` to mutate unspecified values — updates are sparse.
- Passing invalid colors (must be `#RGB`/`#RRGGBB`/`#AARRGGBB`).
- Expecting a queued toast's timer to run while it waits — timers start on visibility.
- Passing PHP closures to `action()` — actions are native buttons identified by `id`.
- Treating ToastKit as a Blade component — it is controlled from PHP and rendered by a native overlay.
