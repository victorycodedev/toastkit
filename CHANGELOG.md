# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

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

- **Breaking:** ToastKit's public exceptions now extend the package-owned `ToastKitException` base, which extends `RuntimeException`. Replace ToastKit-specific `InvalidArgumentException` catches with `ToastKitException`.
- Composer requirement raised to PHP `^8.4` and NativePHP Mobile `^4.1`.
- Pest upgraded to `^4.0`.
