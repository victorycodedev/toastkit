<?php

use Native\Mobile\Testing\FakeBridge;
use Victorycodedev\ToastKit\ToastKitServiceProvider;

beforeEach(function () {
    ToastKitServiceProvider::registerTestMacros();
    $this->bridge = new FakeBridge();
});

function record(FakeBridge $bridge, string $method, array $params): FakeBridge
{
    $bridge->call($method, json_encode($params, JSON_THROW_ON_ERROR));

    return $bridge;
}

it('asserts a toast was shown', function () {
    record($this->bridge, 'ToastKit.Show', ['id' => 'one', 'message' => 'Hello']);

    expect($this->bridge->assertToastShown())->toBe($this->bridge);
});

it('asserts a toast was shown with a specific message', function () {
    record($this->bridge, 'ToastKit.Show', ['id' => 'one', 'message' => 'Profile updated']);

    expect($this->bridge->assertToastShownWithMessage('Profile updated'))->toBe($this->bridge);
});

it('asserts a toast was shown with a specific ID', function () {
    record($this->bridge, 'ToastKit.Show', ['id' => 'one', 'message' => 'Hello']);

    expect($this->bridge->assertToastShownWithId('one'))->toBe($this->bridge);
});

it('asserts a toast was updated', function () {
    record($this->bridge, 'ToastKit.Update', ['id' => 'one', 'changes' => ['message' => 'Done']]);

    expect($this->bridge->assertToastUpdated('one'))->toBe($this->bridge);
});

it('asserts a toast was updated with specific changes', function () {
    record($this->bridge, 'ToastKit.Update', ['id' => 'one', 'changes' => ['variant' => 'success']]);

    expect($this->bridge->assertToastUpdated('one', fn (array $changes) => ($changes['variant'] ?? null) === 'success'))
        ->toBe($this->bridge);
});

it('asserts a toast was dismissed', function () {
    record($this->bridge, 'ToastKit.Dismiss', ['id' => 'one']);

    expect($this->bridge->assertToastDismissed('one'))->toBe($this->bridge);
});

it('asserts all toasts were dismissed', function () {
    record($this->bridge, 'ToastKit.DismissAll', []);

    expect($this->bridge->assertAllToastsDismissed())->toBe($this->bridge);
});

it('fails the message assertion when the message differs', function () {
    record($this->bridge, 'ToastKit.Show', ['id' => 'one', 'message' => 'Other']);

    expect(fn () => $this->bridge->assertToastShownWithMessage('Profile updated'))
        ->toThrow(PHPUnit\Framework\AssertionFailedError::class);
});

it('fails the ID assertion when the ID differs', function () {
    record($this->bridge, 'ToastKit.Show', ['id' => 'one', 'message' => 'Hello']);

    expect(fn () => $this->bridge->assertToastShownWithId('two'))
        ->toThrow(PHPUnit\Framework\AssertionFailedError::class);
});

it('fails the dismissed assertion when nothing was dismissed', function () {
    expect(fn () => $this->bridge->assertToastDismissed('one'))
        ->toThrow(PHPUnit\Framework\AssertionFailedError::class);
});
