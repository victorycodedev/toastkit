<?php

use Victorycodedev\ToastKit\Events\ToastActionPressed;
use Victorycodedev\ToastKit\Events\ToastDismissed;
use Victorycodedev\ToastKit\Events\ToastShown;

function manifest(): array
{
    return json_decode(
        file_get_contents(dirname(__DIR__, 2).'/nativephp.json'),
        true,
        flags: JSON_THROW_ON_ERROR
    );
}

it('registers every bridge function', function () {
    expect(array_column(manifest()['bridge_functions'], 'name'))->toBe([
        'ToastKit.Show',
        'ToastKit.Update',
        'ToastKit.Dismiss',
        'ToastKit.DismissAll',
    ]);
});

it('points every bridge function at the matching native classes', function () {
    foreach (manifest()['bridge_functions'] as $function) {
        expect($function['android'])->toStartWith('com.victorycodedev.plugins.toastkit.ToastKitFunctions.')
            ->and($function['ios'])->toStartWith('ToastKitFunctions.');
    }
});

it('registers every event', function () {
    expect(manifest()['events'])->toBe([
        ToastShown::class,
        ToastDismissed::class,
        ToastActionPressed::class,
    ]);
});

it('registers Android and iOS init functions', function () {
    expect(manifest()['android']['init_function'])->toBe('com.victorycodedev.plugins.toastkit.initializeToastKit')
        ->and(manifest()['ios']['init_function'])->toBe('initializeToastKit');
});

it('declares the correct platform minimums', function () {
    expect(manifest()['android']['min_version'])->toBe(29)
        ->and(manifest()['ios']['min_version'])->toBe('18.0');
});

it('declares no Android permissions', function () {
    expect(manifest()['android']['permissions'] ?? [])->toBe([]);
});

it('declares no iOS permission strings', function () {
    expect(manifest()['ios']['info_plist'] ?? [])->toBe([]);
});

it('declares no third-party native dependencies', function () {
    expect(manifest()['android']['dependencies'] ?? [])->toBe([])
        ->and(manifest()['ios']['dependencies'] ?? [])->toBe([]);
});

it('supports both platforms', function () {
    expect(manifest()['platforms'])->toBe(['android', 'ios']);
});

it('keeps marketplace metadata accurate', function () {
    expect(manifest()['namespace'])->toBe('ToastKit')
        ->and(manifest()['license'])->toBe('MIT')
        ->and(manifest()['category'])->toBe('utilities')
        ->and(manifest()['pricing']['type'])->toBe('free');
});
