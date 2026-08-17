<?php

use Illuminate\Container\Container;
use Illuminate\Support\Facades\Facade;
use Victorycodedev\ToastKit\Facades\Toast;
use Victorycodedev\ToastKit\PendingToast;
use Victorycodedev\ToastKit\ToastKit;
use Victorycodedev\ToastKit\ToastKitServiceProvider;

it('resolves the Toast facade to a PendingToast', function () {
    $container = new Container();
    $container->instance('toastkit', $this->kit);
    Facade::setFacadeApplication($container);

    expect(Toast::make('Hello'))->toBeInstanceOf(PendingToast::class);

    Facade::clearResolvedInstances();
    Facade::setFacadeApplication(null);
});

it('resolves the ToastKit service from the container', function () {
    $container = new Container();
    (new ToastKitServiceProvider($container))->register();

    expect($container->make(ToastKit::class))->toBeInstanceOf(ToastKit::class)
        ->and($container->make('toastkit'))->toBeInstanceOf(ToastKit::class);
});

it('binds the service as a singleton', function () {
    $container = new Container();
    (new ToastKitServiceProvider($container))->register();

    expect($container->make(ToastKit::class))->toBe($container->make(ToastKit::class));
});

it('registers the FakeBridge test macros on boot', function () {
    $container = new Container();
    $provider = new ToastKitServiceProvider($container);
    $provider->register();
    $provider->boot();

    expect(\Native\Mobile\Testing\FakeBridge::hasMacro('assertToastShown'))->toBeTrue()
        ->and(\Native\Mobile\Testing\FakeBridge::hasMacro('assertToastShownWithMessage'))->toBeTrue()
        ->and(\Native\Mobile\Testing\FakeBridge::hasMacro('assertToastShownWithId'))->toBeTrue()
        ->and(\Native\Mobile\Testing\FakeBridge::hasMacro('assertToastUpdated'))->toBeTrue()
        ->and(\Native\Mobile\Testing\FakeBridge::hasMacro('assertToastDismissed'))->toBeTrue()
        ->and(\Native\Mobile\Testing\FakeBridge::hasMacro('assertAllToastsDismissed'))->toBeTrue();
});
