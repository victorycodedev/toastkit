<?php

use Victorycodedev\ToastKit\PendingToast;

it('builds a pending toast from make()', function () {
    expect($this->kit->make())->toBeInstanceOf(PendingToast::class);
});

it('generates a UUID for every toast by default', function () {
    $first = $this->kit->make('Hello')->show();
    $second = $this->kit->make('Hello')->show();

    expect($first)->toMatch('/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i')
        ->and($second)->not->toBe($first);
});

it('accepts a custom ID', function () {
    expect($this->kit->make('Hello')->id('profile-updated')->show())->toBe('profile-updated');
});

it('requires a message before showing', function () {
    expect(fn () => $this->kit->make()->show())->toThrow(InvalidArgumentException::class, 'message');
});

it('requires a non-empty message', function () {
    expect(fn () => $this->kit->make('   '))->toThrow(InvalidArgumentException::class, 'message');
});

it('keeps builder state isolated between toasts', function () {
    $this->kit->success('Saved')->position('top')->duration(1200)->show();
    $this->kit->error('Failed')->show();

    [$method, $second] = $this->calls[1];

    expect($method)->toBe('ToastKit.Show')
        ->and($second['variant'])->toBe('error')
        ->and($second['position'])->toBe('bottom')
        ->and($second['duration'])->toBe(3000);
});

it('supports the neutral, success, error, warning and info variants', function (string $variant) {
    $this->kit->make('Message')->variant($variant)->show();

    expect($this->calls[0][1]['variant'])->toBe($variant);
})->with(['neutral', 'success', 'error', 'warning', 'info']);

it('exposes a shortcut method for each variant', function (string $method, string $variant) {
    $this->kit->{$method}('Message')->show();

    expect($this->calls[0][1]['variant'])->toBe($variant);
})->with([
    ['neutral', 'neutral'],
    ['success', 'success'],
    ['error', 'error'],
    ['warning', 'warning'],
    ['info', 'info'],
]);

it('rejects an unknown variant', function () {
    expect(fn () => $this->kit->make('x')->variant('danger'))->toThrow(InvalidArgumentException::class);
});
