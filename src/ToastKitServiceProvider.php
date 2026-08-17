<?php

namespace Victorycodedev\ToastKit;

use Illuminate\Support\ServiceProvider;

class ToastKitServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        $this->app->singleton(ToastKit::class, fn() => new ToastKit());
        $this->app->alias(ToastKit::class, 'toastkit');
    }
}
