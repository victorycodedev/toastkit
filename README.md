# ToastKit for NativePHP Mobile

[![Fund my work on FLOSSAfrica](https://flossafrica.com/badge.svg?theme=dark)](https://flossafrica.com/m/victorycodedev?p=toastkit)

ToastKit provides rich, customizable native toast notifications for [NativePHP Mobile](https://nativephp.com/docs/mobile).

Toasts render as native overlays with Jetpack Compose on Android and SwiftUI on iOS, so they look and feel at home on each platform. No Blade component is required.

## Features

- Five variants: `success`, `error`, `warning`, `info`, and `neutral`.
- Titles, messages, system icons, custom SVG icons, and per-platform icon overrides.
- Standard, platform-native, and expanding dynamic presentations.
- Custom colors, typography, opacity, icon sizes, corner radius, padding, and shadow.
- Entrance and exit animations with configurable direction.
- Determinate progress and indeterminate loading states.
- Live sparse updates with content crossfades.
- Unique keys that prevent duplicate active work.
- One or two action buttons with attached event data.
- Whole-toast press, shown, dismissed, and action events.
- Queue and stack strategies.
- Swipe, close-control, programmatic, and bulk dismissal.
- Fluent PHP and JavaScript APIs.
- NativePHP testing assertions for application tests.

## Compatibility

ToastKit 1.x supports PHP 8.4+, NativePHP Mobile 4.7+, Android API 29+, and iOS 18.0+.

ToastKit requires no Android permissions or iOS permission strings and adds no third-party native dependencies.

## Documentation

Read the complete documentation at [viqecodes.com/opensource/toastkit](https://viqecodes.com/opensource/toastkit).

## Installation

See the [getting started and installation guide](https://viqecodes.com/opensource/toastkit/docs/getting-started).

## Quick example

```php
use Victorycodedev\ToastKit\Facades\Toast;

Toast::success('Changes saved')
    ->icon('check')
    ->show();
```

## Feature guides

- [Creating toasts](https://viqecodes.com/opensource/toastkit/docs/creating-toasts)
- [Styling](https://viqecodes.com/opensource/toastkit/docs/styling)
- [Icons and custom SVGs](https://viqecodes.com/opensource/toastkit/docs/icons)
- [Presentations and animation](https://viqecodes.com/opensource/toastkit/docs/presentations-and-animation)
- [Updates and progress](https://viqecodes.com/opensource/toastkit/docs/updates-and-progress)
- [Unique toasts](https://viqecodes.com/opensource/toastkit/docs/unique-toasts)
- [Presets](https://viqecodes.com/opensource/toastkit/docs/presets)
- [Exceptions](https://viqecodes.com/opensource/toastkit/docs/exceptions)
- [Actions](https://viqecodes.com/opensource/toastkit/docs/actions)
- [Events](https://viqecodes.com/opensource/toastkit/docs/events)
- [Queue, stack, and dismissal](https://viqecodes.com/opensource/toastkit/docs/queue-stack-and-dismissal)
- [JavaScript usage](https://viqecodes.com/opensource/toastkit/docs/javascript)
- [Examples](https://viqecodes.com/opensource/toastkit/docs/examples)
- [Testing](https://viqecodes.com/opensource/toastkit/docs/testing)
- [Compatibility and permissions](https://viqecodes.com/opensource/toastkit/docs/compatibility)
- [API reference](https://viqecodes.com/opensource/toastkit/docs/api-reference)

## Support

Report bugs, request features, or ask for help through the [GitHub issue tracker](https://github.com/victorycodedev/toastkit/issues).

## Contributing

Please see the [contributing guide](CONTRIBUTING.md).

## License

ToastKit is open-source software released under the [MIT License](LICENSE).
