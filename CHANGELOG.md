# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.6.0] - 2026-11-20

### Added

- Activity-scoped Android overlay windows that keep ToastKit notifications visible above Material 3 bottom sheets, NativePHP modals, alert dialogs, Compose popups, and detached LayoutKit tab bars.
- Keyboard-aware bottom positioning that moves visible toasts above the software keyboard without stealing focus or interrupting text input.

### Changed

- Android toast entrance and dismissal transitions now use a stable measured layout, keeping `fade`, `slide`, `scale`, `spring`, `snap`, `pop`, `reveal`, and `bounce` visually distinct across emulators and physical devices.
- Directional Android animations now dismiss through the same edge used for their entrance.
- Android toast windows no longer apply device- or ROM-provided window animations on top of ToastKit's Compose transitions.
- Android toast cards reserve transparent space for elevation blur so shadows fade naturally instead of being clipped into a rectangular band.

### Fixed

- Fixed Android toasts appearing behind overlays that opened after the toast was already visible.
- Fixed bottom toasts preventing continued typing while positioned above the keyboard.
- Fixed black navigation-bar flashes while opening NativePHP modals and bottom sheets with a toast visible.
- Fixed dynamic Android notifications flickering, shaking, restarting their animation, or losing the punch-hole connector.
- Fixed first-frame animation loss caused by transitions starting while the activity-scoped host window was still transparent.
- Fixed duplicate or orphaned overlay hosts across rotation, backgrounding, and activity recreation.

### Compatibility

- No PHP or JavaScript API changes are required. Existing toast configuration, positioning, timing, dismissal, actions, and event callbacks remain compatible.
- On Android 14 and newer, only the visible toast cards consume touches. On Android 10 through 13, transparent padding around a toast may also receive touches because the precise touchable-region API is unavailable.

### Contributors

- Special thanks to [@simonhamp](https://github.com/simonhamp) for the original Android overlay-window contribution in [PR #5](https://github.com/victorycodedev/toastkit/pull/5), which established the foundation for drawing ToastKit notifications above Android dialogs, sheets, and other activity overlays.

## Project history

### Added

- NativePHP custom SVG icon support through `custom:<name>`, including shared icons, mixed SF Symbol/Material/custom platform overrides, tinting, sizing, validation, and missing-asset fallback behavior.
- Optional whole-toast `opacity()` styling with clamping, sparse updates, preset support, and PHP/JavaScript parity.
- Platform-selective `native()` rendering with iOS Liquid Glass/native material fallback, Android Material 3 appearance, sparse update support, presets, and JavaScript parity.
- Unique toasts through `unique()`, `updateUnique()`, and `dismissUnique()`, with native lifecycle-aware deduplication across visible, queued, stacked, and persistent states on Android and iOS.
- JavaScript parity for unique toast creation, updates, dismissal, and bridge-resolved UUIDs.
- Native `snap`, `pop`, `reveal`, and `bounce` animations with independent `auto`, `left`, `right`, `top`, and `bottom` direction control.
- Determinate `progress()` and indeterminate `loading()` APIs with smooth sparse updates on Android and iOS.
- Laravel-container-backed reusable toast presets through `Toast::definePreset()` and `Toast::preset()`.
- A catchable ToastKit exception hierarchy with configuration and missing-preset exceptions.
- JavaScript parity for animations, direction, progress, and loading.

- Initial public API: `Toast` facade, `PendingToast` and `PendingToastUpdate` builders.
- Five variants (`neutral`, `success`, `error`, `warning`, `info`) with native defaults.
- Content options: title, message, icons with iOS/Android overrides, and action buttons.
- Presentation options: position, duration, persistent toasts, native animations, swipe-to-dismiss, close control, queue/stack strategies, and stack limits.
- Styling options: background, foreground, icon color, action color, corner radius, padding, and shadow.
- Sparse toast updates and idempotent dismissal by ID and all-toasts.
- Native events: `ToastShown`, `ToastDismissed`, and `ToastActionPressed`.
- Android Jetpack Compose overlay and iOS SwiftUI overlay with lifecycle-aware window installation.
- JavaScript bridge library and fluent `Toast` API.
- FakeBridge test macros for consuming-app assertions.
- Pest 4 PHP test suite and Node test suite.

### Changed

- NativePHP Mobile minimum requirement raised from `^4.1` to `^4.7` for the custom icon build and runtime asset pipeline.
- **Breaking:** ToastKit's public exceptions now extend the package-owned `ToastKitException` base, which extends `RuntimeException`. Replace ToastKit-specific `InvalidArgumentException` catches with `ToastKitException`.
- Composer requirement raised to PHP `^8.4` and NativePHP Mobile `^4.1`.
- Pest upgraded to `^4.0`.
