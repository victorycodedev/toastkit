<?php

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
})->in('.');
