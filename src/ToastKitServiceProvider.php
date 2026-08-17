<?php

namespace Victorycodedev\Toastkit;

use Illuminate\Support\ServiceProvider;
use Victorycodedev\Toastkit\Commands\CopyAssetsCommand;

class ToastkitServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        $this->app->singleton(Toastkit::class, function () {
            return new Toastkit();
        });
    }

    public function boot(): void
    {
        // Register plugin hook commands
        if ($this->app->runningInConsole()) {
            $this->commands([
                CopyAssetsCommand::class,
            ]);
        }
    }
}