<?php

namespace Victorycodedev\ToastKit;

use Illuminate\Support\ServiceProvider;
use Native\Mobile\Testing\FakeBridge;

class ToastKitServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        $this->app->singleton(ToastPresetRegistry::class);
        $this->app->singleton(ToastKit::class, fn($app) => new ToastKit(presets: $app->make(ToastPresetRegistry::class)));
        $this->app->alias(ToastKit::class, 'toastkit');
    }

    public function boot(): void
    {
        static::registerTestMacros();
    }

    /**
     * Register FakeBridge macros so consuming apps can assert on ToastKit
     * bridge traffic using domain vocabulary instead of raw method names.
     *
     * @internal
     */
    public static function registerTestMacros(): void
    {
        FakeBridge::macro('assertToastShown', function (?callable $paramsFilter = null) {
            return $this->assertCalled('ToastKit.Show', $paramsFilter);
        });

        FakeBridge::macro('assertToastShownWithMessage', function (string $message) {
            return $this->assertCalled('ToastKit.Show', fn(array $params) => ($params['message'] ?? null) === $message);
        });

        FakeBridge::macro('assertToastShownWithId', function (string $id) {
            return $this->assertCalled('ToastKit.Show', fn(array $params) => ($params['id'] ?? null) === $id);
        });

        FakeBridge::macro('assertToastUpdated', function (string $id, ?callable $changesFilter = null) {
            return $this->assertCalled('ToastKit.Update', function (array $params) use ($id, $changesFilter) {
                if (($params['id'] ?? null) !== $id) {
                    return false;
                }

                return $changesFilter === null || $changesFilter($params['changes'] ?? []) === true;
            });
        });

        FakeBridge::macro('assertToastDismissed', function (string $id) {
            return $this->assertCalled('ToastKit.Dismiss', fn(array $params) => ($params['id'] ?? null) === $id);
        });

        FakeBridge::macro('assertAllToastsDismissed', function () {
            return $this->assertCalled('ToastKit.DismissAll');
        });
    }
}