<?php

use Victorycodedev\ToastKit\PendingToast;

it('omits opacity unless explicitly configured', function () {
    $payload = $this->kit->success('Saved')->payload();

    expect($payload)->not->toHaveKey('opacity');
});

it('uses 0.8 when opacity is called without an argument', function () {
    $this->kit->success('Saved')->opacity()->show();

    expect($this->calls[0][1]['opacity'])->toBe(0.8);
});

it('preserves explicit opacity values', function (float $opacity) {
    $this->kit->success('Saved')->opacity($opacity)->show();

    expect($this->calls[0][1]['opacity'])->toBe($opacity);
})->with([0.0, 0.5, 0.65, 1.0]);

it('clamps opacity to the supported range', function (float $input, float $expected) {
    $this->kit->success('Saved')->opacity($input)->show();

    expect($this->calls[0][1]['opacity'])->toBe($expected);
})->with([
    'below zero' => [-1.0, 0.0],
    'above one' => [2.0, 1.0],
]);

it('sends opacity as a sparse update and preserves zero', function () {
    $this->kit->update('toast')->opacity(0.0)->show();
    $this->kit->update('toast')->message('Almost finished')->show();

    expect($this->calls[0][1]['changes'])->toBe(['opacity' => 0.0])
        ->and($this->calls[1][1]['changes'])->toBe(['message' => 'Almost finished']);
});

it('supports preset opacity and explicit per-toast overrides', function () {
    $this->kit->definePreset('subtle', fn(PendingToast $toast) => $toast
        ->opacity()
        ->position('bottom')
        ->animation('fade'));

    $this->kit->preset('subtle')->message('Copied')->show();
    $this->kit->preset('subtle')->message('Saved')->opacity(0.45)->show();

    expect($this->calls[0][1]['opacity'])->toBe(0.8)
        ->and($this->calls[1][1]['opacity'])->toBe(0.45);
});
