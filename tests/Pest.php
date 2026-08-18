<?php

if (! trait_exists(Illuminate\Foundation\Events\Dispatchable::class)) {
    eval('namespace Illuminate\\Foundation\\Events; trait Dispatchable {}');
}

if (! trait_exists(Illuminate\Queue\SerializesModels::class)) {
    eval('namespace Illuminate\\Queue; trait SerializesModels {}');
}

/*
|--------------------------------------------------------------------------
| Test Case
|--------------------------------------------------------------------------
*/

use Victorycodedev\ToastKit\ToastKit;

/*
| Each test receives a fresh ToastKit bound to a recording bridge so we can
| assert on the exact native bridge traffic without a device or container.
*/

uses()->beforeEach(function () {
    $this->calls = [];
    $this->kit = new ToastKit(function (string $method, array $payload) {
        $this->calls[] = [$method, $payload];
    });
})->in(__DIR__ . '/Unit');