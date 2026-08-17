# Contributing

Thanks for your interest in contributing to ToastKit.

## Development setup

```bash
composer install
vendor/bin/pest          # PHP test suite
(cd resources/js && node --test tests/*.test.js)   # JS test suite
```

## Guidelines

- Follow the existing code style and naming conventions.
- Keep the public PHP and JS APIs stable — avoid breaking changes without a clear reason.
- Add or update tests for any behavior change.
- Native code lives in `resources/android/src/` (Kotlin) and `resources/ios/Sources/` (Swift). Native changes require a rebuild in a consuming app (`php artisan native:run` or `php artisan native:install --force`).
- Validate the manifest before opening a pull request: `php artisan native:plugin:validate`.

## Pull requests

1. Fork the repository and create a feature branch.
2. Make your change with tests.
3. Run the test suites and `composer validate --strict`.
4. Open a pull request describing the change and why it is needed.
