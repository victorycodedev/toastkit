<?php

use Victorycodedev\ToastKit\Exceptions\InvalidToastConfigurationException;

it('rejects invalid colors', function () {
    expect(fn () => $this->kit->make('x')->background('red'))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn () => $this->kit->make('x')->foreground('#12'))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn () => $this->kit->make('x')->iconColor('123456'))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn () => $this->kit->make('x')->actionColor('#gggggg'))->toThrow(InvalidToastConfigurationException::class);
});

it('rejects an invalid duration', function () {
    expect(fn () => $this->kit->make('x')->duration(0))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn () => $this->kit->make('x')->duration(-5))->toThrow(InvalidToastConfigurationException::class);
});

it('rejects an invalid position', function () {
    expect(fn () => $this->kit->make('x')->position('left'))->toThrow(InvalidToastConfigurationException::class);
});

it('rejects an invalid animation', function () {
    expect(fn () => $this->kit->make('x')->animation('spin'))->toThrow(InvalidToastConfigurationException::class);
});

it('rejects an invalid strategy', function () {
    expect(fn () => $this->kit->make('x')->strategy('grid'))->toThrow(InvalidToastConfigurationException::class);
});

it('rejects an invalid action', function () {
    expect(fn () => $this->kit->make('x')->action('', 'retry'))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn () => $this->kit->make('x')->action('Retry', ''))->toThrow(InvalidToastConfigurationException::class);
});

it('rejects an invalid custom ID', function () {
    expect(fn () => $this->kit->make('x')->id(''))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn () => $this->kit->make('x')->id('   '))->toThrow(InvalidToastConfigurationException::class);
});

it('rejects an invalid maxVisible', function () {
    expect(fn () => $this->kit->make('x')->maxVisible(0))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn () => $this->kit->make('x')->maxVisible(-1))->toThrow(InvalidToastConfigurationException::class);
});

it('rejects negative padding and corner radius', function () {
    expect(fn () => $this->kit->make('x')->padding(-1))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn () => $this->kit->make('x')->cornerRadius(-1))->toThrow(InvalidToastConfigurationException::class);
});

it('rejects an empty dismiss ID', function () {
    expect(fn () => $this->kit->dismiss(' '))->toThrow(InvalidToastConfigurationException::class);
});

it('rejects an empty update ID', function () {
    expect(fn () => $this->kit->update(' '))->toThrow(InvalidToastConfigurationException::class);
});

it('rejects an update with no changes', function () {
    expect(fn () => $this->kit->update('one')->show())->toThrow(InvalidToastConfigurationException::class);
});
