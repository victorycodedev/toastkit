<?php

use Victorycodedev\ToastKit\Exceptions\InvalidToastConfigurationException;
use Victorycodedev\ToastKit\Exceptions\PresetNotFoundException;
use Victorycodedev\ToastKit\Exceptions\ToastKitException;
use Victorycodedev\ToastKit\PendingToast;

it('builds a fresh configurable toast from a preset', function () {
    $this->kit->definePreset('upload', fn(PendingToast $toast) => $toast
        ->message('Uploading')
        ->info()
        ->persistent()
        ->loading());

    $first = $this->kit->preset('upload')->message('First')->show();
    $second = $this->kit->preset('upload')->message('Second')->progress(50)->show();

    expect($first)->not->toBe($second)
        ->and($this->calls[0][1]['message'])->toBe('First')
        ->and($this->calls[1][1]['message'])->toBe('Second')
        ->and($this->calls[1][1]['progress'])->toBe(50);
});

it('uses the last definition when a preset is redefined', function () {
    $this->kit->definePreset('status', fn(PendingToast $toast) => $toast->message('Old'));
    $this->kit->definePreset('status', fn(PendingToast $toast) => $toast->message('New'));
    $this->kit->preset('status')->show();
    expect($this->calls[0][1]['message'])->toBe('New');
});

it('throws package exceptions for invalid presets', function () {
    expect(fn() => $this->kit->preset('missing'))->toThrow(PresetNotFoundException::class)
        ->and(fn() => $this->kit->definePreset('', fn($toast) => $toast))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn() => $this->kit->definePreset('bad', fn() => 'wrong'))->not->toThrow(InvalidToastConfigurationException::class);

    expect(fn() => $this->kit->preset('bad'))->toThrow(InvalidToastConfigurationException::class);
});

it('provides one catchable package exception hierarchy', function () {
    expect(new PresetNotFoundException('missing'))->toBeInstanceOf(ToastKitException::class)
        ->and(new InvalidToastConfigurationException('invalid'))->toBeInstanceOf(ToastKitException::class)
        ->and(new ToastKitException('compatible'))->toBeInstanceOf(InvalidArgumentException::class);
});

it('keeps preset configuration while later updates stay sparse', function () {
    $this->kit->definePreset('uploading', fn(PendingToast $toast) => $toast
        ->message('Uploading')->position('top')->loading()->persistent());
    $id = $this->kit->preset('uploading')->show();
    $this->kit->update($id)->progress(0)->loading(false)->message('Starting')->show();

    expect($this->calls[0][1]['position'])->toBe('top')
        ->and($this->calls[1][1]['changes'])->toBe([
            'progress' => 0,
            'loading' => false,
            'message' => 'Starting',
        ]);
});
