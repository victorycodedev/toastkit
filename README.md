# ToastKit

Rich, customizable native toast notifications for [NativePHP Mobile](https://nativephp.com/docs/mobile) v4.

ToastKit gives your PHP, Livewire, and JavaScript-driven NativePHP apps first-class native toasts — success, error, warning, info, and neutral variants with titles, icons, custom styling, swipe-to-dismiss, action buttons, and full queue/stack management. Toasts are rendered by native Jetpack Compose (Android) and SwiftUI (iOS) overlays, so they look and feel like part of the operating system.

## Feature Highlights

- **Five variants** — `success`, `error`, `warning`, `info`, and `neutral` with sensible native defaults.
- **Rich content** — title, message, and NativePHP-compatible icons with per-platform overrides.
- **Full positioning** — `top`, `center`, or `bottom`, with safe-area handling.
- **Timing** — timed toasts with a custom duration, or persistent toasts that stay until dismissed.
- **Animations** — native `fade`, `slide`, `scale`, and `spring` animations.
- **Gestures** — swipe-to-dismiss that springs back on a failed swipe.
- **Actions** — a native action button with its own ID and pressed event.
- **Close control** — an optional visible dismiss button.
- **Custom styling** — background, foreground, icon, and action colors, corner radius, padding, and shadow.
- **Queue strategy** — FIFO, one toast at a time.
- **Stack strategy** — up to `maxVisible` toasts on screen with FIFO overflow.
- **Live updates** — change a visible or queued toast's message, variant, icon, style, or timer.
- **Dismissal** — dismiss by ID or dismiss everything, idempotently.
- **Events** — `ToastShown`, `ToastDismissed`, and `ToastActionPressed`.
- **Test vocabulary** — FakeBridge macros for asserting on toast traffic in your own test suite.

## Compatibility

| Requirement | Version |
| --- | --- |
| PHP | 8.3+ |
| NativePHP Mobile | 4.1+ |
| Android | API 29+ (Android 10) |
| iOS | 18.0+ |
| Platforms | Android and iOS |

## Installation

Install the package with Composer:

```bash
composer require victorycodedev/toastkit
```

ToastKit's PHP service provider is auto-discovered by Laravel. The native code must be registered explicitly before it is compiled into your app.

### Registering the plugin

First, publish the `NativeServiceProvider` (only needed once per app):

```bash
php artisan vendor:publish --tag=nativephp-plugins-provider
```

Then register ToastKit:

```bash
php artisan native:plugin:register victorycodedev/toastkit
```

### Verify installation

```bash
php artisan native:plugin:list
```

### Rebuild your app

Native code changes require a rebuild because Swift and Kotlin are compiled into your app:

```bash
php artisan native:run
```

If you change ToastKit's native code or manifest while developing, force a fresh install of the native projects:

```bash
php artisan native:install --force
```

## Quick Start

```php
use Victorycodedev\ToastKit\Facades\Toast;

Toast::success('Changes saved')->show();
```

A richer example:

```php
use Victorycodedev\ToastKit\Facades\Toast;

Toast::make('Profile updated')
    ->title('Success')
    ->success()
    ->icon('check')
    ->position('top')
    ->duration(3000)
    ->animation('spring')
    ->swipeToDismiss()
    ->show();
```

## PHP Usage

ToastKit is controlled entirely from PHP. The native overlay renders independently — there is no Blade/EDGE component to add.

### The `Toast` facade

| Method | Description |
| --- | --- |
| `Toast::make(?string $message = null)` | Start building a toast with an optional message. |
| `Toast::success(string $message)` | A success-variant toast. |
| `Toast::error(string $message)` | An error-variant toast. |
| `Toast::warning(string $message)` | A warning-variant toast. |
| `Toast::info(string $message)` | An info-variant toast. |
| `Toast::neutral(string $message)` | A neutral-variant toast. |
| `Toast::update(string $id)` | Start building an update for an existing toast. |
| `Toast::dismiss(string $id)` | Dismiss a toast by ID. |
| `Toast::dismissAll()` | Dismiss all active and queued toasts. |

Every shortcut returns a `PendingToast`. Call `->show()` to actually display it. `show()` returns the toast's ID (a UUID by default, or a custom ID you supply).

### The `PendingToast` builder

`make()` and the variant shortcuts return a `PendingToast`. All methods are chainable and `show()` sends the toast to the native bridge.

| Method | Description |
| --- | --- |
| `id(string $id)` | Set a custom ID instead of the generated UUID. |
| `message(string $message)` | Set the message text (required). |
| `title(?string $title)` | Set or clear an optional title. |
| `success()` / `error()` / `warning()` / `info()` / `neutral()` | Set the variant. |
| `variant(ToastVariant\|string $variant)` | Set the variant by enum or string. |
| `icon(?string $name = null, $ios = null, $android = null)` | Set an icon with optional platform overrides. |
| `position(ToastPosition\|string $position)` | `top`, `center`, or `bottom`. |
| `duration(int $milliseconds)` | Set the visible duration (makes the toast timed). |
| `persistent(bool $persistent = true)` | Make the toast persistent (no timeout). |
| `animation(ToastAnimation\|string $animation)` | `fade`, `slide`, `scale`, or `spring`. |
| `swipeToDismiss(bool $enabled = true)` | Enable or disable swipe-to-dismiss. |
| `dismissible(bool $enabled = true)` | Show a visible close control. |
| `action(string $label, string $id)` | Add an action button with a label and ID. |
| `background(string $color)` | Set the background color. |
| `foreground(string $color)` | Set the text color. |
| `iconColor(string $color)` | Set the icon color. |
| `actionColor(string $color)` | Set the action button color. |
| `cornerRadius(float $radius)` | Set the corner radius. |
| `padding(float $padding)` | Set the inner padding. |
| `shadow(bool $enabled = true)` | Enable or disable the shadow. |
| `queue()` | Use the queue strategy (one toast at a time). |
| `stack()` | Use the stack strategy (multiple toasts on screen). |
| `strategy(ToastStrategy\|string $strategy)` | Set the strategy by enum or string. |
| `maxVisible(int $count)` | Set the maximum visible stack size. |
| `show()` | Send the toast to the native bridge and return its ID. |

### The `PendingToastUpdate` builder

`Toast::update($id)` returns a `PendingToastUpdate`. It exposes the same configuration methods as `PendingToast` (except `id()`, since the ID is fixed) and a `show()` method.

Updates are **sparse**: only the properties you explicitly set are sent to the native side. Everything else is preserved.

## Variants

Each variant is a shortcut that applies native defaults:

```php
Toast::success('Saved')->show();
Toast::error('Something went wrong')->show();
Toast::warning('Storage almost full')->show();
Toast::info('Downloading...')->show();
Toast::neutral('Copied')->show();
```

Variant styles are just defaults — any explicit style option you set overrides them:

```php
Toast::success('Saved')
    ->icon('star')
    ->background('#111827')
    ->show();
```

## Content

```php
Toast::make('Your files are ready')
    ->title('Export complete')
    ->show();
```

`title()` accepts `null` to clear a title. Messages must be non-empty.

## Icons

ToastKit passes NativePHP logical icon names through to the native renderers:

```php
Toast::make('Saved')->icon('check')->show();
```

- iOS resolves logical names to SF Symbols.
- Android resolves them through NativePHP's Material icon support.

You can override per platform when you need a precise native glyph:

```php
Toast::make('Saved')
    ->icon('check', ios: 'checkmark.circle.fill', android: 'done')
    ->show();
```

Unknown logical names follow the host's fallback behavior. Logical names are preferred because they work across both platforms.

## Positioning

```php
Toast::make('Hello')->position('top')->show();
Toast::make('Hello')->position('center')->show();
Toast::make('Hello')->position('bottom')->show();
```

The default is `bottom`. Toasts respect safe areas (status bar on Android, navigation bar, and the iOS home indicator) so they never sit under a cutout or system gesture area.

## Duration & Persistent Toasts

Timed:

```php
Toast::make('Hello')->duration(5000)->show();
```

Persistent:

```php
$id = Toast::info('Uploading...')->persistent()->show();
```

The default duration is `3000` ms. A persistent toast has no timeout and stays until dismissed or updated.

Queue waiting time does **not** consume the duration — the timer only begins when a toast becomes visible.

## Animations

```php
Toast::make('Hello')->animation('fade')->show();
Toast::make('Hello')->animation('slide')->show();
Toast::make('Hello')->animation('scale')->show();
Toast::make('Hello')->animation('spring')->show();
```

The default is `spring`. Animations are entirely native. When the user enables reduced motion (or disables system animations on Android), ToastKit falls back to a simple fade.

## Swipe to Dismiss

```php
Toast::make('Swipe me')->swipeToDismiss()->show();
```

Swipe is enabled by default. A top toast swipes upward, a bottom toast swipes downward, and a center toast swipes horizontally. A failed swipe springs back; a successful swipe emits `ToastDismissed` with reason `swipe`.

## Action Buttons

```php
Toast::error('Connection lost')
    ->action(
        label: 'Retry',
        id: 'retry',
    )
    ->show();
```

Handle the press with a native event listener:

```php
use Native\Mobile\Attributes\OnNative;
use Victorycodedev\ToastKit\Events\ToastActionPressed;

#[OnNative(ToastActionPressed::class)]
public function handleToastAction(
    string $toastId,
    string $actionId,
): void {
    if ($actionId === 'retry') {
        $this->retry();
    }
}
```

Actions are native buttons — never pass PHP closures to `action()`. Pressing an action also dismisses the toast and emits `ToastDismissed` with reason `action`.

## Dismissible / Close Control

```php
Toast::make('Dismiss me')->dismissible()->show();
```

`dismissible()` shows a visible close control (an "x") on the toast. It is independent of `swipeToDismiss()` — you can enable either, both, or neither.

## Custom Styling

```php
Toast::make('Custom toast')
    ->background('#111827')
    ->foreground('#FFFFFF')
    ->icon('star')
    ->iconColor('#FBBF24')
    ->actionColor('#60A5FA')
    ->cornerRadius(18)
    ->padding(16)
    ->shadow()
    ->show();
```

Colors accept the following hexadecimal formats and are normalized to uppercase:

- `#RGB`
- `#RRGGBB`
- `#AARRGGBB` (alpha is the leading byte, i.e. `#FFRRGGBB`)

## Queue Strategy

The default strategy is `queue` — one toast on screen at a time, FIFO:

```php
Toast::info('First')->queue()->show();
Toast::info('Second')->queue()->show();
Toast::info('Third')->queue()->show();
```

Each toast appears after the previous one finishes. Timers start only when a toast becomes visible.

## Stack Strategy

```php
Toast::success('Saved A')
    ->stack()
    ->maxVisible(3)
    ->show();
```

With `stack`, up to `maxVisible` toasts appear on screen at once. The default `maxVisible` is `3`. When the stack is full, additional toasts wait in FIFO order and are admitted as existing toasts dismiss.

A queue toast and a stack group never compete on screen simultaneously.

## Updating Toasts

Update a visible or queued toast by its ID:

```php
$id = Toast::info('Uploading...')
    ->persistent()
    ->show();

// ...later, when the upload finishes:

Toast::update($id)
    ->message('Upload complete')
    ->success()
    ->icon('check')
    ->duration(2000)
    ->show();
```

Updates are **sparse** — only properties you explicitly supply change. Everything else is preserved.

Timer behavior on update:

- No timing fields changed → the existing deadline is preserved.
- `duration()` changed → a new timer starts from the update.
- `persistent()` → `duration()` → the toast becomes timed and a timer starts from the update.
- `duration()` → `persistent()` → the timer is cancelled and the toast becomes persistent.

```php
Toast::update($id)->variant(ToastVariant::Error)->show();
Toast::update($id)->icon('check')->iconColor('#86EFAC')->show();
Toast::update($id)->background('#1F2937')->foreground('#FFFFFF')->show();
```

## Dismissing Toasts

```php
Toast::dismiss($id);

Toast::dismissAll();
```

Dismissals are idempotent — dismissing an ID that is already gone is a no-op. `dismissAll()` clears both active and queued toasts.

## Events

ToastKit dispatches three events. Listen with NativePHP's `#[OnNative]` attribute.

### `ToastShown`

Fired when a toast becomes visible.

```php
use Native\Mobile\Attributes\OnNative;
use Victorycodedev\ToastKit\Events\ToastShown;

#[OnNative(ToastShown::class)]
public function handleToastShown(string $toastId): void
{
    // $toastId is now visible
}
```

### `ToastDismissed`

Fired when a toast is dismissed. Carries the toast ID and a reason.

```php
use Native\Mobile\Attributes\OnNative;
use Victorycodedev\ToastKit\Events\ToastDismissed;

#[OnNative(ToastDismissed::class)]
public function handleToastDismissed(string $toastId, string $reason): void
{
    if ($reason === 'swipe') {
        // user swiped it away
    }
}
```

Valid reasons (see `ToastDismissReason`): `timeout`, `swipe`, `programmatic`, `action`, `replaced`. The current native renderers emit `timeout`, `swipe`, `programmatic`, and `action`; `replaced` is declared for future use.

### `ToastActionPressed`

Fired when a toast action button is pressed.

```php
use Native\Mobile\Attributes\OnNative;
use Victorycodedev\ToastKit\Events\ToastActionPressed;

#[OnNative(ToastActionPressed::class)]
public function handleToastAction(string $toastId, string $actionId): void
{
    // handle the action
}
```

## Complete Component Example

A profile screen that saves, shows progress, and handles a retry action.

**PHP NativeComponent:**

```php
<?php

namespace App\Livewire;

use Livewire\Component;
use Native\Mobile\Attributes\OnNative;
use Victorycodedev\ToastKit\Events\ToastActionPressed;
use Victorycodedev\ToastKit\Facades\Toast;

class ProfileScreen extends Component
{
    public string $name = '';

    public function save(): void
    {
        $this->validate(['name' => 'required|min:2']);

        $id = Toast::info('Saving profile...')->persistent()->show();

        // ...persist the profile, then:
        Toast::update($id)
            ->message('Profile updated')
            ->success()
            ->icon('check')
            ->duration(2500)
            ->show();
    }

    #[OnNative(ToastActionPressed::class)]
    public function handleToastAction(string $toastId, string $actionId): void
    {
        if ($actionId === 'retry') {
            $this->save();
        }
    }

    public function render()
    {
        return view('livewire.profile');
    }
}
```

**Blade screen:**

```blade
<native:scroll-view>
    <native:column>
        <native:text>Profile</native:text>

        <native:text-input wire:model="name" placeholder="Your name" />

        <native:button @tap="save">
            Save
        </native:button>
    </native:column>
</native:scroll-view>
```

ToastKit is controlled from PHP — the native overlay renders independently, so you never add a Blade toast component.

## Real-world Examples

### Saved successfully

```php
Toast::success('Changes saved')->show();
```

### Validation error

```php
Toast::error('Please fix the highlighted fields')->show();
```

### Copied to clipboard

```php
Toast::neutral('Copied')->show();
```

### Connection lost with Retry

```php
Toast::error('Connection lost')
    ->action(label: 'Retry', id: 'retry')
    ->show();
```

### Persistent sync

```php
$id = Toast::info('Syncing...')->persistent()->show();

// ...sync completes:
Toast::update($id)->message('Synced')->success()->duration(2000)->show();
```

### Delete confirmation result

```php
Toast::success('Item deleted')->dismissible()->show();
```

## Fetch Integration (Optional)

ToastKit pairs naturally with [victorycodedev/nativephp-fetch](https://github.com/victorycodedev/nativephp-fetch) for upload/download progress. Fetch is **not** a ToastKit dependency — this is an optional integration example.

```php
use Victorycodedev\ToastKit\Facades\Toast;

$id = Toast::info('Downloading file...')
    ->persistent()
    ->show();

$requestId = Fetch::download('https://example.com/report.pdf');

// ...on completion:
Toast::update($id)
    ->message('Download complete')
    ->success()
    ->icon('check')
    ->duration(2500)
    ->show();

// ...on failure:
Toast::update($id)
    ->message('Download failed')
    ->error()
    ->duration(3000)
    ->show();
```

Refer to the Fetch package's documentation for its exact API surface.

## JavaScript Usage

ToastKit ships a JavaScript library in `resources/js/` for Inertia/Vue/React/WebView-oriented apps. It exposes a fluent `Toast` API plus a named export for every bridge function.

```js
import { Toast } from 'victorycodedev/toastkit';

Toast.success('Saved').show();
```

Rich builders mirror the PHP API:

```js
const id = await Toast.make('Uploading...')
    .info()
    .persistent()
    .show();

await Toast.update(id)
    .message('Done')
    .success()
    .duration(2000)
    .show();

await Toast.dismiss(id);
await Toast.dismissAll();
```

Raw bridge functions are available as named exports:

```js
import { Show, Update, Dismiss, DismissAll } from 'victorycodedev/toastkit';

await Show({ id: 'one', message: 'Hello' });
await Update('one', { message: 'Done' });
await Dismiss('one');
await DismissAll();
```

> **Note:** npm publishing is future work. Today the library lives in `resources/js/` and can be imported directly, copied into your app, or bundled by your build tool. No npm package is published yet.

## Testing ToastKit in Your App

ToastKit registers FakeBridge test macros so your own tests can assert on toast traffic using domain vocabulary. These tests assert on bridge calls and do **not** require an emulator or device.

```php
use Native\Mobile\Testing\Native;

Native::test(ProfileScreen::class)
    ->call('save')
    ->assertToastShownWithMessage('Profile updated');
```

Available macros:

| Macro | Description |
| --- | --- |
| `assertToastShown(?callable $filter = null)` | A `ToastKit.Show` call was made. |
| `assertToastShownWithMessage(string $message)` | A toast with the given message was shown. |
| `assertToastShownWithId(string $id)` | A toast with the given ID was shown. |
| `assertToastUpdated(string $id, ?callable $changesFilter = null)` | A toast was updated with the given ID (and optionally matching changes). |
| `assertToastDismissed(string $id)` | A toast with the given ID was dismissed. |
| `assertAllToastsDismissed()` | `ToastKit.DismissAll` was called. |

These are assertions on the bridge — native visual behavior must still be verified on a device or simulator.

To test how a screen reacts to a ToastKit event, use NativePHP's own `emitNative()` mechanism to deliver the event yourself:

```php
use Native\Mobile\Testing\Native;
use Victorycodedev\ToastKit\Events\ToastActionPressed;

Native::test(ProfileScreen::class)
    ->emitNative(ToastActionPressed::class, [
        'toastId' => 'one',
        'actionId' => 'retry',
    ])
    ->assertSet('retried', true);
```

`emitNative()` fires your `#[OnNative]` listeners with the supplied payload, exactly as the device would, without an emulator or device.

## API Reference

### Enums

| Enum | Values |
| --- | --- |
| `ToastVariant` | `success`, `error`, `warning`, `info`, `neutral` |
| `ToastPosition` | `top`, `center`, `bottom` |
| `ToastAnimation` | `fade`, `slide`, `scale`, `spring` |
| `ToastStrategy` | `queue`, `stack` |
| `ToastDismissReason` | `timeout`, `swipe`, `programmatic`, `action`, `replaced` |

### Defaults

| Property | Default |
| --- | --- |
| `variant` | `neutral` |
| `position` | `bottom` |
| `duration` | `3000` ms |
| `persistent` | `false` |
| `animation` | `spring` |
| `swipe_to_dismiss` | `true` |
| `dismissible` | `false` |
| `strategy` | `queue` |
| `max_visible` | `3` |
| `corner_radius` | `16` |
| `padding` | `16` |
| `shadow` | `true` |

## Event Reference

| Event | Payload |
| --- | --- |
| `Victorycodedev\ToastKit\Events\ToastShown` | `toastId` (string) |
| `Victorycodedev\ToastKit\Events\ToastDismissed` | `toastId` (string), `reason` (string) |
| `Victorycodedev\ToastKit\Events\ToastActionPressed` | `toastId` (string), `actionId` (string) |

## Permissions & Native Dependencies

ToastKit requires no Android permissions and no iOS permission strings. It adds no third-party native dependencies — it relies on the NativePHP host toolchain and native platform frameworks (Jetpack Compose and SwiftUI).

## Platform Support

| Platform | Status |
| --- | --- |
| Android (Compose) | Implemented |
| iOS (SwiftUI) | Implemented |

Physical-device support is claimed only after it has been verified on real hardware.

## Behavior & Semantics

- `show()` returns a UUID, or the custom ID supplied with `->id()`.
- Messages must be non-empty.
- A queued toast's timer does not run while it is waiting.
- Stack overflow is FIFO; overflow toasts are admitted as others dismiss.
- All dismissals are exactly-once — timeout, swipe, action, and bridge calls cannot emit duplicate dismissal events.
- Toasts install a lifecycle-aware window overlay; they survive screen navigation and activity/scene changes.

## Accessibility

- Titles and messages use scalable text (iOS Dynamic Type, Android `sp` units).
- Icons are decorative and hidden from assistive technologies; the close control is labeled "Dismiss".
- The iOS action and close controls are native `Button`s; the Android close control exposes a "Dismiss" description.
- Reduced-motion preferences are respected (iOS `accessibilityReduceMotion`, Android animator scale / touch exploration).
- Default variant colors use dark backgrounds with light foregrounds for strong contrast.

Known limitation: on Android, the action control is rendered as tappable text rather than a fully semantic button, so its accessibility label may be less explicit than on iOS. ToastKit does not claim full WCAG compliance.

## Limitations

- Native rendering requires a rebuild of the consuming app (`php artisan native:run`) after installation.
- ToastKit is not a Blade/EDGE component — it is controlled from PHP/JS and renders through a native overlay.
- Physical-device behavior (especially gestures and lifecycle edge cases) must still be certified on real devices.

## Troubleshooting

**Plugin not discovered?**
- Verify `composer.json` has `"type": "nativephp-plugin"`.
- Run `composer dump-autoload`.
- Run `php artisan native:plugin:list`.

**Toasts not appearing at runtime?**
- Rebuild the app after installing or changing native code: `php artisan native:run` (or `php artisan native:install --force`).
- Confirm the plugin is registered: `php artisan native:plugin:list`.

**Events not firing?**
- Confirm you're dispatching on the main thread in native code.
- Check that the event class name matches the manifest.
- Verify the `#[OnNative]` attribute uses the correct event class.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

The MIT License (MIT). See [LICENSE](LICENSE).
