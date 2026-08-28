# ToastKit

Rich, customizable native toast notifications for [NativePHP Mobile](https://nativephp.com/docs/mobile).

ToastKit renders toasts as native overlays — Jetpack Compose on Android, SwiftUI on iOS — so they look and feel like part of the operating system. No Blade toast component is required.

## Feature Highlights

- **Five variants** — `success`, `error`, `warning`, `info`, and `neutral`.
- **Rich content** — title, message, and icons with per-platform overrides.
- **Full customization** — position, animation direction, progress, loading, swipe-to-dismiss, close control, colors, corner radius, padding, and shadow.
- **Action buttons** — a native action button with its own ID and pressed event.
- **Queue strategy** — FIFO, one toast at a time.
- **Stack strategy** — up to `maxVisible` toasts on screen with FIFO overflow.
- **Live updates** — change a visible or queued toast's message, variant, icon, style, or timer without creating a new toast.
- **Idempotent dismissal** — dismiss by ID or dismiss everything.
- **Events** — `ToastShown`, `ToastDismissed`, and `ToastActionPressed`.
- **JavaScript API** — a fluent `Toast` API for web-facing apps.

## Installation

Install the package with Composer:

```bash
composer require victorycodedev/toastkit
```

The service provider is auto-discovered by Laravel. Register the native code (only needed once per app):

```bash
php artisan vendor:publish --tag=nativephp-plugins-provider
php artisan native:plugin:register victorycodedev/toastkit
```

Verify it is registered:

```bash
php artisan native:plugin:list
```

Then rebuild your app, since Swift and Kotlin are compiled into it:

```bash
php artisan native:run
```

## Quick Start

```php
use Victorycodedev\ToastKit\Facades\Toast;

Toast::success('Changes saved')->show();
```

ToastKit is fully customizable:

```php
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

ToastKit is controlled from PHP and renders as a native overlay — you never add a Blade toast component.

## Basic Toasts

```php
Toast::success('Saved')->show();
Toast::error('Something went wrong')->show();
Toast::warning('Storage almost full')->show();
Toast::info('Downloading...')->show();
Toast::neutral('Copied')->show();
```

Each variant applies sensible native defaults, which any explicit style option overrides.

## Custom Toasts

```php
Toast::make('Download complete')
    ->title('invoice.pdf')
    ->success()
    ->icon('check')
    ->position('top')
    ->background('#111827')
    ->foreground('#FFFFFF')
    ->iconColor('#22C55E')
    ->cornerRadius(18)
    ->padding(16)
    ->shadow()
    ->animation('spring')
    ->swipeToDismiss()
    ->duration(3000)
    ->show();
```

Supported options include `title()`, `icon()`, `position()`, `duration()`, `persistent()`, `animation()`, `direction()`, `progress()`, `loading()`, `swipeToDismiss()`, `dismissible()`, `action()`, and the styling methods `background()`, `foreground()`, `iconColor()`, `actionColor()`, `cornerRadius()`, `padding()`, and `shadow()`. See the [API Reference](#api-reference) for the full list.

## Icons

`icon()` accepts the same icon names as NativePHP's [`<native:icon>`](https://nativephp.com/docs/mobile/4/edge-components/icon) component, so any icon you already use in your Blade views works unchanged:

```php
Toast::make('Download complete')->icon('check')->show();
Toast::make('New message')->icon('email')->show();
Toast::make('Storage full')->icon('warning')->show();
```

A shared name resolves per platform automatically — SF Symbols on iOS and Material Icons on Android.

### Platform overrides

When each platform needs a different symbol, pass the platform-native name directly:

```php
Toast::make('Saved')
    ->icon(
        'check',
        ios: 'checkmark.circle.fill', // SF Symbol
        android: 'done',              // Material Icon ligature
    )
    ->show();
```

- `ios:` — an [SF Symbol](https://developer.apple.com/sf-symbols/) name (dotted, e.g. `house.fill`, `checkmark.circle.fill`).
- `android:` — a [Material Icon](https://fonts.google.com/icons) ligature name (underscored, e.g. `shopping_cart`, `qr_code_2`).

You can pass either override on its own:

```php
Toast::make('Rated')->icon(ios: 'star.fill')->show();
```

See NativePHP's [Icon name reference](https://nativephp.com/docs/mobile/4/edge-components/icon#icon-name-reference) for the names guaranteed to work consistently on both platforms.

## Typography

Customize the message and title typography independently with `text()` and `titleText()`. Every argument is optional — anything left unset falls back to the native defaults (message: `base` size, `medium` weight, left-aligned, non-italic; title: left-aligned, non-italic, semibold weight):

```php
Toast::make('Your download is complete')
    ->title('Download complete')
    ->text(
        font: 'Inter',
        size: 'sm',
        weight: 'normal',
    )
    ->titleText(
        size: 'lg',
        weight: 'bold',
    )
    ->success()
    ->show();
```

| Option | Values |
| --- | --- |
| `font` | A font name resolvable by the platform. |
| `size` | `xs`, `sm`, `base`, `lg`, `xl` |
| `weight` | `normal`, `medium`, `semibold`, `bold` |
| `align` | `left`, `center`, `right` |
| `italic` | `true` or `false` |

Typed enums are available too — `ToastTextSize`, `ToastTextWeight`, `ToastTextAlign`:

```php
use Victorycodedev\ToastKit\Enums\ToastTextAlign;
use Victorycodedev\ToastKit\Enums\ToastTextSize;
use Victorycodedev\ToastKit\Enums\ToastTextWeight;

Toast::make('Saved')
    ->text(
        size: ToastTextSize::Small,
        weight: ToastTextWeight::Medium,
        align: ToastTextAlign::Center,
    )
    ->show();
```

Typography updates follow the same sparse rules as the rest of `update()` — only the supplied values change:

```php
Toast::update($id)
    ->message('Download complete')
    ->text(weight: 'semibold')
    ->success()
    ->show();
```

This changes only the message weight; `font`, `size`, `align`, and `italic` are left untouched.

### Font resolution

`font` delegates to NativePHP's font resolver, so it honors the `fonts` array in `config/native-ui.php`. Pass a bundled font file's basename (`Inter-Bold`) or a config alias (`accent`, `body`, `headline`, …) and ToastKit renders the typeface your app already configured:

```php
Toast::make('Saved')->text(font: 'accent')->show();
```

A name that can't be resolved falls back to the system font on both platforms. ToastKit does not bundle, download, or register fonts itself — it renders whatever NativePHP resolves.

## Updating Toasts

`update()` changes a visible or queued toast in place, keeping the same ID:

```php
$id = Toast::info('Uploading file...')
    ->persistent()
    ->show();

// Later...
Toast::update($id)
    ->message('Upload complete')
    ->success()
    ->icon('check')
    ->duration(2000)
    ->show();
```

Updates are sparse:

- The toast keeps its original ID.
- Only properties you explicitly set change; everything else is preserved.
- A persistent toast becomes timed once you supply a `duration()`.

A message-only update is ideal for live progress:

```php
Toast::update($id)
    ->message('Uploading 50%...')
    ->show();
```

## Progress & Loading

Use determinate progress for work with a known percentage. Values are clamped to `0`–`100`, and updates animate without replacing the toast:

```php
$id = Toast::info('Uploading...')->persistent()->progress(0)->show();

Toast::update($id)->progress(42.5)->show();
Toast::update($id)->progress(100)->message('Upload complete')->show();
```

Use `loading()` when progress is unknown, and `loading(false)` to stop it:

```php
$id = Toast::info('Connecting...')->persistent()->loading()->show();
Toast::update($id)->loading(false)->message('Connected')->duration(1500)->show();
```

If a payload contains both `progress` and `loading: true`, determinate progress wins. This makes partial updates deterministic while preserving both values in the transport contract.

## Animations & Direction

ToastKit supports `fade`, `slide`, `scale`, `spring`, `snap`, `pop`, `reveal`, and `bounce`. Direction is independent and accepts `auto`, `left`, `right`, `top`, or `bottom`:

```php
Toast::success('Published')
    ->animation('bounce')
    ->direction('right')
    ->show();
```

`auto` is the default and derives the motion from the toast position. Reduced-motion platform settings use a short fade instead of spatial or spring motion.

## Presets

Define reusable presets during application boot, such as in a service provider:

```php
namespace App\Providers;

use Illuminate\Support\ServiceProvider;
use Victorycodedev\ToastKit\PendingToast;
use Victorycodedev\ToastKit\Facades\Toast;

final class AppServiceProvider extends ServiceProvider
{
    public function boot(): void
    {
        Toast::definePreset('syncing', fn (PendingToast $toast) => $toast
            ->message('Syncing...')
            ->info()
            ->persistent()
            ->loading());
    }
}
```

Each call creates a fresh builder, and options chained afterward override the preset:

```php
Toast::preset('syncing')->message('Syncing contacts...')->show();
```

Defining the same name again replaces the previous definition. Missing presets throw `PresetNotFoundException`. All intentional public configuration failures extend `ToastKitException` (and remain compatible with `InvalidArgumentException`). Presets are PHP-only because they are registered in the Laravel application lifecycle.

Small applications can keep these definitions directly in `AppServiceProvider::boot()`. For larger applications, a convenient optional organization is an `App\Toasts\ToastPresets` class with a static `register()` method, called from `AppServiceProvider::boot()`. ToastKit does not generate or require this class; it simply keeps a longer preset catalog out of the provider. Precedence is: ToastKit defaults → preset → per-toast configuration → sparse update configuration.

```php
namespace App\Toasts;

use Victorycodedev\ToastKit\Facades\Toast;

final class ToastPresets
{
    public static function register(): void
    {
        Toast::definePreset('payment-success', fn ($toast) => $toast
            ->success()->icon('check_circle')->animation('reveal')
            ->position('top')->duration(3000));

        Toast::definePreset('payment-failed', fn ($toast) => $toast
            ->error()->icon('error')->animation('snap')->direction('left')
            ->position('top')->duration(5000));

        Toast::definePreset('uploading', fn ($toast) => $toast
            ->info()->loading()->persistent());
    }
}
```

Then call `ToastPresets::register()` from your application's `AppServiceProvider::boot()`:

```php
namespace App\Providers;

use App\Toasts\ToastPresets;
use Illuminate\Support\ServiceProvider;

final class AppServiceProvider extends ServiceProvider
{
    public function boot(): void
    {
        ToastPresets::register();
    }
}
```

You may instead keep all toast presets in a dedicated service provider, such as `ToastServiceProvider`:

```php
namespace App\Providers;

use Illuminate\Support\ServiceProvider;
use Victorycodedev\ToastKit\PendingToast;
use Victorycodedev\ToastKit\Facades\Toast;

final class ToastServiceProvider extends ServiceProvider
{
    public function boot(): void
    {
        Toast::definePreset('payment-success', fn (PendingToast $toast) => $toast
            ->success()
            ->icon('check_circle')
            ->animation('reveal')
            ->position('top')
            ->duration(3000));

        Toast::definePreset('payment-failed', fn (PendingToast $toast) => $toast
            ->error()
            ->icon('error')
            ->animation('snap')
            ->direction('left')
            ->position('top')
            ->duration(5000));

        Toast::definePreset('uploading', fn (PendingToast $toast) => $toast
            ->info()
            ->loading()
            ->persistent());
    }
}
```

Register that provider using your Laravel application's normal provider registration, for example in `bootstrap/providers.php`:

```php
return [
    App\Providers\AppServiceProvider::class,
    App\Providers\ToastServiceProvider::class,
];
```

## Dismissing Toasts

```php
$id = Toast::info('Syncing...')
    ->persistent()
    ->show();

Toast::dismiss($id);
```

Dismiss everything at once:

```php
Toast::dismissAll();
```

Dismissals are idempotent — dismissing an ID that is already gone is a no-op.

## Actions & Events

Add a native action button and handle its press:

```php
use Native\Mobile\Attributes\On;
use Victorycodedev\ToastKit\Events\ToastActionPressed;

Toast::error('Connection lost')
    ->action(
        label: 'Retry',
        id: 'retry',
    )
    ->show();

#[On(ToastActionPressed::class)]
public function handleToastAction(
    string $toastId,
    string $actionId,
): void {
    if ($actionId === 'retry') {
        $this->retry();
    }
}
```

Pressing an action emits `ToastActionPressed` and dismisses the toast.

The other events follow the same pattern:

```php
use Native\Mobile\Attributes\On;
use Victorycodedev\ToastKit\Events\ToastShown;
use Victorycodedev\ToastKit\Events\ToastDismissed;

#[On(ToastShown::class)]
public function handleToastShown(string $toastId): void
{
    // The toast is now visible.
}

#[On(ToastDismissed::class)]
public function handleToastDismissed(string $toastId, string $reason): void
{
    // $reason is one of: timeout, swipe, programmatic, action.
}
```

See [Events](#events) for the full event reference.

## Queue & Stack

The default strategy is a FIFO **queue** — one toast at a time:

```php
Toast::info('First')->queue()->show();
Toast::info('Second')->queue()->show();
```

Each toast appears after the previous one finishes. Its duration only begins once it becomes visible.

The **stack** strategy shows up to `maxVisible` toasts at once:

```php
Toast::success('Saved')
    ->stack()
    ->maxVisible(3)
    ->show();
```

When the stack is full, additional toasts wait and are admitted in FIFO order as existing toasts dismiss.

## JavaScript Usage

ToastKit ships a JavaScript library in `resources/js/` (Composer-installed at `vendor/victorycodedev/toastkit/resources/js/`). There is no published npm package — copy the files into your app or bundle them with your build tool.

```js
import { Toast } from './resources/js';

Toast.success('Saved').show();
```

Updates mirror the PHP API:

```js
const id = await Toast.info('Uploading...')
    .persistent()
    .show();

await Toast.update(id)
    .message('Upload complete')
    .success()
    .duration(2000)
    .show();

await Toast.dismiss(id);
await Toast.dismissAll();
```

Raw bridge functions are also exported:

```js
import { Show, Update, Dismiss, DismissAll } from './resources/js';

await Show({ id: 'one', message: 'Hello' });
await Update('one', { message: 'Done' });
await Dismiss('one');
await DismissAll();
```

## Complete NativeComponent Example

```php
<?php

namespace App\Screens;

use Native\Mobile\Attributes\On;
use Native\Mobile\Edge\NativeComponent;
use Victorycodedev\ToastKit\Events\ToastActionPressed;
use Victorycodedev\ToastKit\Facades\Toast;

class ToastDemoScreen extends NativeComponent
{
    public ?string $toastId = null;

    public function startUpload(): void
    {
        $this->toastId = Toast::info('Uploading...')
            ->persistent()
            ->show();
    }

    public function updateUpload(): void
    {
        if (! $this->toastId) {
            return;
        }

        Toast::update($this->toastId)
            ->message('Uploading 50%...')
            ->show();
    }

    public function completeUpload(): void
    {
        if (! $this->toastId) {
            return;
        }

        Toast::update($this->toastId)
            ->message('Upload complete')
            ->success()
            ->icon('check')
            ->duration(2000)
            ->show();
    }

    public function dismissToast(): void
    {
        if ($this->toastId) {
            Toast::dismiss($this->toastId);
        }
    }

    #[On(ToastActionPressed::class)]
    public function handleToastAction(string $toastId, string $actionId): void
    {
        if ($actionId === 'retry') {
            $this->startUpload();
        }
    }
}
```

Blade screen using native components:

```blade
<native:scroll-view>
    <native:column>
        <native:text>ToastKit Demo</native:text>

        <native:button
            label="Start"
            @press="startUpload"
        />

        <native:button
            label="Update"
            @press="updateUpload"
        />

        <native:button
            label="Complete"
            @press="completeUpload"
        />

        <native:button
            label="Dismiss"
            @press="dismissToast"
        />
    </native:column>
</native:scroll-view>
```

## Real-world Example with NativePHP Fetch

ToastKit pairs naturally with a Fetch package for upload/download progress. Fetch is **not** a ToastKit dependency — this is an optional integration example.

```php
$toastId = Toast::info('Downloading...')
    ->persistent()
    ->show();

$request = Fetch::timeout(120);

$this->requestId = $request->id();

$request->download(/* ... */);
```

Then handle the Fetch events to drive live toast updates:

```php
use Native\Mobile\Attributes\On;
use Victorycodedev\ToastKit\Facades\Toast;

#[On(FetchDownloadProgress::class)]
public function onDownloadProgress(
    string $requestId,
    int $bytesReceived,
    ?int $bytesTotal,
    ?float $progress,
): void {
    if ($progress === null || ! $this->toastId) {
        return;
    }

    Toast::update($this->toastId)
        ->message(
            'Downloading '.(int) round($progress * 100).'%...'
        )
        ->show();
}

#[On(FetchDownloadCompleted::class)]
public function onDownloadCompleted(): void
{
    Toast::update($this->toastId)
        ->message('Download complete')
        ->success()
        ->icon('check')
        ->duration(2000)
        ->show();
}

#[On(FetchRequestFailed::class)]
public function onDownloadFailed(): void
{
    Toast::update($this->toastId)
        ->message('Download failed')
        ->error()
        ->duration(3000)
        ->show();
}
```

Refer to your Fetch package's documentation for its exact API surface.

## Events

ToastKit dispatches three events. Listen with NativePHP's `#[On]` attribute:

| Event | Payload |
| --- | --- |
| `Victorycodedev\ToastKit\Events\ToastShown` | `toastId` (string) |
| `Victorycodedev\ToastKit\Events\ToastDismissed` | `toastId` (string), `reason` (string) |
| `Victorycodedev\ToastKit\Events\ToastActionPressed` | `toastId` (string), `actionId` (string) |

```php
use Native\Mobile\Attributes\On;
use Victorycodedev\ToastKit\Events\ToastDismissed;

#[On(ToastDismissed::class)]
public function handleToastDismissed(string $toastId, string $reason): void
{
    // $reason is one of: timeout, swipe, programmatic, action.
}
```

`ToastDismissReason` declares `timeout`, `swipe`, `programmatic`, `action`, and `replaced`; the native renderers currently emit `timeout`, `swipe`, `programmatic`, and `action`.

## Testing

Run the PHP suite:

```bash
./vendor/bin/pest
```

Run the JavaScript suite:

```bash
node --test resources/js/tests/*.test.js
```

ToastKit registers `FakeBridge` macros so your own tests can assert on toast traffic using domain vocabulary — no emulator or device required:

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
| `assertToastUpdated(string $id, ?callable $changesFilter = null)` | A toast was updated with the given ID. |
| `assertToastDismissed(string $id)` | A toast with the given ID was dismissed. |
| `assertAllToastsDismissed()` | `ToastKit.DismissAll` was called. |

To test how a screen reacts to a ToastKit event, deliver the event yourself with NativePHP's `emitNative()`:

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

## API Reference

### `Toast` facade

| Method | Description |
| --- | --- |
| `Toast::make(?string $message = null)` | Start building a toast. |
| `Toast::success(string $message)` | A success-variant toast. |
| `Toast::error(string $message)` | An error-variant toast. |
| `Toast::warning(string $message)` | A warning-variant toast. |
| `Toast::info(string $message)` | An info-variant toast. |
| `Toast::neutral(string $message)` | A neutral-variant toast. |
| `Toast::definePreset(string $name, Closure $preset)` | Define or replace a reusable PHP preset. |
| `Toast::preset(string $name)` | Create a fresh builder from a preset. |
| `Toast::update(string $id)` | Start building an update for an existing toast. |
| `Toast::dismiss(string $id)` | Dismiss a toast by ID. |
| `Toast::dismissAll()` | Dismiss all active and queued toasts. |

### `PendingToast` builder

`make()` and the variant shortcuts return a `PendingToast`. All methods are chainable; `show()` sends the toast to the native bridge and returns its ID (a UUID by default, or a custom ID from `id()`).

| Method | Description |
| --- | --- |
| `id(string $id)` | Set a custom ID instead of the generated UUID. |
| `message(string $message)` | Set the message text (required). |
| `title(?string $title)` | Set or clear an optional title. |
| `text(?string $font = null, $size = null, $weight = null, $align = null, ?bool $italic = null)` | Configure message typography. See [Typography](#typography). |
| `titleText(?string $font = null, $size = null, $weight = null, $align = null, ?bool $italic = null)` | Configure title typography. See [Typography](#typography). |
| `success()` / `error()` / `warning()` / `info()` / `neutral()` | Set the variant. |
| `variant(ToastVariant\|string $variant)` | Set the variant by enum or string. |
| `icon(?string $name = null, ?string $ios = null, ?string $android = null)` | Set an icon using string names, with optional SF Symbol (`ios:`) / Material Icon (`android:`) overrides. See [Icons](#icons). |
| `position(ToastPosition\|string $position)` | `top`, `center`, or `bottom`. |
| `duration(int $milliseconds)` | Set the visible duration (makes the toast timed). |
| `persistent(bool $persistent = true)` | Make the toast persistent (no timeout). |
| `animation(ToastAnimation\|string $animation)` | `fade`, `slide`, `scale`, `spring`, `snap`, `pop`, `reveal`, or `bounce`. |
| `direction(ToastDirection\|string $direction)` | `auto`, `left`, `right`, `top`, or `bottom`. |
| `progress(int\|float $progress)` | Set determinate progress, clamped to `0`–`100`. |
| `loading(bool $loading = true)` | Enable or disable indeterminate progress. |
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

### `PendingToastUpdate` builder

`Toast::update($id)` returns a `PendingToastUpdate`. It exposes the same configuration methods as `PendingToast` — except `id()`, since the ID is fixed — plus a `show()` method that applies the update. Only properties you explicitly set are sent; everything else is preserved.

### Enums

| Enum | Values |
| --- | --- |
| `ToastVariant` | `success`, `error`, `warning`, `info`, `neutral` |
| `ToastPosition` | `top`, `center`, `bottom` |
| `ToastAnimation` | `fade`, `slide`, `scale`, `spring`, `snap`, `pop`, `reveal`, `bounce` |
| `ToastDirection` | `auto`, `left`, `right`, `top`, `bottom` |
| `ToastStrategy` | `queue`, `stack` |
| `ToastTextSize` | `xs`, `sm`, `base`, `lg`, `xl` |
| `ToastTextWeight` | `normal`, `medium`, `semibold`, `bold` |
| `ToastTextAlign` | `left`, `center`, `right` |
| `ToastDismissReason` | `timeout`, `swipe`, `programmatic`, `action`, `replaced` |

### Defaults

| Property | Default |
| --- | --- |
| `variant` | `neutral` |
| `position` | `bottom` |
| `duration` | `3000` ms |
| `persistent` | `false` |
| `animation` | `scale` |
| `direction` | `auto` |
| `loading` | `false` |
| `swipe_to_dismiss` | `true` |
| `dismissible` | `false` |
| `strategy` | `queue` |
| `max_visible` | `3` |
| `corner_radius` | `16` |
| `padding` | `16` |
| `shadow` | `true` |

## Compatibility

| Requirement | Version |
| --- | --- |
| PHP | 8.4+ |
| NativePHP Mobile | 4.1+ |
| Android | API 29+ (Android 10) |
| iOS | 18.0+ |

ToastKit supports Android and iOS.

## Permissions & Dependencies

ToastKit requires no Android permissions and no iOS permission strings. It adds no third-party native dependencies — it relies on the NativePHP host toolchain and the native platform frameworks (Jetpack Compose and SwiftUI).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

The MIT License (MIT). See [LICENSE](LICENSE).
