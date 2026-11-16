## ToastKit

ToastKit adds native in-app toast notifications to NativePHP Mobile apps. It is controlled from PHP or JavaScript;
the native iOS/Android overlay renders independently. There is no Blade/EDGE component to add.

### Installation assumption

ToastKit is already installed and registered in this app (`composer require victorycodedev/toastkit`, then `php artisan
native:plugin:register victorycodedev/toastkit`). Native source changes require `php artisan native:run` (or `php
artisan native:install --force`) before they appear in the app.

### Facade and terminal operations

```php
use Victorycodedev\ToastKit\Facades\Toast;
```

- `Toast::make(?string $message = null)` starts a fresh builder.
- `Toast::success()`, `error()`, `warning()`, `info()`, and `neutral()` are variant shortcuts.
- `Toast::update(string $id)` and `Toast::updateUnique(string $key)` start sparse updates.
- `Toast::dismiss(string $id)`, `dismissUnique(string $key)`, and `dismissAll()` dismiss toasts.
- `Toast::definePreset(string $name, Closure $preset)` registers a preset; `Toast::preset($name)` returns a fresh
  builder with that preset applied.

Every create or update builder requires a final `->show()`. Creating a toast returns its UUID/custom ID. A unique
duplicate returns the ID of the already-active toast rather than creating another one.

### Builder methods

Both `PendingToast` and `PendingToastUpdate` support:

- Content: `message()`, `title(?string)`, `text(...)`, `titleText(...)`.
- Variants: `success()`, `error()`, `warning()`, `info()`, `neutral()`, `variant()`.
- Icons: `icon(name:, ios:, android:)`, `iconSize('sm'|'md'|'lg')`. Icon slots accept platform icon names or
  NativePHP custom SVG references such as `custom:payment`; shared and per-platform values may be mixed.
- Placement/presentation: `position()`, `presentation('standard'|'dynamic')`, `dynamic()`, and
  `native(ios: true, android: true)`.
- Timing/motion: `duration(int $milliseconds)`, `persistent()`, `animation()`, `direction()`.
- State: `progress(0..100)`, `loading(bool)`, `opacity(0..1)`, `swipeToDismiss()`, `dismissible()`.
- Interaction: `action(string $label, string $id)`, `actions(array $actions)`, and `data(array $data)`.
- Styling: `background()`, `foreground()`, `iconColor()`, `actionColor()`, `cornerRadius()`, `padding()`, `shadow()`.
- Delivery: `queue()`, `stack()`, `strategy()`, `maxVisible()`.

`PendingToast` additionally supports `id(string)` and `unique(string $key)`. Update builders cannot change identity.

Animations are `fade`, `slide`, `scale`, `spring`, `snap`, `pop`, `reveal`, and `bounce`. Directions are `auto`,
`left`, `right`, `top`, and `bottom`. Dynamic presentation is independent of entrance/exit animation.

### Quick example

```php
$toastId = Toast::success('Payment completed')
    ->title('Success')
    ->icon(name: 'check', android: 'custom:payment')
    ->iconSize('lg')
    ->position('bottom')
    ->action(label: 'View', id: 'view-payment')
    ->data(['payment_id' => $payment->id])
    ->show();
```

### Actions and attached data

`action()` preserves the original single-action API. `actions()` accepts one or two actions; every action requires a
non-empty unique `id` and `label`, and may include its own hexadecimal `color`. The last call to `action()` or
`actions()` wins. Pressing an action emits `ToastActionPressed` and dismisses the toast.

Whole-toast press handling is enabled only when `data()` contains attached data. Ordinary toasts without data do not
dismiss merely because they were tapped. Attached data must be JSON-serializable and is included in toast/action press
events.

```php
Toast::info('Two updates are available')->actions([
    ['id' => 'review', 'label' => 'Review', 'color' => '#60A5FA'],
    ['id' => 'later', 'label' => 'Later'],
])->data(['screen' => 'updates'])->show();
```

### Unique toasts

`unique()` suppresses duplicate active work across visible, queued, stacked, and persistent toasts. The semantic key
is released after the toast terminates. Update or dismiss by that key when the generated UUID is not convenient.

```php
Toast::make('Syncing contacts…')->unique('contacts-sync')->loading()->persistent()->show();
Toast::updateUnique('contacts-sync')->message('Contacts synced')->loading(false)->duration(2000)->show();
Toast::dismissUnique('contacts-sync');
```

Ordinary toasts without `unique()` may repeat normally.

### Presets

Define presets during application boot, usually in `AppServiceProvider::boot()` or a dedicated
`ToastServiceProvider`. A preset callback receives and may return its `PendingToast`; each `Toast::preset()` call gets
a fresh builder.

```php
use Victorycodedev\ToastKit\PendingToast;
use Victorycodedev\ToastKit\Facades\Toast;

public function boot(): void
{
    Toast::definePreset('syncing', fn (PendingToast $toast) => $toast
        ->message('Syncing…')
        ->info()
        ->loading()
        ->persistent()
        ->unique('syncing'));
}
```

Use it with `Toast::preset('syncing')->show()`. Later calls may override preset values before `show()`.

### Updates are sparse

Only properties explicitly set on `Toast::update()` or `Toast::updateUnique()` change; unspecified values remain.
Title/message updates crossfade in place. Timer semantics: no timing change preserves the deadline; `duration()` starts
a new timer; switching to `persistent()` cancels it; switching back to `duration()` starts a new timer.

### Queue and stack

The default strategy is `queue` (FIFO, one visible toast). `stack()` shows up to `maxVisible` (default 3); overflow
waits FIFO. Waiting time does not consume duration because timers start only after visibility.

### Typography and fonts

`text()` and `titleText()` accept `font`, size (`xs`, `sm`, `base`, `lg`, `xl`), weight (`normal`, `medium`,
`semibold`, `bold`), alignment (`left`, `center`, `right`), and `italic`. Fonts use NativePHP's resolver and therefore
honor aliases or bundled font basenames in `config/native-ui.php`. Unresolved names fall back to the system font.
ToastKit does not bundle or register fonts.

### Styling and native rendering

Colors accept `#RGB`, `#RRGGBB`, or `#AARRGGBB` (alpha first) and are normalized to uppercase. `dismissible()` shows a
close control; `swipeToDismiss()` independently controls the gesture and is enabled by default.

`native()` selects the platform's native-looking standard renderer per platform while preserving content,
interaction, queue/stack, timing, identity, and event behavior. Platform-native rendering may ignore custom visual
styling that the native component does not support. Dynamic presentation is ToastKit's expanding foreground pill and
is separate from `native()`.

On Android, ToastKit automatically detects LayoutKit when available and hosts toasts above its Compose popup windows.
No consumer configuration or hard LayoutKit dependency is required. Apps without LayoutKit retain the standard
activity-decor host. Rebuild native code after changing either plugin.

### Events

Listen with NativePHP's `#[OnNative]` attribute. Payloads arrive as named method parameters.

- `Victorycodedev\ToastKit\Events\ToastShown` — `toastId`.
- `Victorycodedev\ToastKit\Events\ToastDismissed` — `toastId`, `reason` (`timeout`, `swipe`, `programmatic`, `action`,
  or `replaced`).
- `Victorycodedev\ToastKit\Events\ToastPressed` — `toastId`, `data`.
- `Victorycodedev\ToastKit\Events\ToastActionPressed` — `toastId`, `actionId`, `data`.

```php
use Native\Mobile\Attributes\OnNative;
use Victorycodedev\ToastKit\Events\ToastActionPressed;

#[OnNative(ToastActionPressed::class)]
public function handleToastAction(string $toastId, string $actionId, array $data = []): void
{
    // ...
}
```

### Exceptions

Catch `Victorycodedev\ToastKit\Exceptions\ToastKitException` for package errors. More specific exceptions include
`InvalidToastConfigurationException` and `PresetNotFoundException`.

### FakeBridge test macros

```php
Native::test(ProfileScreen::class)
    ->call('save')
    ->assertToastShownWithMessage('Profile updated');
```

Macros: `assertToastShown()`, `assertToastShownWithMessage($message)`, `assertToastShownWithId($id)`,
`assertToastUpdated($id, $changesFilter = null)`, `assertToastDismissed($id)`, and `assertAllToastsDismissed()`.

### JavaScript API

`import { Toast, Show, Update, Dismiss, DismissAll } from 'victorycodedev/toastkit';`

The fluent JavaScript API mirrors the PHP configuration surface. `Toast.update(id).message(...).show()`,
`Toast.dismiss(id)`, and `Toast.dismissAll()` perform lifecycle operations. Raw bridge functions are also exported.
npm publishing is future work; the source currently lives in `resources/js/`.

### Common mistakes

- Forgetting `->show()`—builders do nothing until shown.
- Expecting sparse updates to reset unspecified values.
- Passing invalid colors or non-serializable attached data.
- Passing PHP closures to `action()`; actions are native buttons identified by IDs.
- Defining a preset outside application boot and accidentally redefining it per request.
- Expecting an ordinary toast without `data()` to emit `ToastPressed`.
- Expecting a queued toast's timer to run while it waits.
- Treating ToastKit as a Blade component.
